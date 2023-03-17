package models.automation

case class TestSuiteLinkRequest(
  testSuite: String,
  specifications: List[TestSuiteLinkRequestSpecification]
)