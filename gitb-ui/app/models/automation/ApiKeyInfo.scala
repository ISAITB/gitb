package models.automation

case class ApiKeyInfo(organisation: Option[String], systems: List[ApiKeySystemInfo], specifications: List[ApiKeySpecificationInfo]) {}
