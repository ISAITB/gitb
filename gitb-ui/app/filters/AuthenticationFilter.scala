package filters

import akka.stream.Materializer
import com.gitb.utils.HmacUtils
import config.Configurations
import config.Configurations.API_ROOT
import controllers.util.{Parameters, ResponseConstructor}
import exceptions._

import javax.inject.Inject
import org.slf4j.{Logger, LoggerFactory}
import managers.AccountManager
import models.Constants
import persistence.cache.TokenCache
import play.api.mvc._
import play.mvc.Http.HeaderNames._

import scala.concurrent.{ExecutionContext, Future}

class AuthenticationFilter @Inject() (implicit ec: ExecutionContext, implicit val mat: Materializer, accountManager: AccountManager) extends Filter {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthenticationFilter])

  val BEARER = "Bearer"

  def apply(next: (RequestHeader) => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    //if public service called, execute it immediately
    if (isPublic(requestHeader)){
      next(requestHeader)
    } else {
      // Check Authorization headers
      val authzHeader = requestHeader.headers.get(AUTHORIZATION)
      if(authzHeader.isDefined){
        try {
          //parse access token info
          val list = authzHeader.get.split(BEARER + " ")

          if(list.length == 2){
            val accessToken = list(1)
            //check if access token exists for any user
            val userId = TokenCache.checkAccessToken(accessToken)
            //a workaround of customizing request headers to add our userId data, so that controllers can process it
            val customHeaders = requestHeader.headers.add((Parameters.USER_ID,  "" + userId))
            val customRequestHeader = requestHeader.withHeaders(customHeaders)
            next(customRequestHeader)
          } else{
            //There is a problem with the authorization header
            Future{
              ResponseConstructor.constructUnauthorizedResponse(ErrorCodes.INVALID_AUTHORIZATION_HEADER, "Invalid authorization header")
            }
          }
        } catch {
          // Catch-all for authorization header problems
          case e: Exception => {
            Future{
              ResponseConstructor.constructUnauthorizedResponse(ErrorCodes.INVALID_AUTHORIZATION_HEADER, "Invalid authorization header")
            }
          }
        }
      } else {
        if (isPublicWithOptionalAuthentication(requestHeader)) {
          // No authentication token but not a problem.
          next(requestHeader)
        } else {
          if (isHmacAuthenticationAllowed(requestHeader)) {
            val hmacHeader = requestHeader.headers.get(HmacUtils.HMAC_HEADER_TOKEN)
            if (hmacHeader.isDefined) {
              next(requestHeader)
            } else {
              //Requires authorization to execute this service
              logger.warn("Request blocked due to missing user or HMAC authentication token [" + requestHeader.path + "]")
              Future {
                ResponseConstructor.constructUnauthorizedResponse(ErrorCodes.AUTHORIZATION_REQUIRED, "Needs authorization header")
              }
            }
          } else if (isAutomationAccessAllowed(requestHeader)) {
            if (Configurations.AUTOMATION_API_ENABLED) {
              val apiKeyHeader = requestHeader.headers.get(Constants.AutomationHeader)
              if (apiKeyHeader.isDefined) {
                next(requestHeader)
              } else {
                //Requires authorization to execute this service
                logger.warn("Request blocked due to missing API key header [" + requestHeader.path + "]")
                Future {
                  ResponseConstructor.constructUnauthorizedResponse(ErrorCodes.AUTHORIZATION_REQUIRED, "Needs API key header")
                }
              }
            } else {
              logger.warn("Request blocked because the Test Bed's automation API is disabled [" + requestHeader.path + "]")
              Future {
                ResponseConstructor.constructUnauthorizedResponse(ErrorCodes.UNAUTHORIZED_ACCESS, "Automation API not enabled")
              }
            }
          } else {
            //Requires authorization to execute this service
            logger.warn("Request blocked due to missing user token ["+requestHeader.path+"]")
            Future{
              ResponseConstructor.constructUnauthorizedResponse(ErrorCodes.AUTHORIZATION_REQUIRED, "Needs authorization header")
            }
          }
        }
      }
    }
  }

  def isAutomationAccessAllowed(request:RequestHeader): Boolean = {
    request.path.startsWith("/"+API_ROOT+"/rest/")
  }

  def isHmacAuthenticationAllowed(request:RequestHeader):Boolean = {
    request.path.startsWith("/"+API_ROOT+"/repository/")
  }

  def isPublic(request:RequestHeader):Boolean = {
    //public services
    request.method.equals("OPTIONS") ||
      request.path.equals("/") ||
      request.path.equals("/app") ||
      request.path.equals("/"+API_ROOT+"/app/configuration") ||
      request.path.equals("/"+API_ROOT+"/notices/tbdefault") ||
      request.path.equals("/"+API_ROOT+"/user/selfreg") ||
      request.path.startsWith("/"+API_ROOT+"/sso/") ||
      request.path.startsWith("/"+API_ROOT+"/oauth/") ||
      request.path.startsWith("/"+API_ROOT+"/theme/") ||
      request.path.equals("/"+API_ROOT+"/initdata") ||
      //public assets
      request.path.startsWith("/assets/") ||
      request.path.startsWith("/webjars/") ||
      request.path.startsWith("/template/") ||
      request.path.equals("/favicon.ico") ||
      // CAS callback
      request.path.equals("/callback")
  }

  def isPublicWithOptionalAuthentication(request:RequestHeader):Boolean = {
    request.path.equals("/"+API_ROOT+"/user/feedback")
  }

}