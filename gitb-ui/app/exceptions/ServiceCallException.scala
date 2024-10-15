package exceptions

case class ServiceCallException(message: String,
                                cause: Option[Throwable],
                                responseCode: Option[Int],
                                responseContentType: Option[String],
                                responseBody: Option[String]) extends Exception(message, cause.orNull)
