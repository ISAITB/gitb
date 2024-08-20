package models.automation

case class CreateActorRequest(identifier: String,
                              name: String,
                              description: Option[String],
                              default: Option[Boolean],
                              hidden: Option[Boolean],
                              displayOrder: Option[Short],
                              apiKey: Option[String],
                              specificationApiKey: String,
                              communityApiKey: String) {
}
