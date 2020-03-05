package models

case class Specifications(id: Long, shortname:String, fullname:String, description:Option[String], hidden: Boolean, domain:Long) {

  def withDomainId(domain:Long):Specifications = {
    Specifications(this.id, this.shortname, this.fullname, this.description, this.hidden, domain)
  }
}
