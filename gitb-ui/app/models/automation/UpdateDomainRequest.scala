package models.automation

case class UpdateDomainRequest(domainApiKey: Option[String],
                               shortName: Option[String],
                               fullName: Option[String],
                               description: Option[Option[String]],
                               communityApiKey: Option[String]) {
 }
