package models.automation

case class TestSuiteLinkResponseSpecification(
  specification: String,
  linked: Boolean,
  message: Option[String]
)