package exceptions

case class InvalidAuthorizationException(error:Int, msg:String) extends Exception(msg:String){
  def getError: Int = {
    return error
  }
}