package models.automation

case class ApiKeyTestSuiteInfo(id: Long, name: String, key: String, testcases: List[ApiKeyTestCaseInfo]) {}
