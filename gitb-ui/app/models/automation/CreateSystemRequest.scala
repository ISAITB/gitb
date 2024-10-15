package models.automation

case class CreateSystemRequest(shortName: String,
                               fullName: String,
                               description: Option[String],
                               version: Option[String],
                               apiKey: Option[String],
                               organisationApiKey: String,
                               communityApiKey: String) {
}