package models

case class Communities(id: Long = 0, shortname: String, fullname: String, supportEmail: Option[String], selfRegType: Short, selfRegToken: Option[String], selfregNotification: Boolean, description: Option[String], selfRegRestriction: Short, domain: Option[Long]) {
}

class Community(_id:Long, _shortname:String, _fullname:String, _supportEmail:Option[String], _selfRegType: Short, _selfRegToken: Option[String], _selfregNotification: Boolean, _description: Option[String], _selfRegRestriction: Short, _domain:Option[Domain]) {
  var id:Long = _id
  var shortname:String = _shortname
  var fullname:String = _fullname
  var supportEmail:Option[String] = _supportEmail
  var selfRegType:Short = _selfRegType
  var selfRegToken:Option[String] = _selfRegToken
  var selfRegNotification:Boolean = _selfregNotification
  var description:Option[String] = _description
  var selfRegRestriction:Short = _selfRegRestriction
  var domain:Option[Domain] = _domain

  def this(_case:Communities, _domain:Option[Domain]) =
    this(_case.id, _case.shortname, _case.fullname, _case.supportEmail, _case.selfRegType, _case.selfRegToken, _case.selfregNotification, _case.description, _case.selfRegRestriction, _domain)

  def toCaseObject:Communities = {
    val d = domain match {
      case Some(d) => Some(d.id)
      case None => None
    }
    Communities(id, shortname, fullname, supportEmail, selfRegType, selfRegToken, selfRegNotification, description, selfRegRestriction, d)
  }

}