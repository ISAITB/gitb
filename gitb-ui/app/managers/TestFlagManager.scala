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

import models.{SearchResult, TestFlags}
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestFlagManager @Inject()(dbConfigProvider: DatabaseConfigProvider)
                                (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  private val logger = LoggerFactory.getLogger(classOf[TestFlagManager])

  /** Safety cap for the Test Bed administrator's "all communities" login payload - if the total number
   * of flags across all communities exceeds this, an empty result is returned and the client falls back
   * to fetching a specific community's flags on demand once one is in scope. */
  private val MaxFlagsForAllCommunitiesLogin = 1000

  def getCommunityId(testFlagId: Long): Future[Long] = {
    DB.run(PersistenceSchema.testFlags.filter(_.id === testFlagId).map(_.community).result.head)
  }

  def checkUniqueName(name: String, communityId: Long): Future[Boolean] = {
    DB.run(
      PersistenceSchema.testFlags
        .filter(_.community === communityId)
        .filter(_.name === name)
        .exists
        .result
    ).map(!_)
  }

  def checkUniqueName(testFlagIdToIgnore: Long, name: String, communityId: Long): Future[Boolean] = {
    DB.run(
      PersistenceSchema.testFlags
        .filter(_.community === communityId)
        .filter(_.name === name)
        .filter(_.id =!= testFlagIdToIgnore)
        .exists
        .result
    ).map(!_)
  }

  def getTestFlagsByCommunity(communityId: Long, page: Long, limit: Long): Future[SearchResult[TestFlags]] = {
    val queryBuilder = (forCount: Boolean) => {
      var baseQuery = PersistenceSchema.testFlags.filter(_.community === communityId)
      if (!forCount) {
        baseQuery = baseQuery.sortBy(x => (x.displayOrder.asc, x.name.asc))
      }
      baseQuery
    }
    DB.run(
      for {
        results <- queryBuilder(false).drop((page - 1) * limit).take(limit).result
        resultCount <- queryBuilder(true).size.result
      } yield SearchResult(results, resultCount)
    )
  }

  def getAllTestFlagsByCommunity(communityId: Long): Future[List[TestFlags]] = {
    DB.run(
      PersistenceSchema.testFlags
        .filter(_.community === communityId)
        .sortBy(x => (x.displayOrder.asc, x.name.asc))
        .result
    ).map(_.toList)
  }

  /** All communities' test flags, keyed by community id, for the Test Bed administrator's login cache.
   * Returns None if the total flag count exceeds the safety cap, signalling the caller to fall back to
   * a per-community fetch instead. */
  def getAllTestFlagsForAdminLogin: Future[Option[Map[Long, List[TestFlags]]]] = {
    DB.run(PersistenceSchema.testFlags.sortBy(x => (x.displayOrder.asc, x.name.asc)).result).map { flags =>
      if (flags.size > MaxFlagsForAllCommunitiesLogin) {
        logger.warn(s"Skipping the all-communities test flag login cache - the total flag count [${flags.size}] exceeds the configured cap [$MaxFlagsForAllCommunitiesLogin].")
        None
      } else {
        Some(flags.groupBy(_.community).view.mapValues(_.toList).toMap)
      }
    }
  }

  def getTestFlagById(testFlagId: Long): Future[TestFlags] = {
    DB.run(PersistenceSchema.testFlags.filter(_.id === testFlagId).result.head)
  }

  def createTestFlag(testFlag: TestFlags): Future[Long] = {
    DB.run(createTestFlagInternal(testFlag).transactionally)
  }

  private[managers] def createTestFlagInternal(testFlag: TestFlags): DBIO[Long] = {
    for {
      maxOrder <- PersistenceSchema.testFlags.filter(_.community === testFlag.community).map(_.displayOrder).max.result
      testFlagId <- PersistenceSchema.insertTestFlags += testFlag.copy(displayOrder = (maxOrder.getOrElse(0.toShort) + 1).toShort)
    } yield testFlagId
  }

  def updateTestFlag(testFlag: TestFlags): Future[Unit] = {
    DB.run(updateTestFlagInternal(testFlag).transactionally)
  }

  private[managers] def updateTestFlagInternal(testFlag: TestFlags): DBIO[Unit] = {
    val q = for { t <- PersistenceSchema.testFlags if t.id === testFlag.id } yield (t.name, t.description, t.colour, t.publicName, t.publicColour, t.adminOnly)
    q.update((testFlag.name, testFlag.description, testFlag.colour, testFlag.publicName, testFlag.publicColour, testFlag.adminOnly)).map(_ => ())
  }

  def deleteTestFlag(testFlagId: Long): Future[Unit] = {
    DB.run(deleteTestFlagInternal(testFlagId).transactionally)
  }

  private[managers] def deleteTestFlagInternal(testFlagId: Long): DBIO[Unit] = {
    for {
      // The flag is only removed from sessions that reference it - the sessions themselves are untouched.
      _ <- {
        val q = for { t <- PersistenceSchema.testResults if t.flagId === testFlagId } yield t.flagId
        q.update(None)
      }
      _ <- PersistenceSchema.testFlags.filter(_.id === testFlagId).delete
    } yield ()
  }

  private[managers] def deleteTestFlagsByCommunity(communityId: Long): DBIO[_] = {
    for {
      testFlagIds <- PersistenceSchema.testFlags.filter(_.community === communityId).map(_.id).result
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        testFlagIds.foreach { id => dbActions += deleteTestFlagInternal(id) }
        toDBIO(dbActions)
      }
    } yield ()
  }

  def orderTestFlags(communityId: Long, orderedIds: List[Long]): Future[Unit] = {
    val dbActions = ListBuffer[DBIO[_]]()
    var counter = 0
    orderedIds.foreach { id =>
      counter += 1
      val q = for { t <- PersistenceSchema.testFlags.filter(_.community === communityId).filter(_.id === id) } yield t.displayOrder
      dbActions += q.update(counter.toShort)
    }
    DB.run(toDBIO(dbActions).transactionally).map(_ => ())
  }

  /** Reverts all of a community's flags to display order 0 - since flags are always sorted by
   * (displayOrder, name), this collapses them all onto the secondary sort key, i.e. alphabetical by
   * name (mirrors SpecificationManager.resetSpecificationOrder). */
  def resetTestFlagOrder(communityId: Long): Future[Unit] = {
    DB.run(PersistenceSchema.testFlags.filter(_.community === communityId).map(_.displayOrder).update(0)).map(_ => ())
  }

}
