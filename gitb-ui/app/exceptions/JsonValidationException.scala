package exceptions

case class JsonValidationException(msg: String) extends Exception(msg: String) {}
