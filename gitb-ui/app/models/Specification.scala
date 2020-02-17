package models

case class Specifications(id: Long, shortname:String, fullname:String, urls:Option[String], diagram:Option[String],
                          description:Option[String], specificationType:Short, hidden: Boolean, domain:Long) {

  def withDomainId(domain:Long):Specifications = {
    Specifications(this.id, this.shortname, this.fullname, this.urls, this.diagram, this.description,
                   this.specificationType, this.hidden, domain)
  }
}
