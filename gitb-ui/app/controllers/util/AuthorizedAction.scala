package controllers.util

import javax.inject.Inject
import managers.AuthorizationManager
import play.api.mvc.{Request, _}

import scala.concurrent.{ExecutionContext, Future}

class AuthorizedAction @Inject() (val parser: BodyParsers.Default) (implicit val ec: ExecutionContext) extends ActionBuilder[RequestWithAttributes, AnyContent] {

  def wrap[A](request: Request[A], block: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    // Perform an input sanitization check first. This is done here and not via filter to benefit from the already parsed request body.
    InputSanitizer.check(request)
    val enhancedRequest = new RequestWithAttributes(scala.collection.mutable.Map.empty[String, String], request)
    block(enhancedRequest).map(result =>
      if (enhancedRequest.attributes.contains(AuthorizationManager.AUTHORIZATION_OK)) {
        result
      } else {
        throw new IllegalStateException("Authorization check missing for path ["+enhancedRequest.path+"]")
      }
    )
  }

  def invokeBlock[A](request: Request[A], block: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    wrap(request, block)
  }

  override protected def executionContext: ExecutionContext = ec

}

