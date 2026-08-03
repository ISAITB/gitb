/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package managers

import com.gitb.core.{AnyContent, ValueEmbeddingEnumeration}
import com.gitb.tr.TAR
import org.apache.commons.codec.binary.Base64
import utils.{MimeUtil, ReportNameResolver}

import java.nio.file.{Files, Path}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

object TestSessionDataFileNamer {

  /**
   * A single file reference found in a step's context, and the step it belongs to.
   *
   * @param uuid The current name of the underlying data file (for a "___[[uuid]]___" pointer), or a
   *             freshly generated identifier (for `inlineContent`, which has no file of its own yet).
   * @param inlineContent Set only for a small value that was embedded inline as a data URL rather than
   *                      decoupled to a file (see [[utils.RepositoryUtils.decoupleLargeData]]'s size
   *                      threshold) - the decoded bytes to write out as a new file in the export archive.
   */
  case class DataFileReference(stepId: String, uuid: String, item: AnyContent, inlineContent: Option[Array[Byte]] = None)

  private val FileReferencePrefix = "___[["
  private val FileReferenceSuffix = "]]___"

  /**
   * If the given value is a "___[[uuid]]___" file pointer (see [[utils.RepositoryUtils.decoupleLargeData]]),
   * returns its UUID. Uses plain string checks rather than a regex - cheaper, and mirrors how the Angular
   * front end recognises the same pointer (see report-support.ts#isFileReference/getFileReference).
   */
  private def fileReferenceUuid(value: String): Option[String] = {
    if (value.length > FileReferencePrefix.length + FileReferenceSuffix.length &&
        value.startsWith(FileReferencePrefix) && value.endsWith(FileReferenceSuffix)) {
      Some(value.substring(FileReferencePrefix.length, value.length - FileReferenceSuffix.length))
    } else {
      None
    }
  }

}

/**
 * Resolves the internal "___[[uuid]]___" file pointers used within a test session's context data
 * (see [[utils.RepositoryUtils.decoupleLargeData]]) to user-friendly, step-based file names, for use when
 * producing the test session data export archive (see [[ReportManager.generateTestSessionDataArchive]]).
 */
@Singleton
class TestSessionDataFileNamer @Inject() (implicit ec: ExecutionContext) {

  import TestSessionDataFileNamer._

  /**
   * Walks the given steps' report contexts (depth-first, matching the order in which
   * [[utils.RepositoryUtils.decoupleLargeData]] and JAXB marshalling visit them) and collects the file
   * references found - both:
   *  - a "___[[uuid]]___" pointer to a value already decoupled to its own file (a UUID referenced more
   *    than once, e.g. the same value appearing twice, is only reported once, keeping its first
   *    occurrence), and
   *  - a small BASE64 value embedded inline as a data URL, which never got decoupled (it never exceeded
   *    [[config.Configurations.TEST_SESSION_EMBEDDED_REPORT_DATA_THRESHOLD]]) but is still, in origin, a
   *    provided file - so it is treated the same way for the export: assigned a fresh identifier, and its
   *    content carried along to be written out as a new file (see [[inlineFileContents]]).
   */
  def collectReferences(steps: List[TitledTestStepReportType]): List[DataFileReference] = {
    val seenUuids = mutable.Set[String]()
    val result = ListBuffer[DataFileReference]()
    steps.zipWithIndex.foreach { case (step, index) =>
      val rawStepId = Option(step.getWrapped).flatMap(wrapped => Option(wrapped.getId)).getOrElse((index + 1).toString)
      val stepId = sanitizeStepId(rawStepId)
      step.getWrapped match {
        case tar: TAR if tar.getContext != null =>
          val itemsInStep = ListBuffer[AnyContent]()
          collectItems(tar.getContext, itemsInStep)
          itemsInStep.foreach { item =>
            Option(item.getValue).foreach { value =>
              fileReferenceUuid(value) match {
                case Some(uuid) =>
                  if (seenUuids.add(uuid)) {
                    result += DataFileReference(stepId, uuid, item)
                  }
                case None =>
                  if (item.getEmbeddingMethod == ValueEmbeddingEnumeration.BASE_64 && MimeUtil.isDataURL(value)) {
                    val content = Base64.decodeBase64(MimeUtil.getBase64FromDataURL(value))
                    result += DataFileReference(stepId, UUID.randomUUID().toString, item, Some(content))
                  }
              }
            }
          }
        case _ =>
      }
    }
    result.toList
  }

  /**
   * The decoded content to write out as a new file, for each reference collected from an inline data URL
   * value (see [[collectReferences]]) - keyed by the reference's (freshly generated) identifier.
   */
  def inlineFileContents(refs: List[DataFileReference]): Map[String, Array[Byte]] = {
    refs.view.flatMap(ref => ref.inlineContent.map(ref.uuid -> _)).toMap
  }

  private def collectItems(item: AnyContent, acc: ListBuffer[AnyContent]): Unit = {
    if (item != null) {
      acc += item
      item.getItem.forEach { child => collectItems(child, acc) }
    }
  }

  private def sanitizeStepId(stepId: String): String = {
    ReportNameResolver.sanitiseFileNameCharacters(stepId)
  }

  /**
   * Resolves the user-friendly file name to use for each reference's underlying data file, as
   * "[STEP_ID]_[FILE_COUNTER].[EXTENSION]" (or "[STEP_ID].[EXTENSION]" if the step only references a
   * single file). The extension is a best-effort content type detection (see `utils.MimeUtil`'s
   * `getFileExtension` overloads - from disk for an already-decoupled file, from memory for
   * `inlineContent`), run in parallel across all referenced files since the archive generation already
   * reads every byte of every file to copy it, so scanning each file's header first is a negligible
   * addition.
   *
   * @return A map from UUID (the underlying data file's current name, or the freshly generated identifier
   *         for `inlineContent`) to the resolved file name.
   */
  def resolveNames(refs: List[DataFileReference], dataFolder: Path): Future[Map[String, String]] = {
    val referenceCountByStep = refs.groupBy(_.stepId).view.mapValues(_.size).toMap
    val counters = mutable.Map[String, Int]().withDefaultValue(0)
    val namedRefs = refs.map { ref =>
      val baseName = if (referenceCountByStep(ref.stepId) > 1) {
        counters(ref.stepId) += 1
        s"${ref.stepId}_${counters(ref.stepId)}"
      } else {
        ref.stepId
      }
      (ref, baseName)
    }
    Future.sequence(namedRefs.map { case (ref, baseName) =>
      Future {
        val extension = ref.inlineContent match {
          case Some(content) => MimeUtil.getFileExtension(content)
          case None =>
            val filePath = dataFolder.resolve(ref.uuid)
            if (Files.exists(filePath)) MimeUtil.getFileExtension(filePath) else ""
        }
        ref.uuid -> (baseName + extension)
      }
    }).map(_.toMap)
  }

  /**
   * Rewrites each reference's underlying report item in place to show its resolved file name (falling
   * back to the UUID if for some reason it was not resolved) instead of the raw "___[[uuid]]___" pointer.
   * The embedding method is left untouched - it still accurately describes the original value (BASE64 for
   * an uploaded/binary file; gitb-reports recognises this bracketed form and renders it as-is instead of
   * the generic "[File content]", see ReportGenerator#isResolvedFileReference). This is applied once to
   * the in-memory report objects, before they are used to produce both the XML and PDF test session data
   * export reports, so the two cannot drift out of sync.
   */
  def applyNames(refs: List[DataFileReference], names: Map[String, String]): Unit = {
    refs.foreach { ref =>
      val name = names.getOrElse(ref.uuid, ref.uuid)
      ref.item.setValue(s"[$name]")
    }
  }

}
