package models.automation

case class TestSuiteLinkResponseSpecification(
  specificationKey: String,
  specificationId: Option[Long],
  linked: Boolean,
  message: Option[String],
  actors: Option[List[KeyValueRequired]] = None
) {

  def withActorIdentifiers(actorInfo: List[KeyValueRequired]): TestSuiteLinkResponseSpecification = {
    TestSuiteLinkResponseSpecification(specificationKey, specificationId, linked, message, Some(actorInfo))
  }

}