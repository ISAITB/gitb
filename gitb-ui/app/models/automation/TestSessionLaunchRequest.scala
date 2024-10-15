package models.automation

case class TestSessionLaunchRequest(
  organisation: String,
  system: Option[String],
  actor: Option[String],
  testSuite: List[String],
  testCase: List[String],
  inputMapping: List[InputMapping],
  forceSequentialExecution: Boolean
) {}
