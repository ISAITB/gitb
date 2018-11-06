package models

case class Users (id:Long, name:String, email:String, password:String, onetimePassword:Boolean, role:Short, organization:Long) {

  def withOrganizationId(id:Long) = {
    Users(this.id, this.name, this.email, this.password, this.onetimePassword, this.role, id)
  }
}

//no password field!
class User (_id:Long, _name:String, _email:String, _role:Short, _onetimePassword:Boolean, _organization:Option[Organizations])
{
  var id:Long = _id
  var name:String = _name
  var email:String = _email
  var role:Short = _role
  var onetimePassword:Boolean = _onetimePassword
  var organization:Option[Organizations] = _organization

  def this(_case:Users) =
    this(_case.id, _case.name, _case.email, _case.role, _case.onetimePassword, None)

  def this(_case:Users, _organization:Organizations) =
    this(_case.id, _case.name, _case.email, _case.role, _case.onetimePassword, Some(_organization))

  def toCaseObject:Users = {
    if(organization.isDefined){
      Users(id, name, email, null, onetimePassword, role, organization.get.id)
    } else{
      Users(id, name, email, null, onetimePassword, role, 0)
    }
  }

}
