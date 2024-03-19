package models.automation

case class ConfigurationRequest(
  domainProperties: List[KeyValue],
  organisationProperties: List[PartyConfiguration],
  systemProperties: List[PartyConfiguration],
  statementProperties: List[StatementConfiguration]
)
