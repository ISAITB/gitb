package models

case class LandingPages (id:Long, name:String, description: Option[String], content:String, default:Boolean) {
}

class LandingPage (_id:Long, _name:String, _description:Option[String], _content:String, _default:Boolean)
{
  var id:Long = _id
  var name:String = _name
  var description:Option[String] = _description
  var content:String = _content
  var default:Boolean = _default

  def this(_case:LandingPages) =
    this(_case.id, _case.name, _case.description, _case.content, _case.default)

  def toCaseObject:LandingPages = {
    LandingPages(id, name, description, content, default)
  }

}
