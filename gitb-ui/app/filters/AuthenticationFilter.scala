package filters

import akka.stream.Materializer
import com.gitb.utils.HmacUtils
import controllers.util.{Parameters, ResponseConstructor}
import exceptions._
import javax.inject.Inject
import org.slf4j.{Logger, LoggerFactory}
import persistence.AccountManager
import persistence.cache.TokenCache
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.mvc.Http.HeaderNames._

import scala.concurrent.Future

class AuthenticationFilter @Inject() (implicit val mat: Materializer, accountManager: AccountManager) extends Filter {

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
            val customRequestHeader = requestHeader.copy(headers = customHeaders)
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
        if (isHmacAuthenticationAllowed(requestHeader)) {
          val hmacHeader = requestHeader.headers.get(HmacUtils.HMAC_HEADER_TOKEN)
          if (hmacHeader.isDefined) {
            next(requestHeader)
          } else {
            //Requires authorization to execute this service
            logger.warn("Request blocked due to missing user or HMAC authentication token ["+requestHeader.path+"]")
            Future{
              ResponseConstructor.constructUnauthorizedResponse(ErrorCodes.AUTHORIZATION_REQUIRED, "Needs authorization header")
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

  def isHmacAuthenticationAllowed(request:RequestHeader):Boolean = {
    request.path.startsWith("/repository/")
  }

  def isPublic(request:RequestHeader):Boolean = {
    //public services
    request.method.equals("OPTIONS") ||
      request.path.equals("/") ||
      request.path.equals("/app") ||
      request.path.equals("/app/configuration") ||
      request.path.equals("/notices/tbdefault") ||
      request.path.equals("/user/feedback") ||
      request.path.startsWith("/sso/") ||
      request.path.startsWith("/oauth/") ||
      request.path.startsWith("/theme/") ||
      //public assets
      request.path.startsWith("/assets/") ||
      request.path.startsWith("/webjars/") ||
      request.path.startsWith("/template/") ||
      request.path.equals("/favicon.ico") ||
      // CAS callback
      request.path.equals("/callback")
  }

}