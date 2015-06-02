package exceptions

case class InvalidRequestException(error:Int, msg:String) extends Exception(msg:String){
  def getError: Int = {
    return error
  }
}
