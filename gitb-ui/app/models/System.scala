package models

case class Systems(id:Long, shortname:String, fullname:String, description:Option[String], version:String, owner:Long) {

  def withOrganizationId(id:Long) = {
    Systems(this.id, this.shortname, this.fullname, this.description, this.version, id)
  }
}

class System(_id: Long, _shortname: String, _fullname:String, _description:Option[String], _version:String,
               _owner:Option[Organizations], _admins:Option[List[Users]])
{
  var id:Long = _id
  var shortname:String = _shortname
  var fullname:String = _fullname
  var description:Option[String] = _description
  var version:String = _version
  var owner:Option[Organizations] = _owner
  var admins:Option[List[Users]] = _admins

  def this(_case:Systems) =
    this(_case.id, _case.shortname, _case.fullname, _case.description, _case.version, None, None)

  def this(_case:Systems, _owner:Organizations, _admins:List[Users]) =
      this(_case.id, _case.shortname, _case.fullname, _case.description, _case.version, Some(_owner), Some(_admins))

  def toCaseObject:Systems = {
    if(owner.isDefined) {
      Systems(id, shortname, fullname, description, version, owner.get.id)
    } else{
      Systems(id, shortname, fullname, description, version, 0)
    }
  }
}