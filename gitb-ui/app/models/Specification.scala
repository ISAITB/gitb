package models

case class Specifications(id: Long, shortname:String, fullname:String, description:Option[String], hidden: Boolean, apiKey: String, domain:Long, displayOrder: Short, group: Option[Long]) {

  def withGroupNames(groupShortName: String, groupFullName: String): Specifications = {
    Specifications(this.id, groupShortName + " - " + this.shortname, groupFullName  + " - " + this.fullname, this.description, this.hidden, this.apiKey, this.domain, this.displayOrder, this.group)
  }

  def withApiKey(apiKey: String): Specifications = {
    Specifications(this.id, this.shortname, this.fullname, this.description, this.hidden, apiKey, this.domain, this.displayOrder, this.group)
  }
}
