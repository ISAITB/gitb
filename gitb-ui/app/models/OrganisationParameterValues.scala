package models

case class OrganisationParameterValues(organisation: Long, parameter: Long, value: String, contentType: Option[String]) {

  def withOrgId(orgId: Long) = {
    OrganisationParameterValues(orgId, parameter, value, contentType)
  }

}