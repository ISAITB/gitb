package exceptions

case class NotFoundException(error:Int, msg:String) extends Exception(msg:String){
  def getError: Int = {
    return error
  }
}
