package models.automation

case class UpdateSystemRequest(systemApiKey: String,
                               shortName: Option[String],
                               fullName: Option[String],
                               description: Option[Option[String]],
                               version: Option[Option[String]],
                               communityApiKey: String) {
 }
