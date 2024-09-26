package models

case class Actors(id: Long, actorId:String, name: String, description:Option[String], reportMetadata: Option[String], default: Option[Boolean], hidden: Boolean, displayOrder: Option[Short], apiKey: String, domain:Long) {

	def withApiKey(apiKey: String): Actors = {
		Actors(this.id, this.actorId, this.name, this.description, this.reportMetadata, this.default, this.hidden, this.displayOrder, apiKey, this.domain)
	}
}

class Actor(_id: Long, _actorId: String, _name: String, _description: Option[String], _reportMetadata: Option[String], _default: Option[Boolean], _hidden: Boolean, _displayOrder: Option[Short], _domain: Option[Domain], _endpoints: Option[List[Endpoint]], _specificationId: Option[Long], _apiKey: Option[String]) {
	var id: Long = _id
	var actorId: String = _actorId
	var name: String = _name
	var description: Option[String] = _description
	var reportMetadata: Option[String] = _reportMetadata
	var default: Option[Boolean] = _default
	var hidden: Boolean = _hidden
	var displayOrder: Option[Short] = _displayOrder
	var domain: Option[Domain] = _domain
	var endpoints: Option[List[Endpoint]] = _endpoints
	var specificationId: Option[Long] = _specificationId
	var apiKey: Option[String] = _apiKey

	def this(_case: Actors) = this(_case.id, _case.actorId, _case.name, _case.description, _case.reportMetadata, _case.default, _case.hidden, _case.displayOrder, None, None, None, Some(_case.apiKey))

	def this(_case: Actors, _domain: Domain) = this(_case.id, _case.actorId, _case.name, _case.description, _case.reportMetadata, _case.default, _case.hidden, _case.displayOrder, Option(_domain), None, None, Some(_case.apiKey))

	def this(_case: Actors, _endpoints: List[Endpoint]) = this(_case.id, _case.actorId, _case.name, _case.description, _case.reportMetadata, _case.default, _case.hidden, _case.displayOrder, None, Option(_endpoints), None, Some(_case.apiKey))

	def this(_case: Actors, _domain: Domain, _endpoints: List[Endpoint]) = this(_case.id, _case.actorId, _case.name, _case.description, _case.reportMetadata, _case.default, _case.hidden, _case.displayOrder, Option(_domain), Option(_endpoints), None, Some(_case.apiKey))

	def this(_case: Actors, _domain: Domain, _endpoints: List[Endpoint], _specificationId: Long) = this(_case.id, _case.actorId, _case.name, _case.description, _case.reportMetadata, _case.default, _case.hidden, _case.displayOrder, Option(_domain), Option(_endpoints), Option(_specificationId), Some(_case.apiKey))

	def toCaseObject(apiKey: String, domainId: Long): Actors = {
		Actors(this.id, this.actorId, this.name, this.description, this.reportMetadata, this.default, this.hidden, this.displayOrder, apiKey, domainId)
	}
}