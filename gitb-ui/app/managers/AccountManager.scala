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

import config.Configurations
import exceptions._
import models.Enums.UserRole.UserRole
import models.Enums._
import models._
import org.slf4j.{Logger, LoggerFactory}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{CryptoUtil, EmailUtil}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccountManager @Inject()(dbConfigProvider: DatabaseConfigProvider,
                               landingPageManager: LandingPageManager,
                               legalNoticeManager: LegalNoticeManager,
                               errorTemplateManager: ErrorTemplateManager)
                              (implicit ec: ExecutionContext)extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger: Logger = LoggerFactory.getLogger("AccountManager")

  def disconnectAccount(userId: Long): Future[Int] = {
    val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.ssoUid, u.ssoStatus)
    DB.run(q.update(None, UserSSOStatus.NotLinked.id.toShort).transactionally)
  }

  def migrateAccount(userId: Long, userInfo: ActualUserInfo): Future[Unit] = {
    val dbAction = PersistenceSchema.users
        .filter(_.id === userId)
        .map(x => (x.ssoUid, x.ssoEmail, x.name, x.ssoStatus, x.onetimePassword))
        .update((Some(userInfo.uid), Some(userInfo.email), userInfo.name, UserSSOStatus.Linked.id.toShort, false))
    DB.run(dbAction.transactionally).map { _ =>
      if (Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD && !Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD_ORIGINAL) {
        /*
         * The Test Bed was started in non-migration mode but was forced to migration mode because no migrated accounts
         * existed. In this case, upon migration of a first account, the forced migration mode is reset. The point of
         * this is essentially to allow a initial installation to be done with SSO without needing to set and then unset
         * the migration mode to migrate the initial administrator.
         */
        Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD = false
      }
    }
  }

  def getUnlinkedUserAccountsForEmail(email: String): Future[List[UserAccount]] = {
    DB.run(
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
    ).map { results =>
      results.map(x => new UserAccount(
        Users(x._1, x._2, x._3, null, onetimePassword = false, x._4, x._5, None, None, UserSSOStatus.NotLinked.id.toShort),
        Organizations(x._5, x._6, x._7, -1, x._8, null, null, null, template = false, None, None, x._9),
        Communities(x._9, x._10, x._11, None, -1, None, None, selfRegNotification = false, interactionNotification = false, None, SelfRegistrationRestriction.NoRestriction.id.toShort, selfRegForceTemplateSelection = false, selfRegForceRequiredProperties = false,
          allowCertificateDownload = false, allowStatementManagement = false, allowSystemManagement = false,
          allowPostTestOrganisationUpdates = false, allowPostTestSystemUpdates = false, allowPostTestStatementUpdates = false,
          allowAutomationApi = false, allowCommunityView = false, "", None,
          None)
      )).sorted
    }
  }

  def linkAccountInternal(userId: Long, userInfo: ActualUserInfo): DBIO[_] = {
    val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.ssoUid, u.name, u.ssoStatus)
    q.update(Some(userInfo.uid), userInfo.name, UserSSOStatus.Linked.id.toShort)
  }

  def linkAccount(userId: Long, userInfo: ActualUserInfo): Future[Unit] = {
    DB.run(linkAccountInternal(userId, userInfo).transactionally).map(_ => ())
  }

  def getUserAccountsForUid(uid: String): Future[List[UserAccount]] = {
    DB.run(
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
    ).map { results =>
      results
        .map(x => new UserAccount(
          Users(x._1, x._2, x._3, null, onetimePassword = false, x._4, x._5, None, None, UserSSOStatus.Linked.id.toShort),
          Organizations(x._5, x._6, x._7, -1, x._8, null, null, null, template = false, None, None, x._9),
          Communities(x._9, x._10, x._11, None, -1, None, None, selfRegNotification = false, interactionNotification = false, None, SelfRegistrationRestriction.NoRestriction.id.toShort, selfRegForceTemplateSelection = false, selfRegForceRequiredProperties = false,
            allowCertificateDownload = false, allowStatementManagement = false, allowSystemManagement = false,
            allowPostTestOrganisationUpdates = false, allowPostTestSystemUpdates = false, allowPostTestStatementUpdates = false,
            allowAutomationApi = false, allowCommunityView = false, "", None,
            None))
        ).sorted
    }
  }

  def getVendorProfile(userId: Long): Future[Organization] = {
    val result = DB.run(for {
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
      communityLegalNoticeAppliesAndExists <- {
        if (legalNotice.isEmpty) {
          legalNoticeManager.communityHasDefaultLegalNotice(organisation.community)
        } else {
          DBIO.successful(false)
        }
      }
      errorTemplate <- {
        if (organisation.errorTemplate.isDefined) {
          errorTemplateManager.getErrorTemplateByIdInternal(organisation.errorTemplate.get)
        } else {
          DBIO.successful(None)
        }
      }
    } yield (organisation, landingPage, legalNotice, errorTemplate, communityLegalNoticeAppliesAndExists))
    result.map { result =>
      new Organization(result._1, result._2.orNull, result._3.orNull, result._4.orNull, result._5)
    }
  }

  def registerUser(adminId: Long, user: Users): Future[Unit] = {
    DB.run(
      (for {
        //1) Get organization id of the admin
        orgId <- PersistenceSchema.users.filter(_.id === adminId).result.headOption
        //2) Insert new user to Users table
        _ <- PersistenceSchema.insertUser += user.withOrganizationId(orgId.get.organization)
      } yield ()).transactionally
    )
  }

  def getUserProfile(userId: Long): Future[User] = {
    DB.run(
      PersistenceSchema.users
        .join(PersistenceSchema.organizations).on(_.organization === _.id)
        .filter(_._1.id === userId)
        .result
        .head
    ).map { result =>
      new User(result._1, result._2)
    }
  }

  def updateUserProfile(userId: Long, name: Option[String], password: Option[String], oldPassword: Option[String]): Future[Unit] = {
    val dbAction = for {
      // Update name.
      _ <- {
        if (name.isDefined) {
          PersistenceSchema.users.filter(_.id === userId).map(_.name).update(name.get)
        } else {
          DBIO.successful(())
        }
      }
      // Check and update password.
      _ <- {
        if (password.isDefined && oldPassword.isDefined && (password.get.trim != oldPassword.get.trim)) {
          for {
            // Load current password for user.
            currentPassword <- PersistenceSchema.users
              .filter(_.id === userId)
              .map(_.password)
              .result.headOption
            _ <- {
              if (currentPassword.isDefined && CryptoUtil.checkPassword(oldPassword.get.trim, currentPassword.get)) {
                // Provided password matches.
                PersistenceSchema.users.filter(_.id === userId)
                  .map(x => (x.password, x.onetimePassword))
                  .update((CryptoUtil.hashPassword(password.get.trim), false))
              } else {
                // Invalid password.
                throw InvalidRequestException(ErrorCodes.INVALID_CREDENTIALS, "Incorrect password.")
              }
            }
          } yield ()
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
    DB.run(dbAction.transactionally)
  }

  def getVendorUsers(userId: Long): Future[List[Users]] = {
    DB.run(for {
      //1) Get organization id of the user first
      orgId <- PersistenceSchema.users.filter(_.id === userId).map(_.organization).result.headOption
      //2) Get all users of the organization
      users <- PersistenceSchema.users.filter(_.organization === orgId)
        .sortBy(x => (x.role.asc, x.name.asc))
        .result
        .map(_.toList)
    } yield users)
  }

  def isAdmin(userId: Long): Future[Boolean] = {
    checkUserRole(userId, UserRole.VendorAdmin, UserRole.SystemAdmin, UserRole.CommunityAdmin)
  }

  def isSystemAdmin(userId: Long): Future[Boolean] = {
    checkUserRole(userId, UserRole.SystemAdmin)
  }

  def checkUserRole(userId: Long, roles: UserRole*): Future[Boolean] = {
    DB.run(PersistenceSchema.users.filter(_.id === userId).result.headOption).map { user =>
      user.isDefined && (roles.map(r => r.id.toShort) contains user.get.role)
    }
  }

  private def getCommunityNameAndEmail(communityId: Long): Future[(String, Option[String])] = {
    DB.run(
      PersistenceSchema.communities
        .filter(_.id === communityId)
        .map(x => (x.fullname, x.supportEmail))
        .result
        .head
    )
  }

  def submitFeedback(userId: Option[Long], userEmail: String, messageTypeId: String, messageTypeDescription: String, messageContent: String, attachments: Array[AttachmentType]): Future[Unit] = {
    val subject = "Test Bed feedback form submission"
    val baseContent = "<h2>Message information</h2>"
    var toAddresses: Array[String] = Configurations.EMAIL_TO.getOrElse(Array.empty)
    var ccAddresses: Array[String] = null
    for {
      emailData <- {
        if (userId.isDefined) {
          getUserProfile(userId.get).flatMap { user =>
            var newContent = baseContent + s"<b>User:</b> ${user.name} [$userEmail]<br/>"
            if (user.organization.isDefined) {
              newContent += s"<b>Organisation:</b> ${user.organization.get.fullname}<br/>"
            }
            val communityInfo = if (user.organization.isDefined) {
              getCommunityNameAndEmail(user.organization.get.community).map(Some(_))
            } else {
              Future.successful(None)
            }
            communityInfo.map { communityInfo =>
              if (communityInfo.isDefined) {
                newContent += s"<b>Community:</b> ${communityInfo.get._1}<br/>"
                if (communityInfo.get._2.isDefined) {
                  toAddresses = Array[String](communityInfo.get._2.get)
                  if (Configurations.EMAIL_CONTACT_FORM_COPY_DEFAULT_MAILBOX.getOrElse(true) && Configurations.EMAIL_TO.isDefined) {
                    ccAddresses = Configurations.EMAIL_TO.get
                  }
                }
              }
              (newContent, toAddresses, Some(ccAddresses))
            }
          }
        } else {
          // Form submission before an account is selected
          val newContent = baseContent + s"<b>User: </b>$userEmail<br/>"
          Future.successful {
            (newContent, toAddresses, None)
          }
        }
      }
      _ <- {
        var finalContent = emailData._1
        finalContent += s"<b>Message type:</b> $messageTypeId - $messageTypeDescription<br/>"
        finalContent += "<h2>Message content</h2>"
        finalContent += s"<p>$messageContent</p>"
        EmailUtil.sendEmail(emailData._2, emailData._3.orNull, subject, finalContent, attachments)
        Future.successful(())
      }
    } yield ()
  }
}
