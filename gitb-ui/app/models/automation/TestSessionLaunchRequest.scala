package models.automation

case class TestSessionLaunchRequest(
  organisation: String,
  system: String,
  actor:String,
  testSuite: List[String],
  testCase: List[String],
  inputMapping: List[InputMapping],
  forceSequentialExecution: Boolean
) {}
