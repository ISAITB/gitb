package managers

import config.Configurations
import models.{Enums, Token, Users}
import org.apache.commons.lang3.RandomStringUtils
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import persistence.cache.TokenCache
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider
import scala.concurrent.ExecutionContext.Implicits.global

import javax.inject.{Inject, Singleton}

@Singleton
class AuthenticationManager @Inject()(dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("AuthManager")

  def checkEmailAvailability(email:String, organisationId: Option[Long], communityId: Option[Long], roleId: Option[Short]): Boolean = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      if (organisationId.isDefined) {
        // We should not have a member user with the same email address within the organisation
        var q = PersistenceSchema.users
          .filter(_.ssoEmail === email)
          .filter(_.organization === organisationId.get)
        if (roleId.isDefined) {
          q = q.filter(_.role === roleId.get)
        }
        exec(q.result.headOption).isEmpty
      } else {
        if (communityId.isDefined) {
          // We should not have multiple community admins in the same community with the same email.
          val q = PersistenceSchema.users
            .join(PersistenceSchema.organizations).on(_.organization === _.id)
            .filter(_._1.ssoEmail === email)
            .filter(_._2.community === communityId.get)
            .filter(_._1.role === Enums.UserRole.CommunityAdmin.id.toShort)
          exec(q.result.headOption).isEmpty
        } else {
          // We should not have multiple system admins with the same email.
          val q = PersistenceSchema.users
            .filter(_.ssoEmail === email)
            .filter(_.role === Enums.UserRole.SystemAdmin.id.toShort)
          exec(q.result.headOption).isEmpty
        }
      }
    } else {
      // The email is a username and needs to be unique across the entire test bed.
      val q = PersistenceSchema.users.filter(_.email === email)
      exec(q.result.headOption).isEmpty
    }
  }

  def checkUserByEmail(email:String, passwd:String): Option[Users] = {
    val user = exec(PersistenceSchema.users.filter(_.email === email).result.headOption)
    if (user.isDefined && BCrypt.checkpw(passwd, user.get.password)) user else None
  }

  def replaceOnetimePassword(email: String, newPassword: String, oldPassword: String): Option[Long] = {
    val q = for {
      userData <- PersistenceSchema.users
        .filter(_.email === email)
        .filter(_.onetimePassword === true)
        .map(x => (x.id, x.password)).result.headOption
      resultUserId <- {
        if (userData.isDefined) {
          if (BCrypt.checkpw(oldPassword, userData.get._2)) {
            // Old password matches - do update
            val update = for { u <- PersistenceSchema.users.filter(_.id === userData.get._1)} yield (u.password, u.onetimePassword)
            update.update(BCrypt.hashpw(newPassword, BCrypt.gensalt()), false) andThen
              DBIO.successful(Some(userData.get._1))
          } else {
            DBIO.successful(None)
          }
        } else {
          DBIO.successful(None)
        }
      }
    } yield resultUserId
    exec(q.transactionally)
  }


  def generateTokens(userId:Long): Token = {
    //1) Create access and refresh tokens
    val access_token  = RandomStringUtils.randomAlphanumeric(Configurations.TOKEN_LENGTH)
    val tokens = Token(access_token)

    //2) Persist new access & refresh token information
    TokenCache.saveOAuthTokens(userId, tokens)
    tokens
  }

}
