package filters

import play.api.mvc._
import scala.concurrent.Future
import play.mvc.Http.HeaderNames._
import controllers.util.{ResponseConstructor, Parameters}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import persistence.cache.TokenCache
import exceptions._
import persistence.AccountManager
import org.slf4j.{LoggerFactory, Logger}

class CustomizableHeaders(override protected val data:Seq[(String, Seq[String])]) extends Headers

class AuthenticationFilter extends Filter {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthenticationFilter])

  val BEARER = "Bearer"

  def apply(next: (RequestHeader) => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    //if public service called, execute it immediately
    if(isPublic(requestHeader)){
      next(requestHeader)
    }
    //check Authorization headers
    else{
      val authzHeader = requestHeader.headers.get(AUTHORIZATION)
      if(authzHeader.isDefined){
        //parse access token info
        val list = authzHeader.get.split(BEARER + " ")

        if(list.length == 2){
          val accessToken = list(1)
          //check if access token exists for any user
          TokenCache.checkAccessToken(accessToken) flatMap { userId =>
            //a workaround of customizing request headers to add our userId data, so that controllers can process it
            val map = requestHeader.headers.toMap + ( Parameters.USER_ID -> Seq("" + userId) )
            val headers:Seq[(String, Seq[String])] = map.toSeq
            val customHeaders = new CustomizableHeaders(headers)
            val customRequestHeader = requestHeader.copy(headers = customHeaders)

            //check if requested service requires admin access
	          if(requiresSystemAdminAccess(requestHeader)) {
		          next(customRequestHeader)
	          } else if(requiresAdminAccess(requestHeader)){
              AccountManager.isAdmin(userId) flatMap { isAdmin =>
                if(isAdmin){
                  //has access, execute service
                  next(customRequestHeader)
                } else{
                  //admin access required, send error response
                  Future{
                    ResponseConstructor.constructUnauthorizedResponse(ErrorCodes.UNAUTHORIZED_ACCESS, "Requires admin access")
                  }
                }
              }
            } else{
              //no admin access required, execute service
              next(customRequestHeader)
            }
          }
        }
        else{
          //There is a problem with the authorization header
          Future{
            ResponseConstructor.constructUnauthorizedResponse(ErrorCodes.INVALID_AUTHORIZATION_HEADER, "Invalid authorization header")
          }
        }
      }
      else{
        //Requires authorization to execute this service
        Future{
          ResponseConstructor.constructUnauthorizedResponse(ErrorCodes.AUTHORIZATION_REQUIRED, "Needs authorization header")
        }
      }
    }
  }

  def isPublic(request:RequestHeader):Boolean = {
      //public services
      request.method.equals("OPTIONS") ||
      request.path.equals("/") ||
      request.path.equals("/oauth/access_token") ||
      request.path.equals("/vendor/register") ||
      request.path.equals("/check/email") ||
      request.path.equals("/theme/css") ||
      request.path.equals("/theme/logo") ||
      request.path.equals("/theme/footer") ||
      request.path.equals("/notices/default") ||
      //public assets
      request.path.startsWith("/assets/") ||
      request.path.startsWith("/webjars/") ||
      request.path.startsWith("/template/") ||
	    request.path.startsWith("/repository/")
  }
	
	def requiresSystemAdminAccess(request:RequestHeader): Boolean = {
		(request.path.equals("/domains") && request.method.equals("POST")) ||
      (request.path.matches("/domains/\\d*") || request.method.equals("DELETE")) ||
      (request.path.equals("/specs") && request.method.equals("POST")) ||
      (request.path.equals("/suite/\\d*/deploy") && request.method.equals("POST")) ||
      (request.path.equals("/actors") && request.method.equals("POST")) ||
      (request.path.equals("/options") && request.method.equals("POST")) ||
      (request.path.matches("/suite/\\d*/undeploy") && request.method.equals("DELETE"))
	}

  def requiresAdminAccess(request:RequestHeader):Boolean = {
    (request.path.equals("/vendor/profile") && request.method.equals("POST")) ||
      request.path.equals("/user/register") ||
      request.path.equals("/suts/register") ||
      request.path.matches("/suts/\\d*/profile") ||
      (request.path.matches("/suts/\\d*/conformance")
	      && (request.method.equals("POST") || request.method.equals("DELETE")))
  }
}