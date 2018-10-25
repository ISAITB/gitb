package models

case class Actors(id: Long, actorId:String, name: String, description:Option[String], default: Option[Boolean], displayOrder: Option[Short], domain:Long) {

  def withDomainId(domain:Long) = {
    Actors(this.id, this.actorId, this.name, this.description, this.default, this.displayOrder, domain)
  }

}

class Actor(_id: Long, _actorId: String, _name: String, _description: Option[String], _default: Option[Boolean], _displayOrder: Option[Short], _domain: Option[Domain], _endpoints: Option[List[Endpoint]], _specificationId: Option[Long]) {
	var id = _id
	var actorId = _actorId
	var name = _name
	var description = _description
	var default = _default
	var displayOrder = _displayOrder
	var domain = _domain
	var endpoints = _endpoints
	var specificationId = _specificationId

	def this(_case: Actors) = this(_case.id, _case.actorId, _case.name, _case.description, _case.default, _case.displayOrder, None, None, None)

	def this(_case: Actors, _domain: Domain) = this(_case.id, _case.actorId, _case.name, _case.description, _case.default, _case.displayOrder, Option(_domain), None, None)

	def this(_case: Actors, _endpoints: List[Endpoint]) = this(_case.id, _case.actorId, _case.name, _case.description, _case.default, _case.displayOrder, None, Option(_endpoints), None)

	def this(_case: Actors, _domain: Domain, _endpoints: List[Endpoint]) = this(_case.id, _case.actorId, _case.name, _case.description, _case.default, _case.displayOrder, Option(_domain), Option(_endpoints), None)

	def this(_case: Actors, _domain: Domain, _endpoints: List[Endpoint], _specificationId: Long) = this(_case.id, _case.actorId, _case.name, _case.description, _case.default, _case.displayOrder, Option(_domain), Option(_endpoints), Option(_specificationId))

	def toCaseObject = {
		val domainId = domain match {
			case Some(d) => d.id
			case None => 0l
		}
		Actors(this.id, this.actorId, this.name, this.description, this.default, this.displayOrder, domainId)
	}
}