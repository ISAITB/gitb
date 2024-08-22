package models.automation

case class DomainParameterInfo(parameterInfo: KeyValue, description: Option[Option[String]], inTests: Option[Boolean], domainApiKey: Option[String])
