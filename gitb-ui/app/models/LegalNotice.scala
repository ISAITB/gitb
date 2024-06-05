package models

case class LegalNotices (id:Long, name:String, description: Option[String], content:String, default:Boolean, community:Long) {

  def toLegalNotice(): LegalNotice = {
    new LegalNotice(id, name, description, Some(content), default)
  }

}

class LegalNotice (_id:Long, _name:String, _description:Option[String], _content: Option[String], _default:Boolean) {

  var id:Long = _id
  var name:String = _name
  var description:Option[String] = _description
  var content: Option[String] = _content
  var default:Boolean = _default

}

