package managers

import java.util

import models.Enums.TestResultStatus
import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import utils.{MimeUtil, RepositoryUtils}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

object ConformanceManager extends BaseManager {
  def logger = LoggerFactory.getLogger("ConformanceManager")

	import dbConfig.driver.api._

	/**
	 * Checks if domain exists
	 */
	def checkDomainExists(domainId: Long): Boolean = {
		exec(PersistenceSchema.domains.filter(_.id === domainId).result.headOption).isDefined
	}

	def getDomainOfSpecification(specificationId: Long ): Domain = {
		val query = PersistenceSchema.domains
  			.join(PersistenceSchema.specifications).on(_.id === _.domain)
		val result = query
			.filter(_._2.id === specificationId)
			.result
			.headOption
		exec(result).get._1
	}

  def getDomains(ids: Option[List[Long]] = None):List[Domain] = {
		val domains = {
			val q = PersistenceSchema.domains
			val q2 = ids match {
				case Some(list) => q.filter(_.id inSet list)
				case None => q
			}
			q2.sortBy(_.shortname.asc)
				.result
  			.map(_.toList)
		}
		exec(domains)
  }

	def getCommunityDomain(communityId: Long): Domain = {
		val community = exec(PersistenceSchema.communities.filter(_.id === communityId).result.headOption).get

		val domain = community.domain match {
			case Some(d) => exec(PersistenceSchema.domains.filter(_.id === d).result.headOption).get
			case None => null
		}
		domain
	}

	def createDomain(domain:Domain) = {
		exec(PersistenceSchema.domains.returning(PersistenceSchema.domains.map(_.id)) += domain)
	}

	def createDomainParameter(parameter:DomainParameter) = {
		exec(PersistenceSchema.domainParameters.returning(PersistenceSchema.domainParameters.map(_.id)) += parameter)
	}

	def updateDomainParameter(parameterId: Long, name: String, description: Option[String], kind: String, value: Option[String]) = {
		val actions = new ListBuffer[DBIO[_]]()
		val q1 = for {d <- PersistenceSchema.domainParameters if d.id === parameterId} yield (d.name)
		actions += q1.update(name)

		val q2 = for {d <- PersistenceSchema.domainParameters if d.id === parameterId} yield (d.desc)
		actions += q2.update(description)

		val q3 = for {d <- PersistenceSchema.domainParameters if d.id === parameterId} yield (d.kind)
		actions += q3.update(kind)

		if (value.isDefined) {
			// If there is no value provided this means that we don't want to update this
			val q4 = for {d <- PersistenceSchema.domainParameters if d.id === parameterId} yield (d.value)
			actions += q4.update(value)
		}
		exec(DBIO.seq(actions.map(a => a): _*).transactionally)
	}

	def deleteDomainParameterWrapper(domainParameter: Long) = {
		exec(deleteDomainParameter(domainParameter).transactionally)
	}

	def deleteDomainParameter(domainParameter: Long) = {
		PersistenceSchema.domainParameters.filter(_.id === domainParameter).delete
	}

	def getDomainParameter(domainParameterId: Long) = {
		exec(PersistenceSchema.domainParameters.filter(_.id === domainParameterId).result.head)
	}

	def getDomainParameters(domainId: Long) = {
		exec(
			PersistenceSchema.domainParameters.filter(_.domain === domainId)
				.sortBy(_.name.asc)
				.result
  			.map(_.toList)
		)
	}

	def getDomainParameterByDomainAndName(domainId: Long, name: String) = {
		exec(
			PersistenceSchema.domainParameters
				.filter(_.domain === domainId)
				.filter(_.name === name)
				.result
  			.headOption
		)
	}

	def updateDomain(domainId: Long, shortName: String, fullName: String, description: Option[String]) = {
		val q = for {d <- PersistenceSchema.domains if d.id === domainId} yield (d.shortname, d.fullname, d.description)
		exec((
				q.update(shortName, fullName, description) andThen
				TestResultManager.updateForUpdatedDomain(domainId, shortName)
			).transactionally
		)
	}

	def deleteDomainParameters(domainId: Long) = {
		(for {
			ids <- PersistenceSchema.domainParameters.filter(_.domain === domainId).map(_.id).result
			_ <- DBIO.seq(ids.map(id => deleteDomainParameter(id)): _*)
		} yield()).transactionally
	}

	def deleteDomain(domain: Long) {
		exec(
			(
				ActorManager.deleteActorByDomain(domain) andThen
				SpecificationManager.deleteSpecificationByDomain(domain) andThen
				TransactionManager.deleteTransactionByDomain(domain) andThen
				TestResultManager.updateForDeletedDomain(domain) andThen
				deleteDomainParameters(domain) andThen
				PersistenceSchema.domains.filter(_.id === domain).delete
			).transactionally
		)
		RepositoryUtils.deleteDomainTestSuiteFolder(domain)
	}

	def getSpecifications(ids: Option[List[Long]] = None): List[Specifications] = {
		val specs = {
			val q = PersistenceSchema.specifications

			val q2 = ids match {
				case Some(list) => q.filter(_.id inSet list)
				case None => q
			}

			q2.sortBy(_.shortname.asc)
				.result
  			.map(_.toList)
		}
		exec(specs)
	}

  def getSpecifications(domain:Long): List[Specifications] = {
			val specs = PersistenceSchema.specifications.filter(_.domain === domain)
			  	.sortBy(_.shortname.asc)
					.result
  				.map(_.toList)
			exec(specs)
  }

	def createSpecifications(specification: Specifications) = {
		exec((PersistenceSchema.specifications.returning(PersistenceSchema.specifications.map(_.id)) += specification).transactionally)
	}

	def createActorWrapper(actor: Actors, specificationId: Long) = {
		exec(createActor(actor, specificationId).transactionally)
	}

	def createActor(actor: Actors, specificationId: Long) = {
		for {
			savedActorId <- PersistenceSchema.actors.returning(PersistenceSchema.actors.map(_.id)) += actor
			_ <- {
				val actions = new ListBuffer[DBIO[_]]()
				actions += (PersistenceSchema.specificationHasActors += (specificationId, savedActorId))
				if (actor.default.isDefined && actor.default.get) {
					// Ensure no other default actors are defined.
					actions += ActorManager.setOtherActorsAsNonDefault(savedActorId, specificationId)
				}
				DBIO.seq(actions.map(a => a): _*)
			}
		} yield savedActorId
	}

	def getActorsWithDomainId(domainId: Long): List[Actors] = {
		exec(
			PersistenceSchema.actors.filter(_.domain === domainId)
			  .sortBy(_.actorId.desc)
				.result
  			.map(_.toList)
		)
	}

  def getActorsWithSpecificationId(actorIds:Option[List[Long]], spec:Option[Long]): List[Actor] = {
		var actors: List[Actor] = List()

		var query = PersistenceSchema.actors
  			.join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
		if (actorIds.isDefined) {
			query = actorIds match {
				case Some(list) => query.filter(_._1.id inSet list)
				case None => query
			}
		}
		if (spec.isDefined) {
			query = query.filter(_._2.specId === spec.get)
		}

		exec(query.sortBy(_._1.actorId.desc).result).foreach { result =>
			actors ::= new Actor(result._1, null, null, result._2._1)
		}

		actors
  }

	def relateActorWithSpecification(actorId: Long, specificationId: Long) = {
		exec((PersistenceSchema.specificationHasActors += (specificationId, actorId)).transactionally)
	}

	def createOption(option:Options) = {
		exec((PersistenceSchema.options += option).transactionally)
	}

	def getOptionsForActor(actorId:Long): List[Options] = {
		exec(PersistenceSchema.options.filter(_.actor === actorId).result.map(_.toList))
	}

	def getEndpointsForActor(actorId: Long): List[Endpoint] = {
		val endpoints = new ListBuffer[Endpoint]()
		exec(PersistenceSchema.endpoints.filter(_.actor === actorId).sortBy(_.name.asc).result).map { caseObject =>
			val actor = exec(PersistenceSchema.actors.filter(_.id === caseObject.actor).result.head)
			val parameters = exec(PersistenceSchema.parameters.filter(_.endpoint === caseObject.id).result.map(_.toList))
			endpoints += new Endpoint(caseObject, actor, parameters)
		}
		endpoints.toList
	}

	def getEndpoints(ids: Option[List[Long]]): List[Endpoint] = {
		val endpoints = new ListBuffer[Endpoint]()
		val q = ids match {
			case Some(list) => PersistenceSchema.endpoints.filter(_.id inSet list)
			case None => PersistenceSchema.endpoints
		}
		exec(q.sortBy(_.name.asc).result).map { caseObject =>
			val actor = exec(PersistenceSchema.actors.filter(_.id === caseObject.actor).result.head)
			val parameters = exec(PersistenceSchema.parameters.filter(_.endpoint === caseObject.id).result.map(_.toList))
			endpoints += new Endpoint(caseObject, actor, parameters)
		}
		endpoints.toList
	}

	def getParameters(ids: Option[List[Long]]): List[models.Parameters] = {
		val q = ids match {
			case Some(ids) => PersistenceSchema.parameters.filter(_.id inSet ids)
			case None => PersistenceSchema.parameters
		}
		exec(q.sortBy(_.name.asc).result.map(_.toList))
	}

	def getEndpointParameters(endpointId: Long): List[models.Parameters] = {
		exec(
			PersistenceSchema.parameters.filter(_.endpoint === endpointId)
			  .sortBy(_.name.asc)
				.result
  			.map(_.toList)
		)
	}

	def getOptions(ids:Option[List[Long]]): List[Options] = {
		val options = {
			val q = PersistenceSchema.options

			val q2 = ids match {
				case Some(s) => q.filter(_.actor inSet s)
				case None => q
			}

			q2.result.map(_.toList)
		}
		exec(options)
	}

	def getById(id: Long) = {
		exec(PersistenceSchema.domains.filter(_.id === id).result.head)
	}

	def getConformanceStatus(actorId: Long, sutId: Long, testSuiteId: Option[Long]) = {
		var query = PersistenceSchema.conformanceResults
  			.join(PersistenceSchema.testCases).on(_.testcase === _.id)
  			.join(PersistenceSchema.testSuites).on(_._1.testsuite === _.id)
		query = query
			.filter(_._1._1.actor === actorId)
			.filter(_._1._1.sut === sutId)
		if (testSuiteId.isDefined) {
			query = query.filter(_._1._1.testsuite === testSuiteId.get)
		}
		query = query
			.sortBy(x => (x._2.shortname))
		val results = exec(query.result.map(_.toList)).map(r => {
			(r._1._1.copy(), r._1._2.copy(), r._2.copy())
		})
		results
	}

	def getConformanceStatementsFull(domainIds: Option[List[Long]], specIds: Option[List[Long]], actorIds: Option[List[Long]], communityIds: Option[List[Long]], organizationIds: Option[List[Long]], systemIds: Option[List[Long]]): List[ConformanceStatementFull] = {
		var query = PersistenceSchema.conformanceResults
			.join(PersistenceSchema.specifications).on(_.spec === _.id)
			.join(PersistenceSchema.actors).on(_._1.actor === _.id)
			.join(PersistenceSchema.domains).on(_._2.domain === _.id)
			.join(PersistenceSchema.systems).on(_._1._1._1.sut === _.id)
			.join(PersistenceSchema.organizations).on(_._2.owner === _.id)
			.join(PersistenceSchema.communities).on(_._2.community === _.id)
  		.join(PersistenceSchema.testSuites).on(_._1._1._1._1._1._1.testsuite === _.id)
  		.join(PersistenceSchema.testCases).on(_._1._1._1._1._1._1._1.testcase === _.id)
		if (domainIds.isDefined) {
			query = query.filter(_._1._1._1._1._1._2.id inSet domainIds.get)
		}
		if (specIds.isDefined) {
			query = query.filter(_._1._1._1._1._1._1._1._1.spec inSet specIds.get)
		}
		if (actorIds.isDefined) {
			query = query.filter(_._1._1._1._1._1._1._1._1.actor inSet actorIds.get)
		}
		if (communityIds.isDefined) {
			query = query.filter(_._1._1._2.id inSet communityIds.get)
		}
		if (organizationIds.isDefined) {
			query = query.filter(_._1._1._1._2.id inSet organizationIds.get)
		}
		if (systemIds.isDefined) {
			query = query.filter(_._1._1._1._1._1._1._1._1.sut inSet systemIds.get)
		}
		query = query.sortBy(x => (x._1._1._2.shortname, x._1._1._1._2.shortname, x._1._1._1._1._2.shortname, x._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._2.actorId, x._1._2.shortname, x._2.shortname))

		val results = exec(query
			.result
  		.map(_.toList))

		var statements = new ListBuffer[ConformanceStatementFull]
		results.foreach { result =>
			val resultCommunities = result._1._1._2
			val resultConfResult = result._1._1._1._1._1._1._1._1
			val resultTestSuite= result._1._2
			val resultTestCase = result._2
			val resultOrganisation = result._1._1._1._2
			val resultSystem = result._1._1._1._1._2
			val resultDomain = result._1._1._1._1._1._2
			val resultActor = result._1._1._1._1._1._1._2

			val conformanceStatement = ConformanceStatementFull(
				resultCommunities.id, resultCommunities.shortname, resultOrganisation.id, resultOrganisation.shortname,
					resultSystem.id, resultSystem.shortname,
					resultDomain.id, resultDomain.shortname, resultDomain.fullname,
					resultActor.id, resultActor.actorId, resultActor.name,
					result._2.id, result._2.shortname, result._2.fullname,
					Some(resultTestSuite.shortname), Some(resultTestCase.shortname), resultTestCase.description,
					Some(resultConfResult.result), resultConfResult.testsession, 0L, 0L, 0L)
			statements += conformanceStatement
		}
		statements.toList
	}

	def getConformanceStatements(domainIds: Option[List[Long]], specIds: Option[List[Long]], actorIds: Option[List[Long]], communityIds: Option[List[Long]], organizationIds: Option[List[Long]], systemIds: Option[List[Long]]): List[ConformanceStatementFull] = {
		var query = PersistenceSchema.conformanceResults
			.join(PersistenceSchema.specifications).on(_.spec === _.id)
			.join(PersistenceSchema.actors).on(_._1.actor === _.id)
			.join(PersistenceSchema.domains).on(_._2.domain === _.id)
			.join(PersistenceSchema.systems).on(_._1._1._1.sut === _.id)
  		.join(PersistenceSchema.organizations).on(_._2.owner === _.id)
  		.join(PersistenceSchema.communities).on(_._2.community === _.id)
		if (domainIds.isDefined) {
			query = query.filter(_._1._1._1._2.id inSet domainIds.get)
		}
		if (specIds.isDefined) {
			query = query.filter(_._1._1._1._1._1._1.spec inSet specIds.get)
		}
		if (actorIds.isDefined) {
			query = query.filter(_._1._1._1._1._1._1.actor inSet actorIds.get)
		}
		if (communityIds.isDefined) {
			query = query.filter(_._2.id inSet communityIds.get)
		}
		if (organizationIds.isDefined) {
			query = query.filter(_._1._2.id inSet organizationIds.get)
		}
		if (systemIds.isDefined) {
			query = query.filter(_._1._1._1._1._1._1.sut inSet systemIds.get)
		}
		query = query.sortBy(x => (x._2.shortname, x._1._2.shortname, x._1._1._2.shortname, x._1._1._1._2.shortname, x._1._1._1._1._1._2.shortname, x._1._1._1._1._2.actorId))

		val results = exec(query.result.map(_.toList))
		val conformanceMap = new util.LinkedHashMap[String, ConformanceStatementFull]
		results.foreach { result =>
			val resultConfResult = result._1._1._1._1._1._1
			val resultCommunity = result._2
			val resultOrganisation = result._1._2
			val resultSystem = result._1._1._2
			val resultDomain = result._1._1._1._2
			val resultSpecification = result._1._1._1._1._1._2
			val resultActor = result._1._1._1._1._2

			val key = resultConfResult.sut + "|" + resultConfResult.actor
			var conformanceStatement = conformanceMap.get(key)
			if (conformanceStatement == null) {
				conformanceStatement = ConformanceStatementFull(
					resultCommunity.id, resultCommunity.shortname, resultOrganisation.id, resultOrganisation.shortname,
					resultSystem.id, resultSystem.shortname,
					resultDomain.id, resultDomain.shortname, resultDomain.fullname,
					resultActor.id, resultActor.actorId, resultActor.name,
					resultSpecification.id, resultSpecification.shortname, resultSpecification.fullname,
					None, None, resultConfResult.testsession, None, None,
					0L, 0L, 0L)
				conformanceMap.put(key, conformanceStatement)
			}
			if (TestResultStatus.withName(resultConfResult.result) == TestResultStatus.SUCCESS) {
				conformanceStatement.completedTests += 1
			} else if (TestResultStatus.withName(resultConfResult.result) == TestResultStatus.FAILURE) {
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

	def getConformanceCertificateSettingsWrapper(communityId: Long) = {
		exec(getConformanceCertificateSettings(communityId))
	}

	def getConformanceCertificateSettings(communityId: Long) = {
		PersistenceSchema.conformanceCertificates.filter(_.community === communityId).result.headOption
	}

	def updateConformanceCertificateSettings(conformanceCertificate: ConformanceCertificates, updatePasswords: Boolean, removeKeystore: Boolean) = {
		exec(
			(for {
				existingSettings <- getConformanceCertificateSettings(conformanceCertificate.community)
				_ <- {
					var action: DBIO[_] = null
					if (existingSettings.isDefined) {
						if (removeKeystore && conformanceCertificate.keystoreFile.isEmpty) {
							val q = for {c <- PersistenceSchema.conformanceCertificates if c.id === existingSettings.get.id} yield (
								c.message, c.title, c.includeMessage, c.includeTestStatus, c.includeTestCases, c.includeDetails,
								c.includeSignature, c.keystoreFile, c.keystoreType, c.keystorePassword, c.keyPassword
							)
							action = q.update(
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
								action = q.update(
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
								action = q.update(
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
						action = PersistenceSchema.insertConformanceCertificate += conformanceCertificate
					}
					action
				}
			} yield()).transactionally
		)
	}

  def deleteConformanceCertificateSettings(communityId: Long) = {
    PersistenceSchema.conformanceCertificates.filter(_.community === communityId).delete
  }

}
