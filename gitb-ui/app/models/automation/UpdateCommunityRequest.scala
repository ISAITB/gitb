package models.automation

case class UpdateCommunityRequest(communityApiKey: String,
                                  shortName: Option[String],
                                  fullName: Option[String],
                                  description: Option[Option[String]],
                                  supportEmail: Option[Option[String]],
                                  interactionNotifications: Option[Boolean],
                                  domainApiKey: Option[Option[String]]) {
 }
