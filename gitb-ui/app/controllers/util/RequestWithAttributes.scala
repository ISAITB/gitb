package controllers.util

import play.api.mvc.{Request, WrappedRequest}

object RequestWithAttributes {

  def apply[A](request: Request[A]): RequestWithAttributes[A] = {
    new RequestWithAttributes(scala.collection.mutable.Map.empty[String, String], request)
  }
}

class RequestWithAttributes[A](val attributes: scala.collection.mutable.Map[String, String], val request: Request[A]) extends WrappedRequest[A](request) {
}