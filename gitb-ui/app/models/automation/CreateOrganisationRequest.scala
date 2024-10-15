package models.automation

case class CreateOrganisationRequest(shortName: String,
                                     fullName: String,
                                     apiKey: Option[String],
                                     communityApiKey: String) {
}