package exceptions

case class AutomationApiException(errorCode: Int, msg: String) extends Exception(msg: String) {
  def getCode: Int = {
    errorCode
  }
}
