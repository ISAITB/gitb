package models.automation

case class CreateCommunityRequest(shortName: String,
                                  fullName: String,
                                  description: Option[String],
                                  supportEmail: Option[String],
                                  interactionNotifications: Option[Boolean],
                                  apiKey: Option[String],
                                  domainApiKey: Option[String]) {
}