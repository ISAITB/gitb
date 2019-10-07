package models

case class OrganisationParameterValues(organisation: Long, parameter: Long, value: String) {

  def withOrgId(orgId: Long) = {
    OrganisationParameterValues(orgId, parameter, value)
  }

}