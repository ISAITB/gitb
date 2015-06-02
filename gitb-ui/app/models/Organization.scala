package models

case class Organizations(id:Long = 0, shortname:String, fullname:String, organizationType:Short)

class Organization(_id: Long, _sname: String, _fname:String, _otype:Short,
                   _systems:Option[List[Systems]], _admin:Option[Users])
{
  var id:Long = _id
  var shortname:String = _sname
  var fullname:String = _fname
  var organizationType:Short  = _otype
  var systems:Option[List[Systems]] = _systems
  var admin:Option[Users] = _admin

  def this(_case:Organizations) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, None, None)

  def this(_case:Organizations, _systems:List[Systems], _admin:Users) =
    this(_case.id, _case.shortname, _case.fullname, _case.organizationType, Some(_systems), Option(_admin))

  def toCaseObject:Organizations = {
    Organizations(id, shortname, fullname, organizationType)
  }
}