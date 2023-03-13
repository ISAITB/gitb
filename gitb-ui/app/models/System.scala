package models

case class Systems(id:Long, shortname:String, fullname:String, description:Option[String], version:Option[String], apiKey: Option[String], owner:Long) {

  def withOrganizationId(id:Long): Systems = {
    Systems(this.id, this.shortname, this.fullname, this.description, this.version, this.apiKey, id)
  }

  def withApiKey(apiKey: String): Systems = {
    Systems(this.id, this.shortname, this.fullname, this.description, this.version, Some(apiKey), this.owner)
  }

}

class System(_id: Long, _shortname: String, _fullname:String, _description:Option[String], _version:Option[String], _apiKey: Option[String],
               _owner:Option[Organizations], _admins:Option[List[Users]])
{
  var id:Long = _id
  var shortname:String = _shortname
  var fullname:String = _fullname
  var description:Option[String] = _description
  var version:Option[String] = _version
  var apiKey: Option[String] = _apiKey
  var owner:Option[Organizations] = _owner
  var admins:Option[List[Users]] = _admins

  def this(_case:Systems) =
    this(_case.id, _case.shortname, _case.fullname, _case.description, _case.version, _case.apiKey, None, None)

  def this(_case:Systems, _owner:Organizations, _admins:List[Users]) =
      this(_case.id, _case.shortname, _case.fullname, _case.description, _case.version, _case.apiKey, Some(_owner), Some(_admins))

  def toCaseObject:Systems = {
    if(owner.isDefined) {
      Systems(id, shortname, fullname, description, version, apiKey, owner.get.id)
    } else{
      Systems(id, shortname, fullname, description, version, apiKey, 0)
    }
  }
}