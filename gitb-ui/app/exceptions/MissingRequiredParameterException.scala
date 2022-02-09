package exceptions

case class MissingRequiredParameterException(parameterName: String, message: String) extends IllegalStateException(message) {}
