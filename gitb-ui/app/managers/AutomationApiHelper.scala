package managers

import exceptions.{AutomationApiException, ErrorCodes}
import models.automation.{OrganisationIdsForApi, StatementIds}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AutomationApiHelper @Inject()(dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def loadOrganisationDataForAutomationProcessing(organisationKey: String): DBIO[Option[OrganisationIdsForApi]] = {
    PersistenceSchema.organizations
      .join(PersistenceSchema.communities).on(_.community === _.id)
      .filter(_._1.apiKey === organisationKey)
      .map(x => (x._1.id, x._1.community, x._2.domain, x._2.allowAutomationApi))
      .result
      .headOption
      .map(_.map(x => OrganisationIdsForApi(x._1, x._2, x._3, x._4)))
  }

  def checkOrganisationForAutomationApiUse(organisationData: Option[OrganisationIdsForApi]): DBIO[_] = {
    if (organisationData.isEmpty) {
      throw AutomationApiException(ErrorCodes.API_ORGANISATION_NOT_FOUND, "Unable to find organisation based on provided API key")
    } else if (!organisationData.get.apiEnabled) {
      throw AutomationApiException(ErrorCodes.API_COMMUNITY_DOES_NOT_ENABLE_API, "Community does not allow use of the test automation API")
    } else {
      DBIO.successful(())
    }
  }

  def getStatementIdsForApiKeys(organisationKey: String, systemKey: Option[String], actorKey: Option[String], snapshotKey: Option[String], testSuiteKeys: Option[List[String]], testCaseKeys: Option[List[String]]): DBIO[StatementIds] = {
    for {
      // We don't check the snapshot case for the organisation because we need a current one to load the community from.
      organisationData <- loadOrganisationDataForAutomationProcessing(organisationKey)
      _ <- checkOrganisationForAutomationApiUse(organisationData)
      // Load snapshot ID (if needed).
      snapshotId <- if (snapshotKey.isDefined) {
        PersistenceSchema.conformanceSnapshots
          .filter(_.apiKey === snapshotKey.get)
          .filter(_.community === organisationData.get.communityId)
          .map(_.id).result.headOption
      } else {
        DBIO.successful(None)
      }
      // System ID.
      systemId <- {
        if (systemKey.isDefined) {
          // Lookup using provided system key.
          if (snapshotId.isEmpty) {
            PersistenceSchema.systems
              .filter(_.owner === organisationData.get.organisationId)
              .filter(_.apiKey === systemKey.get)
              .map(_.id).result.headOption
          } else {
            PersistenceSchema.conformanceSnapshotSystems
              .filter(_.apiKey === systemKey.get)
              .filter(_.snapshotId === snapshotId.get)
              .map(_.id).result.headOption
          }
        } else {
          // Lookup from organisation (it should define a single system). We ignore the snapshot in this case.
          for {
            organisationSystemIds <- PersistenceSchema.systems
              .filter(_.owner === organisationData.get.organisationId)
              .map(_.id).result
            organisationSystemId <- {
              if (organisationSystemIds.size == 1) {
                DBIO.successful(organisationSystemIds.headOption)
              } else {
                DBIO.successful(None)
              }
            }
          } yield organisationSystemId
        }
      }
      _ <- {
        if (systemId.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_SYSTEM_NOT_FOUND, "Unable to determine system based on the provided information")
        } else {
          DBIO.successful(())
        }
      }
      actorId <- {
        if (actorKey.isDefined) {
          // Lookup using provided actor key.
          if (snapshotId.isEmpty) {
            PersistenceSchema.actors
              .filter(_.apiKey === actorKey.get)
              .filterOpt(organisationData.get.domainId)((q, domain) => q.domain === domain)
              .map(_.id).result.headOption
          } else {
            PersistenceSchema.conformanceSnapshotActors
              .filter(_.apiKey === actorKey.get)
              .map(_.id).result.headOption
          }
        } else {
          // No actor API key was provided - determine otherwise.  We ignore the snapshot in this case.
          if (testSuiteKeys.isDefined && testSuiteKeys.get.nonEmpty) {
            // Lookup using test suite ID(s).
            for {
              matchedActorIds <- PersistenceSchema.testSuites
                .join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testsuite)
                .join(PersistenceSchema.testCases).on(_._2.testcase === _.id)
                .join(PersistenceSchema.testCaseHasActors).on(_._2.id === _.testcase)
                .filter(_._1._1._1.identifier inSet testSuiteKeys.get.toSet)
                .filterOpt(organisationData.get.domainId)((q, domainId) => q._1._1._1.domain === domainId)
                .filterIf(testCaseKeys.exists(_.nonEmpty))(q => q._1._2.identifier inSet testCaseKeys.get.toSet)
                .filter(_._2.sut === true)
                .map(_._2.actor)
                .distinct
                .result
              matchedActorId <- {
                if (matchedActorIds.size == 1) {
                  DBIO.successful(matchedActorIds.headOption)
                } else {
                  DBIO.successful(None)
                }
              }
            } yield matchedActorId
          } else if (testCaseKeys.isDefined && testCaseKeys.get.nonEmpty) {
            // Lookup using test case ID(s).
            for {
              matchedActorIds <- PersistenceSchema.testCaseHasActors
                .join(PersistenceSchema.testCases).on(_.testcase === _.id)
                .join(PersistenceSchema.actors).on(_._1.actor === _.id)
                .filterOpt(organisationData.get.domainId)((q, domainId) => q._2.domain === domainId)
                .filter(_._1._2.identifier inSet testCaseKeys.get.toSet)
                .filter(_._1._1.sut === true)
                .map(_._1._1.actor)
                .distinct
                .result
              matchedActorId <- {
                if (matchedActorIds.size == 1) {
                  DBIO.successful(matchedActorIds.headOption)
                } else {
                  DBIO.successful(None)
                }
              }
            } yield matchedActorId
          } else {
            // There's no way to lookup the actor.
            DBIO.successful(None)
          }
        }
      }
      statementIds <- {
        if (actorId.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_ACTOR_NOT_FOUND, "Unable to determine actor based on the provided information")
        } else {
          DBIO.successful(StatementIds(organisationData.get.organisationId, systemId.get, actorId.get, organisationData.get.communityId, snapshotId))
        }
      }
    } yield statementIds
  }

  def getDomainIdByCommunityApiKey(communityApiKey: String, domainApiKey: Option[String]): DBIO[Long] = {
    for {
      // Look-up the community's domain.
      communityInfo <- PersistenceSchema.communities
        .filter(_.apiKey === communityApiKey)
        .map(_.domain)
        .result
        .headOption
      // Make sure a community was matched and return its (optional) domain ID.
      communityDomainId <- {
        if (communityInfo.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_DOMAIN_NOT_FOUND, "No domain found for the provided community API key")
        } else {
          DBIO.successful(communityInfo.get)
        }
      }
      // Look-up the domain related to the domain API key (if provided).
      domainInfo <- {
        if (domainApiKey.isDefined) {
          PersistenceSchema.domains
            .filter(_.apiKey === domainApiKey.get)
            .map(_.id)
            .result
            .headOption
        } else {
          DBIO.successful(None)
        }
      }
      // Make consistency checks and return the domain ID.
      domainId <- {
        if (domainApiKey.isDefined) {
          if (domainInfo.isEmpty) {
            throw AutomationApiException(ErrorCodes.API_DOMAIN_NOT_FOUND, "No domain found for the provided domain API key")
          } else if (communityDomainId.isDefined && communityDomainId.get != domainInfo.get) {
            throw AutomationApiException(ErrorCodes.API_DOMAIN_NOT_FOUND, "The provided domain API key did not match the community's domain")
          } else {
            DBIO.successful(domainInfo.get)
          }
        } else {
          if (communityDomainId.isEmpty) {
            throw AutomationApiException(ErrorCodes.API_DOMAIN_NOT_FOUND, "You need to specify the domain API key to identify a specific domain")
          } else {
            DBIO.successful(communityDomainId.get)
          }
        }
      }
    } yield domainId
  }

}
