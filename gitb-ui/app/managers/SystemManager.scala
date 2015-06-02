package managers

import akka.dispatch.Futures
import models.Enums.TestResultStatus
import play.api.Play.current
import scala.slick.driver.MySQLDriver.simple._
import play.api.libs.concurrent.Execution.Implicits._

import models._
import org.slf4j.LoggerFactory
import scala.concurrent.Future
import persistence.db._

object SystemManager extends BaseManager {
	def logger = LoggerFactory.getLogger("SystemManager")

	def checkSystemExists(sysId: Long): Future[Boolean] = {
		Future {
			DB.withSession { implicit session =>
				val firstOption = PersistenceSchema.systems.filter(_.id === sysId).firstOption
				firstOption.isDefined
			}
		}
	}

	def registerSystem(adminId: Long, system: Systems): Future[Unit] = {
		Future {
			DB.withSession { implicit session =>
				//1) Get organization id of the admin
				val orgId = PersistenceSchema.users.filter(_.id === adminId).firstOption.get.organization

				//2) Persist new System
				PersistenceSchema.insertSystem += system.withOrganizationId(orgId)
			}
		}
	}

	def updateSystemProfile(systemId: Long, sname: Option[String], fname: Option[String], description: Option[String], version: Option[String]): Future[Unit] = {
		Future {
			DB.withSession { implicit session =>
				//update short name of the system
				if (sname.isDefined) {
					val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield ( s.shortname )
					q.update(sname.get)
				}
				//update full name of the system
				if (fname.isDefined) {
					val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield ( s.fullname )
					q.update(fname.get)
				}
				//update description of the system
				if (description.isDefined) {
					val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield ( s.description )
					q.update(description)
				}
				//update version of the system
				if (version.isDefined) {
					val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield ( s.version )
					q.update(version.get)
				}
			}
		}
	}

	def getSystemProfile(systemId: Long): Future[models.System] = {
		Future {
			DB.withSession { implicit session =>
				//1) Get system info
				val s = PersistenceSchema.systems.filter(_.id === systemId).firstOption.get

				//2) Get organization info
				val o = PersistenceSchema.organizations.filter(_.id === s.owner).firstOption.get

				//3) Get admins
				val list = PersistenceSchema.systemHasAdmins.filter(_.systemId === systemId).map(_.userId).list
				var admins: List[Users] = List()
				list.foreach { adminId =>
					val user = PersistenceSchema.users.filter(_.id === adminId).firstOption.get
					admins ::= user
				}

				//4) Merge all info and return
				val system: models.System = new models.System(s, o, admins)
				system
			}
		}
	}

	def getVendorSystems(userId: Long): Future[List[Systems]] = {
		Future {
			DB.withSession { implicit session =>
				//1) Get organization id of the user, first
				val orgId = PersistenceSchema.users.filter(_.id === userId).firstOption.get.organization

				//2) Get systems of the organization
				val systems = PersistenceSchema.systems.filter(_.owner === orgId).list
				systems
			}
		}
	}

	def defineConformanceStatement(system: Long, spec: Long, actor: Long, options: Option[List[Long]]): Future[Unit] = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.systemImplementsActors.insert(system, spec, actor)
				options match {
				  case Some(optionIds) => optionIds foreach ((optionId) => PersistenceSchema.systemImplementsOptions.insert((system, optionId)))
				  case _ =>
				}
			}
		}
	}

	def getConformanceStatements(systemId: Long, spec:Option[String], actor:Option[String]): Future[List[ConformanceStatement]] = {
		Future {
			DB.withSession { implicit session =>
        var implementedRows = PersistenceSchema.systemImplementsActors.filter(_.systemId === systemId)

        if(spec.isDefined && actor.isDefined) {
          implementedRows = implementedRows
            .filter(_.specId  === spec.get.toLong)
            .filter(_.actorId === actor.get.toLong)
        }

        val implementedActorIds = implementedRows.map(_.actorId).list.distinct
        val implementedActors = implementedActorIds.map ((actorId) => PersistenceSchema.actors.filter(_.id === actorId).first)

        val implementedOptionIds = PersistenceSchema.systemImplementsOptions.filter(_.systemId === systemId).map(_.optionId).list.distinct
        val implementedOptions = implementedOptionIds.map ((optionId) => PersistenceSchema.options.filter(_.id === optionId).first)

				val implementedActorTestCaseIds =
          if(spec.isDefined)
            PersistenceSchema.testCaseHasActors.filter(_.specification === spec.get.toLong).filter(_.actor inSet implementedActorIds).list
          else
            PersistenceSchema.testCaseHasActors.filter(_.actor inSet implementedActorIds).list
				val implementedOptionTestCaseIds = PersistenceSchema.testCaseCoversOptions.filter(_.option inSet implementedOptionIds).list

        val implementedSpecificationIds = implementedRows.map(_.specId).list.distinct
        val implementedSpecifications = PersistenceSchema.specifications.filter(_.id inSet implementedSpecificationIds).list


				val actorIdTestCaseMap = implementedActorTestCaseIds.groupBy(_._2) //group by specifications
				val optionIdTestCaseMap = implementedOptionTestCaseIds.groupBy(_._2)

				implementedRows.list.map { entry:(Long, Long, Long) =>
          val (systemId, specificationId, actorId) = entry

          val actor = implementedActors.filter(_.id == actorId).head
          val specification = implementedSpecifications.filter(_.id == specificationId).head

					val actorOptions = implementedOptions.filter(_.actor == actor.id)
          //remember we grouped by specifications
					val actorTestCaseIds = actorIdTestCaseMap.get(specificationId) match {
						case Some(ids) => ids.map(_._1)
						case None => List()
					}
					val optionTestCaseIds = actorOptions.map({ (option) =>
						optionIdTestCaseMap.get(option.id) match {
							case Some(ids) => ids.map(_._1)
							case None => List()
						}
					}).flatten

					val testCaseIds = (actorTestCaseIds ++ optionTestCaseIds).distinct

					val testCaseResults = testCaseIds map { testCaseId =>
						val lastResult = {
							val result = PersistenceSchema.testResults
								.filter(_.testcaseId === testCaseId)
								.filter(_.endTime isDefined)
								.sortBy(_.endTime.desc)
								.map(_.result)
								.firstOption

							result match {
								case None => TestResultStatus.UNDEFINED
								case Some(resultStr) => TestResultStatus.withName(resultStr)
							}
						}

						lastResult
					}

					val total = testCaseIds.length
					val completed = testCaseResults.count(_ == TestResultStatus.SUCCESS)

					ConformanceStatement(actor, specification, actorOptions, TestResults(completed, total))
				}
			}
		}
	}

	def deleteConformanceStatments(systemId: Long, actorIds: List[Long]) = Future[Unit] {
		Future {
			DB.withSession { implicit session =>
				actorIds foreach { actorId =>
					PersistenceSchema.systemImplementsActors
						.filter(_.systemId === systemId)
						.filter(_.actorId === actorId)
						.delete

					val optionIds = PersistenceSchema.options
													.filter(_.actor === actorId)
													.map(_.id).list

					optionIds foreach { optionId =>
						PersistenceSchema.systemImplementsOptions
							.filter(_.systemId === systemId)
							.filter(_.optionId === optionId)
							.delete
					}
				}
			}
		}
	}

	def getImplementedActors(system: Long): Future[List[Actors]] = {
		getActorsForSystem(system) flatMap {
			ids =>
				Future {
					DB.withSession { implicit session =>
						PersistenceSchema.actors.filter(_.id inSet ids).list
					}
				}
		}
	}

	private def getActorsForSystem(system:Long): Future[List[Long]] = {
		Future {
			DB.withSession { implicit session =>
				//1) Get actors that the system implements
				val actors = PersistenceSchema.systemImplementsActors.filter(_.systemId === system).map(_.actorId).list

				actors
			}
		}
	}

	def getEndpointConfigurations(endpoint:Long, system:Long): Future[List[Config]] = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.configs.filter(_.endpoint === endpoint).filter(_.system === system).list
			}
		}
	}

	def saveEndpointConfiguration(config: Config): Future[Unit] = {
		Future {
			DB.withSession { implicit session =>
				val size = PersistenceSchema.configs
                    .filter(_.system === config.system)
										.filter(_.parameter === config.parameter)
										.filter(_.endpoint === config.endpoint)
										.size.run
				if(size == 0) {
					PersistenceSchema.configs
						.insert(config)
				} else {
					PersistenceSchema.configs
            .filter(_.system === config.system)
						.filter(_.parameter === config.parameter)
						.filter(_.endpoint === config.endpoint)
						.update(config)
				}
			}
		}
	}

	def getConfigurationsWithEndpointIds(system:Long, ids: List[Long]): Future[List[Config]] = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.configs.filter(_.system === system).filter(_.endpoint inSet ids).list
			}
		}
	}

  /*def getRequiredConfigurations(endpoint:Long): Future[List[Config]] = {
    getActorsForSystem(system) map { actors =>
      DB.withSession { implicit session =>
        val configs = PersistenceSchema.configs.filter(_.actor inSet actors).list
        configs
      }
    }
  }

  def saveConfigurations(configs:List[Config]): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        configs.foreach { config =>
          if(config.value.isDefined && !config.value.get.equals("")) {
            val q = for {
              c <- PersistenceSchema.configs
                         if c.name === config.name && c.actor === config.actor && c.endpoint === config.endpoint
            } yield ( c.value )
            q.update(config.value)
          }
        }
      }
    }
  }*/
}
