package models.automation

case class UpdateSpecificationGroupRequest(groupApiKey: String,
                                           shortName: Option[String],
                                           fullName: Option[String],
                                           description: Option[Option[String]],
                                           displayOrder: Option[Short],
                                           communityApiKey: String) {
 }
