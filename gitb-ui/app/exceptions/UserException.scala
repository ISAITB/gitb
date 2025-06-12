package exceptions

case class UserException(errorCode: Int, message: String) extends Exception(message)