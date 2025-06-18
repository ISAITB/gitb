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
import models.{Enums, Token, Users}
import org.apache.commons.lang3.RandomStringUtils
import persistence.cache.TokenCache
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider
import utils.CryptoUtil

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticationManager @Inject()(dbConfigProvider: DatabaseConfigProvider)
                                     (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def checkEmailAvailability(email:String, organisationId: Option[Long], communityId: Option[Long], roleId: Option[Short]): Future[Boolean] = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      if (organisationId.isDefined) {
        // We should not have a member user with the same email address within the organisation
        var q = PersistenceSchema.users
          .filter(_.ssoEmail.toLowerCase === email.toLowerCase())
          .filter(_.organization === organisationId.get)
        if (roleId.isDefined) {
          q = q.filter(_.role === roleId.get)
        }
        DB.run(q.result.headOption).map(_.isEmpty)
      } else {
        if (communityId.isDefined) {
          // We should not have multiple community admins in the same community with the same email.
          val q = PersistenceSchema.users
            .join(PersistenceSchema.organizations).on(_.organization === _.id)
            .filter(_._1.ssoEmail.toLowerCase === email.toLowerCase())
            .filter(_._2.community === communityId.get)
            .filter(_._1.role === Enums.UserRole.CommunityAdmin.id.toShort)
          DB.run(q.result.headOption).map(_.isEmpty)
        } else {
          // We should not have multiple system admins with the same email.
          val q = PersistenceSchema.users
            .filter(_.ssoEmail.toLowerCase === email.toLowerCase())
            .filter(_.role === Enums.UserRole.SystemAdmin.id.toShort)
          DB.run(q.result.headOption).map(_.isEmpty)
        }
      }
    } else {
      // The email is a username and needs to be unique across the entire test bed.
      val q = PersistenceSchema.users.filter(_.email === email)
      DB.run(q.result.headOption).map(_.isEmpty)
    }
  }

  def replaceDefaultAdminPasswordIfNeeded(username: String): Future[Option[String]] = {
    DB.run(for {
      adminIdToProcess <- PersistenceSchema.users.filter(_.email === username).filter(_.onetimePassword === true).map(_.id).result.headOption
      passwordToUse <- {
        if (adminIdToProcess.isDefined) {
          val newPassword = UUID.randomUUID().toString
          PersistenceSchema.users.filter(_.id === adminIdToProcess).map(_.password).update(CryptoUtil.hashPassword(newPassword)).map(_ => Some(newPassword))
        } else {
          DBIO.successful(None)
        }
      }
    } yield passwordToUse)
  }

  def checkUserByEmail(email:String, passwd:String): Future[Option[Users]] = {
    DB.run(PersistenceSchema.users.filter(_.email === email).result.headOption).map { user =>
      if (user.isDefined && CryptoUtil.checkPassword(passwd, user.get.password)) user else None
    }
  }

  def replaceOnetimePassword(email: String, newPassword: String, oldPassword: String): Future[Option[Long]] = {
    val q = for {
      userData <- PersistenceSchema.users
        .filter(_.email === email)
        .map(x => (x.id, x.password, x.onetimePassword)).result.headOption
      resultUserId <- {
        if (userData.isDefined) {
          if (userData.get._3 || !CryptoUtil.isAcceptedPassword(oldPassword)) {
            if (CryptoUtil.checkPassword(oldPassword, userData.get._2)) {
              // Old password matches - do update
              val update = for { u <- PersistenceSchema.users.filter(_.id === userData.get._1)} yield (u.password, u.onetimePassword)
              update.update(CryptoUtil.hashPassword(newPassword), false).map { _ =>
                Some(userData.get._1)
              }
            } else {
              DBIO.successful(None)
            }
          } else {
            DBIO.successful(None)
          }
        } else {
          DBIO.successful(None)
        }
      }
    } yield resultUserId
    DB.run(q.transactionally)
  }


  def generateTokens(userId:Long): Token = {
    //1) Create access and refresh tokens
    val access_token  = RandomStringUtils.secure().nextAlphanumeric(Configurations.TOKEN_LENGTH)
    val tokens = Token(access_token)

    //2) Persist new access & refresh token information
    TokenCache.saveOAuthTokens(userId, tokens)
    tokens
  }

}
