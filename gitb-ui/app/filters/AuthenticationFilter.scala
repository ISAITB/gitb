package filters

import org.apache.pekko.stream.Materializer
import com.gitb.utils.HmacUtils
import config.Configurations
import config.Configurations.{API_ROOT, WEB_CONTEXT_ROOT, WEB_CONTEXT_ROOT_WITH_SLASH}
import controllers.util.ResponseConstructor.NotFound
import controllers.util.{Parameters, ResponseConstructor}
import exceptions._
import models.Constants
import org.slf4j.{Logger, LoggerFactory}
import persistence.cache.TokenCache
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import play.api.routing.Router
import play.mvc.Http.HeaderNames._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthenticationFilter @Inject() (router: Router)
                                     (implicit ec: ExecutionContext, implicit val mat: Materializer) extends Filter {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthenticationFilter])

  val BEARER = "Bearer"

  private def downstreamHeaderFromAccessToken(accessToken: String, originalRequest: RequestHeader): RequestHeader = {
    // Check if access token exists for any user
    val userId = TokenCache.checkAccessToken(accessToken)
    // A workaround of customizing request headers to add our userId data, so that controllers can process it
    val customHeaders = originalRequest.headers.add((Parameters.USER_ID, "" + userId))
    val customRequestHeader = originalRequest.withHeaders(customHeaders)
    customRequestHeader
  }

  def apply(next: (RequestHeader) => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    if (router.handlerFor(requestHeader).isEmpty) {
      if (requestHeader.path == Configurations.WEB_CONTEXT_ROOT_WITH_SLASH) {
        next(requestHeader)
      } else {
        // If this is an unknown route stop any further processing and return a 404.
        Future.successful {
          logger.debug("Received request for non-existent path [{}]", requestHeader.path)
          NotFound("")
        }
      }
    } else {
      //if public service called, execute it immediately
      if (isPublic(requestHeader)){
        next(requestHeader)
      } else {
        // Check Authorization headers
        val authzHeader = requestHeader.headers.get(AUTHORIZATION)
        if (authzHeader.isDefined) {
          try {
            // Parse access token info
            val list = authzHeader.get.split(BEARER + " ")
            if (list.length == 2){
              next(downstreamHeaderFromAccessToken(list(1), requestHeader))
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
          if (isAuthenticatedHttpAccessAllowed(requestHeader)) {
            val sessionCookie = requestHeader.cookies.get("tat")
            if (sessionCookie.isDefined) {
              next(downstreamHeaderFromAccessToken(sessionCookie.get.value, requestHeader))
            } else {
              Future.successful { Unauthorized }
            }
          } else if (isPublicWithOptionalAuthentication(requestHeader)) {
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
                Future.successful {
                  ResponseConstructor.constructUnauthorizedResponse(ErrorCodes.AUTHORIZATION_REQUIRED, "Needs authorization header")
                }
              }
            } else if (isAutomationAccessAllowed(requestHeader)) {
              if (Configurations.AUTOMATION_API_ENABLED) {
                if (isPublicAutomationAccessAllowed(requestHeader)) {
                  next(requestHeader)
                } else {
                  val apiKeyHeader = requestHeader.headers.get(Constants.AutomationHeader)
                  if (apiKeyHeader.isDefined) {
                    next(requestHeader)
                  } else {
                    //Requires authorization to execute this service
                    logger.warn("Request blocked due to missing API key header [" + requestHeader.path + "]")
                    Future.successful {
                      ResponseConstructor.constructUnauthorizedResponse(ErrorCodes.AUTHORIZATION_REQUIRED, "Needs API key header")
                    }
                  }
                }
              } else {
                logger.warn("Request blocked because the Test Bed's automation API is disabled [" + requestHeader.path + "]")
                Future.successful {
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
  }

  def isAutomationAccessAllowed(request:RequestHeader): Boolean = {
    isPublicAutomationAccessAllowed(request) ||
      isProtectedAutomationAccessAllowed(request)
  }

  def isProtectedAutomationAccessAllowed(request:RequestHeader): Boolean = {
      request.path.startsWith("%s/rest/".formatted(API_ROOT))
  }

  def isPublicAutomationAccessAllowed(request:RequestHeader): Boolean = {
    request.path.equals("%s/rest".formatted(API_ROOT)) ||
    request.path.equals("%s/rest/".formatted(API_ROOT))
  }

  def isHmacAuthenticationAllowed(request:RequestHeader):Boolean = {
    request.path.startsWith("%s/repository/".formatted(API_ROOT))
  }

  def isAuthenticatedHttpAccessAllowed(request:RequestHeader): Boolean = {
    request.path.startsWith("%sresources/".formatted(WEB_CONTEXT_ROOT_WITH_SLASH)) ||
      request.path.startsWith("%sbadgereportpreview/".formatted(WEB_CONTEXT_ROOT_WITH_SLASH))
  }

  def isPublic(request:RequestHeader):Boolean = {
    //public services
    request.method.equals("OPTIONS") ||
      request.path.equals(WEB_CONTEXT_ROOT) ||
      request.path.equals(WEB_CONTEXT_ROOT_WITH_SLASH) ||
      request.path.equals("%sapp".formatted(WEB_CONTEXT_ROOT_WITH_SLASH)) ||
      request.path.startsWith("%sapp/".formatted(WEB_CONTEXT_ROOT_WITH_SLASH)) ||
      request.path.equals("%s/app/configuration".formatted(API_ROOT)) ||
      request.path.equals("%s/notices/tbdefault".formatted(API_ROOT)) ||
      request.path.equals("%s/user/selfreg".formatted(API_ROOT)) ||
      request.path.startsWith("%s/sso/".formatted(API_ROOT)) ||
      request.path.startsWith("%s/oauth/".formatted(API_ROOT)) ||
      request.path.startsWith("%s/theme/".formatted(API_ROOT)) ||
      request.path.equals("%s/initdata".formatted(API_ROOT)) ||
      request.path.equals("%s/healthcheck".formatted(API_ROOT)) ||
      request.path.startsWith("%sbadge/".formatted(WEB_CONTEXT_ROOT_WITH_SLASH)) ||
      request.path.startsWith("%ssystemResources/".formatted(WEB_CONTEXT_ROOT_WITH_SLASH)) ||
      //public assets
      request.path.startsWith("%sassets/".formatted(WEB_CONTEXT_ROOT_WITH_SLASH)) ||
      request.path.startsWith("%swebjars/".formatted(WEB_CONTEXT_ROOT_WITH_SLASH)) ||
      request.path.startsWith("%stemplate/".formatted(WEB_CONTEXT_ROOT_WITH_SLASH)) ||
      request.path.equals("%sfavicon.ico".formatted(WEB_CONTEXT_ROOT_WITH_SLASH)) ||
      // CAS callback
      request.path.equals("%scallback".formatted(WEB_CONTEXT_ROOT_WITH_SLASH))
  }

  def isPublicWithOptionalAuthentication(request:RequestHeader):Boolean = {
    request.path.equals("%s/user/feedback".formatted(API_ROOT))
  }

}