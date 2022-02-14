package models.automation

case class ApiKeySpecificationInfo(name: String, actors: List[ApiKeyActorInfo], testSuites: List[ApiKeyTestSuiteInfo]) {}
