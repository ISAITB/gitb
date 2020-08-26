package models

class OrganisationCreationDbInfo(var organisationId:Long, var systems: Option[List[SystemCreationDbInfo]]) {
}
