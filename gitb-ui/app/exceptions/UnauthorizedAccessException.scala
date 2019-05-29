package exceptions

case class UnauthorizedAccessException(msg: String) extends Exception(msg:String){
}