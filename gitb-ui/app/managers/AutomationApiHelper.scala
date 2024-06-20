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

  def getStatementIdsForApiKeys(organisationKey: String, systemKey: String, actorKey: String, snapshotKey: Option[String]): DBIO[StatementIds] = {
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
      systemId <- if (snapshotId.isEmpty) {
        PersistenceSchema.systems
          .filter(_.apiKey === systemKey)
          .filter(_.owner === organisationData.get.organisationId)
          .map(_.id).result.headOption
      } else {
        PersistenceSchema.conformanceSnapshotSystems
          .filter(_.apiKey === systemKey)
          .filter(_.snapshotId === snapshotId.get)
          .map(_.id).result.headOption
      }
      _ <- {
        if (systemId.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_SYSTEM_NOT_FOUND, "Unable to find system based on provided API key")
        } else {
          DBIO.successful(())
        }
      }
      matchedActor <- {
        if (snapshotId.isEmpty) {
          PersistenceSchema.actors
            .filter(_.apiKey === actorKey)
            .filterOpt(organisationData.get.domainId)((q, domain) => q.domain === domain)
            .map(_.id).result.headOption
        } else {
          PersistenceSchema.conformanceSnapshotActors
            .filter(_.apiKey === actorKey)
            .map(_.id).result.headOption
        }
      }
      statementIds <- {
        if (matchedActor.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_ACTOR_NOT_FOUND, "Unable to find actor based on provided key")
        } else {
          DBIO.successful(StatementIds(organisationData.get.organisationId, systemId.get, matchedActor.get, organisationData.get.communityId, snapshotId))
        }
      }
    } yield statementIds
  }

}
