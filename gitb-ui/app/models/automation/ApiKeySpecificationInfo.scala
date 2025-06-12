package models.automation

case class ApiKeySpecificationInfo(id: Long, name: String, actors: List[ApiKeyActorInfo], testSuites: List[ApiKeyTestSuiteInfo]) {}
