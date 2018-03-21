package models

case class Communities(id: Long = 0, shortname: String, fullname: String, domain: Option[Long]) {
}

class Community(_id:Long, _shortname:String, _fullname:String, _domain:Option[Domain]) {
  var id:Long = _id
  var shortname:String = _shortname
  var fullname:String = _fullname
  var domain:Option[Domain] = _domain

  def this(_case:Communities, _domain:Option[Domain]) =
    this(_case.id, _case.shortname, _case.fullname, _domain)

  def toCaseObject:Communities = {
    val d = domain match {
      case Some(d) => Some(d.id)
      case None => None
    }
    Communities(id, shortname, fullname, d)
  }

}