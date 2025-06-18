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

import exceptions.{AutomationApiException, ErrorCodes}
import models.Domain
import models.automation.UpdateDomainRequest
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{CryptoUtil, RepositoryUtils}

import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DomainManager @Inject() (domainParameterManager: DomainParameterManager,
                               repositoryUtils: RepositoryUtils,
                               conformanceManager: ConformanceManager,
                               testSuiteManager: TestSuiteManager,
                               specificationManager: SpecificationManager,
                               testResultManager: TestResultManager,
                               automationApiHelper: AutomationApiHelper,
                               dbConfigProvider: DatabaseConfigProvider)
                              (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def getDomainById(id: Long): Future[Domain] = {
    DB.run(PersistenceSchema.domains.filter(_.id === id).result.head)
  }

  def getDomainByIdAsync(id: Long): Future[Domain] = {
    DB.run(PersistenceSchema.domains.filter(_.id === id).result.head)
  }

  def getByApiKey(apiKey: String): Future[Option[Domain]] = {
    DB.run(PersistenceSchema.domains.filter(_.apiKey === apiKey).result.headOption)
  }

  /**
   * Checks if domain exists
   */
  def checkDomainExists(domainId: Long): Future[Boolean] = {
    DB.run(
      PersistenceSchema.domains
        .filter(_.id === domainId)
        .exists
        .result
    )
  }

  def getDomains(ids: Option[List[Long]] = None): Future[List[Domain]] = {
    DB.run(
      PersistenceSchema.domains
        .filterOpt(ids)((q, ids) => q.id inSet ids)
        .sortBy(_.shortname.asc)
        .result
        .map(_.toList)
    )
  }

  def getCommunityDomain(communityId: Long): Future[Option[Domain]] = {
    DB.run(
      PersistenceSchema.communities
      .join(PersistenceSchema.domains).on(_.domain === _.id)
      .filter(_._1.id === communityId)
      .map(_._2)
      .result
      .headOption
    )
  }

  def getDomainOfActor(actorId: Long): Future[Domain] = {
    DB.run(
      PersistenceSchema.actors
        .join(PersistenceSchema.domains).on(_.domain === _.id)
        .filter(_._1.id === actorId)
        .map(x => x._2)
        .result
        .head
    )
  }

  def getDomainOfSpecification(specificationId: Long): Future[Domain] = {
    DB.run(
      PersistenceSchema.domains
        .join(PersistenceSchema.specifications).on(_.id === _.domain)
        .filter(_._2.id === specificationId)
        .map(x => x._1)
        .result
        .head
    )
  }

  def createDomainForImport(domain: Domain): DBIO[Long] = {
    for {
      result <- createDomainInternal(domain, checkApiKeyUniqueness = true)
    } yield result._1
  }

  private def createDomainInternal(domain: Domain, checkApiKeyUniqueness: Boolean): DBIO[(Long, String)] = {
    for {
      replaceApiKey <- {
        if (checkApiKeyUniqueness) {
          PersistenceSchema.domains.filter(_.apiKey === domain.apiKey).exists.result
        } else {
          DBIO.successful(false)
        }
      }
      apiKeyToUse <- {
        if (replaceApiKey) {
          DBIO.successful(CryptoUtil.generateApiKey())
        } else {
          DBIO.successful(domain.apiKey)
        }
      }
      newDomainId <- {
        if (replaceApiKey) {
          PersistenceSchema.domains.returning(PersistenceSchema.domains.map(_.id)) += domain.withApiKey(apiKeyToUse)
        } else {
          PersistenceSchema.domains.returning(PersistenceSchema.domains.map(_.id)) += domain
        }
      }
    } yield (newDomainId, apiKeyToUse)
  }

  def createDomain(domain: Domain): Future[Long] = {
    DB.run(createDomainInternal(domain, checkApiKeyUniqueness = false)).map(_._1)
  }

  def createDomain(domain: Domain, checkApiKeyUniqueness: Boolean): Future[String] = {
    DB.run(createDomainInternal(domain, checkApiKeyUniqueness)).map(_._2)
  }

  def updateDomain(domainId: Long, shortName: String, fullName: String, description: Option[String], reportMetadata: Option[String]): Future[Unit] = {
    DB.run(updateDomainInternal(domainId, shortName, fullName, description, reportMetadata, None).transactionally).map(_ => ())
  }

  def updateDomainThroughAutomationApi(updateRequest: UpdateDomainRequest): Future[Unit] = {
    val action = for {
      domain <- {
        if (updateRequest.domainApiKey.isDefined) {
          PersistenceSchema.domains
            .filter(_.apiKey === updateRequest.domainApiKey.get)
            .result
            .headOption
        } else if (updateRequest.communityApiKey.isDefined) {
          PersistenceSchema.communities
            .join(PersistenceSchema.domains).on(_.domain === _.id)
            .filter(_._1.apiKey === updateRequest.communityApiKey.get)
            .filter(_._1.domain.isDefined)
            .map(_._2)
            .result
            .headOption
        } else {
          DBIO.successful(None)
        }
      }
      _ <- {
        if (domain.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_DOMAIN_NOT_FOUND, "No domain found for the provided API key")
        } else {
          PersistenceSchema.domains.filter(_.id === domain.get.id).map(x => (x.shortname, x.fullname, x.description, x.reportMetadata)).update((
            updateRequest.shortName.getOrElse(domain.get.shortname),
            updateRequest.fullName.getOrElse(domain.get.fullname),
            updateRequest.description.getOrElse(domain.get.description),
            updateRequest.reportMetadata.getOrElse(domain.get.reportMetadata)
          ))
        }
      }
    } yield ()
    DB.run(action.transactionally)
  }

  def updateDomainInternal(domainId: Long, shortName: String, fullName: String, description: Option[String], reportMetadata: Option[String], apiKey: Option[String]): DBIO[_] = {
    for {
      replaceApiKey <- {
        if (apiKey.isDefined) {
          PersistenceSchema.domains.filter(_.apiKey === apiKey.get).filter(_.id =!= domainId).exists.result
        } else {
          DBIO.successful(false)
        }
      }
      _ <- {
        if (apiKey.isDefined) {
          val apiKeyToUse = if (replaceApiKey) CryptoUtil.generateApiKey() else apiKey.get
          PersistenceSchema.domains.filter(_.id === domainId)
            .map(x => (x.shortname, x.fullname, x.description, x.reportMetadata, x.apiKey))
            .update((shortName, fullName, description, reportMetadata, apiKeyToUse))
        } else {
          PersistenceSchema.domains.filter(_.id === domainId)
            .map(x => (x.shortname, x.fullname, x.description, x.reportMetadata))
            .update((shortName, fullName, description, reportMetadata))
        }
      }
      _ <- {
        testResultManager.updateForUpdatedDomain(domainId, shortName)
      }
    } yield ()
  }

  private def removeDomainFromCommunities(domainId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      communityIds <- PersistenceSchema.communities.filter(_.domain === domainId).map(x => x.id).result
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        communityIds.foreach { communityId =>
          // Delete conformance statements
          actions += conformanceManager.deleteConformanceStatementsForDomainAndCommunity(communityId, domainId, onSuccess)
          // Set domain of community to null
          actions += (for {x <- PersistenceSchema.communities if x.domain === domainId} yield x.domain).update(None)
        }
        toDBIO(actions)
      }
    } yield ()
  }

  def deleteDomainInternal(domain: Long, onSuccessCalls: ListBuffer[() => _]): DBIO[_] = {
    for {
      _ <- removeDomainFromCommunities(domain, onSuccessCalls)
      _ <- specificationManager.deleteSpecificationByDomain(domain, onSuccessCalls)
      _ <- PersistenceSchema.specificationGroups.filter(_.domain === domain).delete
      sharedTestSuiteIds <- PersistenceSchema.testSuites.filter(_.domain === domain).map(_.id).result
      _ <- DBIO.seq(sharedTestSuiteIds.map(testSuiteManager.undeployTestSuite(_, onSuccessCalls)): _*)
      _ <- deleteTransactionByDomain(domain)
      _ <- testResultManager.updateForDeletedDomain(domain)
      _ <- domainParameterManager.deleteDomainParameters(domain, onSuccessCalls)
      _ <- PersistenceSchema.conformanceOverviewCertificateMessages.filter(row => row.domain.isDefined && row.domain === domain).delete
      _ <- PersistenceSchema.conformanceSnapshotResults.filter(_.domainId === domain).map(_.domainId).update(domain * -1)
      _ <- PersistenceSchema.conformanceSnapshotDomains.filter(_.id === domain).map(_.id).update(domain * -1)
      _ <- PersistenceSchema.conformanceSnapshotDomainParameters.filter(_.domainId === domain).map(_.domainId).update(domain * -1)
      _ <- PersistenceSchema.conformanceSnapshotOverviewCertificateMessages.filter(row => row.domainId.isDefined && row.domainId === domain).map(_.domainId).update(Some(domain * -1))
      _ <- PersistenceSchema.domains.filter(_.id === domain).delete
      _ <- {
        onSuccessCalls += (() => {
          repositoryUtils.deleteDomainTestSuiteFolder(domain)
        })
        DBIO.successful(())
      }
    } yield ()
  }

  def deleteDomain(domain: Long): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = deleteDomainInternal(domain, onSuccessCalls)
    DB.run(
      dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally
    ).map(_ => ())
  }

  def deleteDomainThroughAutomationApi(apiKey: String): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      domainId <- automationApiHelper.getDomainIdByDomainApiKey(apiKey)
      _ <- deleteDomainInternal(domainId, onSuccessCalls)
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  private def deleteTransactionByDomain(domainId: Long) = {
    PersistenceSchema.transactions.filter(_.domain === domainId).delete
  }

}
