package managers

import java.util

import models.Enums.TestResultStatus
import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import utils.{MimeUtil, RepositoryUtils}

import scala.collection.mutable.ListBuffer
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

				q2.sortBy(_.shortname.asc)
					.list
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

	def updateDomainParameter(parameterId: Long, name: String, description: Option[String], kind: String, value: Option[String]) = {
		DB.withTransaction { implicit session =>

			val q1 = for {d <- PersistenceSchema.domainParameters if d.id === parameterId} yield (d.name)
			q1.update(name)

			val q2 = for {d <- PersistenceSchema.domainParameters if d.id === parameterId} yield (d.desc)
			q2.update(description)

			val q3 = for {d <- PersistenceSchema.domainParameters if d.id === parameterId} yield (d.kind)
			q3.update(kind)

			if (value.isDefined) {
				// If there is no value provided this means that we don't want to update this
				val q4 = for {d <- PersistenceSchema.domainParameters if d.id === parameterId} yield (d.value)
				q4.update(value)
			}

		}
	}

	def deleteDomainParameterWrapper(domainParameter: Long) = {
		DB.withTransaction { implicit session =>
			deleteDomainParameter(domainParameter)
		}
	}

	def deleteDomainParameter(domainParameter: Long)(implicit session: Session) = {
		PersistenceSchema.domainParameters.filter(_.id === domainParameter).delete
	}

	def getDomainParameter(domainParameterId: Long) = {
		DB.withSession { implicit session =>
			PersistenceSchema.domainParameters.filter(_.id === domainParameterId).first
		}
	}

	def getDomainParameters(domainId: Long) = {
		DB.withSession { implicit session =>
			PersistenceSchema.domainParameters.filter(_.domain === domainId)
			  	.sortBy(_.name.asc)
					.list
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

	def deleteDomainParameters(domainId: Long)(implicit session: Session) = {
		val ids = PersistenceSchema.domainParameters.filter(_.domain === domainId).map(_.id).list
		ids foreach { id =>
			deleteDomainParameter(id)
		}
	}

	def deleteDomain(domain: Long) {
		DB.withTransaction { implicit session =>
			ActorManager.deleteActorByDomain(domain)
			SpecificationManager.deleteSpecificationByDomain(domain)
			TransactionManager.deleteTransactionByDomain(domain)
			TestResultManager.updateForDeletedDomain(domain)
			deleteDomainParameters(domain)
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

				q2.sortBy(_.shortname.asc)
					.list
			}
			specs
		}
	}

  def getSpecifications(domain:Long): List[Specifications] = {
		DB.withSession { implicit session =>
			val specs = PersistenceSchema.specifications.filter(_.domain === domain)
			  	.sortBy(_.shortname.asc)
					.list
			specs
    }
  }

	def createSpecifications(specification: Specifications) = {
		DB.withTransaction { implicit session =>
			PersistenceSchema.specifications.insert(specification)
		}
	}

	def createActorWrapper(actor: Actors, specificationId: Long) = {
		DB.withTransaction { implicit session =>
			createActor(actor, specificationId)
		}
	}

	def createActor(actor: Actors, specificationId: Long)(implicit session:Session) = {
		val savedActorId = PersistenceSchema.actors.returning(PersistenceSchema.actors.map(_.id)).insert(actor)
		PersistenceSchema.specificationHasActors.insert(specificationId, savedActorId)
		if (actor.default.isDefined && actor.default.get) {
			// Ensure no other default actors are defined.
			ActorManager.setOtherActorsAsNonDefault(savedActorId, specificationId)
		}
		savedActorId
	}

	def getActorsWithDomainId(domainId: Long): List[Actors] = {
		DB.withSession { implicit session =>
			PersistenceSchema.actors.filter(_.domain === domainId)
			  	.sortBy(_.actorId.desc)
					.list
		}
	}

  def getActorsWithSpecificationId(actorIds:Option[List[Long]], spec:Option[Long]): List[Actor] = {
		DB.withSession { implicit session =>
			var actors: List[Actor] = List()
			var query = for {
				actor <- PersistenceSchema.actors
				specificationHasActors <- PersistenceSchema.specificationHasActors if specificationHasActors.actorId === actor.id
			} yield (actor, specificationHasActors)
			if (actorIds.isDefined) {
				query = actorIds match {
					case Some(list) => query.filter(_._1.id inSet list)
					case None => query
				}
			}
      if (spec.isDefined) {
        query = query.filter(_._2.specId === spec.get)
      }
			query.sortBy(_._1.actorId.desc)
					.list
					.foreach{ result =>
						actors ::= new Actor(result._1, null, null, result._2._1)
					}
			actors
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
			val caseObjects = PersistenceSchema.endpoints.filter(_.actor === actorId)
			  	.sortBy(_.name.asc)
					.list
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

				q.sortBy(_.name.asc)
				 .list
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

			q.sortBy(_.name.asc)
				.list
		}
	}

	def getEndpointParameters(endpointId: Long): List[models.Parameters] = {
		DB.withSession { implicit session =>
			PersistenceSchema.parameters.filter(_.endpoint === endpointId)
			  	.sortBy(_.name.asc)
					.list
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

	def getById(id: Long)(implicit session: Session): Option[Domain] = {
		PersistenceSchema.domains.filter(_.id === id).firstOption
	}

	def getConformanceStatus(actorId: Long, sutId: Long, testSuiteId: Option[Long]) = {
		DB.withSession { implicit session =>
			var query = for {
				conformanceResults <- PersistenceSchema.conformanceResults
				testCases <- PersistenceSchema.testCases if testCases.id === conformanceResults.testcase
				testSuites <- PersistenceSchema.testSuites if testSuites.id === conformanceResults.testsuite
			} yield (conformanceResults, testCases, testSuites)
			query = query
				.filter(_._1.actor === actorId)
				.filter(_._1.sut === sutId)
			if (testSuiteId.isDefined) {
				query = query.filter(_._1.testsuite === testSuiteId.get)
			}
			query = query.sortBy(x => (x._3.shortname, x._2.shortname))
			val results = query.list
			results
		}
	}

	def getConformanceStatementsFull(domainIds: Option[List[Long]], specIds: Option[List[Long]], actorIds: Option[List[Long]], communityIds: Option[List[Long]], organizationIds: Option[List[Long]], systemIds: Option[List[Long]]): List[ConformanceStatementFull] = {
		DB.withSession { implicit session =>
			var query = for {
				conformanceResults <- PersistenceSchema.conformanceResults
				specifications <- PersistenceSchema.specifications if specifications.id === conformanceResults.spec
				actors <- PersistenceSchema.actors if actors.id === conformanceResults.actor
				domains <- PersistenceSchema.domains if domains.id === actors.domain
				systems <- PersistenceSchema.systems if systems.id === conformanceResults.sut
				organizations <- PersistenceSchema.organizations if organizations.id === systems.owner
				communities <- PersistenceSchema.communities if communities.id === organizations.community
				testSuites <- PersistenceSchema.testSuites if testSuites.id === conformanceResults.testsuite
				testCases <- PersistenceSchema.testCases if testCases.id === conformanceResults.testcase
			} yield (conformanceResults, specifications, actors, domains, systems, organizations, communities, testSuites, testCases)
			if (domainIds.isDefined) {
				query = query.filter(_._4.id inSet domainIds.get)
			}
			if (specIds.isDefined) {
				query = query.filter(_._1.spec inSet specIds.get)
			}
			if (actorIds.isDefined) {
				query = query.filter(_._1.actor inSet actorIds.get)
			}
			if (communityIds.isDefined) {
				query = query.filter(_._7.id inSet communityIds.get)
			}
			if (organizationIds.isDefined) {
				query = query.filter(_._6.id inSet organizationIds.get)
			}
			if (systemIds.isDefined) {
				query = query.filter(_._1.sut inSet systemIds.get)
			}
			query = query.sortBy(x => (x._7.shortname, x._6.shortname, x._5.shortname, x._4.shortname, x._2.shortname, x._3.actorId, x._8.shortname, x._9.shortname))

			val results = query
			  	.sortBy(s => (s._4.shortname, s._2.shortname, s._3.actorId))
					.list
			var statements = new ListBuffer[ConformanceStatementFull]
			results.foreach { result =>
				val conformanceStatement = ConformanceStatementFull(
						result._7.id, result._7.shortname, result._6.id, result._6.shortname,
						result._5.id, result._5.shortname,
						result._4.id, result._4.shortname, result._4.fullname,
						result._3.id, result._3.actorId, result._3.name,
						result._2.id, result._2.shortname, result._2.fullname,
						Some(result._8.shortname), Some(result._9.shortname), result._9.description,
						Some(result._1.result), result._1.testsession, 0L, 0L, 0L)
				statements += conformanceStatement
			}
			statements.toList
		}
	}

	def getConformanceStatements(domainIds: Option[List[Long]], specIds: Option[List[Long]], actorIds: Option[List[Long]], communityIds: Option[List[Long]], organizationIds: Option[List[Long]], systemIds: Option[List[Long]]): List[ConformanceStatementFull] = {
		DB.withSession { implicit session =>
			var query = for {
					conformanceResults <- PersistenceSchema.conformanceResults
					specifications <- PersistenceSchema.specifications if specifications.id === conformanceResults.spec
					actors <- PersistenceSchema.actors if actors.id === conformanceResults.actor
					domains <- PersistenceSchema.domains if domains.id === actors.domain
					systems <- PersistenceSchema.systems if systems.id === conformanceResults.sut
					organizations <- PersistenceSchema.organizations if organizations.id === systems.owner
					communities <- PersistenceSchema.communities if communities.id === organizations.community
				} yield (conformanceResults, specifications, actors, domains, systems, organizations, communities)
			if (domainIds.isDefined) {
				query = query.filter(_._4.id inSet domainIds.get)
			}
			if (specIds.isDefined) {
				query = query.filter(_._1.spec inSet specIds.get)
			}
			if (actorIds.isDefined) {
				query = query.filter(_._1.actor inSet actorIds.get)
			}
			if (communityIds.isDefined) {
				query = query.filter(_._7.id inSet communityIds.get)
			}
			if (organizationIds.isDefined) {
				query = query.filter(_._6.id inSet organizationIds.get)
			}
			if (systemIds.isDefined) {
				query = query.filter(_._1.sut inSet systemIds.get)
			}
			query = query.sortBy(x => (x._7.shortname, x._6.shortname, x._5.shortname, x._4.shortname, x._2.shortname, x._3.actorId))

			val results = query.list
			val conformanceMap = new util.LinkedHashMap[String, ConformanceStatementFull]
			results.foreach { result =>
				val key = result._1.sut + "|" + result._1.actor
				var conformanceStatement = conformanceMap.get(key)
				if (conformanceStatement == null) {
					conformanceStatement = ConformanceStatementFull(
						result._7.id, result._7.shortname, result._6.id, result._6.shortname,
						result._5.id, result._5.shortname,
						result._4.id, result._4.shortname, result._4.fullname,
						result._3.id, result._3.actorId, result._3.name,
						result._2.id, result._2.shortname, result._2.fullname,
						None, None, result._1.testsession, None, None,
						0L, 0L, 0L)
					conformanceMap.put(key, conformanceStatement)
				}
				if (TestResultStatus.withName(result._1.result) == TestResultStatus.SUCCESS) {
					conformanceStatement.completedTests += 1
				} else if (TestResultStatus.withName(result._1.result) == TestResultStatus.FAILURE) {
          conformanceStatement.failedTests += 1
        } else {
          conformanceStatement.undefinedTests += 1
        }
			}
			var statements = new ListBuffer[ConformanceStatementFull]
			import scala.collection.JavaConversions._
			for (conformanceEntry <- conformanceMap) {
				statements += conformanceEntry._2
			}
			statements.toList
		}
	}

	def getConformanceCertificateSettingsWrapper(communityId: Long) = {
		DB.withSession { implicit session =>
			getConformanceCertificateSettings(communityId)
		}
	}

	def getConformanceCertificateSettings(communityId: Long)(implicit session:Session) = {
		PersistenceSchema.conformanceCertificates.filter(_.community === communityId).firstOption
	}

	def updateConformanceCertificateSettings(conformanceCertificate: ConformanceCertificates, updatePasswords: Boolean, removeKeystore: Boolean) = {
		DB.withTransaction { implicit session =>
			val existingSettings = getConformanceCertificateSettings(conformanceCertificate.community)
			if (existingSettings.isDefined) {

				if (removeKeystore && conformanceCertificate.keystoreFile.isEmpty) {
					val q = for {c <- PersistenceSchema.conformanceCertificates if c.id === existingSettings.get.id} yield (
						c.message, c.title, c.includeMessage, c.includeTestStatus, c.includeTestCases, c.includeDetails,
						c.includeSignature, c.keystoreFile, c.keystoreType, c.keystorePassword, c.keyPassword
					)
					q.update(
						conformanceCertificate.message,
						conformanceCertificate.title,
						conformanceCertificate.includeMessage,
						conformanceCertificate.includeTestStatus,
						conformanceCertificate.includeTestCases,
						conformanceCertificate.includeDetails,
						conformanceCertificate.includeSignature,
						None,
						None,
						None,
						None
					)
				} else {
					if (updatePasswords) {
						val q = for {c <- PersistenceSchema.conformanceCertificates if c.id === existingSettings.get.id} yield (
							c.message, c.title, c.includeMessage, c.includeTestStatus, c.includeTestCases, c.includeDetails,
							c.includeSignature, c.keystoreFile, c.keystoreType, c.keystorePassword, c.keyPassword
						)
						var keystorePasswordToUpdate = conformanceCertificate.keystorePassword
						if (keystorePasswordToUpdate.isDefined) {
							keystorePasswordToUpdate = Some(MimeUtil.encryptString(keystorePasswordToUpdate.get))
						}
						var keyPasswordToUpdate = conformanceCertificate.keyPassword
						if (keyPasswordToUpdate.isDefined) {
							keyPasswordToUpdate = Some(MimeUtil.encryptString(keyPasswordToUpdate.get))
						}
						q.update(
							conformanceCertificate.message,
							conformanceCertificate.title,
							conformanceCertificate.includeMessage,
							conformanceCertificate.includeTestStatus,
							conformanceCertificate.includeTestCases,
							conformanceCertificate.includeDetails,
							conformanceCertificate.includeSignature,
							conformanceCertificate.keystoreFile,
							conformanceCertificate.keystoreType,
							keystorePasswordToUpdate,
							keyPasswordToUpdate
						)
					} else {
						val q = for {c <- PersistenceSchema.conformanceCertificates if c.id === existingSettings.get.id} yield (
							c.message, c.title, c.includeMessage, c.includeTestStatus, c.includeTestCases, c.includeDetails,
							c.includeSignature, c.keystoreFile, c.keystoreType
						)
						q.update(
							conformanceCertificate.message,
							conformanceCertificate.title,
							conformanceCertificate.includeMessage,
							conformanceCertificate.includeTestStatus,
							conformanceCertificate.includeTestCases,
							conformanceCertificate.includeDetails,
							conformanceCertificate.includeSignature,
							conformanceCertificate.keystoreFile,
							conformanceCertificate.keystoreType
						)
					}
				}
			} else {
				PersistenceSchema.insertConformanceCertificate += conformanceCertificate
			}
		}
	}

  def deleteConformanceCertificateSettings(communityId: Long)(implicit session:Session) = {
    PersistenceSchema.conformanceCertificates.filter(_.community === communityId).delete
  }

}
