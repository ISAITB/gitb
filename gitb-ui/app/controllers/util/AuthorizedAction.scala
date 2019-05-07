package controllers.util

import managers.AuthorizationManager
import play.api.mvc.{ActionBuilder, Request, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AuthorizedAction extends ActionBuilder[RequestWithAttributes] {

  def invokeBlock[A](request: Request[A], block: RequestWithAttributes[A] => Future[Result]): Future[Result] = {
    val enhancedRequest = new RequestWithAttributes(scala.collection.mutable.Map.empty[String, String], request)
    block(enhancedRequest).map(result =>
      if (enhancedRequest.attributes.contains(AuthorizationManager.AUTHORIZATION_OK)) {
        result
      } else {
        throw new IllegalStateException("Authorization check missing for path ["+enhancedRequest.path+"]")
      }
    )
  }

}
