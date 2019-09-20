package models

case class OrganisationParameters(id: Long, name: String, testKey: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean, community: Long)

class OrganisationParametersWithValue(_parameter: OrganisationParameters, _value: Option[OrganisationParameterValues]) {
  var parameter: OrganisationParameters = _parameter
  var value: Option[OrganisationParameterValues] = _value
}
