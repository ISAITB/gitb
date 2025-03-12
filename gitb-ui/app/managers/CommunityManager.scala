package managers

import config.Configurations
import exceptions.{AutomationApiException, ErrorCodes}
import models.Enums.OverviewLevelType.OverviewLevelType
import models.Enums._
import models._
import models.automation._
import org.apache.commons.lang3.StringUtils
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider
import utils.{CryptoUtil, JsonUtil, MimeUtil, RepositoryUtils}

import java.util
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

@Singleton
class CommunityManager @Inject() (repositoryUtils: RepositoryUtils,
                                  communityResourceManager: CommunityResourceManager,
                                  triggerHelper: TriggerHelper,
                                  testResultManager: TestResultManager,
                                  organizationManager: OrganizationManager,
                                  landingPageManager: LandingPageManager,
                                  legalNoticeManager: LegalNoticeManager,
                                  errorTemplateManager: ErrorTemplateManager,
                                  conformanceManager: ConformanceManager,
                                  accountManager: AccountManager,
                                  domainParameterManager: DomainParameterManager,
                                  automationApiHelper: AutomationApiHelper,
                                  dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def existsOrganisationWithSameUserEmail(communityId: Long, email: String): Boolean = {
    exec(PersistenceSchema.users
      .join(PersistenceSchema.organizations).on(_.organization === _.id)
      .filter(_._2.community === communityId)
      .filter(_._1.ssoEmail.toLowerCase === email.toLowerCase)
      .result
      .headOption
    ).isDefined
  }

  def existsOrganisationWithSameUserEmailDomain(communityId: Long, email: String): Boolean = {
    val existingUsers = exec(PersistenceSchema.users
      .join(PersistenceSchema.organizations).on(_.organization === _.id)
      .filter(_._2.community === communityId)
      .filter(_._1.ssoEmail.isDefined)
      .map(x => x._1.ssoEmail)
      .distinct
      .result
      .map(_.toSet)
    )
    val newUserEmailDomain = StringUtils.substringAfter(email.toLowerCase, "@")
    existingUsers.foreach { existingUserEmail =>
      if (existingUserEmail.isDefined) {
        val userEmailDomain = StringUtils.substringAfter(existingUserEmail.get.toLowerCase, "@")
        if (newUserEmailDomain.equals(userEmailDomain)) {
          return true
        }
      }
    }
    false
  }

  def selfRegister(organisation: Organizations, organisationAdmin: Users, templateId: Option[Long], actualUserInfo: Option[ActualUserInfo], customPropertyValues: Option[List[OrganisationParameterValues]], customPropertyFiles: Option[Map[Long, FileInfo]], requireMandatoryPropertyValues: Boolean): Long = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction: DBIO[(Long, OrganisationCreationDbInfo)] = for {
      // Save organisation
      organisationInfo <- organizationManager.createOrganizationWithRelatedData(organisation, templateId, None, None, copyOrganisationParameters = true, copySystemParameters = true, copyStatementParameters = true, checkApiKeyUniqueness = false, setDefaultPropertyValues = false, onSuccessCalls)
      // Save custom organisation property values
      _ <- {
        if (customPropertyValues.isDefined && customPropertyFiles.isDefined) {
          organizationManager.saveOrganisationParameterValues(organisationInfo.organisationId, organisation.community, isAdmin = false, isSelfRegistration = true, customPropertyValues.get, customPropertyFiles.get, requireMandatoryPropertyValues, onSuccessCalls)
        } else {
          DBIO.successful(())
        }
      }
      // Apply property defaults.
      _ <- organizationManager.applyDefaultPropertyValues(organisationInfo.organisationId, organisation.community)
      // Save admin user account
      userId <- PersistenceSchema.insertUser += organisationAdmin.withOrganizationId(organisationInfo.organisationId)
      // Link current session user with created admin user account
      _ <-
        if (actualUserInfo.isDefined) {
          accountManager.linkAccountInternal(userId, actualUserInfo.get)
        } else {
          DBIO.successful(())
        }
    } yield (userId, organisationInfo)
    val ids = exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
    triggerHelper.triggersFor(organisation.community, ids._2)
    ids._1
  }

  /**
    * Gets all communities with given ids or all if none specified
    */
  def getCommunities(ids: Option[List[Long]], skipDefault: Boolean): List[Communities] = {
    var q = ids match {
      case Some(idList) =>
        PersistenceSchema.communities.filter(_.id inSet idList)
      case None =>
        PersistenceSchema.communities
    }
    if (skipDefault) {
      q = q.filter(_.id =!= Constants.DefaultCommunityId)
    }
    exec(q.sortBy(_.shortname.asc)
      .result.map(_.toList))
  }

  def getSelfRegistrationOptions(): List[SelfRegOption] = {
    exec(
      PersistenceSchema.communities
        .joinLeft(PersistenceSchema.domains).on(_.domain === _.id)
        .filter(x => x._1.selfRegType === SelfRegistrationType.PublicListing.id.toShort || x._1.selfRegType === SelfRegistrationType.PublicListingWithToken.id.toShort)
        .sortBy(_._1.shortname.asc).result
    ).map(x => new SelfRegOption(x._1.id, x._1.shortname, selfRegDescriptionToUse(x._1.description, x._2), x._1.selfRegTokenHelpText, x._1.selfRegType, organizationManager.getOrganisationTemplates(x._1.id), getCommunityLabels(x._1.id), getOrganisationParametersForSelfRegistration(x._1.id), x._1.selfRegForceTemplateSelection, x._1.selfRegForceRequiredProperties)).toList
  }

  private def selfRegDescriptionToUse(communityDescription: Option[String], communityDomain: Option[Domain]): Option[String] = {
    if (communityDescription.isDefined) {
      communityDescription
    } else {
      if (communityDomain.isDefined) {
        communityDomain.get.description
      } else {
        None
      }
    }
  }

  def getById(id: Long): Option[Communities] = {
    exec(getByIdInternal(id))
  }

  def getByIdAsync(id: Long): Future[Option[Communities]] = {
    DB.run(getByIdInternal(id))
  }

  def getByApiKey(apiKey: String): Option[Communities] = {
    exec(PersistenceSchema.communities.filter(_.apiKey === apiKey).result.headOption)
  }

  private def getByIdInternal(id: Long) = {
    PersistenceSchema.communities.filter(_.id === id).result.headOption
  }

  def getCommunityDomain(communityId: Long): Option[Long] = {
    exec(PersistenceSchema.communities.filter(_.id === communityId).map(_.domain).result.headOption).flatten
  }

  /**
    * Gets the user community
    */
  def getUserCommunity(userId: Long): Community = {
    val u = exec(PersistenceSchema.users.filter(_.id === userId).result.head)
    val o = exec(PersistenceSchema.organizations.filter(_.id === u.organization).result.head)
    val c = exec(PersistenceSchema.communities.filter(_.id === o.community).result.head)
    val d = exec(PersistenceSchema.domains.filter(_.id === c.domain).result.headOption)
    val community = new Community(c, d)
    community
  }

  def getUserCommunityId(userId: Long): Long = {
    exec(getUserCommunityIdInternal(userId)).get
  }

  def getUserCommunityIdInternal(userId: Long): DBIO[Option[Long]] = {
    PersistenceSchema.users
      .join(PersistenceSchema.organizations).on(_.organization === _.id)
      .filter(_._1.id === userId)
      .map(_._2.community)
      .result
      .headOption
  }

  /**
    * Creates new community
    */
  def createCommunity(community: Communities): (Long, Long) = {
    exec(createCommunityInternal(community).transactionally)
  }

  def createCommunityThroughAutomationApi(input: CreateCommunityRequest): String = {
    val action = for {
      apiKeyToUse <- {
        for {
          generateApiKey <- if (input.apiKey.isEmpty) {
            DBIO.successful(true)
          } else {
            PersistenceSchema.communities.filter(_.apiKey === input.apiKey.get).exists.result
          }
          apiKeyToUse <- if (generateApiKey) {
            DBIO.successful(CryptoUtil.generateApiKey())
          } else {
            DBIO.successful(input.apiKey.get)
          }
        } yield apiKeyToUse
      }
      domainId <- {
        if (input.domainApiKey.isEmpty) {
          DBIO.successful(None)
        } else {
          for {
            domainId <- automationApiHelper.getDomainIdByDomainApiKey(input.domainApiKey.get)
          } yield Some(domainId)
        }
      }
      _ <- {
        createCommunityInternal(Communities(0L, input.shortName, input.fullName, input.supportEmail,
          SelfRegistrationType.NotSupported.id.toShort, None, None, selfRegNotification = false,
          interactionNotification = input.interactionNotifications.getOrElse(false), input.description, SelfRegistrationRestriction.NoRestriction.id.toShort,
          selfRegForceTemplateSelection = false, selfRegForceRequiredProperties = false, allowCertificateDownload = false,
          allowStatementManagement = true, allowSystemManagement = true, allowPostTestOrganisationUpdates = true,
          allowPostTestSystemUpdates = true, allowPostTestStatementUpdates = true,
          allowAutomationApi = true, apiKeyToUse, None, domainId
        ))
      }
    } yield apiKeyToUse
    exec(action.transactionally)
  }

  def createCommunityInternal(community: Communities): DBIO[(Long, Long)] = {
    createCommunityInternal(community, checkApiKeyUniqueness = false)
  }

  def createCommunityInternal(community: Communities, checkApiKeyUniqueness: Boolean): DBIO[(Long, Long)] = {
    for {
      replaceApiKey <- if (checkApiKeyUniqueness) {
        PersistenceSchema.communities.filter(_.apiKey === community.apiKey).exists.result
      } else {
        DBIO.successful(false)
      }
      communityId <- {
        val communityToUse = if (replaceApiKey) community.withApiKey(CryptoUtil.generateApiKey()) else community
        PersistenceSchema.insertCommunity += communityToUse
      }
      adminOrganisationId <- {
        organizationManager.createOrganizationInTrans(Organizations(0L, Constants.AdminOrganizationName, Constants.AdminOrganizationName, OrganizationType.Vendor.id.toShort, adminOrganization = true, None, None, None, template = false, None, None, communityId))
      }
    } yield (communityId, adminOrganisationId)
  }

  /**
    * Gets community with specified id
    */
  def getCommunityById(communityId: Long): Community = {
    val c = exec(PersistenceSchema.communities.filter(_.id === communityId).result.head)
    val d = exec(PersistenceSchema.domains.filter(_.id === c.domain).result.headOption)
    val community = new Community(c, d)
    community
  }

  def checkCommunityAllowsAutomationApi(communityId: Long): Boolean = {
    exec(PersistenceSchema.communities.filter(_.id === communityId).map(_.allowAutomationApi).result.headOption).getOrElse(false)
  }

  private def updateCommunityDomainDependencies(community: Communities, domainId: Option[Long], onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    val actions = new ListBuffer[DBIO[_]]()
    if (community.domain.isDefined && domainId.isDefined && community.domain.get != domainId.get) {
      // New domain doesn't match previous domain. Remove conformance statements for previous domain.
      actions += conformanceManager.deleteConformanceStatementsForDomainAndCommunity(community.domain.get, community.id, onSuccess)
      // Remove also any trigger data that referred to domain parameters.
      actions += triggerHelper.deleteTriggerDataOfCommunityAndDomain(community.id, community.domain.get)
    } else if (community.domain.isEmpty && domainId.isDefined) {
      // Domain set for community that was not previously linked to a domain. Remove statements for other domains.
      val action = for {
        systemActors <- {
          // Get the conformance statement information to delete (system and actor IDs).
          PersistenceSchema.systemImplementsActors
            .join(PersistenceSchema.systems).on(_.systemId === _.id)
            .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
            .join(PersistenceSchema.actors).on(_._1._1.actorId === _.id)
            .filter(_._1._2.community === community.id)
            .filter(_._2.domain =!= domainId.get)
            .map(x => (x._1._1._1.actorId, x._1._1._1.systemId))
            .result
            .map(_.toList)
        }
        systemAndActorIds <- {
          // Put IDs in sets for easier processing (no need for deletion per each match).
          val actorIds = new util.HashSet[Long]
          val systemIds = new util.HashSet[Long]
          systemActors.foreach { systemActorPair =>
            actorIds.add(systemActorPair._1)
            systemIds.add(systemActorPair._2)
          }
          import scala.jdk.CollectionConverters._
          DBIO.successful((actorIds.asScala.toSet, systemIds.asScala.toSet))
        }
        _ <- {
          // Delete conformance statement data for collected IDs.
          PersistenceSchema.systemImplementsActors.filter(_.systemId inSet systemAndActorIds._2).filter(_.actorId inSet systemAndActorIds._1).delete andThen
            PersistenceSchema.conformanceResults.filter(_.sut inSet systemAndActorIds._2).filter(_.actor inSet systemAndActorIds._1).delete
        }
      } yield ()
      actions += action
    }
    toDBIO(actions)
  }

  private[managers] def updateCommunityDomain(community: Communities, domainId: Option[Long], onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    updateCommunityDomainDependencies(community, domainId, onSuccess) andThen {
      val qs = for {c <- PersistenceSchema.communities if c.id === community.id} yield c.domain
      qs.update(domainId)
    }
  }

  private[managers] def updateCommunityInternal(community: Communities, shortName: String, fullName: String, supportEmail: Option[String],
                                                selfRegType: Short, selfRegToken: Option[String], selfRegTokenHelpText: Option[String], selfRegNotification: Boolean, interactionNotification: Boolean,
                                                description: Option[String], selfRegRestriction: Short, selfRegForceTemplateSelection: Boolean, selfRegForceRequiredProperties: Boolean,
                                                allowCertificateDownload: Boolean, allowStatementManagement: Boolean, allowSystemManagement: Boolean,
                                                allowPostTestOrganisationUpdates: Boolean, allowPostTestSystemUpdates: Boolean, allowPostTestStatementUpdates: Boolean, allowAutomationApi: Option[Boolean],
                                                apiKey: Option[String], domainId: Option[Long], checkApiKeyUniqueness: Boolean, onSuccess: mutable.ListBuffer[() => _]) = {
    for {
      // Update short name.
      _ <- {
        if (shortName.nonEmpty && community.shortname != shortName) {
          PersistenceSchema.communities.filter(_.id === community.id).map(_.shortname).update(shortName) andThen
            testResultManager.updateForUpdatedCommunity(community.id, shortName)
        } else {
          DBIO.successful(())
        }
      }
      // Update full name.
      _ <- {
        if (fullName.nonEmpty && community.fullname != fullName) {
          PersistenceSchema.communities.filter(_.id === community.id).map(_.fullname).update(fullName)
        } else {
          DBIO.successful(())
        }
      }
      // Handle domain update.
      _ <- updateCommunityDomainDependencies(community, domainId, onSuccess)
      // Update core properties.
      _ <- PersistenceSchema.communities
        .filter(_.id === community.id)
        .map(c => (
          c.supportEmail, c.domain, c.description, c.allowCertificateDownload, c.allowStatementManagement, c.allowSystemManagement,
          c.allowPostTestOrganisationUpdates, c.allowPostTestSystemUpdates, c.allowPostTestStatementUpdates, c.interactionNotification
        ))
        .update(supportEmail, domainId, description, allowCertificateDownload, allowStatementManagement, allowSystemManagement,
          allowPostTestOrganisationUpdates, allowPostTestSystemUpdates, allowPostTestStatementUpdates, interactionNotification
        )
      // Update self-registration properties.
      _ <- {
        if (Configurations.REGISTRATION_ENABLED) {
          PersistenceSchema.communities
            .filter(_.id === community.id)
            .map(c => (
              c.selfRegType, c.selfRegToken, c.selfRegTokenHelpText, c.selfRegNotification,
              c.selfRegRestriction, c.selfRegForceTemplateSelection, c.selfRegForceRequiredProperties
            ))
            .update(selfRegType, selfRegToken, selfRegTokenHelpText, selfRegNotification,
              selfRegRestriction, selfRegForceTemplateSelection, selfRegForceRequiredProperties
            )
        } else {
          DBIO.successful(())
        }
      }
      // Update REST-API properties.
      _ <- {
        if (Configurations.AUTOMATION_API_ENABLED) {
          PersistenceSchema.communities.filter(_.id === community.id)
            .map(c => c.allowAutomationApi)
            .update(allowAutomationApi.getOrElse(community.allowAutomationApi))
        } else {
          DBIO.successful(())
        }
      }
      // API key update.
      replaceApiKey <- {
        if (apiKey.isDefined && checkApiKeyUniqueness) {
          PersistenceSchema.communities.filter(_.apiKey === apiKey.get).filter(_.id =!= community.id).exists.result
        } else {
          DBIO.successful(false)
        }
      }
      _ <- {
        if (apiKey.isDefined) {
          val apiKeyToUse = if (replaceApiKey) CryptoUtil.generateApiKey() else apiKey.get
          PersistenceSchema.communities.filter(_.id === community.id).map(_.apiKey).update(apiKeyToUse)
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
  }

  def updateCommunityThroughAutomationApi(updateRequest: UpdateCommunityRequest, allowDomainChange: Boolean): Unit = {
    val onSuccess = ListBuffer[() => _]()
    val action = for {
      community <- {
        for {
          community <- PersistenceSchema.communities
            .filter(_.apiKey === updateRequest.communityApiKey)
            .result
            .headOption
          _ <- {
            if (community.isEmpty) {
              throw AutomationApiException(ErrorCodes.API_COMMUNITY_NOT_FOUND, "No community found for the provided API key")
            } else {
              DBIO.successful(())
            }
          }
        } yield community.get
      }
      domainIdToUse <- {
        if (updateRequest.domainApiKey.isDefined) {
          // We have a domain API key specified.
          if (updateRequest.domainApiKey.get.isEmpty && community.domain.isEmpty) {
            // No change for the domain (no domain is defined).
            DBIO.successful(None)
          } else if (updateRequest.domainApiKey.get.isEmpty && community.domain.isDefined ||
                     updateRequest.domainApiKey.get.isDefined && community.domain.isEmpty) {
            // We are setting or removing the community's domain. We can only do this when authenticating with the master API key.
            if (allowDomainChange) {
              if (updateRequest.domainApiKey.get.isDefined) {
                for {
                  domainId <- automationApiHelper.getDomainIdByDomainApiKey(updateRequest.domainApiKey.get.get)
                } yield Some(domainId)
              } else {
                DBIO.successful(None)
              }
            } else {
              throw AutomationApiException(ErrorCodes.API_COMMUNITY_DOMAIN_CHANGE_NOT_ALLOWED, "You are not allowed to change the community's domain when using a community API key for the authorisation header")
            }
          } else {
            // Both API keys are defined.
            for {
              newDomainId <- automationApiHelper.getDomainIdByDomainApiKey(updateRequest.domainApiKey.get.get)
              _ <- {
                if (newDomainId != community.domain.get) {
                  if (allowDomainChange) {
                    DBIO.successful(())
                  } else {
                    throw AutomationApiException(ErrorCodes.API_COMMUNITY_DOMAIN_CHANGE_NOT_ALLOWED, "You are not allowed to change the community's domain when using a community API key for the authorisation header")
                  }
                } else {
                  DBIO.successful(())
                }
              }
            } yield Some(newDomainId)
          }
        } else {
          // No change requested for the community's domain.
          DBIO.successful(community.domain)
        }
      }
      _ <- {
        updateCommunityInternal(community,
          updateRequest.shortName.getOrElse(community.shortname),
          updateRequest.fullName.getOrElse(community.fullname),
          updateRequest.supportEmail.getOrElse(community.supportEmail),
          community.selfRegType, community.selfRegToken, community.selfRegTokenHelpText, community.selfRegNotification,
          updateRequest.interactionNotifications.getOrElse(community.interactionNotification),
          updateRequest.description.getOrElse(community.description),
          community.selfRegRestriction, community.selfRegForceTemplateSelection, community.selfRegForceRequiredProperties,
          community.allowCertificateDownload, community.allowStatementManagement, community.allowSystemManagement,
          community.allowPostTestOrganisationUpdates, community.allowPostTestSystemUpdates, community.allowPostTestStatementUpdates,
          Some(community.allowAutomationApi), None, domainIdToUse, checkApiKeyUniqueness = false, onSuccess
        )
      }
    } yield ()
    exec(dbActionFinalisation(Some(onSuccess), None, action).transactionally)
  }

  /**
    * Update community
    */
  def updateCommunity(communityId: Long, shortName: String, fullName: String, supportEmail: Option[String],
                      selfRegType: Short, selfRegToken: Option[String], selfRegTokenHelpText: Option[String],
                      selfRegNotification: Boolean, interactionNotification: Boolean, description: Option[String], selfRegRestriction: Short,
                      selfRegForceTemplateSelection: Boolean, selfRegForceRequiredProperties: Boolean,
                      allowCertificateDownload: Boolean, allowStatementManagement: Boolean, allowSystemManagement: Boolean,
                      allowPostTestOrganisationUpdates: Boolean, allowPostTestSystemUpdates: Boolean,
                      allowPostTestStatementUpdates: Boolean, allowAutomationApi: Option[Boolean],
                      domainId: Option[Long]): Unit = {

    val onSuccess = ListBuffer[() => _]()
    val dbAction = for {
      community <- PersistenceSchema.communities.filter(_.id === communityId).result.headOption
      _ <- {
        if (community.isDefined) {
          updateCommunityInternal(
            community.get, shortName, fullName, supportEmail, selfRegType, selfRegToken, selfRegTokenHelpText,
            selfRegNotification, interactionNotification, description, selfRegRestriction, selfRegForceTemplateSelection, selfRegForceRequiredProperties,
            allowCertificateDownload, allowStatementManagement, allowSystemManagement,
            allowPostTestOrganisationUpdates, allowPostTestSystemUpdates, allowPostTestStatementUpdates, allowAutomationApi, None,
            domainId, checkApiKeyUniqueness = false, onSuccess
          )
        } else {
          throw new IllegalArgumentException("Community with ID '" + communityId + "' not found")
        }
      }
    } yield ()
    exec(dbActionFinalisation(Some(onSuccess), None, dbAction).transactionally)
  }

  /**
    * Deletes the community with specified id
    */
  def deleteCommunity(communityId: Long): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteCommunityInternal(communityId, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteCommunityThroughAutomationApi(communityApiKey: String): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      communityId <- automationApiHelper.getCommunityByCommunityApiKey(communityApiKey)
      _ <- deleteCommunityInternal(communityId, onSuccessCalls)
    } yield ()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  def deleteCommunityInternal(communityId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    conformanceManager.deleteConformanceSnapshotsOfCommunity(communityId, onSuccessCalls) andThen
      organizationManager.deleteOrganizationByCommunity(communityId, onSuccessCalls) andThen
      landingPageManager.deleteLandingPageByCommunity(communityId) andThen
      legalNoticeManager.deleteLegalNoticeByCommunity(communityId) andThen
      errorTemplateManager.deleteErrorTemplateByCommunity(communityId) andThen
      triggerHelper.deleteTriggersByCommunity(communityId) andThen
      testResultManager.updateForDeletedCommunity(communityId) andThen
      deleteConformanceCertificateSettings(communityId) andThen
      deleteConformanceOverviewCertificateSettings(communityId) andThen
      deleteOrganisationParametersByCommunity(communityId) andThen
      deleteSystemParametersByCommunity(communityId) andThen
      communityResourceManager.deleteResourcesOfCommunity(communityId, onSuccessCalls) andThen
      deleteCommunityKeystoreInternal(communityId) andThen
      deleteCommunityReportStylesheets(communityId, onSuccessCalls) andThen
      PersistenceSchema.communityLabels.filter(_.community === communityId).delete andThen
      PersistenceSchema.communities.filter(_.id === communityId).delete
  }

  private def deleteCommunityReportStylesheets(communityId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    onSuccessCalls += (() => repositoryUtils.deleteCommunityReportStylesheets(communityId))
    DBIO.successful(())
  }

  def createOrganisationParameterInternal(parameter: OrganisationParameters): DBIO[Long] = {
    PersistenceSchema.organisationParameters.returning(PersistenceSchema.organisationParameters.map(_.id)) += parameter
  }

  def createOrganisationParameter(parameter: OrganisationParameters): Long = {
    exec(createOrganisationParameterInternal(parameter).transactionally)
  }

  private def setOrganisationParameterPrerequisitesForKey(communityId: Long, key: String, newKey: Option[String]): DBIO[_] = {
    if (newKey.isDefined) {
      val q = for {p <- PersistenceSchema.organisationParameters.filter(_.community === communityId).filter(_.dependsOn === key)} yield p.dependsOn
      q.update(newKey)
    } else {
      val q = for {p <- PersistenceSchema.organisationParameters.filter(_.community === communityId).filter(_.dependsOn === key)} yield (p.dependsOn, p.dependsOnValue)
      q.update(None, None)
    }
  }

  private def setSystemParameterPrerequisitesForKey(communityId: Long, key: String, newKey: Option[String]): DBIO[_] = {
    if (newKey.isDefined) {
      val q = for {p <- PersistenceSchema.systemParameters.filter(_.community === communityId).filter(_.dependsOn === key)} yield p.dependsOn
      q.update(newKey)
    } else {
      val q = for {p <- PersistenceSchema.systemParameters.filter(_.community === communityId).filter(_.dependsOn === key)} yield (p.dependsOn, p.dependsOnValue)
      q.update(None, None)
    }
  }

  def updateOrganisationParameterInternal(parameter: OrganisationParameters, updateDisplayOrder: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      existingParameter <- PersistenceSchema.organisationParameters.filter(_.id === parameter.id).map(x => (x.community, x.testKey, x.kind)).result.head
      _ <- {
        if (!existingParameter._2.equals(parameter.testKey)) {
          // Update the dependsOn of other properties.
          setOrganisationParameterPrerequisitesForKey(existingParameter._1, existingParameter._2, Some(parameter.testKey))
        } else if (existingParameter._3.equals("SIMPLE") && !existingParameter._3.equals(parameter.kind)) {
          // Remove the dependsOn of other properties.
          setOrganisationParameterPrerequisitesForKey(existingParameter._1, existingParameter._2, None)
        } else {
          DBIO.successful(())
        }
      }
      _ <- {
        if (existingParameter._3 != parameter.kind) {
          // Remove previous values.
          onSuccessCalls += (() => repositoryUtils.deleteOrganisationPropertiesFolder(parameter.id))
          PersistenceSchema.organisationParameterValues.filter(_.parameter === parameter.id).delete
        } else {
          DBIO.successful(())
        }
      }
      _ <- {
        // Don't update display order here.
        val q = for {p <- PersistenceSchema.organisationParameters if p.id === parameter.id} yield (p.description, p.use, p.kind, p.name, p.testKey, p.adminOnly, p.notForTests, p.inExports, p.inSelfRegistration, p.hidden, p.allowedValues, p.dependsOn, p.dependsOnValue, p.defaultValue)
        q.update(parameter.description, parameter.use, parameter.kind, parameter.name, parameter.testKey, parameter.adminOnly, parameter.notForTests, parameter.inExports, parameter.inSelfRegistration, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue, parameter.defaultValue)
      }
      _ <- {
        if (updateDisplayOrder) {
          PersistenceSchema.organisationParameters.filter(_.id === parameter.id).map(_.displayOrder).update(parameter.displayOrder)
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
  }

  def updateOrganisationParameter(parameter: OrganisationParameters): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, updateOrganisationParameterInternal(parameter, updateDisplayOrder = false, onSuccessCalls)).transactionally)
  }

  def updateOrganisationParameterDefinitionThroughAutomationApi(communityKey: String, input: CustomPropertyInfo): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      // Load communityID.
      communityId <- automationApiHelper.getCommunityByCommunityApiKey(communityKey)
      // Ensure property exists.
      property <- checkOrganisationParameterExistence(communityId, input.key, expectedToExist = true, None)
      // If this depends on another property check that it exists.
      dependency <- checkDependedOrganisationParameterExistence(communityId, input.dependsOn.flatten, property.map(_.id))
      // Proceed with update.
      _ <- {
        val dependsOnStatus = automationApiHelper.propertyDependsOnStatus(input, dependency.flatMap(_.allowedValues))
        updateOrganisationParameterInternal(OrganisationParameters(
          property.get.id,
          input.name.getOrElse(property.get.name),
          input.key,
          input.description.getOrElse(property.get.description),
          automationApiHelper.propertyUseText(input.required, property.get.use),
          "SIMPLE",
          !input.editableByUsers.getOrElse(!property.get.adminOnly),
          !input.inTests.getOrElse(!property.get.notForTests),
          input.inExports.getOrElse(property.get.inExports),
          input.inSelfRegistration.getOrElse(property.get.inSelfRegistration),
          input.hidden.getOrElse(property.get.hidden),
          input.allowedValues.map(x => automationApiHelper.propertyAllowedValuesText(x)).getOrElse(property.get.allowedValues),
          input.displayOrder.getOrElse(property.get.displayOrder),
          dependsOnStatus._1.getOrElse(property.get.dependsOn),
          dependsOnStatus._2.getOrElse(property.get.dependsOnValue),
          automationApiHelper.propertyDefaultValue(
            input.defaultValue.getOrElse(property.get.defaultValue),
            input.allowedValues.getOrElse(property.get.allowedValues.map(x => JsonUtil.parseJsAllowedPropertyValues(x)))
          ),
          communityId
        ), updateDisplayOrder = true, onSuccessCalls)
      }
    } yield ()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction.transactionally))
  }

  def updateSystemParameterDefinitionThroughAutomationApi(communityKey: String, input: CustomPropertyInfo): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      // Load communityID.
      communityId <- automationApiHelper.getCommunityByCommunityApiKey(communityKey)
      // Ensure property exists.
      property <- checkSystemParameterExistence(communityId, input.key, expectedToExist = true, None)
      // If this depends on another property check that it exists.
      dependency <- checkDependedSystemParameterExistence(communityId, input.dependsOn.flatten, property.map(_.id))
      // Proceed with update.
      _ <- {
        val dependsOnStatus = automationApiHelper.propertyDependsOnStatus(input, dependency.flatMap(_.allowedValues))
        updateSystemParameterInternal(SystemParameters(
          property.get.id,
          input.name.getOrElse(property.get.name),
          input.key,
          input.description.getOrElse(property.get.description),
          automationApiHelper.propertyUseText(input.required, property.get.use),
          "SIMPLE",
          !input.editableByUsers.getOrElse(!property.get.adminOnly),
          !input.inTests.getOrElse(!property.get.notForTests),
          input.inExports.getOrElse(property.get.inExports),
          input.hidden.getOrElse(property.get.hidden),
          input.allowedValues.map(x => automationApiHelper.propertyAllowedValuesText(x)).getOrElse(property.get.allowedValues),
          input.displayOrder.getOrElse(property.get.displayOrder),
          dependsOnStatus._1.getOrElse(property.get.dependsOn),
          dependsOnStatus._2.getOrElse(property.get.dependsOnValue),
          automationApiHelper.propertyDefaultValue(
            input.defaultValue.getOrElse(property.get.defaultValue),
            input.allowedValues.getOrElse(property.get.allowedValues.map(x => JsonUtil.parseJsAllowedPropertyValues(x)))
          ),
          communityId
        ), updateDisplayOrder = true, onSuccessCalls)
      }
    } yield ()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction.transactionally))
  }

  private def checkDependedOrganisationParameterExistence(communityId: Long, dependsOn: Option[String], propertyIdToIgnore: Option[Long]): DBIO[Option[OrganisationParameters]] = {
    if (dependsOn.isEmpty) {
      DBIO.successful(None)
    } else {
      for {
        property <- checkOrganisationParameterExistence(communityId, dependsOn.get, expectedToExist = true, propertyIdToIgnore)
        _ <- {
          if (property.get.kind != "SIMPLE") {
            throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "Property [%s] upon which this property depends on must be of simple type".formatted(dependsOn.get))
          } else {
            DBIO.successful(())
          }
        }
      } yield property
    }
  }

  private def checkDependedSystemParameterExistence(communityId: Long, dependsOn: Option[String], propertyIdToIgnore: Option[Long]): DBIO[Option[SystemParameters]] = {
    if (dependsOn.isEmpty) {
      DBIO.successful(None)
    } else {
      for {
        property <- checkSystemParameterExistence(communityId, dependsOn.get, expectedToExist = true, propertyIdToIgnore)
        _ <- {
          if (property.get.kind != "SIMPLE") {
            throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "Property [%s] upon which this property depends on must be of simple type".formatted(dependsOn.get))
          } else {
            DBIO.successful(())
          }
        }
      } yield property
    }
  }

  def deleteOrganisationParameterWrapper(parameterId: Long): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteOrganisationParameter(parameterId, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteOrganisationParameterDefinitionThroughAutomationApi(communityKey: String, propertyKey: String): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      // Load community ID.
      communityId <- automationApiHelper.getCommunityByCommunityApiKey(communityKey)
      // Ensure the property exists.
      property <- checkOrganisationParameterExistence(communityId, propertyKey, expectedToExist = true, None)
      // Delete property
      _ <- {
        deleteOrganisationParameter(property.get.id, onSuccessCalls)
      }
    } yield ()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteSystemParameterDefinitionThroughAutomationApi(communityKey: String, propertyApiKey: String): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      // Load community ID.
      communityId <- automationApiHelper.getCommunityByCommunityApiKey(communityKey)
      // Ensure the property exists.
      property <- checkSystemParameterExistence(communityId, propertyApiKey, expectedToExist = true, None)
      // Delete property
      _ <- {
        deleteSystemParameter(property.get.id, onSuccessCalls)
      }
    } yield ()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteOrganisationParameter(parameterId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    onSuccessCalls += (() => repositoryUtils.deleteOrganisationPropertiesFolder(parameterId))
    triggerHelper.deleteTriggerDataByDataType(parameterId, TriggerDataType.OrganisationParameter) andThen
      PersistenceSchema.organisationParameterValues.filter(_.parameter === parameterId).delete andThen
      (for {
        existingParameter <- PersistenceSchema.organisationParameters.filter(_.id === parameterId).map(x => (x.community, x.testKey, x.kind)).result.head
        _ <- {
          if (existingParameter._3.equals("SIMPLE")) {
            setOrganisationParameterPrerequisitesForKey(existingParameter._1, existingParameter._2, None)
          } else {
            DBIO.successful(())
          }
        }
      } yield ()) andThen
      PersistenceSchema.organisationParameters.filter(_.id === parameterId).delete
  }

  private def deleteOrganisationParametersByCommunity(communityId: Long) = {
    // The values and files are already deleted as part of the organisation deletes
    PersistenceSchema.organisationParameters.filter(_.community === communityId).delete
  }

  def orderOrganisationParameters(communityId: Long, orderedIds: List[Long]): Unit = {
    val dbActions = ListBuffer[DBIO[_]]()
    var counter = 0
    orderedIds.foreach { id =>
      counter += 1
      val q = for { p <- PersistenceSchema.organisationParameters.filter(_.community === communityId).filter(_.id === id) } yield p.displayOrder
      dbActions += q.update(counter.toShort)
    }
    exec(toDBIO(dbActions).transactionally)
  }

  def orderSystemParameters(communityId: Long, orderedIds: List[Long]): Unit = {
    val dbActions = ListBuffer[DBIO[_]]()
    var counter = 0
    orderedIds.foreach { id =>
      counter += 1
      val q = for { p <- PersistenceSchema.systemParameters.filter(_.community === communityId).filter(_.id === id) } yield p.displayOrder
      dbActions += q.update(counter.toShort)
    }
    exec(toDBIO(dbActions).transactionally)
  }

  def createSystemParameterInternal(parameter: SystemParameters): DBIO[Long] = {
    PersistenceSchema.systemParameters.returning(PersistenceSchema.systemParameters.map(_.id)) += parameter
  }

  def createSystemParameter(parameter: SystemParameters): Long = {
    exec(createSystemParameterInternal(parameter).transactionally)
  }

  def updateSystemParameterInternal(parameter: SystemParameters, updateDisplayOrder: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      existingParameter <- PersistenceSchema.systemParameters.filter(_.id === parameter.id).map(x => (x.community, x.testKey, x.kind)).result.head
      _ <- {
        if (!existingParameter._2.equals(parameter.testKey)) {
          // Update the dependsOn of other properties.
          setSystemParameterPrerequisitesForKey(existingParameter._1, existingParameter._2, Some(parameter.testKey))
        } else if (existingParameter._3.equals("SIMPLE") && !existingParameter._3.equals(parameter.kind)) {
          // Remove the dependsOn of other properties.
          setSystemParameterPrerequisitesForKey(existingParameter._1, existingParameter._2, None)
        } else {
          DBIO.successful(())
        }
      }
      _ <- {
        if (existingParameter._3 != parameter.kind) {
          // Remove previous values.
          onSuccessCalls += (() => repositoryUtils.deleteSystemPropertiesFolder(parameter.id))
          PersistenceSchema.systemParameterValues.filter(_.parameter === parameter.id).delete
        } else {
          DBIO.successful(())
        }
      }
      _ <- {
        // Don't update display order here.
        val q = for {p <- PersistenceSchema.systemParameters if p.id === parameter.id} yield (p.description, p.use, p.kind, p.name, p.testKey, p.adminOnly, p.notForTests, p.inExports, p.hidden, p.allowedValues, p.dependsOn, p.dependsOnValue, p.defaultValue)
        q.update(parameter.description, parameter.use, parameter.kind, parameter.name, parameter.testKey, parameter.adminOnly, parameter.notForTests, parameter.inExports, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue, parameter.defaultValue)
      }
      _ <- {
        if (updateDisplayOrder) {
          PersistenceSchema.systemParameters.filter(_.id === parameter.id).map(_.displayOrder).update(parameter.displayOrder)
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
  }

  def updateSystemParameter(parameter: SystemParameters): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, updateSystemParameterInternal(parameter, updateDisplayOrder = false, onSuccessCalls)).transactionally)
  }

  def deleteSystemParameterWrapper(parameterId: Long): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteSystemParameter(parameterId, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteSystemParameter(parameterId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    onSuccessCalls += (() => repositoryUtils.deleteSystemPropertiesFolder(parameterId))
    triggerHelper.deleteTriggerDataByDataType(parameterId, TriggerDataType.SystemParameter) andThen
    PersistenceSchema.systemParameterValues.filter(_.parameter === parameterId).delete andThen
      (for {
        existingParameter <- PersistenceSchema.systemParameters.filter(_.id === parameterId).map(x => (x.community, x.testKey, x.kind)).result.head
        _ <- {
          if (existingParameter._3.equals("SIMPLE")) {
            setSystemParameterPrerequisitesForKey(existingParameter._1, existingParameter._2, None)
          } else {
            DBIO.successful(())
          }
        }
      } yield ()) andThen
      PersistenceSchema.systemParameters.filter(_.id === parameterId).delete
  }

  private def deleteSystemParametersByCommunity(communityId: Long): DBIO[_] = {
    // The values and files are already deleted as part of the system deletes
    PersistenceSchema.systemParameters.filter(_.community === communityId).delete
  }

  def checkOrganisationParameterExists(parameter: OrganisationParameters, isUpdate: Boolean): Boolean = {
    var q = PersistenceSchema.organisationParameters
      .filter(_.community === parameter.community)
      .filter(x => {
        x.name === parameter.name || x.testKey === parameter.testKey
      })
    if (isUpdate) {
      q = q.filter(_.id =!= parameter.id)
    }
    exec(q.result.headOption).isDefined
  }

  def checkSystemParameterExists(parameter: SystemParameters, isUpdate: Boolean): Boolean = {
    var q = PersistenceSchema.systemParameters
      .filter(_.community === parameter.community)
      .filter(x => {
        x.name === parameter.name || x.testKey === parameter.testKey
      })
    if (isUpdate) {
      q = q.filter(_.id =!= parameter.id)
    }
    exec(q.result.headOption).isDefined
  }

  def getOrganisationParameterById(parameterId: Long): Option[OrganisationParameters] = {
    exec(PersistenceSchema.organisationParameters.filter(_.id === parameterId).result.headOption)
  }

  def getSystemParameterById(parameterId: Long): Option[SystemParameters] = {
    exec(PersistenceSchema.systemParameters.filter(_.id === parameterId).result.headOption)
  }

  def getOrganisationParameters(communityId: Long): List[OrganisationParameters] = {
    getOrganisationParameters(communityId, None)
  }

  def getOrganisationParameters(communityId: Long, forFiltering: Option[Boolean]): List[OrganisationParameters] = {
    var typeToCheck: Option[String] = None
    if (forFiltering.isDefined && forFiltering.get) {
      typeToCheck = Some("SIMPLE")
    }
    exec(PersistenceSchema.organisationParameters
      .filter(_.community === communityId)
      .filterOpt(typeToCheck)((table, propertyType)=> table.kind === propertyType)
      .sortBy(x => (x.displayOrder.asc, x.name.asc))
      .result).toList
  }

  def getSimpleOrganisationParameters(communityId: Long, forExports: Option[Boolean]): List[OrganisationParameters] = {
    exec(PersistenceSchema.organisationParameters
      .filter(_.community === communityId)
      .filterOpt(forExports)((q, flag) => q.inExports === flag)
      .filter(_.kind === "SIMPLE")
      .sortBy(_.testKey.asc)
      .result).toList
  }

  private def getOrganisationParametersForSelfRegistration(communityId: Long): List[OrganisationParameters] = {
    exec(PersistenceSchema.organisationParameters
      .filter(_.community === communityId)
      .filter(_.inSelfRegistration === true)
      .sortBy(x => (x.displayOrder.asc, x.name.asc))
      .result).toList
  }

  def getOrganisationParametersValuesForExport(communityId: Long, organisationIds: Option[List[Long]]): scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]] = {
    // Maps are based on organisationID, parameterID pointing to value.
    var query = PersistenceSchema.organisationParameterValues
      .join(PersistenceSchema.organisationParameters).on(_.parameter === _.id)
      .join(PersistenceSchema.organizations).on(_._1.organisation === _.id)
      .filter(_._2.community === communityId)
      .filter(_._1._2.inExports === true)
      .filter(_._1._2.kind === "SIMPLE")
    if (organisationIds.isDefined) {
      query = query.filter(_._2.id inSet organisationIds.get)
    }
    val values = exec(query.result).toList
    val valuesPerOrganisation = scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]()
    values.foreach{ value =>
      var parameterMap = valuesPerOrganisation.get(value._2.id)
      if (parameterMap.isEmpty) {
        parameterMap = Some(scala.collection.mutable.Map[Long, String]())
        valuesPerOrganisation(value._2.id) = parameterMap.get
      }
      parameterMap.get(value._1._1.parameter) = value._1._1.value
    }
    valuesPerOrganisation
  }

  def getSystemParameters(communityId: Long): List[SystemParameters] = {
    getSystemParameters(communityId, None)
  }

  def getSystemParameters(communityId: Long, forFiltering: Option[Boolean]): List[SystemParameters] = {
    var typeToCheck: Option[String] = None
    if (forFiltering.isDefined && forFiltering.get) {
      typeToCheck = Some("SIMPLE")
    }
    exec(PersistenceSchema.systemParameters
      .filter(_.community === communityId)
      .filterOpt(typeToCheck)((table, propertyType)=> table.kind === propertyType)
      .sortBy(x => (x.displayOrder.asc, x.name.asc))
      .result).toList
  }

  def getSimpleSystemParameters(communityId: Long, forExports: Option[Boolean]): List[SystemParameters] = {
    exec(PersistenceSchema.systemParameters
      .filter(_.community === communityId)
      .filterOpt(forExports)((q, flag) => q.inExports === flag)
      .filter(_.kind === "SIMPLE")
      .sortBy(_.testKey.asc)
      .result).toList
  }

  def getSystemParametersValuesForExport(communityId: Long, organisationIds: Option[List[Long]], systemIds: Option[List[Long]]): scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]] = {
    // Maps are based on systemID, parameterID pointing to value.
    var query = PersistenceSchema.systemParameterValues
      .join(PersistenceSchema.systemParameters).on(_.parameter === _.id)
      .join(PersistenceSchema.systems).on(_._1.system === _.id)
      .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
      .filter(_._2.community === communityId)
      .filter(_._1._1._2.inExports === true)
      .filter(_._1._1._2.kind === "SIMPLE")
    if (organisationIds.isDefined) {
      query = query.filter(_._2.id inSet organisationIds.get)
    }
    if (systemIds.isDefined) {
      query = query.filter(_._1._2.id inSet systemIds.get)
    }
    val values = exec(query.result).toList
    val valuesPerSystem = scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]()
    values.foreach{ value =>
      var parameterMap = valuesPerSystem.get(value._1._2.id)
      if (parameterMap.isEmpty) {
        parameterMap = Some(scala.collection.mutable.Map[Long, String]())
        valuesPerSystem(value._1._2.id) = parameterMap.get
      }
      parameterMap.get(value._1._1._2.id) = value._1._1._1.value
    }
    valuesPerSystem
  }

  def deleteCommunityLabel(communityId: Long, labelType: Short): DBIO[_] = {
    PersistenceSchema.communityLabels.filter(_.community === communityId).filter(_.labelType === labelType).delete
  }

  def createCommunityLabel(label: CommunityLabels): DBIO[_] = {
    PersistenceSchema.communityLabels += label
  }

  private def setCommunityLabelsInternal(communityId: Long, labels: List[CommunityLabels]): DBIO[_] = {
    val actions = new ListBuffer[DBIO[_]]()
    // Delete existing labels
    actions += PersistenceSchema.communityLabels.filter(_.community === communityId).delete
    // Add new labels
    labels.foreach { label =>
      actions += (PersistenceSchema.communityLabels += label)
    }
    toDBIO(actions)
  }

  def setCommunityLabels(communityId: Long, labels: List[CommunityLabels]): Unit = {
    exec(setCommunityLabelsInternal(communityId, labels).transactionally)
  }

  def getCommunityLabels(communityId: Long): List[CommunityLabels] = {
    exec(PersistenceSchema.communityLabels.filter(_.community === communityId).result).toList
  }

  def getCommunityKeystoreType(communityId: Long): Option[String] = {
    exec(PersistenceSchema.communityKeystores.filter(_.community === communityId).map(_.keystoreType).result.headOption)
  }

  def getCommunityKeystore(communityId: Long, decryptKeys: Boolean): Option[CommunityKeystore] = {
    val keystore = exec(PersistenceSchema.communityKeystores.filter(_.community === communityId).result.headOption)
    if (decryptKeys && keystore.isDefined) {
      Some(keystore.get.withDecryptedKeys())
    } else {
      keystore
    }
  }

  def deleteCommunityKeystore(communityId: Long): Unit = {
    exec(deleteCommunityKeystoreInternal(communityId).transactionally)
  }

  def deleteCommunityKeystoreInternal(communityId: Long): DBIO[_] = {
    PersistenceSchema.communityKeystores.filter(_.community === communityId).delete
  }

  def saveCommunityKeystoreInternal(communityId: Long, keystoreType: String, keystoreData: Option[String], keyPass: Option[String], keystorePass: Option[String]): DBIO[_] = {
    // Prepare passwords
    val keyPassToUse = if (keyPass.isDefined) {
      Some(MimeUtil.encryptString(keyPass.get))
    } else {
      None
    }
    val keystorePassToUse = if (keystorePass.isDefined) {
      Some(MimeUtil.encryptString(keystorePass.get))
    } else {
      None
    }
    // Proceed with updates
    val query = for {
      existingSettingsInfo <- PersistenceSchema.communityKeystores.filter(_.community === communityId).map(x => (x.id, x.keystoreType)).result.headOption
      _ <- if (existingSettingsInfo.isEmpty) {
        // Create new keystore
        if (keystoreData.isEmpty || keystorePassToUse.isEmpty || keyPassToUse.isEmpty) {
          throw new IllegalArgumentException("Missing expect keystore information")
        }
        PersistenceSchema.insertCommunityKeystore += CommunityKeystore(0L, keystoreData.get, keystoreType, keystorePassToUse.get, keyPassToUse.get, communityId)
      } else {
        // Update existing keystore
        val actions = ListBuffer[DBIO[_]]()
        if (keystoreData.isDefined) {
          actions += PersistenceSchema.communityKeystores.filter(_.id === existingSettingsInfo.get._1)
            .map(_.keystoreFile)
            .update(keystoreData.get)
        }
        if (keystorePassToUse.isDefined) {
          actions += PersistenceSchema.communityKeystores.filter(_.id === existingSettingsInfo.get._1)
            .map(x => (x.keystorePassword, x.keyPassword))
            .update((keystorePassToUse.get, keyPassToUse.get))
        }
        if (existingSettingsInfo.get._2 != keystoreType) {
          actions += PersistenceSchema.communityKeystores.filter(_.id === existingSettingsInfo.get._1)
            .map(_.keystoreType)
            .update(keystoreType)
        }
        toDBIO(actions)
      }
    } yield ()
    query
  }

  def saveCommunityKeystore(communityId: Long, keystoreType: String, keystoreData: Option[String], keyPass: Option[String], keystorePass: Option[String]): Unit = {
    val query = saveCommunityKeystoreInternal(communityId, keystoreType, keystoreData, keyPass, keystorePass)
    exec(query.transactionally)
  }

  def getConformanceCertificateSettingsForExport(communityId: Long, snapshotId: Option[Long]): ConformanceCertificateInfo = {
    val settings = getConformanceCertificateSettingsWrapper(communityId, defaultIfMissing = true, snapshotId).get
    val keystore = getCommunityKeystore(communityId, decryptKeys = true)
    settings.toConformanceCertificateInfo(keystore)
  }

  def getConformanceCertificateSettingsWrapper(communityId: Long, defaultIfMissing: Boolean, snapshotId: Option[Long]): Option[ConformanceCertificate] = {
    var settings = exec(getConformanceCertificateSettings(communityId, snapshotId))
    if (settings.isEmpty && defaultIfMissing) {
      val title = "Conformance Certificate"
      settings = Some(ConformanceCertificate(
        id = 0L, title = Some(title), includeTitle = true, includeMessage = false, includeTestStatus = true, includeTestCases = true,
        includeDetails = true, includeSignature = false, includePageNumbers = true, message = None, community = communityId
      ))
    }
    settings
  }

  def conformanceOverviewCertificateEnabled(communityId: Long, level: OverviewLevelType): Boolean = {
    val flags = exec(
      PersistenceSchema.conformanceOverviewCertificates
        .filter(_.community === communityId)
        .map(x => (x.enableAllLevel, x.enableDomainLevel, x.enableGroupLevel, x.enableSpecificationLevel))
        .result
        .headOption
    )
    if (flags.isDefined) {
      level match {
        case OverviewLevelType.DomainLevel => flags.get._2
        case OverviewLevelType.SpecificationGroupLevel => flags.get._3
        case OverviewLevelType.SpecificationLevel => flags.get._4
        case _ => flags.get._1
      }
    } else {
      false
    }
  }

  def getConformanceOverviewCertificateSettingsWrapper(communityId: Long, defaultIfMissing: Boolean, snapshotId: Option[Long], levelForMessage: Option[OverviewLevelType], identifierForMessage: Option[Long]): Option[ConformanceOverviewCertificateWithMessages] = {
    var settings = exec(getConformanceOverviewCertificateSettings(communityId, snapshotId, levelForMessage, identifierForMessage))
    if (settings.isEmpty && defaultIfMissing) {
      val title = "Conformance Overview Certificate"
      settings = Some(
        ConformanceOverviewCertificateWithMessages(
          ConformanceOverviewCertificate(
            id = 0L, title = Some(title), includeTitle = true, includeMessage = false, includeStatementStatus = true, includeStatements = true, includeStatementDetails = true,
            includeDetails = true, includeSignature = false, includePageNumbers = true, enableAllLevel = false, enableDomainLevel = false,
            enableGroupLevel = false, enableSpecificationLevel = false, community = communityId
          ),
          List.empty
        )
      )
    }
    settings
  }

  private def getConformanceCertificateSettings(communityId: Long, snapshotId: Option[Long]): DBIO[Option[ConformanceCertificate]] = {
    if (snapshotId.isEmpty) {
      PersistenceSchema.conformanceCertificates.filter(_.community === communityId).result.headOption
    } else {
      for {
        settings <- PersistenceSchema.conformanceCertificates.filter(_.community === communityId).result.headOption
        messageToUse <- {
          if (settings.isDefined && settings.get.includeMessage) {
            PersistenceSchema.conformanceSnapshotCertificateMessages.filter(_.snapshotId === snapshotId).map(_.message).result.headOption
          } else {
            DBIO.successful(None)
          }
        }
        settingsToUse <- {
          if (messageToUse.isDefined) {
            DBIO.successful(Some(settings.get.withMessage(messageToUse.get)))
          } else {
            DBIO.successful(settings)
          }
        }
      } yield settingsToUse
    }
  }

  def getConformanceStatementCertificateMessage(snapshotId: Option[Long], communityId: Long): Option[String] = {
    exec(
      for {
        message <- if (snapshotId.isEmpty) {
          PersistenceSchema.conformanceCertificates.filter(_.community === communityId).map(_.message).result.headOption
        } else {
          DBIO.successful(None)
        }
        snapshotMessage <- if (snapshotId.isDefined) {
          PersistenceSchema.conformanceSnapshotCertificateMessages.filter(_.snapshotId === snapshotId.get).map(_.message).result.headOption
        } else {
          DBIO.successful(None)
        }
        messageToUse <- if (snapshotId.isEmpty) {
          DBIO.successful(message.flatten)
        } else {
          DBIO.successful(snapshotMessage)
        }
      } yield messageToUse
    )
  }

  def getConformanceOverviewCertificateMessage(snapshot: Boolean, messageId: Long): Option[String] = {
    exec(for {
        message <- if (snapshot) {
          PersistenceSchema.conformanceSnapshotOverviewCertificateMessages.filter(_.id === messageId).map(_.message).result.headOption
        } else {
          PersistenceSchema.conformanceOverviewCertificateMessages.filter(_.id === messageId).map(_.message).result.headOption
        }
      } yield message
    )
  }

  private def getConformanceOverviewCertificateSettings(communityId: Long, snapshotId: Option[Long], levelForMessage: Option[OverviewLevelType], identifierForMessage: Option[Long]): DBIO[Option[ConformanceOverviewCertificateWithMessages]] = {
    for {
      settings <- PersistenceSchema.conformanceOverviewCertificates.filter(_.community === communityId).result.headOption
      messages <- {
        // Load the current messages if this not related to a snapshot
        if (snapshotId.isEmpty && settings.isDefined && settings.get.includeMessage) {
          PersistenceSchema.conformanceOverviewCertificateMessages
            .filter(_.community === communityId)
            .filterOpt(levelForMessage)((q, level) => q.messageType === level.id.toShort)
            .result
        } else {
          DBIO.successful(Seq.empty)
        }
      }
      snapshotMessages <- {
        // Load the snapshot messages if this is related to a snapshot
        if (snapshotId.isDefined && settings.isDefined && settings.get.includeMessage) {
          PersistenceSchema.conformanceSnapshotOverviewCertificateMessages
            .filter(_.snapshotId === snapshotId.get)
            .filterOpt(levelForMessage)((q, level) => q.messageType === level.id.toShort)
            .result
        } else {
          DBIO.successful(Seq.empty)
        }
      }
      messagesToUse <- {
        // Pick the current or snapshot messages depending on the case
        if (snapshotId.isEmpty) {
          DBIO.successful(messages)
        } else {
          DBIO.successful(snapshotMessages.map(_.toConformanceOverviewCertificateMessage()))
        }
      }
      result <- if (settings.isDefined) {
        // Check for the message to apply for the specific identifier or return the default for the requested level
        val filteredMessages = if (levelForMessage.isDefined) {
          levelForMessage.get match {
            case OverviewLevelType.DomainLevel =>
              identifierForMessage
                .flatMap(_ => messagesToUse.find(msg => msg.domain.isDefined && msg.domain.get == identifierForMessage.get))
                .orElse(messagesToUse.find(msg => msg.domain.isEmpty))
                .map(Seq(_)).getOrElse(Seq.empty)
            case OverviewLevelType.SpecificationGroupLevel =>
              identifierForMessage
                .flatMap(_ => messagesToUse.find(msg => msg.group.isDefined && msg.group.get == identifierForMessage.get))
                .orElse(messagesToUse.find(msg => msg.group.isEmpty))
                .map(Seq(_)).getOrElse(Seq.empty)
            case OverviewLevelType.SpecificationLevel =>
              identifierForMessage
                .flatMap(_ => messagesToUse.find(msg => msg.specification.isDefined && msg.specification.get == identifierForMessage.get))
                .orElse(messagesToUse.find(msg => msg.specification.isEmpty))
                .map(Seq(_)).getOrElse(Seq.empty)
            case _ =>
              messagesToUse
          }
        } else {
          messagesToUse
        }
        DBIO.successful(Some(ConformanceOverviewCertificateWithMessages(settings.get, filteredMessages)))
      } else {
        DBIO.successful(None)
      }
    } yield result
  }

  def deleteConformanceCertificateSettings(communityId: Long): DBIO[_] = {
    PersistenceSchema.conformanceCertificates.filter(_.community === communityId).delete
  }

  def deleteConformanceOverviewCertificateSettings(communityId: Long): DBIO[_] = {
    PersistenceSchema.conformanceOverviewCertificateMessages.filter(_.community === communityId).delete andThen
      PersistenceSchema.conformanceOverviewCertificates.filter(_.community === communityId).delete
  }

  def applyConfigurationViaAutomationApi(communityKey: String, request: ConfigurationRequest): List[String] = {
    val warnings = new ListBuffer[String]()
    val dbAction = for {
      communityIds <- PersistenceSchema.communities.filter(_.apiKey === communityKey).map(x => (x.id, x.domain)).result.headOption
      // Process domain properties
      _ <- {
        if (communityIds.isDefined) {
          if (request.domainProperties.nonEmpty) {
            domainParameterManager.updateDomainParametersViaApiInternal(communityIds.get._2, request.domainProperties, warnings)
          } else {
            DBIO.successful(())
          }
        } else {
          throw AutomationApiException(ErrorCodes.API_COMMUNITY_NOT_FOUND, "No community found for the provided API key")
        }
      }
      // Process organisation properties
      _ <- {
        val actions = new ListBuffer[DBIO[_]]
        if (communityIds.isDefined && request.organisationProperties.nonEmpty) {
          request.organisationProperties.foreach { updates =>
            actions += updateOrganisationParametersViaApi(updates, communityIds.get._1, warnings)
          }
        }
        toDBIO(actions)
      }
      // Process system properties
      _ <- {
        val actions = new ListBuffer[DBIO[_]]
        if (communityIds.isDefined && request.systemProperties.nonEmpty) {
          request.systemProperties.foreach { updates =>
            actions += updateSystemParametersViaApi(updates, communityIds.get._1, warnings)
          }
        }
        toDBIO(actions)
      }
      // Process statement properties
      _ <- {
        val actions = new ListBuffer[DBIO[_]]
        if (communityIds.isDefined && request.statementProperties.nonEmpty) {
          request.statementProperties.foreach { updates =>
            actions += updateStatementPropertiesViaApi(updates, communityIds.get._1, communityIds.get._2, warnings)
          }
        }
        toDBIO(actions)
      }
    } yield warnings.toList
    exec(dbAction.transactionally)
  }

  private def updateStatementPropertiesViaApi(updateData: StatementConfiguration, communityId: Long, domainId: Option[Long], warnings: ListBuffer[String]): DBIO[_] = {
    for {
      // Get relevant statement information
      statementIds <- PersistenceSchema.conformanceResults
        .join(PersistenceSchema.systems).on(_.sut === _.id)
        .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
        .join(PersistenceSchema.actors).on(_._1._1.actor === _.id)
        .filter(_._1._2.community === communityId)
        .filterOpt(domainId)((q, domainId) => q._2.domain === domainId)
        .filter(_._1._1._2.apiKey === updateData.system)
        .filter(_._2.apiKey === updateData.actor)
        .map(x => (x._1._1._1.sut, x._1._1._1.actor)) // (system ID, actor ID)
        .result
        .headOption
      // Get configuration properties
      existingProperties <- if (statementIds.isDefined) {
        PersistenceSchema.parameters
        .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
        .filter(_._2.actor === statementIds.get._2)
        .map(x => (x._1.id, x._1.endpoint, x._1.testKey, x._1.kind))
        .result
        .map { properties =>
          val keyMap = new mutable.HashMap[String, (Long, Long, String)]() // property key to (property ID, endpoint ID, property type)
          properties.foreach { property =>
            keyMap += (property._3 -> (property._1, property._2, property._4))
          }
          keyMap.toMap
        }
      } else {
        warnings += "No conformance statement defined for system [%s] and actor [%s].".formatted(updateData.system, updateData.actor)
        DBIO.successful(new mutable.HashMap[String, (Long, Long, String)]())
      }
      // Load the properties for which the system has existing values
      existingValues <- if (statementIds.isDefined) {
        PersistenceSchema.configs
          .join(PersistenceSchema.parameters).on(_.parameter === _.id)
          .join(PersistenceSchema.endpoints).on(_._2.endpoint === _.id)
          .filter(_._1._1.system === statementIds.get._1)
          .filter(_._2.actor === statementIds.get._2)
          .map(x => (x._1._2.testKey, x._1._2.id, x._1._2.endpoint))
          .result
          .map { properties =>
            val keyMap = new mutable.HashMap[String, (Long, Long)]() // property key to (property ID, endpoint ID)
            properties.foreach { property =>
              keyMap += (property._1 -> (property._2, property._3))
            }
            keyMap.toMap
          }
      } else {
        DBIO.successful(new mutable.HashMap[String, (Long, Long)]())
      }
      // Process updates
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (statementIds.isDefined) {
          updateData.properties.foreach { configData =>
            if (existingProperties.contains(configData.key)) {
              if (existingProperties(configData.key)._3 == "SIMPLE") {
                if (existingValues.contains(configData.key)) {
                  if (configData.value.isDefined) {
                    // Update
                    actions += PersistenceSchema.configs
                      .filter(_.system === statementIds.get._1)
                      .filter(_.parameter === existingValues(configData.key)._1)
                      .map(_.value)
                      .update(configData.value.get)
                  } else {
                    // Delete
                    actions += PersistenceSchema.configs
                      .filter(_.system === statementIds.get._1)
                      .filter(_.parameter === existingValues(configData.key)._1)
                      .delete
                  }
                } else {
                  if (configData.value.isDefined) {
                    // Insert
                    actions += (PersistenceSchema.configs += Configs(statementIds.get._1, existingProperties(configData.key)._1, existingProperties(configData.key)._2, configData.value.get, None))
                  } else {
                    warnings += "Ignoring delete for conformance statement property [%s] of system [%s] for actor [%s]. No value for this property was defined.".formatted(configData.key, updateData.system, updateData.actor)
                  }
                }
              } else {
                warnings += "Ignoring update for conformance statement property [%s] of system [%s] for actor [%s]. Only simple properties can be updated via the automation API.".formatted(configData.key, updateData.system, updateData.actor)
              }
            } else {
              warnings += "Ignoring update for conformance statement property [%s] of system [%s] for actor [%s]. No property with that key is defined for the conformance statement.".formatted(configData.key, updateData.system, updateData.actor)
            }
          }
        }
        toDBIO(actions)
      }
    } yield ()
  }

  private def updateOrganisationParametersViaApi(updateData: PartyConfiguration, communityId: Long, warnings: ListBuffer[String]): DBIO[_] = {
    for {
      // Load organisation ID
      organisationId <- PersistenceSchema.organizations
        .filter(_.community === communityId)
        .filter(_.apiKey === updateData.partyKey)
        .map(_.id)
        .result
        .headOption
      // Load the properties for which the organisation has existing values
      existingValues <- if (organisationId.isDefined) {
        PersistenceSchema.organisationParameterValues
          .join(PersistenceSchema.organisationParameters).on(_.parameter === _.id)
          .filter(_._1.organisation === organisationId.get)
          .map(x => (x._2.id, x._2.testKey))
          .result
          .map { properties =>
            val keyMap = new mutable.HashMap[String, Long]() // property key to property ID
            properties.foreach { property =>
              keyMap += (property._2 -> property._1)
            }
            keyMap.toMap
        }
      } else {
        warnings += "No organisation was found for API Key [%s]".formatted(updateData.partyKey)
        DBIO.successful(new mutable.HashMap[String, Long]())
      }
      // Load the community's properties and record their type
      existingProperties <- if (organisationId.isDefined) {
        PersistenceSchema.organisationParameters
          .filter(_.community === communityId)
          .map(x => (x.id, x.testKey, x.kind))
          .result
          .map { properties =>
            val keyMap = new mutable.HashMap[String, (Long, String)]() // property key to (property ID, property type)
            properties.foreach { property =>
              keyMap += (property._2 -> (property._1, property._3))
            }
            keyMap.toMap
          }
      } else {
        DBIO.successful(new mutable.HashMap[String, (Long, String)]())
      }
      // Process the updates
      _ <- {
        val actions = new ListBuffer[DBIO[_]]
        if (organisationId.isDefined) {
          updateData.properties.foreach { configData =>
            if (existingProperties.contains(configData.key)) {
              if (existingProperties(configData.key)._2 == "SIMPLE") {
                if (existingValues.contains(configData.key)) {
                  if (configData.value.isDefined) {
                    // Update
                    actions += PersistenceSchema.organisationParameterValues
                      .filter(_.organisation === organisationId.get)
                      .filter(_.parameter === existingValues(configData.key))
                      .map(_.value)
                      .update(configData.value.get)
                  } else {
                    // Delete
                    actions += PersistenceSchema.organisationParameterValues
                      .filter(_.organisation === organisationId.get)
                      .filter(_.parameter === existingValues(configData.key))
                      .delete
                  }
                } else {
                  if (configData.value.isDefined) {
                    // Insert
                    actions += (PersistenceSchema.organisationParameterValues += OrganisationParameterValues(organisationId.get, existingProperties(configData.key)._1, configData.value.get, None))
                  } else {
                    warnings += "Ignoring delete for organisation property [%s] and organisation [%s]. No value for this property was defined for the organisation.".formatted(configData.key, updateData.partyKey)
                  }
                }
              } else {
                warnings += "Ignoring update for organisation property [%s] and organisation [%s]. Only simple properties can be updated via the automation API.".formatted(configData.key, updateData.partyKey)
              }
            } else {
              warnings += "Ignoring update for organisation property [%s] and organisation [%s]. No organisation property with that key is configured for the community.".formatted(configData.key, updateData.partyKey)
            }
          }
        }
        toDBIO(actions)
      }
    } yield ()
  }

  private def updateSystemParametersViaApi(updateData: PartyConfiguration, communityId: Long, warnings: ListBuffer[String]): DBIO[_] = {
    for {
      // Load system ID
      systemId <- PersistenceSchema.systems
        .join(PersistenceSchema.organizations).on(_.owner === _.id)
        .filter(_._2.community === communityId)
        .filter(_._1.apiKey === updateData.partyKey)
        .map(_._1.id)
        .result
        .headOption
      // Load the properties for which the organisation has existing values
      existingValues <- if (systemId.isDefined) {
        PersistenceSchema.systemParameterValues
          .join(PersistenceSchema.systemParameters).on(_.parameter === _.id)
          .filter(_._1.system === systemId.get)
          .map(x => (x._2.id, x._2.testKey))
          .result
          .map { properties =>
            val keyMap = new mutable.HashMap[String, Long]() // property key to property ID
            properties.foreach { property =>
              keyMap += (property._2 -> property._1)
            }
            keyMap.toMap
          }
      } else {
        warnings += "No system was found for API Key [%s]".formatted(updateData.partyKey)
        DBIO.successful(new mutable.HashMap[String, Long]())
      }
      // Load the community's properties and record their type
      existingProperties <- if (systemId.isDefined) {
        PersistenceSchema.systemParameters
          .filter(_.community === communityId)
          .map(x => (x.id, x.testKey, x.kind))
          .result
          .map { properties =>
            val keyMap = new mutable.HashMap[String, (Long, String)]() // property key to (property ID, property type)
            properties.foreach { property =>
              keyMap += (property._2 -> (property._1, property._3))
            }
            keyMap.toMap
          }
      } else {
        DBIO.successful(new mutable.HashMap[String, (Long, String)]())
      }
      // Process the updates
      _ <- {
        val actions = new ListBuffer[DBIO[_]]
        if (systemId.isDefined) {
          updateData.properties.foreach { configData =>
            if (existingProperties.contains(configData.key)) {
              if (existingProperties(configData.key)._2 == "SIMPLE") {
                if (existingValues.contains(configData.key)) {
                  if (configData.value.isDefined) {
                    // Update
                    actions += PersistenceSchema.systemParameterValues
                      .filter(_.system === systemId.get)
                      .filter(_.parameter === existingValues(configData.key))
                      .map(_.value)
                      .update(configData.value.get)
                  } else {
                    // Delete
                    actions += PersistenceSchema.systemParameterValues
                      .filter(_.system === systemId.get)
                      .filter(_.parameter === existingValues(configData.key))
                      .delete
                  }
                } else {
                  if (configData.value.isDefined) {
                    // Insert
                    actions += (PersistenceSchema.systemParameterValues += SystemParameterValues(systemId.get, existingProperties(configData.key)._1, configData.value.get, None))
                  } else {
                    warnings += "Ignoring delete for system property [%s] and system [%s]. No value for this property was defined for the system.".formatted(configData.key, updateData.partyKey)
                  }
                }
              } else {
                warnings += "Ignoring update for system property [%s] and system [%s]. Only simple properties can be updated via the automation API.".formatted(configData.key, updateData.partyKey)
              }
            } else {
              warnings += "Ignoring update for system property [%s] and system [%s]. No system property with that key is configured for the community.".formatted(configData.key, updateData.partyKey)
            }
          }
        }
        toDBIO(actions)
      }
    } yield ()
  }

  private def checkOrganisationParameterExistence(communityId: Long, propertyKey: String, expectedToExist: Boolean, propertyIdToIgnore: Option[Long]): DBIO[Option[OrganisationParameters]] = {
    for {
      property <- PersistenceSchema.organisationParameters
        .filter(_.community === communityId)
        .filter(_.testKey === propertyKey)
        .filterOpt(propertyIdToIgnore)((q, id) => q.id =!= id)
        .result
        .headOption
      _ <- {
        if (property.isDefined && !expectedToExist) {
          throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "A property with name [%s] already exists in the target community".formatted(propertyKey))
        } else if (property.isEmpty && expectedToExist) {
          throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "No property with name [%s] exists in the target community".formatted(propertyKey))
        } else {
          DBIO.successful(())
        }
      }
    } yield property
  }

  private def checkSystemParameterExistence(communityId: Long, propertyKey: String, expectedToExist: Boolean, propertyIdToIgnore: Option[Long]): DBIO[Option[SystemParameters]] = {
    for {
      property <- PersistenceSchema.systemParameters
        .filter(_.community === communityId)
        .filter(_.testKey === propertyKey)
        .filterOpt(propertyIdToIgnore)((q, id) => q.id =!= id)
        .result
        .headOption
      _ <- {
        if (property.isDefined && !expectedToExist) {
          throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "A property with name [%s] already exists in the target community".formatted(propertyKey))
        } else if (property.isEmpty && expectedToExist) {
          throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "No property with name [%s] exists in the target community".formatted(propertyKey))
        } else {
          DBIO.successful(())
        }
      }
    } yield property
  }

  def createOrganisationParameterDefinitionThroughAutomationApi(communityApiKey: String, input: CustomPropertyInfo): Unit = {
    val dbAction = for {
      // Load community ID.
      communityId <- automationApiHelper.getCommunityByCommunityApiKey(communityApiKey)
      // Check for existing property with provided name.
      _ <- checkOrganisationParameterExistence(communityId, input.key, expectedToExist = false, None)
      // If this depends on another property check that it exists.
      dependency <- checkDependedOrganisationParameterExistence(communityId, input.dependsOn.flatten, None)
      // Create property.
      _ <- {
        val dependsOnStatus = automationApiHelper.propertyDependsOnStatus(input, dependency.flatMap(_.allowedValues))
        createOrganisationParameterInternal(OrganisationParameters(0L,
          input.name.getOrElse(input.key),
          input.key,
          input.description.flatten,
          automationApiHelper.propertyUseText(input.required),
          "SIMPLE",
          !input.editableByUsers.getOrElse(true),
          !input.inTests.getOrElse(false),
          input.inExports.getOrElse(false),
          input.inSelfRegistration.getOrElse(false),
          input.hidden.getOrElse(false),
          automationApiHelper.propertyAllowedValuesText(input.allowedValues.flatten),
          input.displayOrder.getOrElse(0),
          dependsOnStatus._1.flatten,
          dependsOnStatus._2.flatten,
          automationApiHelper.propertyDefaultValue(input.defaultValue.flatten, input.allowedValues.flatten),
          communityId
        ))
      }
    } yield ()
    exec(dbAction.transactionally)
  }

  def createSystemParameterDefinitionThroughAutomationApi(communityApiKey: String, input: CustomPropertyInfo): Unit = {
    val dbAction = for {
      // Load community ID.
      communityId <- automationApiHelper.getCommunityByCommunityApiKey(communityApiKey)
      // Check for existing property with provided name.
      _ <- checkSystemParameterExistence(communityId, input.key, expectedToExist = false, None)
      // If this depends on another property check that it exists.
      dependency <- checkDependedSystemParameterExistence(communityId, input.dependsOn.flatten, None)
      // Create property.
      _ <- {
        val dependsOnStatus = automationApiHelper.propertyDependsOnStatus(input, dependency.flatMap(_.allowedValues))
        createSystemParameterInternal(SystemParameters(0L,
          input.name.getOrElse(input.key),
          input.key,
          input.description.flatten,
          automationApiHelper.propertyUseText(input.required),
          "SIMPLE",
          !input.editableByUsers.getOrElse(true),
          !input.inTests.getOrElse(false),
          input.inExports.getOrElse(false),
          input.hidden.getOrElse(false),
          automationApiHelper.propertyAllowedValuesText(input.allowedValues.flatten),
          input.displayOrder.getOrElse(0),
          dependsOnStatus._1.flatten,
          dependsOnStatus._2.flatten,
          automationApiHelper.propertyDefaultValue(input.defaultValue.flatten, input.allowedValues.flatten),
          communityId
        ))
      }
    } yield ()
    exec(dbAction.transactionally)
  }

  def getCommunityIdOfDomain(domainId: Long): Option[Long] = {
    val communityIds = exec(
      PersistenceSchema.communities
        .filter(_.domain === domainId)
        .map(_.id)
        .result
    )
    if (communityIds.size == 1) {
      Some(communityIds.head)
    } else {
      None
    }
  }

  def getCommunityIdOfActor(actorId: Long): Option[Long] = {
    val communityIds = exec(
      PersistenceSchema.communities
        .join(PersistenceSchema.actors).on(_.domain === _.domain)
        .filter(_._2.id === actorId)
        .map(_._1.id)
        .result
    )
    if (communityIds.size == 1) {
      Some(communityIds.head)
    } else {
      None
    }
  }

  def getCommunityIdOfSnapshot(snapshotId: Long): Option[Long] = {
    exec(
      PersistenceSchema.conformanceSnapshots
      .filter(_.id === snapshotId)
      .map(_.community)
      .result
      .headOption
    )
  }

}