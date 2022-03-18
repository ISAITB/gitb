package models

case class Actors(id: Long, actorId:String, name: String, description:Option[String], default: Option[Boolean], hidden: Boolean, displayOrder: Option[Short], apiKey: String, domain:Long) {

  def withDomainId(domain:Long) = {
    Actors(this.id, this.actorId, this.name, this.description, this.default, this.hidden, this.displayOrder, this.apiKey, domain)
  }

}

class Actor(_id: Long, _actorId: String, _name: String, _description: Option[String], _default: Option[Boolean], _hidden: Boolean, _displayOrder: Option[Short], _domain: Option[Domain], _endpoints: Option[List[Endpoint]], _specificationId: Option[Long], _apiKey: Option[String]) {
	var id = _id
	var actorId = _actorId
	var name = _name
	var description = _description
	var default = _default
	var hidden = _hidden
	var displayOrder = _displayOrder
	var domain = _domain
	var endpoints = _endpoints
	var specificationId = _specificationId
	var apiKey = _apiKey

	def this(_case: Actors) = this(_case.id, _case.actorId, _case.name, _case.description, _case.default, _case.hidden, _case.displayOrder, None, None, None, Some(_case.apiKey))

	def this(_case: Actors, _domain: Domain) = this(_case.id, _case.actorId, _case.name, _case.description, _case.default, _case.hidden, _case.displayOrder, Option(_domain), None, None, Some(_case.apiKey))

	def this(_case: Actors, _endpoints: List[Endpoint]) = this(_case.id, _case.actorId, _case.name, _case.description, _case.default, _case.hidden, _case.displayOrder, None, Option(_endpoints), None, Some(_case.apiKey))

	def this(_case: Actors, _domain: Domain, _endpoints: List[Endpoint]) = this(_case.id, _case.actorId, _case.name, _case.description, _case.default, _case.hidden, _case.displayOrder, Option(_domain), Option(_endpoints), None, Some(_case.apiKey))

	def this(_case: Actors, _domain: Domain, _endpoints: List[Endpoint], _specificationId: Long) = this(_case.id, _case.actorId, _case.name, _case.description, _case.default, _case.hidden, _case.displayOrder, Option(_domain), Option(_endpoints), Option(_specificationId), Some(_case.apiKey))

	def toCaseObject(apiKey: String, domainId: Long) = {
		Actors(this.id, this.actorId, this.name, this.description, this.default, this.hidden, this.displayOrder, apiKey, domainId)
	}
}