package models

case class TestEngineHealthCheckResponse(items: List[TestEngineHealthCheckResponseItem], repositoryUrl: String, hmacHash: String)
