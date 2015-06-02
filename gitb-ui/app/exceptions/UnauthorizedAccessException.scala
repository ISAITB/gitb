package exceptions

case class UnauthorizedAccessException(error:Int, msg:String) extends Exception(msg:String){
  def getError: Int = {
    return error
  }
}