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

import actors.events.OrganisationUpdatedEvent
import exceptions.{AutomationApiException, ErrorCodes}
import managers.OrganizationManager.{SpecificationApiKeyInfo, TestSuiteApiKeyInfo}
import managers.triggers.TriggerHelper
import models.Enums.{OrganizationType, UserRole}
import models._
import models.automation._
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{CryptoUtil, MimeUtil, RepositoryUtils}

import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

object OrganizationManager {

  case class TestSuiteApiKeyInfo(testSuiteInfo: ApiKeyTestSuiteInfo, testCaseInfo: mutable.TreeMap[String, ApiKeyTestCaseInfo])
  case class SpecificationApiKeyInfo(specificationName: String, actorInfo: mutable.TreeMap[String, ApiKeyActorInfo], testSuiteInfo: mutable.TreeMap[String, TestSuiteApiKeyInfo], specificationGroupName: Option[String])

}

/**
 * Created by VWYNGAET on 26/10/2016.
 */
@Singleton
class OrganizationManager @Inject() (repositoryUtils: RepositoryUtils,
                                     systemManager: SystemManager,
                                     testResultManager: TestResultManager,
                                     triggerHelper: TriggerHelper,
                                     automationApiHelper: AutomationApiHelper,
                                     dbConfigProvider: DatabaseConfigProvider)
                                    (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def checkOrganizationExistsInternal(orgId: Long): DBIO[Boolean] = {
    PersistenceSchema.organizations
      .filter(_.id =!= Constants.DefaultOrganizationId)
      .filter(_.id === orgId)
      .exists
      .result
  }

  /**
    * Gets all organizations
    */
  def getOrganizations(): Future[List[Organizations]] = {
    DB.run(
      PersistenceSchema.organizations
        .sortBy(_.shortname.asc)
        .result.map(_.toList)
    )
  }

  def searchOrganizations(communityIds: Option[List[Long]]): Future[List[Organizations]] = {
    DB.run(
      PersistenceSchema.organizations
        .filterOpt(communityIds)((q, ids) => q.community inSet ids)
        .sortBy(_.shortname.asc)
        .result
        .map(_.toList)
    )
  }

  /**
    * Gets organizations with specified community
    */
  def getOrganizationsByCommunity(communityId: Long, includeAdmin: Boolean = false, snapshotId: Option[Long]): Future[List[Organizations]] = {
    val organizations = if (snapshotId.isDefined) {
      DB.run(
        PersistenceSchema.conformanceSnapshotOrganisations
          .join(PersistenceSchema.conformanceSnapshots).on(_.snapshotId === _.id)
          .filter(_._2.community === communityId)
          .filter(_._2.id === snapshotId.get)
          .sortBy(_._1.fullname.asc)
          .map(_._1)
          .result
      ).map { results =>
        results.map(x => Organizations(x.id, x.shortname, x.fullname, OrganizationType.Vendor.id.toShort, adminOrganization = false, None, None, None, template = false, None, x.apiKey, communityId)).toList
      }
    } else {
      DB.run(
        PersistenceSchema.organizations
          .filterIf(!includeAdmin)(_.adminOrganization === false)
          .filter(_.community === communityId)
          .sortBy(_.shortname.asc)
          .result.map(_.toList)
      )
    }
    organizations
  }

  def searchOrganizationsByCommunity(communityId: Long, page: Long, limit: Long, filter: Option[String], sortOrder: Option[String], sortColumn: Option[String], creationOrderSort: Option[String]): Future[(Iterable[Organizations], Int)] = {
    var query = PersistenceSchema.organizations
      .filter(_.adminOrganization === false)
      .filter(_.community === communityId)
      .filterOpt(filter)((table, filterValue) => {
        val filterValueToUse = s"%${filterValue.toLowerCase}%"
        table.shortname.toLowerCase.like(filterValueToUse) || table.fullname.toLowerCase.like(filterValueToUse)
      })
    if (creationOrderSort.nonEmpty) {
      if (creationOrderSort.getOrElse("asc") == "asc") {
        query = query.sortBy(_.id)
      } else {
        query = query.sortBy(_.id.desc)
      }
    } else {
      if (sortOrder.getOrElse("asc") == "asc") {
        query = sortColumn.getOrElse("shortname") match {
          case "fullname" => query.sortBy(_.fullname)
          case "template" => query.sortBy(_.templateName)
          case _ => query.sortBy(_.shortname)
        }
      } else {
        query = sortColumn.getOrElse("shortname") match {
          case "fullname" => query.sortBy(_.fullname.desc)
          case "template" => query.sortBy(_.templateName.desc)
          case _ => query.sortBy(_.shortname.desc)
        }
      }
    }
    DB.run(
      for {
        results <- query.drop((page - 1) * limit).take(limit).result
        resultCount <- query.size.result
      } yield (results, resultCount)
    )
  }

  def getById(id: Long): Future[Option[Organizations]] = {
    DB.run(PersistenceSchema.organizations.filter(_.id === id).result.headOption)
  }

  def getByApiKey(apiKey: String): Future[Option[Organizations]] = {
    DB.run(PersistenceSchema.organizations.filter(_.apiKey === apiKey).result.headOption)
  }

  /**
    * Gets organization with specified id
    */
  def getOrganizationById(orgId: Long): Future[Organizations] = {
    DB.run(PersistenceSchema.organizations.filter(_.id === orgId).result.head)
  }

  def getOrganizationBySystemId(systemId: Long): Future[Organizations] = {
    DB.run(
      PersistenceSchema.organizations
      .join(PersistenceSchema.systems).on(_.id === _.owner)
      .filter(_._2.id === systemId)
      .map(x => x._1)
      .result.head
    )
  }

  private def copyTestSetup(fromOrganisation: Long, toOrganisation: Long, copyOrganisationParameters: Boolean, copySystemParameters: Boolean, copyStatementParameters: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Option[List[SystemCreationDbInfo]]] = {
    for {
      systems <- systemManager.getSystemsByOrganizationInternal(fromOrganisation)
      communityId <- PersistenceSchema.organizations.filter(_.id === fromOrganisation).map(x => x.community).result.head
      createdSystemInfo <- {
        val actions = new ListBuffer[DBIO[Option[List[SystemCreationDbInfo]]]]()
        systems.foreach { otherSystem =>
          actions += (
            for {
              newSystemId <- systemManager.registerSystem(Systems(0L, otherSystem.shortname, otherSystem.fullname, otherSystem.description, otherSystem.version, CryptoUtil.generateApiKey(), CryptoUtil.generateApiKey(), toOrganisation), communityId, None, None, None, setPropertiesWithDefaultValues = true, onSuccessCalls)
              createdSystemInfo <- {
                DBIO.successful(Some(List[SystemCreationDbInfo](new SystemCreationDbInfo(newSystemId, None))))
              }
              linkedActorIds <- systemManager.copyTestSetup(otherSystem.id, newSystemId, copySystemParameters, copyStatementParameters, onSuccessCalls)
              _ <- {
                createdSystemInfo.get.head.linkedActorIds = Some(linkedActorIds)
                DBIO.successful(())
              }
            } yield createdSystemInfo
          )
        }
        DBIO.fold(actions.toList, None) {
          (aggregated, current) => {
            Some(aggregated.getOrElse(List[SystemCreationDbInfo]()) ++ current.getOrElse(List[SystemCreationDbInfo]()))
          }
        }
      }
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (copyOrganisationParameters) {
          actions += deleteOrganizationParameterValues(toOrganisation, onSuccessCalls).map(_ => None)
          actions += (for {
            otherValues <- PersistenceSchema.organisationParameterValues.filter(_.organisation === fromOrganisation).result.map(_.toList)
            _ <- {
              val copyActions = new ListBuffer[DBIO[_]]()
              otherValues.foreach(otherValue => {
                onSuccessCalls += (() => repositoryUtils.setOrganisationPropertyFile(otherValue.parameter, toOrganisation, repositoryUtils.getOrganisationPropertyFile(otherValue.parameter, fromOrganisation), copy = true))
                copyActions += (PersistenceSchema.organisationParameterValues += OrganisationParameterValues(toOrganisation, otherValue.parameter, otherValue.value, otherValue.contentType))
              })
              toDBIO(copyActions)
            }
          } yield())
        }
        toDBIO(actions)
      }
    } yield createdSystemInfo
  }

  def createOrganizationInTrans(organization: Organizations): DBIO[Long] = {
    createOrganizationInTrans(organization, checkApiKeyUniqueness = false)
  }

  def createOrganizationInTrans(organization: Organizations, checkApiKeyUniqueness: Boolean): DBIO[Long] = {
    for {
      replaceApiKey <- if (checkApiKeyUniqueness && organization.apiKey.isDefined) {
        PersistenceSchema.organizations.filter(_.apiKey === organization.apiKey).exists.result
      } else {
        DBIO.successful(false)
      }
      newOrgId <- {
        val orgToUse = if (replaceApiKey) organization.withApiKey(CryptoUtil.generateApiKey()) else organization
        PersistenceSchema.insertOrganization += orgToUse
      }
    } yield newOrgId
  }

  def applyDefaultPropertyValues(organisationId: Long, communityId: Long): DBIO[_] ={
    for {
      // 1. Determine the properties that have default values.
      propertiesWithDefaults <-
          PersistenceSchema.organisationParameters
            .filter(_.community === communityId)
            .filter(_.defaultValue.isDefined)
            .map(x => (x.id, x.defaultValue.get))
            .result
      // 2. See which of these properties have values.
      propertiesWithDefaultsThatAreSet <-
        if (propertiesWithDefaults.isEmpty) {
          DBIO.successful(List.empty)
        } else {
          PersistenceSchema.organisationParameterValues
            .filter(_.organisation === organisationId)
            .filter(_.parameter inSet propertiesWithDefaults.map(x => x._1))
            .map(x => x.parameter)
            .result
        }
      // 3. Apply the default values for any properties that are not set.
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (propertiesWithDefaults.nonEmpty) {
          propertiesWithDefaults.foreach { defaultPropertyInfo =>
            if (!propertiesWithDefaultsThatAreSet.contains(defaultPropertyInfo._1)) {
              actions += (PersistenceSchema.organisationParameterValues += OrganisationParameterValues(organisationId, defaultPropertyInfo._1, defaultPropertyInfo._2, None))
            }
          }
        }
        toDBIO(actions)
      }
    } yield ()
  }

  def createOrganizationWithRelatedData(organization: Organizations, otherOrganisationId: Option[Long], propertyValues: Option[List[OrganisationParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], copyOrganisationParameters: Boolean, copySystemParameters: Boolean, copyStatementParameters: Boolean, checkApiKeyUniqueness: Boolean, setDefaultPropertyValues: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[OrganisationCreationDbInfo] = {
    for {
      newOrganisationId <- createOrganizationInTrans(organization, checkApiKeyUniqueness)
      _ <- {
        if (propertyValues.isDefined && (otherOrganisationId.isEmpty || !copyOrganisationParameters)) {
          saveOrganisationParameterValues(newOrganisationId, organization.community, isAdmin = true, propertyValues.get, propertyFiles.get, onSuccessCalls)
        } else {
          DBIO.successful(())
        }
      }
      createdSystemsInfo <- {
        if (otherOrganisationId.isDefined) {
          copyTestSetup(otherOrganisationId.get, newOrganisationId, copyOrganisationParameters, copySystemParameters, copyStatementParameters, onSuccessCalls)
        } else {
          DBIO.successful(Some(List[SystemCreationDbInfo]()))
        }
      }
      // Set properties with default values.
      _ <- if (setDefaultPropertyValues) {
        applyDefaultPropertyValues(newOrganisationId, organization.community)
      } else {
        DBIO.successful(())
      }
      createdOrganisationInfo <- DBIO.successful(new OrganisationCreationDbInfo(newOrganisationId, createdSystemsInfo))
    } yield createdOrganisationInfo
  }

  /**
    * Creates new organization
    */
  def createOrganization(organization: Organizations, otherOrganisationId: Option[Long], propertyValues: Option[List[OrganisationParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], copyOrganisationParameters: Boolean, copySystemParameters: Boolean, copyStatementParameters: Boolean): Future[Long] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = createOrganizationWithRelatedData(organization, otherOrganisationId, propertyValues, propertyFiles, copyOrganisationParameters, copySystemParameters, copyStatementParameters, checkApiKeyUniqueness = false, setDefaultPropertyValues = true, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map { orgInfo =>
      triggerHelper.triggersFor(organization.community, orgInfo)
      orgInfo.organisationId
    }
  }

  def createOrganisationThroughAutomationApi(input: CreateOrganisationRequest): Future[String] = {
    val action = for {
      communityId <- automationApiHelper.getCommunityByCommunityApiKey(input.communityApiKey)
      apiKeyToUse <- {
        for {
          generateApiKey <- if (input.apiKey.isEmpty) {
            DBIO.successful(true)
          } else {
            PersistenceSchema.organizations.filter(_.apiKey === input.apiKey.get).exists.result
          }
          apiKeyToUse <- if (generateApiKey) {
            DBIO.successful(CryptoUtil.generateApiKey())
          } else {
            DBIO.successful(input.apiKey.get)
          }
        } yield apiKeyToUse
      }
      createdOrganisationId <- createOrganizationInTrans(Organizations(0L, input.shortName, input.fullName,
        OrganizationType.Vendor.id.toShort, adminOrganization = false, None, None, None, template = false, None,
        Some(apiKeyToUse), communityId
      ))
    } yield (communityId, apiKeyToUse, new OrganisationCreationDbInfo(createdOrganisationId, None))
    DB.run(action.transactionally).map { result =>
      // Call triggers (separate transaction).
      triggerHelper.triggersFor(result._1, result._3)
      // Return assigned API key.
      result._2
    }
  }

  def isTemplateNameUnique(templateName: String, communityId: Long, organisationIdToIgnore: Option[Long]): Future[Boolean] = {
    DB.run(
      PersistenceSchema.organizations
        .filter(_.community === communityId)
        .filter(_.template === true)
        .filter(_.templateName === templateName)
        .filterOpt(organisationIdToIgnore)((q, idToIgnore) => q.id =!= idToIgnore)
        .exists
        .result
    ).map(!_)
  }

  def updateOwnOrganization(userId: Long, shortName: String, fullName: String, propertyValues: Option[List[OrganisationParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], landingPageId: Option[Option[Long]]): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      userInfo <- PersistenceSchema.users.filter(_.id === userId).map(x => (x.organization, x.role)).result.head
      communityId <- PersistenceSchema.organizations.filter(_.id === userInfo._1).map(_.community).result.head
      // Update core properties.
      _ <- PersistenceSchema.organizations
        .filter(_.id === userInfo._1).map(x => (x.shortname, x.fullname))
        .update(shortName, fullName)
      // Update landing page (if applicable).
      _ <- {
        if (landingPageId.isDefined) {
          PersistenceSchema.organizations.filter(_.id === userInfo._1).map(_.landingPage).update(landingPageId.get)
        } else {
          DBIO.successful(())
        }
      }
      _ <- {
        if (propertyValues.isDefined && propertyFiles.isDefined) {
          val isAdmin = userInfo._2 == UserRole.SystemAdmin.id.toShort || userInfo._2 == UserRole.CommunityAdmin.id.toShort
          saveOrganisationParameterValues(userInfo._1, communityId, isAdmin, propertyValues.get, propertyFiles.get, onSuccessCalls)
        } else {
          DBIO.successful(())
        }
      }
    } yield (communityId, userInfo._1)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map { results =>
      triggerHelper.publishTriggerEvent(new OrganisationUpdatedEvent(results._1, results._2))
    }
  }

  def updateOrganizationInternal(orgId: Long, shortName: String, fullName: String, landingPageId: Option[Long], legalNoticeId: Option[Long], errorTemplateId: Option[Long], otherOrganisation: Option[Long], template: Boolean, templateName: Option[String], apiKey: Option[Option[String]], propertyValues: Option[List[OrganisationParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], copyOrganisationParameters: Boolean, copySystemParameters: Boolean, copyStatementParameters: Boolean, checkApiKeyUniqueness: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Option[List[SystemCreationDbInfo]]] = {
    for {
      org <- PersistenceSchema.organizations.filter(_.id === orgId).result.headOption
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (org.isDefined) {
          var templateNameToSet: Option[String] = null
          if (template) {
            templateNameToSet = templateName
          } else {
            templateNameToSet = None
          }
          if (apiKey.isDefined) {
            // We only optionally update the API key
            actions += (for {
              replaceApiKey <- {
                if (apiKey.isDefined && apiKey.get.isDefined && checkApiKeyUniqueness) {
                  PersistenceSchema.organizations.filter(_.apiKey === apiKey.get.get).filter(_.id =!= orgId).exists.result
                } else {
                  DBIO.successful(false)
                }
              }
              _ <- {
                val apiKeyToUse = if (replaceApiKey) Some(CryptoUtil.generateApiKey()) else apiKey.get
                val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.shortname, o.fullname, o.landingPage, o.legalNotice, o.errorTemplate, o.template, o.templateName, o.apiKey)
                q.update(shortName, fullName, landingPageId, legalNoticeId, errorTemplateId, template, templateNameToSet, apiKeyToUse)
              }
            } yield ())
          } else {
            val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.shortname, o.fullname, o.landingPage, o.legalNotice, o.errorTemplate, o.template, o.templateName)
            actions += q.update(shortName, fullName, landingPageId, legalNoticeId, errorTemplateId, template, templateNameToSet)
          }
          if (shortName.nonEmpty && !org.get.shortname.equals(shortName)) {
            actions += testResultManager.updateForUpdatedOrganisation(orgId, shortName)
          }
        }
        toDBIO(actions)
      }
      createdSystemInfo <- {
        if (otherOrganisation.isDefined) {
          for {
            // Replace the test setup for the organisation with the one from the provided one.
            _ <- systemManager.deleteSystemByOrganization(orgId, onSuccessCalls)
            createdSystemInfo <- copyTestSetup(otherOrganisation.get, orgId, copyOrganisationParameters, copySystemParameters, copyStatementParameters, onSuccessCalls)
          } yield createdSystemInfo
        } else {
          DBIO.successful(Some(List[SystemCreationDbInfo]()))
        }
      }
      _ <- {
        if (propertyValues.isDefined && propertyFiles.isDefined && (otherOrganisation.isEmpty || !copyOrganisationParameters)) {
          saveOrganisationParameterValues(orgId, org.get.community, isAdmin = true, propertyValues.get, propertyFiles.get, onSuccessCalls)
        } else {
          DBIO.successful(())
        }
      }
    } yield createdSystemInfo
  }

  private def getCommunityIdByOrganisationId(organisationId: Long): DBIO[Long] = {
    PersistenceSchema.organizations.filter(_.id === organisationId).map(x => x.community).result.head
  }

  def updateOrganisationThroughAutomationApi(updateRequest: UpdateOrganisationRequest): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      communityId <- automationApiHelper.getCommunityByCommunityApiKey(updateRequest.communityApiKey)
      organisation <- {
        for {
          organisation <- PersistenceSchema.organizations
            .filter(_.community === communityId)
            .filter(_.apiKey === updateRequest.organisationApiKey)
            .result
            .headOption
          _ <- {
            if (organisation.isEmpty) {
              throw AutomationApiException(ErrorCodes.API_ORGANISATION_NOT_FOUND, "No organisation found for the provided API keys")
            } else {
              DBIO.successful(())
            }
          }
        } yield organisation.get
      }
      creationInfo <- updateOrganizationInternal(organisation.id,
        updateRequest.shortName.getOrElse(organisation.shortname),
        updateRequest.fullName.getOrElse(organisation.fullname),
        organisation.landingPage, organisation.legalNotice, organisation.errorTemplate,
        None, organisation.template, organisation.templateName, None, None, None,
        copyOrganisationParameters = false, copySystemParameters = false, copyStatementParameters = false,
        checkApiKeyUniqueness = false, onSuccessCalls
      )
    } yield (communityId, organisation.id, creationInfo)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally).map { result =>
      // Call triggers in separate transactions.
      triggerHelper.publishTriggerEvent(new OrganisationUpdatedEvent(result._1, result._2))
      triggerHelper.triggersFor(result._1, result._3)
    }
  }

  def updateOrganization(orgId: Long, shortName: String, fullName: String, landingPageId: Option[Long], legalNoticeId: Option[Long], errorTemplateId: Option[Long], otherOrganisation: Option[Long], template: Boolean, templateName: Option[String], propertyValues: Option[List[OrganisationParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], copyOrganisationParameters: Boolean, copySystemParameters: Boolean, copyStatementParameters: Boolean): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      communityId <- getCommunityIdByOrganisationId(orgId)
      createdSystemInfo <- updateOrganizationInternal(orgId, shortName, fullName, landingPageId, legalNoticeId, errorTemplateId, otherOrganisation, template, templateName, None, propertyValues, propertyFiles, copyOrganisationParameters, copySystemParameters, copyStatementParameters, checkApiKeyUniqueness = false, onSuccessCalls)
    } yield (communityId, createdSystemInfo)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map { result =>
      triggerHelper.publishTriggerEvent(new OrganisationUpdatedEvent(result._1, orgId))
      triggerHelper.triggersFor(result._1, result._2)
    }
  }

  /**
    * Deletes organization by community
    */
  def deleteOrganizationByCommunity(communityId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
      for {
        _ <- testResultManager.updateForDeletedOrganisationByCommunityId(communityId)
        list <- PersistenceSchema.organizations.filter(_.community === communityId).result
        _ <- DBIO.seq(list.map { org =>
          deleteOrganization(org.id, onSuccess)
        }: _*)
      } yield ()
  }

  private def deleteOrganizationParameterValues(orgId: Long, onSuccess: mutable.ListBuffer[() => _]) = {
    for {
      parameterValueIds <- PersistenceSchema.organisationParameterValues.filter(_.organisation === orgId).map(x => x.parameter).result
      _ <- {
        parameterValueIds.foreach { paramId =>
          onSuccess += (() => repositoryUtils.deleteOrganisationPropertyFile(paramId, orgId))
        }
        PersistenceSchema.organisationParameterValues.filter(_.organisation === orgId).delete
      }
    } yield ()
  }

  def deleteOrganisationThroughAutomationApi(organisationApiKey: String, communityApiKey: String): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      communityId <- automationApiHelper.getCommunityByCommunityApiKey(communityApiKey)
      organisationId <- PersistenceSchema.organizations
        .filter(_.community === communityId)
        .filter(_.apiKey === organisationApiKey)
        .map(_.id)
        .result
        .headOption
      _ <- {
        if (organisationId.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_ORGANISATION_NOT_FOUND, "No organisation found for the provided API keys")
        } else {
          deleteOrganization(organisationId.get, onSuccessCalls)
        }
      }
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  def deleteOrganization(orgId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      _ <- testResultManager.updateForDeletedOrganisation(orgId)
      _ <- deleteUserByOrganization(orgId)
      _ <- systemManager.deleteSystemByOrganization(orgId, onSuccess)
      _ <- deleteOrganizationParameterValues(orgId, onSuccess)
      _ <- PersistenceSchema.conformanceSnapshotResults.filter(_.organisationId === orgId).map(_.organisationId).update(orgId * -1)
      _ <- PersistenceSchema.conformanceSnapshotOrganisations.filter(_.id === orgId).map(_.id).update(orgId * -1)
      _ <- PersistenceSchema.conformanceSnapshotOrganisationProperties.filter(_.organisationId === orgId).map(_.organisationId).update(orgId * -1)
      _ <- PersistenceSchema.organizations.filter(_.id === orgId).delete
    } yield ()
  }

  /**
    * Deletes organization with specified id
    */
  def deleteOrganizationWrapper(orgId: Long): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteOrganization(orgId, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
  }

  /**
    * Deletes all users with specified organization
    */
  private def deleteUserByOrganization(orgId: Long): DBIO[_] = {
    PersistenceSchema.users.filter(_.organization === orgId).delete
  }

  def getOrganisationParameterValues(orgId: Long, onlySimple: Option[Boolean] = None, forExports: Option[Boolean] = None): Future[List[OrganisationParametersWithValue]] = {
    for {
      communityId <- getById(orgId).map(_.get.community)
      result <- {
        val typeToCheck = if (onlySimple.isDefined && onlySimple.get) {
          Some("SIMPLE")
        } else {
          None
        }
        DB.run(PersistenceSchema.organisationParameters
          .joinLeft(PersistenceSchema.organisationParameterValues).on((p, v) => p.id === v.parameter && v.organisation === orgId)
          .filter(_._1.community === communityId)
          .filterOpt(forExports)((q, flag) => q._1.inExports === flag)
          .filterOpt(typeToCheck)((table, propertyType)=> table._1.kind === propertyType)
          .sortBy(x => (x._1.displayOrder.asc, x._1.name.asc))
          .map(x => (x._1, x._2))
          .result
        ).map { results =>
          results.toList.map(r => new OrganisationParametersWithValue(r._1, r._2))
        }
      }
    } yield result
  }

  private def prerequisitesSatisfied(isAdmin: Boolean, parameterToCheck: OrganisationParameters, statusMap: mutable.Map[Long, Boolean], definitionMap: Map[String, OrganisationParameters], providedValueMap: Map[Long, OrganisationParameterValues], existingValueMap: Option[Map[Long, OrganisationParameterValues]], checkedParameters: mutable.Set[Long]): Boolean = {
    var satisfied = false
    if (statusMap.contains(parameterToCheck.id)) {
      satisfied = statusMap(parameterToCheck.id)
    } else {
      if (!checkedParameters.contains(parameterToCheck.id)) {
        checkedParameters += parameterToCheck.id
        // The above check is added to avoid circular dependencies that would never complete.
        if (parameterToCheck.dependsOn.isDefined) {
          if (parameterToCheck.dependsOnValue.isDefined) {
            if (definitionMap.contains(parameterToCheck.dependsOn.get)) {
              val parentParameter = definitionMap(parameterToCheck.dependsOn.get)
              if (prerequisitesSatisfied(isAdmin, parentParameter, statusMap, definitionMap, providedValueMap, existingValueMap, checkedParameters)) {
                var parentValue = providedValueMap.get(parentParameter.id)
                if (parentValue.isEmpty && !isAdmin && parentParameter.adminOnly && parentParameter.hidden && existingValueMap.isDefined) {
                  parentValue = existingValueMap.get.get(parentParameter.id)
                }
                if (parentValue.isDefined && parentValue.get.value != null && parentValue.get.value.equals(parameterToCheck.dependsOnValue.get)) {
                  satisfied = true
                }
              }
            }
          }
        } else {
          satisfied = true
        }
      }
      statusMap += (parameterToCheck.id -> satisfied)
    }
    satisfied
  }

  def saveOrganisationParameterValues(orgId: Long, communityId: Long, isAdmin: Boolean, values: List[OrganisationParameterValues], files: Map[Long, FileInfo], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    saveOrganisationParameterValues(orgId, communityId, isAdmin, isSelfRegistration = false, values, files, requireMandatoryPropertyValues = false, onSuccessCalls)
  }

  def saveOrganisationParameterValues(orgId: Long, communityId: Long, isAdmin: Boolean, isSelfRegistration: Boolean, values: List[OrganisationParameterValues], files: Map[Long, FileInfo], requireMandatoryPropertyValues: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      // Create a map of the provided parameters
      providedParameters <- DBIO.successful(values.map(x => (x.parameter, x)).toMap)
      // Load existing parameter values (needed to check prerequisites that are admin-only and hidden - only needed if we're enforcing required values.
      existingSimpleValues <- {
        if (requireMandatoryPropertyValues && !isSelfRegistration) {
          for {
            simpleValueList <- PersistenceSchema.organisationParameterValues
              .join(PersistenceSchema.organisationParameters).on(_.parameter === _.id)
              .filter(_._1.organisation === orgId)
              .filter(_._2.kind === "SIMPLE")
              .map(x => x._1)
              .result
          } yield Some(simpleValueList.map(x => (x.parameter, x)).toMap)
        } else {
          DBIO.successful(None)
        }
      }
      // Load parameter definitions for the organisation's community.
      parameterDefinitions <- PersistenceSchema.organisationParameters.filter(_.community === communityId).result
      // Create also a map based on the key.
      parameterDefinitionMap <- DBIO.successful(parameterDefinitions.map(x => (x.testKey, x)).toMap)
      // Make checks and updates.
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        val prerequisiteStatusMap = mutable.Map[Long, Boolean]()
        val checkPrerequisitesSet = mutable.Set[Long]()
        parameterDefinitions.foreach { parameterDefinition =>
          if (requireMandatoryPropertyValues && "R".equals(parameterDefinition.use) && !providedParameters.contains(parameterDefinition.id) && prerequisitesSatisfied(isAdmin, parameterDefinition, prerequisiteStatusMap, parameterDefinitionMap, providedParameters, existingSimpleValues, checkPrerequisitesSet)) {
            if (isAdmin || (!parameterDefinition.adminOnly && !parameterDefinition.hidden && (!isSelfRegistration || parameterDefinition.inSelfRegistration))) {
              // No need to check this case before as the UI normally enforces this. Only way we could reach this is if a request is tampered.
              // Admins (if forced) should provide all values. Non-admins if forced would only be expected to provide values for parameters that are editable by them (non-admin, non-hidden).
              throw new IllegalStateException("Required parameter ["+parameterDefinition.testKey+"] missing")
            }
          }
          if ((!parameterDefinition.adminOnly && !parameterDefinition.hidden) || isAdmin) {
            val matchedProvidedParameter = providedParameters.get(parameterDefinition.id)
            if (matchedProvidedParameter.isDefined) {
              // Create or update
              if (parameterDefinition.kind != "SECRET" || (parameterDefinition.kind == "SECRET" && matchedProvidedParameter.get.value != "")) {
                // Special case: No update for secret parameters that are defined but not updated.
                var valueToSet = matchedProvidedParameter.get.value
                var existingBinaryNotUpdated = false
                var contentTypeToSet: Option[String] = None
                if (parameterDefinition.kind == "SECRET") {
                  // Encrypt secret value at rest.
                  valueToSet = MimeUtil.encryptString(valueToSet)
                } else if (parameterDefinition.kind == "BINARY") {
                  // Store file.
                  if (files.contains(parameterDefinition.id)) {
                    contentTypeToSet = files(parameterDefinition.id).contentType
                    onSuccessCalls += (() => repositoryUtils.setOrganisationPropertyFile(parameterDefinition.id, orgId, files(parameterDefinition.id).file))
                  } else {
                    existingBinaryNotUpdated = true
                  }
                }
                if (!existingBinaryNotUpdated) {
                  actions += PersistenceSchema.organisationParameterValues.filter(_.parameter === parameterDefinition.id).filter(_.organisation === orgId).delete
                  actions += (PersistenceSchema.organisationParameterValues += OrganisationParameterValues(orgId, matchedProvidedParameter.get.parameter, valueToSet, contentTypeToSet))
                }
              }
            } else {
              // Delete existing (if present)
              onSuccessCalls += (() => repositoryUtils.deleteOrganisationPropertyFile(parameterDefinition.id, orgId))
              actions += PersistenceSchema.organisationParameterValues.filter(_.parameter === parameterDefinition.id).filter(_.organisation === orgId).delete
            }
          }
        }
        toDBIO(actions)
      }
    } yield()
  }

  def saveOrganisationParameterValuesWrapper(userId: Long, orgId: Long, values: List[OrganisationParameterValues], propertyFiles: Map[Long, FileInfo]): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      isAdmin <- PersistenceSchema.users.filter(_.id === userId).map(x => x.role).result.head.map { userRole =>
        userRole == UserRole.CommunityAdmin.id.toShort || userRole == UserRole.SystemAdmin.id.toShort
      }
      communityId <- PersistenceSchema.organizations
        .filter(_.id === orgId)
        .map(_.community)
        .result
        .head
      _ <- saveOrganisationParameterValues(orgId, communityId, isAdmin, values, propertyFiles, onSuccessCalls)
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
  }

  def updateOrganisationApiKey(organisationId: Long): Future[String] = {
    val newApiKey = CryptoUtil.generateApiKey()
    updateOrganisationApiKeyInternal(organisationId, Some(newApiKey)).map { _ =>
      newApiKey
    }
  }

  def deleteOrganisationApiKey(organisationId: Long): Future[Unit] = {
    updateOrganisationApiKeyInternal(organisationId, None)
  }

  private def updateOrganisationApiKeyInternal(organisationId: Long, apiKey: Option[String]): Future[Unit] = {
    DB.run(PersistenceSchema.organizations.filter(_.id === organisationId).map(_.apiKey).update(apiKey).transactionally).map(_ => ())
  }

  def getAutomationKeysForOrganisation(organisationId: Long, snapshotId: Option[Long]): Future[ApiKeyInfo] = {
    DB.run(for {
      organisationApiKey <- PersistenceSchema.organizations.filter(_.id === organisationId).map(_.apiKey).result.head
      systemApiKeys <- if (snapshotId.isEmpty) {
        PersistenceSchema.systems.filter(_.owner === organisationId).map(x => (x.id, x.fullname, x.apiKey)).sortBy(_._2.asc).result
      } else {
        PersistenceSchema.conformanceSnapshotSystems
          .join(PersistenceSchema.conformanceSnapshotResults).on((a, b) => a.id === b.systemId && a.snapshotId === b.snapshotId)
          .filter(_._2.snapshotId === snapshotId)
          .filter(_._2.organisationId === organisationId)
          .map(x => (x._1.id, x._1.fullname, x._1.apiKey))
          .distinct
          .result
      }
      domainApiKeys <- {
        if (systemApiKeys.nonEmpty) {
          val query = if (snapshotId.isEmpty) {
            PersistenceSchema.conformanceResults
              .join(PersistenceSchema.specifications).on(_.spec === _.id)
              .join(PersistenceSchema.actors).on(_._1.actor === _.id)
              .join(PersistenceSchema.testSuites).on(_._1._1.testsuite === _.id)
              .join(PersistenceSchema.testCases).on(_._1._1._1.testcase === _.id)
              .joinLeft(PersistenceSchema.specificationGroups).on(_._1._1._1._2.group === _.id)
              .filter(_._1._1._1._1._1.sut inSet systemApiKeys.map(_._1).toSet)
              .map(x => (
                x._1._1._1._1._2.shortname, // Specification name [1]
                x._1._1._1._2.name, // Actor name [2]
                x._1._1._1._2.apiKey, // Actor API key [3]
                x._1._1._2.shortname, // Test suite name [4]
                x._1._1._2.identifier, // Test suite identifier [5]
                x._1._2.shortname, // Test case name [6]
                x._1._2.identifier, // Test case identifier [7]
                x._1._1._1._1._2.id, // Specification ID [8]
                x._2.map(_.shortname), // Specification group name [9]
                x._1._1._1._2.id, // Actor ID [10]
                x._1._1._2.id, // Test suite ID [11]
                x._1._2.id // Test case ID [12]
              ))
          } else {
            PersistenceSchema.conformanceSnapshotResults
              .join(PersistenceSchema.conformanceSnapshotSpecifications).on((a, b) => a.specificationId === b.id && a.snapshotId === b.snapshotId)
              .join(PersistenceSchema.conformanceSnapshotActors).on((a, b) => a._1.actorId === b.id && a._1.snapshotId === b.snapshotId)
              .join(PersistenceSchema.conformanceSnapshotTestSuites).on((a, b) => a._1._1.testSuiteId === b.id && a._1._1.snapshotId === b.snapshotId)
              .join(PersistenceSchema.conformanceSnapshotTestCases).on((a, b) => a._1._1._1.testCaseId === b.id && a._1._1._1.snapshotId === b.snapshotId)
              .joinLeft(PersistenceSchema.conformanceSnapshotSpecificationGroups).on((a, b) => a._1._1._1._1.specificationGroupId === b.id && a._1._1._1._1.snapshotId === b.snapshotId)
              .filter(_._1._1._1._1._1.systemId inSet systemApiKeys.map(_._1).toSet)
              .map(x => (
                x._1._1._1._1._2.shortname, // Specification name [1]
                x._1._1._1._2.name, // Actor name [2]
                x._1._1._1._2.apiKey, // Actor API key [3]
                x._1._1._2.shortname, // Test suite name [4]
                x._1._1._2.identifier, // Test suite identifier [5]
                x._1._2.shortname, // Test case name [6]
                x._1._2.identifier, // Test case identifier [7]
                x._1._1._1._1._2.id, // Specification ID [8]
                x._2.map(_.shortname) ,// Specification group name [9]
                x._1._1._1._2.id, // Actor ID [10]
                x._1._1._2.id, // Test suite ID [11]
                x._1._2.id // Test case ID [12]
              ))
          }
          query.distinct.sortBy(x => (x._9.asc, x._1.asc, x._2.asc, x._4.asc, x._6.asc)).result
        } else {
          DBIO.successful(List.empty)
        }
      }
    } yield (organisationApiKey, systemApiKeys, domainApiKeys)).map { results =>
      // Process results from DB
      // Organise results into a tree hierarchy
      val specificationMap = new mutable.LinkedHashMap[Long, SpecificationApiKeyInfo]()
      results._3.foreach { result =>
        // Spec info.
        val spec = specificationMap.getOrElseUpdate(result._8, SpecificationApiKeyInfo(result._1, new mutable.TreeMap(), new mutable.TreeMap(), result._9))
        // Actor info.
        if (!spec.actorInfo.contains(result._3)) {
          spec.actorInfo += (result._3 -> ApiKeyActorInfo(result._10, result._2, result._3))
        }
        // Test suite info.
        val testSuite = spec.testSuiteInfo.getOrElseUpdate(result._5, TestSuiteApiKeyInfo(ApiKeyTestSuiteInfo(result._11, result._4, result._5, List()), new mutable.TreeMap()))
        // Test case info.
        if (!testSuite.testCaseInfo.contains(result._7)) {
          testSuite.testCaseInfo += (result._7 -> ApiKeyTestCaseInfo(result._12, result._6, result._7))
        }
      }
      // Map the tree hierarchies to the types to return.
      val specifications = specificationMap.map { spec =>
        val specName = if (spec._2.specificationGroupName.isDefined) {
          spec._2.specificationGroupName.get + " - " + spec._2.specificationName
        } else {
          spec._2.specificationName
        }
        ApiKeySpecificationInfo(
          spec._1,
          specName,
          spec._2.actorInfo.values.toList,
          spec._2.testSuiteInfo.map { testSuite =>
            testSuite._2.testSuiteInfo.copy(testcases = testSuite._2.testCaseInfo.values.toList)
          }.toList
        )
      }.toList
      // Return final aggregate object.
      ApiKeyInfo(
        results._1,
        results._2.map { system =>
          val id = system._1
          val name = system._2
          val apiKey = system._3
          ApiKeySystemInfo(id, name, apiKey)
        }.toList,
        specifications
      )
    }
  }
}