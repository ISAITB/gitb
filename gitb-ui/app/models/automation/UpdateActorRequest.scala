package models.automation

case class UpdateActorRequest(actorApiKey: String,
                              identifier: Option[String],
                              name: Option[String],
                              description: Option[Option[String]],
                              default: Option[Option[Boolean]],
                              hidden: Option[Boolean],
                              displayOrder: Option[Option[Short]],
                              communityApiKey: String) {
 }
