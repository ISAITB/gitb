package models.automation

case class CreateSpecificationRequest(
                                shortName: String,
                                fullName: String,
                                description: Option[String],
                                hidden: Option[Boolean],
                                displayOrder: Option[Short],
                                apiKey: Option[String],
                                domainApiKey: Option[String],
                                groupApiKey: Option[String],
                                communityApiKey: String) {
}