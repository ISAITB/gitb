/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package models

case class Endpoints (id: Long, name: String,  desc: Option[String],  actor: Long ) {
  def withActorId(actor:Long) = {
    Endpoints(this.id, this.name, this.desc, actor)
  }
}

class Endpoint(_id: Long, _name: String, _desc: Option[String],
               _actor: Option[Actors], _parameters: Option[List[Parameters]]) {
	var id = _id
	var name = _name
	var desc = _desc
	var actor = _actor
	var parameters = _parameters

	def this(_case: Endpoints) = this(_case.id, _case.name, _case.desc, None, None)

	def this(_case: Endpoints, _actor: Actors) = this(_case.id, _case.name, _case.desc, Option(_actor), None)
	
	def this(_case: Endpoints, _parameters: List[Parameters]) = this(_case.id, _case.name, _case.desc, None, Option(_parameters))

	def this(_case: Endpoints, _actor: Actors, _parameters: List[Parameters]) = this(_case.id, _case.name, _case.desc, Option(_actor), Option(_parameters))

	def toCaseObject = {
		val actorId = actor match {
			case Some(a) => a.id
			case None => 0L
		}
		Endpoints(this.id, this.name, this.desc, actorId)
	}
}