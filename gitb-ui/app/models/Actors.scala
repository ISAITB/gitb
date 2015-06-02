package models

case class Actors(id: Long, actorId:String, name: String, description:Option[String], domain:Long) {

  def withDomainId(domain:Long) = {
    Actors(this.id, this.actorId, this.name, this.description, domain)
  }

}

class Actor(_id: Long, _actorId: String, _name: String, _description: Option[String], _domain: Option[Domain], _endpoints: Option[List[Endpoint]]) {
	var id = _id
	var actorId = _actorId
	var name = _name
	var description = _description
	var domain = _domain
	var endpoints = _endpoints

	def this(_case: Actors) = this(_case.id, _case.actorId, _case.name, _case.description, None, None)

	def this(_case: Actors, _domain: Domain) = this(_case.id, _case.actorId, _case.name, _case.description, Option(_domain), None)

	def this(_case: Actors, _endpoints: List[Endpoint]) = this(_case.id, _case.actorId, _case.name, _case.description, None, Option(_endpoints))

	def this(_case: Actors, _domain: Domain, _endpoints: List[Endpoint]) = this(_case.id, _case.actorId, _case.name, _case.description, Option(_domain), Option(_endpoints))

	def toCaseObject = {
		val domainId = domain match {
			case Some(d) => d.id
			case None => 0l
		}
		Actors(this.id, this.actorId, this.name, this.description, domainId)
	}
}