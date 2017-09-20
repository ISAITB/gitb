package managers

import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._

object ConformanceManager extends BaseManager {
  def logger = LoggerFactory.getLogger("ConformanceManager")

	/**
	 * Checks if domain exists
	 */
	def checkDomainExists(domainId: Long): Future[Boolean] = {
		Future {
			DB.withSession { implicit session =>
				val firstOption = PersistenceSchema.domains.filter(_.id === domainId).firstOption
				firstOption.isDefined
			}
		}
	}

  def getDomains(ids: Option[List[Long]] = None):Future[List[Domain]] = {
    Future{
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
  }

	def getCommunityDomain(communityId: Long): Future[Domain] = {
		Future {
			DB.withSession { implicit session =>
				val community = PersistenceSchema.communities.filter(_.id === communityId).firstOption.get

				val domain = community.domain match {
					case Some(d) => PersistenceSchema.domains.filter(_.id === d).firstOption.get
					case None => null
				}
				domain
			}
		}
	}

	def createDomain(domain:Domain) = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.domains.insert(domain)
				Unit
			}
		}
	}

	def updateDomain(domainId: Long, shortName: String, fullName: String, description: Option[String]) = {
		Future {
			DB.withSession { implicit session =>
				val q1 = for {d <- PersistenceSchema.domains if d.id === domainId} yield (d.shortname)
				q1.update(shortName)

				val q2 = for {d <- PersistenceSchema.domains if d.id === domainId} yield (d.fullname)
				q2.update(fullName)

				val q3 = for {d <- PersistenceSchema.domains if d.id === domainId} yield (d.description)
				q3.update(description)
			}
		}
	}

	def deleteDomain(domain: Long) = Future[Unit] {
		Future {
			DB.withTransaction { implicit session =>
				ActorManager.deleteActorByDomain(domain)
				SpecificationManager.deleteSpecificationByDomain(domain)
				TransactionManager.deleteTransactionByDomain(domain)
				PersistenceSchema.domains.filter(_.id === domain).delete
			}
		}
	}

	def getSpecifications(ids: Option[List[Long]] = None): Future[List[Specifications]] = {
		Future {
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
	}

  def getSpecifications(domain:Long):Future[List[Specifications]] = {
    Future{
      DB.withSession { implicit session =>
        val specs = PersistenceSchema.specifications.filter(_.domain === domain).list
        specs
      }
    }
  }

	def createSpecifications(specification: Specifications) = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.specifications.insert(specification)
				Unit
			}
		}
	}

	def createActor(actor: Actors, specificationId:Option[Long] = None) = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.actors.insert(actor)

				if(specificationId.isDefined) {
					PersistenceSchema.specificationHasActors.insert(specificationId.get, actor.id)
				}
				Unit
			}
		}
	}

  def getActors(ids:Option[List[Long]]):Future[List[Actors]] = {
    Future{
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
  }
	
	def getActorsWithDomainId(domainId: Long): Future[List[Actors]] = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.actors.filter(_.domain === domainId).list
			}
		}
	}

  def getActorsWithSpecificationId(spec:Long):Future[List[Actors]] = {
    Future{
      DB.withSession { implicit session =>
        var actors: List[Actors] = List()

        //1) Get the names of the actors of given specification
        val actorIds = PersistenceSchema.specificationHasActors.filter(_.specId === spec).map(_.actorId).list

        //2) Iterate over all actor names and get their rows
        actorIds.foreach{ actorId =>
          val actor = PersistenceSchema.actors.filter(_.id === actorId).firstOption.get
          actors ::= actor
        }

        actors
      }
    }
  }

  def getActorDefinition(actorId:Long):Future[Actors] = {
    Future{
      DB.withSession { implicit  session =>
        PersistenceSchema.actors.filter(_.id === actorId).firstOption.get
      }
    }
  }

	def relateActorWithSpecification(actorId: Long, specificationId: Long): Future[Unit] = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.specificationHasActors.insert(specificationId, actorId)
				Unit
			}
		}
	}

	def createOption(option:Options) = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.options.insert(option)
				Unit
			}
		}
	}

	def getOptionsForActor(actorId:Long): Future[List[Options]] = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.options.filter(_.actor === actorId).list
			}
		}
	}

	def getEndpointsForActor(actorId: Long): Future[List[Endpoint]] = {
		Future {
			DB.withSession { implicit session =>
				val caseObjects = PersistenceSchema.endpoints.filter(_.actor === actorId).list
				caseObjects map { caseObject =>
					val actor = PersistenceSchema.actors.filter(_.id === caseObject.actor).first
					val parameters = PersistenceSchema.parameters.filter(_.endpoint === caseObject.id).list

					new Endpoint(caseObject, actor, parameters)
				}
			}
		}
	}

	def getEndpoints(ids: Option[List[Long]]): Future[List[Endpoint]] = {
		Future {
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
	}

	def getParameters(ids: Option[List[Long]]): Future[List[models.Parameters]] = {
		Future {
			DB.withSession { implicit session =>
				val q = ids match {
					case Some(ids) => PersistenceSchema.parameters.filter(_.id inSet ids)
					case None => PersistenceSchema.parameters
				}

				q.list
			}
		}
	}

	def getEndpointParameters(endpointId: Long): Future[List[models.Parameters]] = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.parameters.filter(_.endpoint === endpointId).list
			}
		}
	}

	def getOptions(ids:Option[List[Long]]): Future[List[Options]] = {
		Future {
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
	}

}
