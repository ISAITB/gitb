package managers

import config.Configurations
import exceptions._
import models.Enums.UserRole.UserRole
import models.Enums._
import models._
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.EmailUtil

import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AccountManager @Inject()(dbConfigProvider: DatabaseConfigProvider, landingPageManager: LandingPageManager, legalNoticeManager: LegalNoticeManager, errorTemplateManager: ErrorTemplateManager) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("AccountManager")

  def disconnectAccount(userId: Long, uid: String) = {
    val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.ssoUid, u.ssoStatus)
    exec(q.update(None, UserSSOStatus.NotLinked.id.toShort).transactionally)
  }

  def migrateAccount(userId: Long, userInfo: ActualUserInfo) = {
    val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.ssoUid, u.ssoEmail, u.name, u.ssoStatus)
    exec(q.update(Some(userInfo.uid), Some(userInfo.email), userInfo.firstName+" "+userInfo.lastName, UserSSOStatus.Linked.id.toShort).transactionally)
  }

  def getUnlinkedUserAccountsForEmail(email: String): List[UserAccount] = {
    val results = exec(
      PersistenceSchema.users
        .join(PersistenceSchema.organizations).on(_.organization === _.id)
        .join(PersistenceSchema.communities).on(_._2.community === _.id)
        .filter(_._1._1.ssoEmail.toLowerCase === email.toLowerCase)
        .filter(_._1._1.ssoUid.isEmpty)
        .map(x => (
          x._1._1.id, x._1._1.name, x._1._1.email, x._1._1.role, // User info
          x._1._2.id, x._1._2.shortname, x._1._2.fullname, x._1._2.adminOrganization, // Organisation info
          x._2.id, x._2.shortname, x._2.fullname) // Community info
        )
        .result
        .map(_.toList)
    )
      .map(x => new UserAccount(
        Users(x._1, x._2, x._3, null, onetimePassword = false, x._4, x._5, None, None, UserSSOStatus.NotLinked.id.toShort),
        Organizations(x._5, x._6, x._7, -1, x._8, null, null, null, template = false, None, None, x._9),
        Communities(x._9, x._10, x._11, None, -1, None, None, selfRegNotification = false, interactionNotification = false, None, SelfRegistrationRestriction.NoRestriction.id.toShort, selfRegForceTemplateSelection = false, selfRegForceRequiredProperties = false,
          allowCertificateDownload = false, allowStatementManagement = false, allowSystemManagement = false,
          allowPostTestOrganisationUpdates = false, allowPostTestSystemUpdates = false, allowPostTestStatementUpdates = false,
          allowAutomationApi = false, "", None,
          None)
      ))
    results.sorted
  }

  def linkAccountInternal(userId: Long, userInfo: ActualUserInfo): DBIO[_] = {
    val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.ssoUid, u.name, u.ssoStatus)
    q.update(Some(userInfo.uid), userInfo.firstName+" "+userInfo.lastName, UserSSOStatus.Linked.id.toShort)
  }

  def linkAccount(userId: Long, userInfo: ActualUserInfo) = {
    exec(linkAccountInternal(userId, userInfo).transactionally)
  }

  def getUserAccountsForUid(uid: String): List[UserAccount] = {
    val results = exec(
      PersistenceSchema.users
        .join(PersistenceSchema.organizations).on(_.organization === _.id)
        .join(PersistenceSchema.communities).on(_._2.community === _.id)
        .filter(_._1._1.ssoUid === uid)
        .map(x => (
          x._1._1.id, x._1._1.name, x._1._1.email, x._1._1.role, // User info
          x._1._2.id, x._1._2.shortname, x._1._2.fullname, x._1._2.adminOrganization, // Organisation info
          x._2.id, x._2.shortname, x._2.fullname) // Community info
        )
        .result
        .map(_.toList)
    )
    .map(x => new UserAccount(
      Users(x._1, x._2, x._3, null, onetimePassword = false, x._4, x._5, None, None, UserSSOStatus.Linked.id.toShort),
      Organizations(x._5, x._6, x._7, -1, x._8, null, null, null, template = false, None, None, x._9),
      Communities(x._9, x._10, x._11, None, -1, None, None, selfRegNotification = false, interactionNotification = false, None, SelfRegistrationRestriction.NoRestriction.id.toShort, selfRegForceTemplateSelection = false, selfRegForceRequiredProperties = false,
        allowCertificateDownload = false, allowStatementManagement = false, allowSystemManagement = false,
        allowPostTestOrganisationUpdates = false, allowPostTestSystemUpdates = false, allowPostTestStatementUpdates = false,
        allowAutomationApi = false, "", None,
        None)
    ))
    results.sorted
  }

  def getVendorProfile(userId: Long) = {
    val result = exec(for {
      organisation <- PersistenceSchema.users
        .join(PersistenceSchema.organizations).on(_.organization === _.id)
        .filter(_._1.id === userId)
        .map(_._2)
        .result
        .head
      landingPage <- {
        if (organisation.landingPage.isDefined) {
          landingPageManager.getLandingPageByIdInternal(organisation.landingPage.get)
        } else {
          landingPageManager.getCommunityDefaultLandingPageInternal(organisation.community)
        }
      }
      legalNotice <- {
        if (organisation.legalNotice.isDefined) {
          legalNoticeManager.getLegalNoticeByIdInternal(organisation.legalNotice.get)
        } else {
          DBIO.successful(None)
        }
      }
      errorTemplate <- {
        if (organisation.errorTemplate.isDefined) {
          errorTemplateManager.getErrorTemplateByIdInternal(organisation.errorTemplate.get)
        } else {
          DBIO.successful(None)
        }
      }
    } yield (organisation, landingPage, legalNotice, errorTemplate))
    new Organization(result._1, result._2.orNull, result._3.orNull, result._4.orNull)
  }

  def registerUser(adminId: Long, user: Users) = {
    exec(
      (for {
        //1) Get organization id of the admin
        orgId <- PersistenceSchema.users.filter(_.id === adminId).result.headOption
        //2) Insert new user to Users table
        _ <- PersistenceSchema.insertUser += user.withOrganizationId(orgId.get.organization)
      } yield ()).transactionally
    )
  }

  def getUserProfile(userId: Long): User = {
    //1) Get User info
    val u = exec(PersistenceSchema.users.filter(_.id === userId).result.headOption).get
    //2) Get Organization info
    val o = exec(PersistenceSchema.organizations.filter(_.id === u.organization).result.headOption).get
    //3) Merge User and Organization info
    val user = new User(u, o)
    user
  }

  def updateUserProfile(userId: Long, name: Option[String], password: Option[String], oldpassword: Option[String]) = {
    val actions = new ListBuffer[DBIO[_]]()
    //1) Update name of the user
    if (name.isDefined) {
      val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name)
      actions += q.update(name.get)
    }
    //2) Update password of the user (passwords must be different
    if (password.isDefined && oldpassword.isDefined && (password.get.trim != oldpassword.get.trim)) {
      //2.1) but first, check his old password if it is correct
      val user = exec(PersistenceSchema.users.filter(_.id === userId).result.headOption)
      if (user.isDefined && BCrypt.checkpw(oldpassword.get.trim, user.get.password.trim)) {
        //2.1.1) password correct, replace it with the new one
        val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.password, u.onetimePassword)
        actions += q.update(BCrypt.hashpw(password.get.trim, BCrypt.gensalt()), false)
      } else {
        //2.1.2) incorrect password => send Invalid Credentials error
        throw InvalidRequestException(ErrorCodes.INVALID_CREDENTIALS, "Invalid credentials")
      }
    }
    if (actions.nonEmpty) {
      exec(DBIO.seq(actions.toList.map(a => a): _*).transactionally)
    }
  }

  def getVendorUsers(userId: Long): List[Users] = {
    //1) Get organization id of the user first
    val orgId = exec(PersistenceSchema.users.filter(_.id === userId).result.headOption).get.organization

    //2) Get all users of the organization
    val users = exec(PersistenceSchema.users.filter(_.organization === orgId)
      .sortBy(x => (x.role.asc, x.name.asc))
      .result
      .map(_.toList))
    users
  }

  def isAdmin(userId: Long) = checkUserRole(userId, UserRole.VendorAdmin, UserRole.SystemAdmin, UserRole.CommunityAdmin)

  def isVendorAdmin(userId: Long) = checkUserRole(userId, UserRole.VendorAdmin)

  def isSystemAdmin(userId: Long) = checkUserRole(userId, UserRole.SystemAdmin)

  def isCommunityAdmin(userId: Long, communityId: Long): Boolean = {
    getCommunityAdministrators(communityId).map(u => u.id).contains(userId)
  }

  /**
    * Gets all community administrators of the given community
    */
  private def getCommunityAdministrators(communityId: Long): List[Users] = {
    val organizations = exec(PersistenceSchema.organizations.filter(_.community === communityId).map(_.id).result.map(_.toList))
    val users = exec(PersistenceSchema.users.filter(_.organization inSet organizations).filter(_.role === UserRole.CommunityAdmin.id.toShort)
      .sortBy(_.name.asc)
      .result
      .map(_.toList))
    users
  }

  def isVendorAdmin(userId: Long, organisationId: Long): Boolean = {
    val user = getUserById(userId)
    user.role == UserRole.VendorAdmin.id.toShort && user.organization.get.id == organisationId
  }

  /**
    * Gets user with specified id
    */
  private def getUserById(userId: Long): User = {
    val u = exec(PersistenceSchema.users.filter(_.id === userId).result.head)
    val o = exec(PersistenceSchema.organizations.filter(_.id === u.organization).result.head)
    val user = new User(u, o)
    user
  }

  def checkUserRole(userId: Long, roles: UserRole*): Boolean = {
    val option = exec(PersistenceSchema.users.filter(_.id === userId).result.headOption)
    option.isDefined && (roles.map(r => r.id.toShort) contains option.get.role)
  }

  /**
    * Gets community with specified id
    */
  private def getCommunityById(communityId: Long): Community = {
    val c = exec(PersistenceSchema.communities.filter(_.id === communityId).result.head)
    val d = exec(PersistenceSchema.domains.filter(_.id === c.domain).result.headOption)
    val community = new Community(c, d)
    community
  }

  def submitFeedback(userId: Option[Long], userEmail: String, messageTypeId: String, messageTypeDescription: String, messageContent: String, attachments: Array[AttachmentType]): Unit = {
    val subject = "Test Bed feedback form submission"
    var content = "<h2>Message information</h2>"
    var toAddresses: Array[String] = Configurations.EMAIL_TO.getOrElse(Array.empty)
    var ccAddresses: Array[String] = null
    if (userId.isDefined) {
      val user = getUserProfile(userId.get)
      var community: Community = null
      if (user.organization.isDefined) {
        community = getCommunityById(user.organization.get.community)
      }
      content += s"<b>User:</b> ${user.name} [$userEmail]<br/>"
      if (user.organization.isDefined) {
        content += s"<b>Organisation:</b> ${user.organization.get.fullname}<br/>"
      }
      if (community != null) {
        content += s"<b>Community:</b> ${community.fullname}<br/>"
        if (community.supportEmail.isDefined) {
          toAddresses = Array[String](community.supportEmail.get)
          if (Configurations.EMAIL_CONTACT_FORM_COPY_DEFAULT_MAILBOX.getOrElse(true) && Configurations.EMAIL_TO.isDefined) {
            ccAddresses = Configurations.EMAIL_TO.get
          }
        }
      }
    } else {
      // Form submission before an account is selected
      content += s"<b>User: </b>$userEmail<br/>"
    }
    content += s"<b>Message type:</b> $messageTypeId - $messageTypeDescription<br/>"
    content += "<h2>Message content</h2>"
    content += s"<p>$messageContent</p>"
    EmailUtil.sendEmail(toAddresses, ccAddresses, subject, content, attachments)
  }
}
