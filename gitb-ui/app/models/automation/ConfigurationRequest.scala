package models.automation

case class ConfigurationRequest(
  domainProperties: List[DomainParameterInfo],
  organisationProperties: List[PartyConfiguration],
  systemProperties: List[PartyConfiguration],
  statementProperties: List[StatementConfiguration]
)
