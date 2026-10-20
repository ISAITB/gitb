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

import models.Enums.CustomContentType
import models._
import org.apache.commons.lang3.StringUtils
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

/**
 * Manages the custom rich content (landing pages, legal notices and error templates) defined at Test Bed or community level.
 */
@Singleton
class CustomContentManager @Inject() (dbConfigProvider: DatabaseConfigProvider)
                                     (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  private var globalDefaultLegalNotice: Option[Option[CustomContents]] = None

  private def isTestBedLegalNotice(communityId: Long, contentType: Short): Boolean = {
    communityId == Constants.DefaultCommunityId && contentType == CustomContentType.LegalNotice
  }

  /**
   * Searches the custom content of the specified community (without rich content), restricted to the given types.
   */
  def searchCustomContentByCommunityWithoutContent(communityId: Long, contentTypes: Set[Short], page: Long, limit: Long): Future[SearchResult[CustomContent]] = {
    val queryBuilder = (forCount: Boolean) => {
      var baseQuery = PersistenceSchema.customContents
        .filter(_.community === communityId)
        .filter(_.contentType inSet contentTypes)
        .map(x => (x.id, x.name, x.description, x.default, x.contentType))
      if (!forCount) {
        baseQuery = baseQuery.sortBy(x => (x._4.desc, x._2.asc))
      }
      baseQuery
    }
    DB.run(
      for {
        results <- queryBuilder(false).drop((page - 1) * limit).take(limit).result.map(_.toList.map(x => new CustomContent(x._1, x._2, x._3, None, x._4, x._5)))
        resultCount <- queryBuilder(true).size.result
      } yield SearchResult(results, resultCount)
    )
  }

  /**
   * Gets all custom content of the given types for the specified community without rich content.
   */
  def getCustomContentByCommunityWithoutContent(communityId: Long, contentTypes: Set[Short]): Future[List[CustomContent]] = {
    DB.run(
      PersistenceSchema.customContents
        .filter(_.community === communityId)
        .filter(_.contentType inSet contentTypes)
        .map(x => (x.id, x.name, x.description, x.default, x.contentType))
        .sortBy(_._2.asc)
        .result
        .map(_.toList.map(x => new CustomContent(x._1, x._2, x._3, None, x._4, x._5)))
    )
  }

  /**
   * Gets all custom content of the given types for the specified community, in a single query, mapped by type.
   */
  def getCustomContentByCommunity(communityId: Long, contentTypes: Set[Short]): Future[Map[Short, List[CustomContents]]] = {
    DB.run(
      PersistenceSchema.customContents
        .filter(_.community === communityId)
        .filter(_.contentType inSet contentTypes)
        .sortBy(_.name.asc)
        .result
        .map(_.toList.groupBy(_.contentType))
    )
  }

  /**
   * Gets the IDs of the custom content of the given types for the specified community, in a single query, mapped by type.
   */
  def getIdsByType(communityId: Long, contentTypes: Set[Short]): Future[Map[Short, List[Long]]] = {
    DB.run(
      PersistenceSchema.customContents
        .filter(_.community === communityId)
        .filter(_.contentType inSet contentTypes)
        .map(x => (x.contentType, x.id))
        .result
        .map(_.toList.groupBy(_._1).view.mapValues(_.map(_._2)).toMap)
    )
  }

  /**
   * Gets the IDs of the custom content of the specified community (of all types), in a single query, mapped by type
   * and then by name (names are unique per type but may be matched against multiple IDs when comparing to imported data).
   */
  def getNameToIdsByType(communityId: Long): Future[Map[Short, Map[String, List[Long]]]] = {
    DB.run(
      PersistenceSchema.customContents
        .filter(_.community === communityId)
        .map(x => (x.contentType, x.name, x.id))
        .result
        .map { results =>
          results.toList.groupBy(_._1).view.mapValues { typeResults =>
            typeResults.groupBy(_._2).view.mapValues(_.map(_._3)).toMap
          }.toMap
        }
    )
  }

  /**
   * Checks that no content of the given type with the given name exists for the community.
   */
  def checkUniqueName(name: String, communityId: Long, contentType: Short): Future[Boolean] = {
    DB.run(
      PersistenceSchema.customContents
        .filter(_.community === communityId)
        .filter(_.contentType === contentType)
        .filter(_.name === name)
        .exists
        .result
    ).map(!_)
  }

  /**
   * As above, ignoring the content with the given ID (for updates).
   */
  def checkUniqueName(contentId: Long, name: String, communityId: Long, contentType: Short): Future[Boolean] = {
    DB.run(
      PersistenceSchema.customContents
        .filter(_.community === communityId)
        .filter(_.contentType === contentType)
        .filter(_.id =!= contentId)
        .filter(_.name === name)
        .exists
        .result
    ).map(!_)
  }

  def getCustomContentById(contentId: Long): Future[Option[CustomContents]] = {
    DB.run(getCustomContentByIdInternal(contentId))
  }

  def getCustomContentByIdInternal(contentId: Long): DBIO[Option[CustomContents]] = {
    PersistenceSchema.customContents.filter(_.id === contentId).result.headOption
  }

  def getCommunityId(contentId: Long): Future[Long] = {
    DB.run(PersistenceSchema.customContents.filter(_.id === contentId).map(_.community).result.head)
  }

  /**
   * Gets the types of the custom content with the given IDs, in a single query, mapped by ID (unknown IDs are not included).
   */
  def getTypesById(contentIds: Iterable[Long]): DBIO[Map[Long, Short]] = {
    val ids = contentIds.toSet
    if (ids.isEmpty) {
      DBIO.successful(Map.empty)
    } else {
      PersistenceSchema.customContents
        .filter(_.id inSet ids)
        .map(x => (x.id, x.contentType))
        .result
        .map(_.toMap)
    }
  }

  def createCustomContent(customContent: CustomContents): Future[Long] = {
    DB.run(createCustomContentInternal(customContent).transactionally)
  }

  def createCustomContentInternal(customContent: CustomContents): DBIO[Long] = {
    for {
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (customContent.default) {
          val q = for {c <- PersistenceSchema.customContents if c.default === true && c.community === customContent.community && c.contentType === customContent.contentType} yield c.default
          actions += q.update(false)
        }
        if (isTestBedLegalNotice(customContent.community, customContent.contentType) && customContent.default) {
          // Remove global legal notice from the cache
          globalDefaultLegalNotice = None
        }
        toDBIO(actions)
      }
      newId <- PersistenceSchema.insertCustomContent += customContent
    } yield newId
  }

  def updateCustomContent(contentId: Long, name: String, description: Option[String], content: String, default: Boolean, communityId: Long): Future[Unit] = {
    DB.run(updateCustomContentInternal(contentId, name, description, content, default, communityId).transactionally)
  }

  def updateCustomContentInternal(contentId: Long, name: String, description: Option[String], content: String, default: Boolean, communityId: Long): DBIO[Unit] = {
    for {
      contentOption <- PersistenceSchema.customContents.filter(_.id === contentId).result.headOption
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (contentOption.isDefined) {
          val existing = contentOption.get
          if (name.nonEmpty && existing.name != name) {
            val q = for {c <- PersistenceSchema.customContents if c.id === contentId} yield c.name
            actions += q.update(name)
          }
          if (content != existing.content) {
            val q = for {c <- PersistenceSchema.customContents if c.id === contentId} yield c.content
            actions += q.update(content)
          }
          if (!existing.default && default) {
            var q = for {c <- PersistenceSchema.customContents if c.default === true && c.community === communityId && c.contentType === existing.contentType} yield c.default
            actions += q.update(false)
            q = for {c <- PersistenceSchema.customContents if c.id === contentId} yield c.default
            actions += q.update(default)
          }
          val q = for {c <- PersistenceSchema.customContents if c.id === contentId} yield c.description
          actions += q.update(description)
          if (isTestBedLegalNotice(communityId, existing.contentType)) {
            // Remove global default notice from cache
            globalDefaultLegalNotice = None
          }
        }
        toDBIO(actions)
      }
    } yield ()
  }

  def deleteCustomContent(contentId: Long): Future[Unit] = {
    DB.run(deleteCustomContentInternal(contentId).transactionally).map(_ => ())
  }

  def deleteCustomContentInternal(contentId: Long): DBIO[_] = {
    for {
      info <- PersistenceSchema.customContents.filter(_.id === contentId).map(x => (x.community, x.default, x.contentType)).result.headOption
      _ <- {
        info.map(_._3) match {
          case Some(CustomContentType.LandingPage) => PersistenceSchema.organizations.filter(_.landingPage === contentId).map(_.landingPage).update(None)
          case Some(CustomContentType.LegalNotice) => PersistenceSchema.organizations.filter(_.legalNotice === contentId).map(_.legalNotice).update(None)
          case Some(CustomContentType.ErrorTemplate) => PersistenceSchema.organizations.filter(_.errorTemplate === contentId).map(_.errorTemplate).update(None)
          case _ => DBIO.successful(())
        }
      }
      _ <- PersistenceSchema.customContents.filter(_.id === contentId).delete
      _ <- {
        if (info.exists(x => isTestBedLegalNotice(x._1, x._3) && x._2)) {
          // Remove the cached global legal notice
          globalDefaultLegalNotice = None
        }
        DBIO.successful(())
      }
    } yield ()
  }

  /**
   * Gets the default content of the given type for the given community (without any fallback).
   */
  def getCommunityDefaultCustomContent(communityId: Long, contentType: Short): Future[Option[CustomContents]] = {
    if (isTestBedLegalNotice(communityId, contentType) && globalDefaultLegalNotice.isDefined) {
      Future.successful(globalDefaultLegalNotice.get)
    } else {
      DB.run(getCommunityDefaultCustomContentInternal(communityId, contentType)).map { defaultContent =>
        if (isTestBedLegalNotice(communityId, contentType)) {
          globalDefaultLegalNotice = Some(defaultContent)
        }
        defaultContent
      }
    }
  }

  private def getCommunityDefaultCustomContentInternal(communityId: Long, contentType: Short): DBIO[Option[CustomContents]] = {
    PersistenceSchema.customContents
      .filter(_.community === communityId)
      .filter(_.contentType === contentType)
      .filter(_.default === true)
      .result.headOption
  }

  def getCommunityDefaultCustomContentWithFallbackInternal(communityId: Long, contentType: Short): DBIO[Option[CustomContents]] = {
    for {
      communityContent <- getCommunityDefaultCustomContentInternal(communityId, contentType)
      contentToUse <- {
        if (communityContent.isDefined || communityId == Constants.DefaultCommunityId) {
          DBIO.successful(communityContent)
        } else {
          getCommunityDefaultCustomContentInternal(Constants.DefaultCommunityId, contentType)
        }
      }
    } yield contentToUse
  }

  /**
   * The content types for which the Test Bed has a default defined.
   */
  def getTestBedDefaultTypes(): Future[Set[Short]] = {
    DB.run(
      PersistenceSchema.customContents
        .filter(_.community === Constants.DefaultCommunityId)
        .filter(_.default === true)
        .map(_.contentType)
        .distinct
        .result
        .map(_.toSet)
    )
  }

  /**
   * Whether the Test Bed has a default legal notice with non-blank content, as reported to clients as part of the
   * application configuration (see JsonUtil.serializeConfigurationProperties). Served from the in-memory cache
   * whenever possible rather than a DB lookup on every call.
   */
  def hasTestBedDefaultLegalNotice(): Future[Boolean] = {
    globalDefaultLegalNotice.map(_.exists(notice => StringUtils.isNotBlank(notice.content))) match {
      case Some(status) => Future.successful(status)
      case None => getCommunityDefaultCustomContent(Constants.DefaultCommunityId, CustomContentType.LegalNotice).map(_.exists(notice => StringUtils.isNotBlank(notice.content)))
    }
  }

  /**
   * Runs the given mutation and reports, alongside its result, whether it changed the Test Bed's default legal notice
   * status as reported in the application configuration (see hasTestBedDefaultLegalNotice).
   *
   * Callers (see CustomContentService) use this to only push a configuration update over the SSE channel (see
   * ServerEventManager.publishConfiguration) when it is actually needed. Most mutations never invalidate the cache
   * checked here, so the before/after comparison costs no DB query in that case; only a mutation that does
   * invalidate it pays for the single DB query needed to repopulate the cache and compare.
   *
   * `action` is passed by name so that the "before" status is always read prior to the mutation running.
   */
  def detectingTestBedDefaultLegalNoticeChange[T](action: => Future[T]): Future[(T, Boolean)] = {
    hasTestBedDefaultLegalNotice().flatMap { before =>
      action.flatMap { result =>
        hasTestBedDefaultLegalNotice().map { after =>
          (result, before != after)
        }
      }
    }
  }

  def communityHasDefaultLegalNotice(communityId: Long): DBIO[Boolean] = {
    if (communityId == Constants.DefaultCommunityId && globalDefaultLegalNotice.flatten.isDefined) {
      DBIO.successful(true)
    } else {
      PersistenceSchema.customContents
        .filter(_.community === communityId)
        .filter(_.contentType === CustomContentType.LegalNotice)
        .filter(_.default === true)
        .exists.result
    }
  }

  def deleteCustomContentByCommunity(communityId: Long): DBIO[Unit] = {
    for {
      _ <- PersistenceSchema.customContents.filter(_.community === communityId).delete
      _ <- {
        if (communityId == Constants.DefaultCommunityId) {
          // Remove the cached global legal notice
          globalDefaultLegalNotice = None
        }
        DBIO.successful(())
      }
    } yield ()
  }

}
