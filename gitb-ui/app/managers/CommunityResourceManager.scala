/*
 * Copyright (C) 2025 European Union
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

import models.{CommunityResources, Constants, SearchResult}
import org.apache.commons.io.{FileUtils, FilenameUtils}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{RepositoryUtils, ZipArchiver}

import java.io.File
import java.nio.file.{Files, Path}
import java.util.regex.Pattern
import java.util.{Locale, UUID}
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.MapHasAsScala

@Singleton
class CommunityResourceManager @Inject()(repositoryUtils: RepositoryUtils,
                                         dbConfigProvider: DatabaseConfigProvider)
                                        (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  private val UNIQUE_NAME_PATTERN = Pattern.compile("^.+ +\\((\\d+)\\)$")

  def getCommunityIdInternal(resourceId: Long): DBIO[Long] = {
    PersistenceSchema.communityResources.filter(_.id === resourceId).map(_.community).result.head
  }

  def getCommunityId(resourceId: Long): Future[Long] = {
    DB.run(getCommunityIdInternal(resourceId))
  }

  def getCommunityResourceFileById(resourceId: Long): Future[(String, File)] = {
    DB.run(for {
      resourceInfo <- PersistenceSchema.communityResources.filter(_.id === resourceId).map(x => (x.name, x.community)).result.head
      file <- {
        DBIO.successful(repositoryUtils.getCommunityResource(resourceInfo._2, resourceId))
      }
    } yield (resourceInfo._1, file))
  }

  def getCommunityResourceFileByNameAndCommunity(communityId: Long, resourceName: String): Future[Option[File]] = {
    DB.run(PersistenceSchema.communityResources.filter(_.name === resourceName).filter(_.community === communityId).map(_.id).result.headOption).map { resourceId =>
      if (resourceId.nonEmpty) {
        Some(repositoryUtils.getCommunityResource(communityId, resourceId.get))
      } else {
        None
      }
    }
  }

  def getSystemResourceFileByName(name: String): Future[Option[File]] = {
    DB.run(PersistenceSchema.communityResources
      .filter(_.community === Constants.DefaultCommunityId)
      .filter(_.name === name)
      .map(x => (x.id, x.community))
      .result
      .headOption
    ).map { result =>
      result.map(x => repositoryUtils.getCommunityResource(x._2, x._1))
    }
  }

  def getCommunityResourceFileByName(communityId: Option[Long], userId: Long, name: String): Future[Option[File]] = {
    DB.run(for {
      resourceCommunityId <- {
        if (communityId.isDefined) {
          DBIO.successful(communityId.get)
        } else {
          PersistenceSchema.users
            .join(PersistenceSchema.organizations).on(_.organization === _.id)
            .filter(_._1.id === userId)
            .map(_._2.community)
            .result
            .head
        }
      }
      userCommunityResult <-
        // Check first in the user's own community.
        PersistenceSchema.communityResources
          .filter(_.community === resourceCommunityId)
          .filter(_.name === name)
          .map(_.id)
          .result
          .headOption
          .map(_.map((_, resourceCommunityId)))
      finalResult <- {
        if (userCommunityResult.isDefined) {
          DBIO.successful(userCommunityResult)
        } else {
          // Check also in the default community.
          PersistenceSchema.communityResources
            .filter(_.community === Constants.DefaultCommunityId)
            .filter(_.name === name)
            .map(x => (x.id, x.community))
            .result
            .headOption
        }
      }
    } yield finalResult).map { resourceIds =>
      if (resourceIds.isDefined) {
        Some(repositoryUtils.getCommunityResource(resourceIds.get._2, resourceIds.get._1))
      } else {
        None
      }
    }
  }

  def getCommunityResources(communityId: Long): Future[List[CommunityResources]] = {
    DB.run(PersistenceSchema.communityResources
      .filter(_.community === communityId)
      .sortBy(_.name.asc)
      .result).map(_.toList)
  }

  def searchCommunityResources(communityId: Long, page: Long, limit: Long, filter: Option[String]): Future[SearchResult[CommunityResources]] = {
    val query = PersistenceSchema.communityResources
      .filter(_.community === communityId)
      .filterOpt(filter)((table, filterValue) => {
        val filterValueToUse = toLowercaseLikeParameter(filterValue)
        table.name.toLowerCase.like(filterValueToUse) || table.description.toLowerCase.like(filterValueToUse)
      })
      .sortBy(_.name.asc)
    DB.run(
      for {
        results <- query.drop((page - 1) * limit).take(limit).result
        resultCount <- query.size.result
      } yield SearchResult(results, resultCount)
    )
  }

  def createCommunityResource(resource: CommunityResources, resourceContent: File): Future[Long] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = createCommunityResourceInternal(resource, resourceContent, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def saveCommunityResourcesInBulk(communityId: Long, archive: File, updateMatching: Boolean): Future[(Int, Int)] = {
    val tempFolder = Path.of(repositoryUtils.getTempFolder().getAbsolutePath, UUID.randomUUID().toString).toFile
    val extractedFiles: Map[String, Path] = new ZipArchiver(archive.toPath, tempFolder.toPath).unzip().asScala.toMap
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      existingResources <- {
        if (updateMatching) {
          PersistenceSchema.communityResources.filter(_.community === communityId).map(x => (x.id, x.name)).result
        } else {
          DBIO.successful(Seq.empty[(Long, String)])
        }
      }
      nameToIdMap <- {
        val nameMap = mutable.HashMap[String, Long]()
        existingResources.foreach { data =>
          nameMap += (data._2.toLowerCase(Locale.getDefault) -> data._1)
        }
        DBIO.successful(nameMap.toMap)
      }
      counts <- {
        val actions = ListBuffer[DBIO[_]]()
        var created = 0
        var updated = 0
        extractedFiles.foreach { fileInfo =>
          if (Files.isRegularFile(fileInfo._2) && !Files.isDirectory(fileInfo._2)) {
            val existingId = nameToIdMap.get(fileInfo._1.toLowerCase(Locale.getDefault))
            if (existingId.isDefined) {
              // Update (same name).
              updated += 1
              actions += updateCommunityResourceInternal(Some(communityId), existingId.get, None, None, Some(fileInfo._2.toFile), onSuccessCalls)
            } else {
              // Insert (different name).
              created += 1
              actions += createCommunityResourceInternal(CommunityResources(0L, toUniqueName(fileInfo._1, nameToIdMap.keySet), None, communityId), fileInfo._2.toFile, onSuccessCalls)
            }
          }
        }
        toDBIO(actions).map(_ => (created, updated))
      }
    } yield counts
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).andThen { _ =>
      if (tempFolder.exists()) FileUtils.deleteQuietly(tempFolder)
    }
  }

  private def toUniqueName(originalName: String, existingNames: Set[String]): String = {
    val nameToCheck = originalName.toLowerCase(Locale.getDefault)
    if (existingNames.contains(nameToCheck)) {
      var baseName = FilenameUtils.getBaseName(originalName)
      var extension = FilenameUtils.getExtension(nameToCheck)
      if (extension.nonEmpty) {
        extension = "." + extension
      }
      do {
        val matcher = UNIQUE_NAME_PATTERN.matcher(baseName)
        if (matcher.matches()) {
          // For e.g. "abc (10)" get "abc (11)"
          baseName = new mutable.StringBuilder(baseName).replace(matcher.start(1), matcher.end(1), String.valueOf(matcher.group(1).toInt + 1)).toString
        } else {
          baseName += " (1)"
        }
      } while (existingNames.contains(baseName+extension))
      baseName+extension
    } else {
      originalName
    }
  }

  def updateCommunityResource(resourceId: Long, name: String, description: Option[String], newFile: Option[File]): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = updateCommunityResourceInternal(None, resourceId, Some(name), Some(description), newFile, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
  }

  private def updateResourceInDb(communityId: Long, resourceId: Long, name: Option[String], description: Option[Option[String]]): DBIO[_] = {
    for {
      existingNames <- PersistenceSchema.communityResources.filter(_.community === communityId).filter(_.id =!= resourceId).map(_.name.toLowerCase).result
      _ <- {
        (name, description) match {
          case (Some(_), Some(_)) =>
            PersistenceSchema.communityResources.filter(_.id === resourceId).map(x => (x.name, x.description)).update((toUniqueName(name.get, existingNames.toSet), description.get))
          case (Some(_), None) =>
            PersistenceSchema.communityResources.filter(_.id === resourceId).map(x => x.name).update(toUniqueName(name.get, existingNames.toSet))
          case (None, Some(_)) =>
            PersistenceSchema.communityResources.filter(_.id === resourceId).map(x => x.description).update(description.get)
          case _ =>
            DBIO.successful(())
        }
      }
    } yield ()
  }

  def updateCommunityResourceInternal(communityId: Option[Long], resourceId: Long, name: Option[String], description: Option[Option[String]], newFile: Option[File], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      // Load if needed the community ID.
      communityIdToUse <- if (communityId.isDefined) DBIO.successful(communityId.get) else getCommunityIdInternal(resourceId)
      // If we are changing the name or description then do the update (otherwise do nothing).
      _ <- if (name.isDefined || description.isDefined) updateResourceInDb(communityIdToUse, resourceId, name, description) else DBIO.successful(())
      // Update the file.
      _ <- {
        if (newFile.isDefined) {
          onSuccessCalls += (() => repositoryUtils.setCommunityResourceFile(communityIdToUse, resourceId, newFile.get))
        }
        DBIO.successful(())
      }
    } yield ()
  }

  def createCommunityResourceInternal(resource: CommunityResources, resourceContent: File, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Long] = {
    for {
      // Ensure that the file is uniquely named.
      existingNames <- PersistenceSchema.communityResources.filter(_.community === resource.community).map(_.name.toLowerCase).result
      resourceId <- PersistenceSchema.insertCommunityResources += resource.withName(toUniqueName(resource.name, existingNames.toSet))
      _ <- {
        onSuccessCalls += (() => repositoryUtils.setCommunityResourceFile(resource.community, resourceId, resourceContent))
        DBIO.successful(())
      }
    } yield resourceId
  }

  def deleteCommunityResource(resourceId: Long): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteCommunityResourceInternal(None, resourceId, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
  }

  def deleteCommunityResources(communityId: Long, resourceIds: Set[Long]): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      _ <- PersistenceSchema.communityResources.filter(_.community === communityId).filter(_.id inSet resourceIds).delete
      _ <- {
        resourceIds.foreach { resourceId =>
          onSuccessCalls += (() => repositoryUtils.deleteCommunityResource(communityId, resourceId))
        }
        DBIO.successful(())
      }
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteCommunityResourceInternal(communityId: Option[Long], resourceId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      communityIdToUse <- {
        if (communityId.isDefined) {
          DBIO.successful(communityId.get)
        } else {
          getCommunityIdInternal(resourceId)
        }
      }
      _ <- PersistenceSchema.communityResources.filter(_.id === resourceId).delete
      _ <- {
        onSuccessCalls += (() => repositoryUtils.deleteCommunityResource(communityIdToUse, resourceId))
        DBIO.successful(())
      }
    } yield ()
  }

  def deleteResourcesOfCommunity(communityId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      resourceIds <- PersistenceSchema.communityResources.filter(_.community === communityId).map(_.id).result
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        resourceIds.foreach { resourceId =>
          actions += deleteCommunityResourceInternal(Some(communityId), resourceId, onSuccessCalls)
        }
        toDBIO(actions)
      }
    } yield ()
  }

  def createCommunityResourceArchive(communityId: Long, filter: Option[String]): Future[Path] = {
    DB.run(for {
      resources <- PersistenceSchema.communityResources
        .filter(_.community === communityId)
        .filterOpt(filter)((table, filterValue) => {
          val filterValueToUse = toLowercaseLikeParameter(filterValue)
          table.name.toLowerCase.like(filterValueToUse) || table.description.toLowerCase.like(filterValueToUse)
        })
        .map(x => (x.id, x.name))
        .result
    } yield resources).map { resources =>
      // Copy all applicable files to temp folder.
      val tempFolder = Path.of(repositoryUtils.getTempFolder().getAbsolutePath, UUID.randomUUID().toString)
      val archive = Path.of(repositoryUtils.getTempFolder().getAbsolutePath, tempFolder.toFile.getName+".zip")
      try {
        Files.createDirectories(tempFolder)
        resources.foreach { resource =>
          val targetFile = Path.of(tempFolder.toString, resource._2)
          Files.copy(repositoryUtils.getCommunityResource(communityId, resource._1).toPath, targetFile)
        }
        new ZipArchiver(tempFolder, archive).zip()
      } catch {
        case e: Exception =>
          if (tempFolder.toFile.exists()) FileUtils.deleteQuietly(tempFolder.toFile)
          if (archive.toFile.exists()) FileUtils.deleteQuietly(archive.toFile)
          throw e
      }
      archive
    }
  }

}
