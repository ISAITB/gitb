package models.automation

case class CreateSpecificationGroupRequest(
                                shortName: String,
                                fullName: String,
                                description: Option[String],
                                displayOrder: Option[Short],
                                apiKey: Option[String],
                                domainApiKey: Option[String],
                                communityApiKey: String) {
}
