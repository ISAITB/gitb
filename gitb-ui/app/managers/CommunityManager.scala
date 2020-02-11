package managers

import javax.inject.{Inject, Singleton}
import models.Enums._
import models._
import org.slf4j.LoggerFactory
import persistence.AccountManager
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class CommunityManager @Inject() (testResultManager: TestResultManager, organizationManager: OrganizationManager, landingPageManager: LandingPageManager, legalNoticeManager: LegalNoticeManager, errorTemplateManager: ErrorTemplateManager, conformanceManager: ConformanceManager, accountManager: AccountManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("CommunityManager")

  def isSelfRegTokenUnique(token: String, communityIdToIgnore: Option[Long]): Boolean = {
    var q = PersistenceSchema.communities.filter(_.selfRegToken === token)
    if (communityIdToIgnore.isDefined) {
      q = q.filter(_.id =!= communityIdToIgnore.get)
    }
    val result = exec(q.result.headOption)
    result.isEmpty
  }

  def selfRegister(organisation: Organizations, organisationAdmin: Users, templateId: Option[Long], actualUserInfo: Option[ActualUserInfo]): Long = {
    val userId = exec(
      (
        for {
          organisationId <- organizationManager.createOrganizationInTrans(organisation, templateId, None, true, true, true)
          userId <- PersistenceSchema.insertUser += organisationAdmin.withOrganizationId(organisationId)
          _ <-
            if (actualUserInfo.isDefined) {
              accountManager.linkAccountInternal(userId, actualUserInfo.get)
            } else {
              DBIO.successful(())
            }
        } yield userId
        ).transactionally
    )
    userId
  }

  /**
    * Gets all communities with given ids or all if none specified
    */
  def getCommunities(ids: Option[List[Long]]): List[Communities] = {
    val q = ids match {
      case Some(idList) => {
        PersistenceSchema.communities
          .filter(_.id inSet idList)
      }
      case None => {
        PersistenceSchema.communities
      }
    }
    exec(q.sortBy(_.shortname.asc)
      .result.map(_.toList))
  }

  def getSelfRegistrationOptions(): List[SelfRegOption] = {
    exec(
      PersistenceSchema.communities
        .filter(x => x.selfRegType === SelfRegistrationType.PublicListing.id.toShort || x.selfRegType === SelfRegistrationType.PublicListingWithToken.id.toShort)
        .sortBy(_.shortname.asc).result
    ).map(x => new SelfRegOption(x.id, x.shortname, x.selfRegType, organizationManager.getOrganisationTemplates(x.id), getCommunityLabels(x.id))).toList
  }

  def getById(id: Long): Option[Communities] = {
    exec(PersistenceSchema.communities.filter(_.id === id).result.headOption)
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
    exec((for {
      communityId <- PersistenceSchema.insertCommunity += community
      _ <- {
        val adminOrganization = Organizations(0L, Constants.AdminOrganizationName, Constants.AdminOrganizationName, OrganizationType.Vendor.id.toShort, true, None, None, None, false, None, communityId)
        PersistenceSchema.insertOrganization += adminOrganization
      }
    } yield ()).transactionally)
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

  /**
    * Update community
    */
  def updateCommunity(communityId: Long, shortName: String, fullName: String, supportEmail: Option[String], selfRegType: Short, selfRegToken: Option[String], selfRegNotification: Boolean, domainId: Option[Long]) = {
    val actions = new ListBuffer[DBIO[_]]()

    val community = exec(PersistenceSchema.communities.filter(_.id === communityId).result.headOption)

    if (community.isDefined) {
      if (!shortName.isEmpty && community.get.shortname != shortName) {
        val q = for {c <- PersistenceSchema.communities if c.id === communityId} yield (c.shortname)
        actions += q.update(shortName)
        actions += testResultManager.updateForUpdatedCommunity(communityId, shortName)
      }

      if (!fullName.isEmpty && community.get.fullname != fullName) {
        val q = for {c <- PersistenceSchema.communities if c.id === communityId} yield (c.fullname)
        actions += q.update(fullName)
      }
      val qs = for {c <- PersistenceSchema.communities if c.id === communityId} yield (c.supportEmail, c.domain, c.selfRegType, c.selfRegToken, c.selfregNotification)
      actions += qs.update(supportEmail, domainId, selfRegType, selfRegToken, selfRegNotification)

      exec(DBIO.seq(actions.map(a => a): _*).transactionally)
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
          testResultManager.updateForDeletedCommunity(communityId) andThen
          conformanceManager.deleteConformanceCertificateSettings(communityId) andThen
          deleteOrganisationParametersByCommunity(communityId) andThen
          deleteSystemParametersByCommunity(communityId) andThen
          PersistenceSchema.communityLabels.filter(_.community === communityId).delete andThen
          PersistenceSchema.communities.filter(_.id === communityId).delete
        ).transactionally
    )
  }

  def createOrganisationParameter(parameter: OrganisationParameters) = {
    exec((PersistenceSchema.organisationParameters.returning(PersistenceSchema.organisationParameters.map(_.id)) += parameter).transactionally)
  }

  def updateOrganisationParameter(parameter: OrganisationParameters) = {
    val q = for {p <- PersistenceSchema.organisationParameters if p.id === parameter.id} yield (p.description, p.use, p.kind, p.name, p.testKey, p.adminOnly, p.notForTests, p.inExports)
    exec(q.update(parameter.description, parameter.use, parameter.kind, parameter.name, parameter.testKey, parameter.adminOnly, parameter.notForTests, parameter.inExports).transactionally)
  }

  def deleteOrganisationParameterWrapper(parameterId: Long) = {
    exec(deleteOrganisationParameter(parameterId).transactionally)
  }

  def deleteOrganisationParameter(parameterId: Long) = {
    PersistenceSchema.organisationParameterValues.filter(_.parameter === parameterId).delete andThen
      PersistenceSchema.organisationParameters.filter(_.id === parameterId).delete
  }

  def deleteOrganisationParametersByCommunity(communityId: Long) = {
    // The values are already deleted as part of the organisation deletes
    PersistenceSchema.organisationParameters.filter(_.community === communityId).delete
  }

  def createSystemParameter(parameter: SystemParameters) = {
    exec((PersistenceSchema.systemParameters.returning(PersistenceSchema.systemParameters.map(_.id)) += parameter).transactionally)
  }

  def updateSystemParameter(parameter: SystemParameters) = {
    val q = for {p <- PersistenceSchema.systemParameters if p.id === parameter.id} yield (p.description, p.use, p.kind, p.name, p.testKey, p.adminOnly, p.notForTests, p.inExports)
    exec(q.update(parameter.description, parameter.use, parameter.kind, parameter.name, parameter.testKey, parameter.adminOnly, parameter.notForTests, parameter.inExports).transactionally)
  }

  def deleteSystemParameterWrapper(parameterId: Long) = {
    exec(deleteSystemParameter(parameterId).transactionally)
  }

  def deleteSystemParameter(parameterId: Long) = {
    PersistenceSchema.systemParameterValues.filter(_.parameter === parameterId).delete andThen
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
    exec(PersistenceSchema.organisationParameters
      .filter(_.community === communityId)
      .sortBy(_.name.asc)
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
    exec(PersistenceSchema.systemParameters
      .filter(_.community === communityId)
      .sortBy(_.name.asc)
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

  def setCommunityLabels(communityId: Long, labels: List[CommunityLabels]) = {
    val actions = new ListBuffer[DBIO[_]]()
    // Delete existing labels
    actions += PersistenceSchema.communityLabels.filter(_.community === communityId).delete
    // Add new labels
    labels.foreach { label =>
      actions += (PersistenceSchema.communityLabels += label)
    }
    exec(DBIO.seq(actions.map(a => a): _*).transactionally)
  }

  def getCommunityLabels(communityId: Long): List[CommunityLabels] = {
    exec(PersistenceSchema.communityLabels.filter(_.community === communityId).result).toList
  }

}