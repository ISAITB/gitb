package managers

import java.util

import javax.inject.{Inject, Singleton}
import models.Enums._
import models._
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class CommunityManager @Inject() (triggerHelper: TriggerHelper, testResultManager: TestResultManager, organizationManager: OrganizationManager, landingPageManager: LandingPageManager, legalNoticeManager: LegalNoticeManager, errorTemplateManager: ErrorTemplateManager, conformanceManager: ConformanceManager, accountManager: AccountManager, triggerManager: TriggerManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("CommunityManager")

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

  def isSelfRegTokenUnique(token: String, communityIdToIgnore: Option[Long]): Boolean = {
    var q = PersistenceSchema.communities.filter(_.selfRegToken === token)
    if (communityIdToIgnore.isDefined) {
      q = q.filter(_.id =!= communityIdToIgnore.get)
    }
    val result = exec(q.result.headOption)
    result.isEmpty
  }

  def selfRegister(organisation: Organizations, organisationAdmin: Users, templateId: Option[Long], actualUserInfo: Option[ActualUserInfo], customPropertyValues: Option[List[OrganisationParameterValues]], requireMandatoryPropertyValues: Boolean): Long = {
    val ids = exec(
      (
        for {
          // Save organisation
          organisationInfo <- organizationManager.createOrganizationInTrans(organisation, templateId, None, true, true, true)
          // Save custom organisation property values
          _ <- {
            if (customPropertyValues.isDefined) {
              organizationManager.saveOrganisationParameterValues(organisationInfo.organisationId, organisation.community, isAdmin = false, isSelfRegistration = true, customPropertyValues.get, requireMandatoryPropertyValues)
            } else {
              DBIO.successful(())
            }
          }
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
        ).transactionally
    )
    triggerHelper.triggersFor(organisation.community, ids._2)
    ids._1
  }

  /**
    * Gets all communities with given ids or all if none specified
    */
  def getCommunities(ids: Option[List[Long]], skipDefault: Boolean): List[Communities] = {
    var q = ids match {
      case Some(idList) => {
        PersistenceSchema.communities
          .filter(_.id inSet idList)
      }
      case None => {
        PersistenceSchema.communities
      }
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

  private def getByIdInternal(id: Long) = {
    PersistenceSchema.communities.filter(_.id === id).result.headOption
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

  /**
    * Creates new community
    */
  def createCommunity(community: Communities) = {
    exec(createCommunityInternal(community).transactionally)
  }

  def createCommunityInternal(community: Communities) = {
    for {
      communityId <- PersistenceSchema.insertCommunity += community
      adminOrganisationId <- {
        val adminOrganization = Organizations(0L, Constants.AdminOrganizationName, Constants.AdminOrganizationName, OrganizationType.Vendor.id.toShort, adminOrganization = true, None, None, None, template = false, None, communityId)
        PersistenceSchema.insertOrganization += adminOrganization
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

  private def updateCommunityDomainDependencies(community: Communities, domainId: Option[Long]): DBIO[_] = {
    val actions = new ListBuffer[DBIO[_]]()
    if (community.domain.isDefined && domainId.isDefined && community.domain.get != domainId.get) {
      // New domain doesn't match previous domain. Remove conformance statements for previous domain.
      actions += conformanceManager.deleteConformanceStatementsForDomainAndCommunity(community.domain.get, community.id)
      // Remove also any trigger data that referred to domain parameters.
      actions += triggerManager.deleteTriggerDataOfCommunityAndDomain(community.id, community.domain.get)
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
          import scala.collection.JavaConverters._
          DBIO.successful((collectionAsScalaIterable(actorIds).toSet, collectionAsScalaIterable(systemIds).toSet))
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

  private[managers] def updateCommunityDomain(community: Communities, domainId: Option[Long]): DBIO[_] = {
    updateCommunityDomainDependencies(community, domainId) andThen {
      val qs = for {c <- PersistenceSchema.communities if c.id === community.id} yield c.domain
      qs.update(domainId)
    }
  }

  private[managers] def updateCommunityInternal(community: Communities, shortName: String, fullName: String, supportEmail: Option[String],
                                                selfRegType: Short, selfRegToken: Option[String], selfRegTokenHelpText: Option[String], selfRegNotification: Boolean,
                                                description: Option[String], selfRegRestriction: Short, selfRegForceTemplateSelection: Boolean, selfRegForceRequiredProperties: Boolean,
                                                allowCertificateDownload: Boolean, allowStatementManagement: Boolean, allowSystemManagement: Boolean,
                                                allowPostTestOrganisationUpdates: Boolean, allowPostTestSystemUpdates: Boolean, allowPostTestStatementUpdates: Boolean,
                                                domainId: Option[Long]) = {
    val actions = new ListBuffer[DBIO[_]]()
    if (!shortName.isEmpty && community.shortname != shortName) {
      val q = for {c <- PersistenceSchema.communities if c.id === community.id} yield (c.shortname)
      actions += q.update(shortName)
      actions += testResultManager.updateForUpdatedCommunity(community.id, shortName)
    }
    // Handle domain update (if any)
    actions += updateCommunityDomainDependencies(community, domainId)
    if (!fullName.isEmpty && community.fullname != fullName) {
      val q = for {c <- PersistenceSchema.communities if c.id === community.id} yield (c.fullname)
      actions += q.update(fullName)
    }
    val qs = for {c <- PersistenceSchema.communities if c.id === community.id} yield (
      c.supportEmail, c.domain, c.selfRegType, c.selfRegToken, c.selfRegTokenHelpText, c.selfregNotification,
      c.description, c.selfRegRestriction, c.selfRegForceTemplateSelection, c.selfRegForceRequiredProperties,
      c.allowCertificateDownload, c.allowStatementManagement, c.allowSystemManagement,
      c.allowPostTestOrganisationUpdates, c.allowPostTestSystemUpdates, c.allowPostTestStatementUpdates,
    )
    actions += qs.update(
      supportEmail, domainId, selfRegType, selfRegToken, selfRegTokenHelpText, selfRegNotification,
      description, selfRegRestriction, selfRegForceTemplateSelection, selfRegForceRequiredProperties,
      allowCertificateDownload, allowStatementManagement, allowSystemManagement,
      allowPostTestOrganisationUpdates, allowPostTestSystemUpdates, allowPostTestStatementUpdates
    )
    toDBIO(actions)
  }

  /**
    * Update community
    */
  def updateCommunity(communityId: Long, shortName: String, fullName: String, supportEmail: Option[String],
                      selfRegType: Short, selfRegToken: Option[String], selfRegTokenHelpText: Option[String],
                      selfRegNotification: Boolean, description: Option[String], selfRegRestriction: Short,
                      selfRegForceTemplateSelection: Boolean, selfRegForceRequiredProperties: Boolean,
                      allowCertificateDownload: Boolean, allowStatementManagement: Boolean, allowSystemManagement: Boolean,
                      allowPostTestOrganisationUpdates: Boolean, allowPostTestSystemUpdates: Boolean, allowPostTestStatementUpdates: Boolean,
                      domainId: Option[Long]) = {
    val community = exec(PersistenceSchema.communities.filter(_.id === communityId).result.headOption)
    if (community.isDefined) {
      exec(updateCommunityInternal(
        community.get, shortName, fullName, supportEmail, selfRegType, selfRegToken, selfRegTokenHelpText,
        selfRegNotification, description, selfRegRestriction, selfRegForceTemplateSelection, selfRegForceRequiredProperties,
        allowCertificateDownload, allowStatementManagement, allowSystemManagement,
        allowPostTestOrganisationUpdates, allowPostTestSystemUpdates, allowPostTestStatementUpdates,
        domainId
      ).transactionally)
    } else {
      throw new IllegalArgumentException("Community with ID '" + communityId + "' not found")
    }
  }

  /**
    * Deletes the community with specified id
    */
  def deleteCommunity(communityId: Long) {
    exec(
      (
        organizationManager.deleteOrganizationByCommunity(communityId) andThen
          landingPageManager.deleteLandingPageByCommunity(communityId) andThen
          legalNoticeManager.deleteLegalNoticeByCommunity(communityId) andThen
          errorTemplateManager.deleteErrorTemplateByCommunity(communityId) andThen
          triggerManager.deleteTriggersByCommunity(communityId) andThen
          testResultManager.updateForDeletedCommunity(communityId) andThen
          conformanceManager.deleteConformanceCertificateSettings(communityId) andThen
          deleteOrganisationParametersByCommunity(communityId) andThen
          deleteSystemParametersByCommunity(communityId) andThen
          PersistenceSchema.communityLabels.filter(_.community === communityId).delete andThen
          PersistenceSchema.communities.filter(_.id === communityId).delete
        ).transactionally
    )
  }

  def createOrganisationParameterInternal(parameter: OrganisationParameters) = {
    PersistenceSchema.organisationParameters.returning(PersistenceSchema.organisationParameters.map(_.id)) += parameter
  }

  def createOrganisationParameter(parameter: OrganisationParameters) = {
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

  def updateOrganisationParameterInternal(parameter: OrganisationParameters): DBIO[_] = {
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
        // Don't update display order here.
        val q = for {p <- PersistenceSchema.organisationParameters if p.id === parameter.id} yield (p.description, p.use, p.kind, p.name, p.testKey, p.adminOnly, p.notForTests, p.inExports, p.inSelfRegistration, p.hidden, p.allowedValues, p.dependsOn, p.dependsOnValue)
        q.update(parameter.description, parameter.use, parameter.kind, parameter.name, parameter.testKey, parameter.adminOnly, parameter.notForTests, parameter.inExports, parameter.inSelfRegistration, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue)
      }
    } yield ()
  }

  def updateOrganisationParameter(parameter: OrganisationParameters) = {
    exec(updateOrganisationParameterInternal(parameter).transactionally)
  }

  def deleteOrganisationParameterWrapper(parameterId: Long) = {
    exec(deleteOrganisationParameter(parameterId).transactionally)
  }

  def deleteOrganisationParameter(parameterId: Long): DBIO[_] = {
    triggerManager.deleteTriggerDataByDataType(parameterId, TriggerDataType.OrganisationParameter) andThen
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

  def deleteOrganisationParametersByCommunity(communityId: Long) = {
    // The values are already deleted as part of the organisation deletes
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

  def createSystemParameterInternal(parameter: SystemParameters) = {
    PersistenceSchema.systemParameters.returning(PersistenceSchema.systemParameters.map(_.id)) += parameter
  }

  def createSystemParameter(parameter: SystemParameters) = {
    exec(createSystemParameterInternal(parameter).transactionally)
  }

  def updateSystemParameterInternal(parameter: SystemParameters): DBIO[_] = {
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
        // Don't update display order here.
        val q = for {p <- PersistenceSchema.systemParameters if p.id === parameter.id} yield (p.description, p.use, p.kind, p.name, p.testKey, p.adminOnly, p.notForTests, p.inExports, p.hidden, p.allowedValues, p.dependsOn, p.dependsOnValue)
        q.update(parameter.description, parameter.use, parameter.kind, parameter.name, parameter.testKey, parameter.adminOnly, parameter.notForTests, parameter.inExports, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue)
      }
    } yield ()
  }

  def updateSystemParameter(parameter: SystemParameters) = {
    exec(updateSystemParameterInternal(parameter).transactionally)
  }

  def deleteSystemParameterWrapper(parameterId: Long) = {
    exec(deleteSystemParameter(parameterId).transactionally)
  }

  def deleteSystemParameter(parameterId: Long): DBIO[_] = {
    triggerManager.deleteTriggerDataByDataType(parameterId, TriggerDataType.SystemParameter) andThen
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

  def deleteSystemParametersByCommunity(communityId: Long) = {
    // The values are already deleted as part of the system deletes
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

  def getOrganisationParametersForExport(communityId: Long): List[OrganisationParameters] = {
    exec(PersistenceSchema.organisationParameters
      .filter(_.community === communityId)
      .filter(_.inExports === true)
      .filter(_.kind === "SIMPLE")
      .sortBy(_.testKey.asc)
      .result).toList
  }

  def getOrganisationParametersForSelfRegistration(communityId: Long): List[OrganisationParameters] = {
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

  def getSystemParametersForExport(communityId: Long): List[SystemParameters] = {
    exec(PersistenceSchema.systemParameters
      .filter(_.community === communityId)
      .filter(_.inExports === true)
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

  def setCommunityLabelsInternal(communityId: Long, labels: List[CommunityLabels]) = {
    val actions = new ListBuffer[DBIO[_]]()
    // Delete existing labels
    actions += PersistenceSchema.communityLabels.filter(_.community === communityId).delete
    // Add new labels
    labels.foreach { label =>
      actions += (PersistenceSchema.communityLabels += label)
    }
    toDBIO(actions)
  }

  def setCommunityLabels(communityId: Long, labels: List[CommunityLabels]) = {
    exec(setCommunityLabelsInternal(communityId, labels).transactionally)
  }

  def getCommunityLabels(communityId: Long): List[CommunityLabels] = {
    exec(PersistenceSchema.communityLabels.filter(_.community === communityId).result).toList
  }

}