package models.automation

object DomainParameterInfo {

  def apply(parameterKey: String, domainApiKey: Option[String]): DomainParameterInfo = {
    new DomainParameterInfo(KeyValue(parameterKey, None), None, None, domainApiKey)
  }

}

case class DomainParameterInfo(parameterInfo: KeyValue, description: Option[Option[String]], inTests: Option[Boolean], domainApiKey: Option[String])
