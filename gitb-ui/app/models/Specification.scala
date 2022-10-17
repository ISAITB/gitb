package models

case class Specifications(id: Long, shortname:String, fullname:String, description:Option[String], hidden: Boolean, apiKey: String, domain:Long) {

  def withDomainId(domain:Long):Specifications = {
    Specifications(this.id, this.shortname, this.fullname, this.description, this.hidden, this.apiKey, domain)
  }

  def withApiKey(apiKey: String): Specifications = {
    Specifications(this.id, this.shortname, this.fullname, this.description, this.hidden, apiKey, this.domain)
  }
}
