package models

case class Domain(id: Long, shortname:String, fullname:String, description:Option[String], apiKey: String) {

  def withApiKey(newApiKey: String): Domain = {
    Domain(id, shortname, fullname, description, newApiKey)
  }

}