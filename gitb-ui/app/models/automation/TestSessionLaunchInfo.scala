package models.automation

case class TestSessionLaunchInfo(
                                  testSuiteIdentifier: String,
                                  testCaseIdentifier: String,
                                  testSessionIdentifier: String,
                                  completed: Option[Boolean]) {

  def withCompletedFlag(completed: Boolean): TestSessionLaunchInfo = {
    TestSessionLaunchInfo(this.testSuiteIdentifier, this.testCaseIdentifier, this.testSessionIdentifier, Some(completed))
  }

}
