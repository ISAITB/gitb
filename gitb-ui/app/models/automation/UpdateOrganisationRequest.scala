package models.automation

case class UpdateOrganisationRequest(organisationApiKey: String,
                                     shortName: Option[String],
                                     fullName: Option[String],
                                     communityApiKey: String) {
 }
