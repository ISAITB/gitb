package models

case class SpecificationGroups(id: Long, shortname:String, fullname:String, description:Option[String], reportMetadata: Option[String], displayOrder: Short, apiKey: String, domain:Long) {

  def withApiKey(newApiKey: String): SpecificationGroups = {
    SpecificationGroups(id, shortname, fullname, description, reportMetadata, displayOrder, newApiKey, domain)
  }

}
