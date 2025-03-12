package controllers.util

import play.api.mvc.{Request, WrappedRequest}

object RequestWithAttributes {

  def apply[A](request: Request[A]): RequestWithAttributes[A] = {
    new RequestWithAttributes(request, false)
  }
}

class RequestWithAttributes[A](val request: Request[A], var authorised: Boolean) extends WrappedRequest[A](request) {
}