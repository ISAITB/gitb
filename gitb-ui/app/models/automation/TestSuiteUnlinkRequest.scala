package models.automation

case class TestSuiteUnlinkRequest(
  testSuite: String,
  specifications: List[String]
)