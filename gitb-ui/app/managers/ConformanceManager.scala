package managers

import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import utils.RepositoryUtils

import scala.slick.driver.MySQLDriver.simple._

object ConformanceManager extends BaseManager {
  def logger = LoggerFactory.getLogger("ConformanceManager")

	/**
	 * Checks if domain exists
	 */
	def checkDomainExists(domainId: Long): Boolean = {
		DB.withSession { implicit session =>
			val firstOption = PersistenceSchema.domains.filter(_.id === domainId).firstOption
			firstOption.isDefined
		}
	}

	def getDomainOfSpecification(specificationId: Long ): Domain = {
		DB.withSession { implicit session =>
			var query = for {
				domain <- PersistenceSchema.domains
				specification <- PersistenceSchema.specifications if specification.domain === domain.id
			} yield (domain, specification)
			val result = query
				.filter(_._2.id === specificationId)
				.firstOption
			result.get._1
		}
	}

  def getDomains(ids: Option[List[Long]] = None):List[Domain] = {
		DB.withSession { implicit session =>
			val domains = {
				val q = PersistenceSchema.domains

				val q2 = ids match {
					case Some(list) => q.filter(_.id inSet list)
					case None => q
				}

				q2.list
			}
			domains
		}
  }

	def getCommunityDomain(communityId: Long): Domain = {
		DB.withSession { implicit session =>
			val community = PersistenceSchema.communities.filter(_.id === communityId).firstOption.get

			val domain = community.domain match {
				case Some(d) => PersistenceSchema.domains.filter(_.id === d).firstOption.get
				case None => null
			}
			domain
		}
	}

	def createDomain(domain:Domain) = {
		DB.withSession { implicit session =>
			PersistenceSchema.domains.insert(domain)
		}
	}

	def createDomainParameter(parameter:DomainParameter) = {
		DB.withTransaction { implicit session =>
			PersistenceSchema.domainParameters.insert(parameter)
		}
	}

	def updateDomainParameter(parameterId: Long, name: String, description: Option[String], kind: String, value: String) = {
		DB.withTransaction { implicit session =>

			val q1 = for {d <- PersistenceSchema.domainParameters if d.id === parameterId} yield (d.name)
			q1.update(name)

			val q2 = for {d <- PersistenceSchema.domainParameters if d.id === parameterId} yield (d.desc)
			q2.update(description)

			val q3 = for {d <- PersistenceSchema.domainParameters if d.id === parameterId} yield (d.kind)
			q3.update(kind)

			val q4 = for {d <- PersistenceSchema.domainParameters if d.id === parameterId} yield (d.value)
			q4.update(value)

		}
	}

	def deleteDomainParameter(domainParameter: Long) = {
		DB.withTransaction { implicit session =>
			PersistenceSchema.domainParameters.filter(_.id === domainParameter).delete
		}
	}

	def getDomainParameter(domainParameterId: Long) = {
		DB.withSession { implicit session =>
			PersistenceSchema.domainParameters.filter(_.id === domainParameterId).first
		}
	}

	def getDomainParameters(domainId: Long) = {
		DB.withSession { implicit session =>
			PersistenceSchema.domainParameters.filter(_.domain === domainId).list
		}
	}

	def deleteDomainParameterByDomain(domainId: Long) = {
		DB.withTransaction { implicit session =>
			val ids = PersistenceSchema.domainParameters.filter(_.domain === domainId).map(_.id).list
			ids foreach { id =>
				deleteDomainParameter(id)
			}
		}
	}

	def getDomainParameterByDomainAndName(domainId: Long, name: String) = {
		DB.withSession { implicit session =>
			PersistenceSchema.domainParameters
				.filter(_.domain === domainId)
				.filter(_.name === name)
				.firstOption
		}
	}

	def updateDomain(domainId: Long, shortName: String, fullName: String, description: Option[String]) = {
		DB.withTransaction { implicit session =>
			val q1 = for {d <- PersistenceSchema.domains if d.id === domainId} yield (d.shortname)
			q1.update(shortName)

			val q2 = for {d <- PersistenceSchema.domains if d.id === domainId} yield (d.fullname)
			q2.update(fullName)

			val q3 = for {d <- PersistenceSchema.domains if d.id === domainId} yield (d.description)
			q3.update(description)

			TestResultManager.updateForUpdatedDomain(domainId, shortName)
		}
	}

	def deleteDomain(domain: Long) {
		DB.withTransaction { implicit session =>
			ActorManager.deleteActorByDomain(domain)
			SpecificationManager.deleteSpecificationByDomain(domain)
			TransactionManager.deleteTransactionByDomain(domain)
			TestResultManager.updateForDeletedDomain(domain)
			PersistenceSchema.domains.filter(_.id === domain).delete
			RepositoryUtils.deleteDomainTestSuiteFolder(domain)
		}
	}

	def getSpecifications(ids: Option[List[Long]] = None): List[Specifications] = {
		DB.withSession { implicit session =>
			val specs = {
				val q = PersistenceSchema.specifications

				val q2 = ids match {
					case Some(list) => q.filter(_.id inSet list)
					case None => q
				}

				q2.list
			}
			specs
		}
	}

  def getSpecifications(domain:Long): List[Specifications] = {
		DB.withSession { implicit session =>
			val specs = PersistenceSchema.specifications.filter(_.domain === domain).list
			specs
    }
  }

	def createSpecifications(specification: Specifications) = {
		DB.withTransaction { implicit session =>
			PersistenceSchema.specifications.insert(specification)
		}
	}

	def createActor(actor: Actors, specificationId: Long) = {
		DB.withSession { implicit session =>
			val savedActorId = PersistenceSchema.actors.returning(PersistenceSchema.actors.map(_.id)).insert(actor)
			PersistenceSchema.specificationHasActors.insert(specificationId, savedActorId)
			savedActorId
		}
	}

  def getActors(ids:Option[List[Long]]): List[Actors] = {
		DB.withSession { implicit session =>
			val actors = {
				val q = PersistenceSchema.actors

				val q2 = ids match {
					case Some(list) => q.filter(_.id inSet list)
					case None => q
				}

				q2.list
			}
			actors
		}
  }
	
	def getActorsWithDomainId(domainId: Long): List[Actors] = {
		DB.withSession { implicit session =>
			PersistenceSchema.actors.filter(_.domain === domainId).list
		}
	}

  def getActorsWithSpecificationId(spec:Long): List[Actors] = {
		DB.withSession { implicit session =>
			var actors: List[Actors] = List()
			var query = for {
				actor <- PersistenceSchema.actors
				specificationHasActors <- PersistenceSchema.specificationHasActors if specificationHasActors.actorId === actor.id
			} yield (actor, specificationHasActors)
			query.filter(_._2.specId === spec).list.foreach{ result =>
				actors ::= result._1
			}
			actors
		}
  }

  def getActorDefinition(ids:List[Long]): List[Actors] = {
		DB.withSession { implicit session =>
			PersistenceSchema.actors.filter(_.id inSet ids).list
		}
  }

	def relateActorWithSpecification(actorId: Long, specificationId: Long) = {
		DB.withTransaction { implicit session =>
			PersistenceSchema.specificationHasActors.insert(specificationId, actorId)
		}
	}

	def createOption(option:Options) = {
		DB.withSession { implicit session =>
			PersistenceSchema.options.insert(option)
		}
	}

	def getOptionsForActor(actorId:Long): List[Options] = {
		DB.withSession { implicit session =>
			PersistenceSchema.options.filter(_.actor === actorId).list
		}
	}

	def getEndpointsForActor(actorId: Long): List[Endpoint] = {
		DB.withSession { implicit session =>
			val caseObjects = PersistenceSchema.endpoints.filter(_.actor === actorId).list
			caseObjects map { caseObject =>
				val actor = PersistenceSchema.actors.filter(_.id === caseObject.actor).first
				val parameters = PersistenceSchema.parameters.filter(_.endpoint === caseObject.id).list

				new Endpoint(caseObject, actor, parameters)
			}
		}
	}

	def getEndpoints(ids: Option[List[Long]]): List[Endpoint] = {
		DB.withSession { implicit session =>
			val caseObjects = {
				val q = ids match {
					case Some(list) => PersistenceSchema.endpoints.filter(_.id inSet list)
					case None => PersistenceSchema.endpoints
				}

				q.list
			}
			caseObjects map { caseObject =>
				val actor = PersistenceSchema.actors.filter(_.id === caseObject.actor).first
				val parameters = PersistenceSchema.parameters.filter(_.endpoint === caseObject.id).list

				new Endpoint(caseObject, actor, parameters)
			}
		}
	}

	def getParameters(ids: Option[List[Long]]): List[models.Parameters] = {
		DB.withSession { implicit session =>
			val q = ids match {
				case Some(ids) => PersistenceSchema.parameters.filter(_.id inSet ids)
				case None => PersistenceSchema.parameters
			}

			q.list
		}
	}

	def getEndpointParameters(endpointId: Long): List[models.Parameters] = {
		DB.withSession { implicit session =>
			PersistenceSchema.parameters.filter(_.endpoint === endpointId).list
		}
	}

	def getOptions(ids:Option[List[Long]]): List[Options] = {
		DB.withSession { implicit session =>
			val options = {
				val q = PersistenceSchema.options

				val q2 = ids match {
					case Some(s) => q.filter(_.actor inSet s)
					case None => q
				}

				q2.list
			}
			options
		}
	}

	def getById(id: Long): Option[Domain] = {
		DB.withSession { implicit session =>
			PersistenceSchema.domains.filter(_.id === id).firstOption
		}
	}

}
