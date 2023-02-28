package managers

import models.Enums.TriggerDataType
import models._
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{CryptoUtil, MimeUtil, RepositoryUtils}

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ConformanceManager @Inject() (systemManager: SystemManager, triggerManager: TriggerManager, actorManager: ActorManager, testResultManager: TestResultManager, testCaseManager: TestCaseManager, repositoryUtils: RepositoryUtils, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  def logger = LoggerFactory.getLogger("ConformanceManager")

	import dbConfig.profile.api._

	/**
	 * Checks if domain exists
	 */
	def checkDomainExists(domainId: Long): Boolean = {
		exec(PersistenceSchema.domains.filter(_.id === domainId).result.headOption).isDefined
	}

  def getDomainOfSpecification(specificationId: Long ): Domain = {
		exec(
			PersistenceSchema.domains
				.join(PersistenceSchema.specifications).on(_.id === _.domain)
				.filter(_._2.id === specificationId)
				.map(x => x._1)
				.result
				.head
		)
	}

  def getDomains(ids: Option[List[Long]] = None):List[Domain] = {
		exec(
			PersistenceSchema.domains
				.filterOpt(ids)((q, ids) => q.id inSet ids)
				.sortBy(_.shortname.asc)
				.result
				.map(_.toList)
		)
  }

	def getCommunityDomain(communityId: Long): Domain = {
		val community = exec(PersistenceSchema.communities.filter(_.id === communityId).result.headOption).get

		val domain = community.domain match {
			case Some(d) => exec(PersistenceSchema.domains.filter(_.id === d).result.headOption).get
			case None => null
		}
		domain
	}

	def createDomainInternal(domain:Domain): DBIO[Long] = {
		PersistenceSchema.domains.returning(PersistenceSchema.domains.map(_.id)) += domain
	}

	def createDomain(domain:Domain): Long = {
		exec(createDomainInternal(domain))
	}

	def createDomainParameterInternal(parameter:DomainParameter, fileToStore: Option[File], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Long] = {
		for {
			id <- PersistenceSchema.domainParameters.returning(PersistenceSchema.domainParameters.map(_.id)) += parameter
			_ <- {
				if (fileToStore.isDefined) {
					onSuccessCalls += (() => repositoryUtils.setDomainParameterFile(parameter.domain, id, fileToStore.get))
				}
				DBIO.successful(())
			}
		} yield id
	}

	def createDomainParameter(parameter:DomainParameter, fileToStore: Option[File]) = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val dbAction = createDomainParameterInternal(parameter, fileToStore, onSuccessCalls)
		exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
	}

	def updateDomainParameter(domainId: Long, parameterId: Long, name: String, description: Option[String], kind: String, value: Option[String], inTests: Boolean, contentType: Option[String], fileToStore: Option[File]) = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val dbAction = updateDomainParameterInternal(domainId, parameterId, name, description, kind, value, inTests, contentType, fileToStore, onSuccessCalls)
		exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
	}

	def updateDomainParameterInternal(domainId: Long, parameterId: Long, name: String, description: Option[String], kind: String, value: Option[String], inTests: Boolean, contentType: Option[String], fileToStore: Option[File], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
		if (kind == "BINARY") {
			if (fileToStore.isDefined) {
				onSuccessCalls += (() => repositoryUtils.setDomainParameterFile(domainId, parameterId, fileToStore.get))
				PersistenceSchema.domainParameters.filter(_.id === parameterId)
					.map(x => (x.name, x.desc, x.kind, x.inTests, x.value, x.contentType))
					.update((name, description, kind, inTests, value, contentType))
			} else {
				PersistenceSchema.domainParameters.filter(_.id === parameterId)
					.map(x => (x.name, x.desc, x.kind, x.inTests, x.value))
					.update((name, description, kind, inTests, value))
			}
		} else {
			onSuccessCalls += (() => repositoryUtils.deleteDomainParameterFile(domainId, parameterId))
			if (kind == "SIMPLE" || (kind == "HIDDEN" && value.isDefined)) {
				PersistenceSchema.domainParameters.filter(_.id === parameterId)
					.map(x => (x.name, x.desc, x.kind, x.inTests, x.value, x.contentType))
					.update((name, description, kind, inTests, value, None))
			} else { // HIDDEN no value
				PersistenceSchema.domainParameters.filter(_.id === parameterId)
					.map(x => (x.name, x.desc, x.kind, x.inTests, x.contentType))
					.update((name, description, kind, inTests, None))
			}
		}
	}

	def deleteDomainParameterWrapper(domainId: Long, domainParameter: Long) = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val dbAction = deleteDomainParameter(domainId, domainParameter, onSuccessCalls)
		exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
	}

	def deleteDomainParameter(domainId: Long, domainParameter: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
		onSuccessCalls += (() => repositoryUtils.deleteDomainParameterFile(domainId, domainParameter))
		triggerManager.deleteTriggerDataByDataType(domainParameter, TriggerDataType.DomainParameter) andThen
			PersistenceSchema.domainParameters.filter(_.id === domainParameter).delete
	}

	def getDomainParameter(domainParameterId: Long) = {
		exec(PersistenceSchema.domainParameters.filter(_.id === domainParameterId).result.head)
	}

	def getDomainParametersByCommunityId(communityId: Long): List[DomainParameter] = {
		exec(
			for {
				domainId <- PersistenceSchema.communities.filter(_.id === communityId).map(x => x.domain).result.head
				domainParameters <- {
					if (domainId.isDefined) {
						PersistenceSchema.domainParameters
							.filter(_.domain === domainId.get)
							.map(x => (x.id, x.name, x.kind))
							.sortBy(_._2.asc)
							.result
							.map(_.toList.map(x => DomainParameter(x._1, x._2, None, x._3, None, inTests = false, None, domainId.get)))
					} else {
						DBIO.successful(List[DomainParameter]())
					}
				}
			} yield domainParameters
		)
	}

	def getDomainParameters(domainId: Long, loadValues: Boolean, onlyForTests: Option[Boolean]): List[DomainParameter] = {
		val query = PersistenceSchema.domainParameters.filter(_.domain === domainId)
			.filterOpt(onlyForTests)((table, filterValue) => table.inTests === filterValue)
			.sortBy(_.name.asc)
		if (loadValues) {
			exec(
				query
					.result
					.map(_.toList)
			)
		} else {
			exec(
				query
					.map(x => (x.id, x.name, x.desc, x.kind, x.inTests, x.contentType))
				.result
			).map(x => DomainParameter(x._1, x._2, x._3, x._4, None, x._5, x._6, domainId)).toList
		}
	}

	def getDomainParameters(domainId: Long): List[DomainParameter] = {
		getDomainParameters(domainId, loadValues = true, None)
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
		exec(updateDomainInternal(domainId, shortName, fullName, description).transactionally)
	}

	def updateDomainInternal(domainId: Long, shortName: String, fullName: String, description: Option[String]) = {
		val q = for {d <- PersistenceSchema.domains if d.id === domainId} yield (d.shortname, d.fullname, d.description)
		q.update(shortName, fullName, description) andThen
			testResultManager.updateForUpdatedDomain(domainId, shortName)
	}

	def deleteSpecification(specId: Long) = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val action = delete(specId, onSuccessCalls)
		exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
	}

	def delete(specId: Long, onSuccessCalls: mutable.ListBuffer[() => _]) = {
		for {
			ids <- PersistenceSchema.specifications.filter(_.id === specId).map(x => (x.id, x.domain)).result.head
			_ <- testResultManager.updateForDeletedSpecification(specId)
			// Delete also actors from the domain (they are now linked only to specifications
			actorIds <- PersistenceSchema.specificationHasActors.filter(_.specId === specId).map(_.actorId).result
			_ <- DBIO.seq(actorIds.map(id => actorManager.deleteActor(id, onSuccessCalls)): _*)
			_ <- PersistenceSchema.specificationHasActors.filter(_.specId === specId).delete
			testSuiteIds <- {
				PersistenceSchema.specificationHasTestSuites
					.join(PersistenceSchema.testSuites).on(_.testSuiteId === _.id)
					.filter(_._1.specId === specId)
					.filter(_._2.shared =!= true) // We must keep shared test suites.
					.map(_._2.id)
					.result
			}
			_ <- DBIO.seq(testSuiteIds.map(id => undeployTestSuite(id, onSuccessCalls)): _*)
			_ <- PersistenceSchema.specificationHasTestSuites.filter(_.specId === specId).delete
			_ <- PersistenceSchema.conformanceResults.filter(_.spec === specId).delete
			_ <- PersistenceSchema.specifications.filter(_.id === specId).delete
		} yield ()
	}

	def undeployTestSuiteWrapper(testSuiteId: Long) = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val action = undeployTestSuite(testSuiteId, onSuccessCalls)
		exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
	}

	def undeployTestSuite(testSuiteId: Long, onSuccessCalls: mutable.ListBuffer[() => _]) = {
		testResultManager.updateForDeletedTestSuite(testSuiteId) andThen
			PersistenceSchema.specificationHasTestSuites.filter(_.testSuiteId === testSuiteId).delete andThen
			PersistenceSchema.testSuiteHasActors.filter(_.testsuite === testSuiteId).delete andThen
			PersistenceSchema.conformanceResults.filter(_.testsuite === testSuiteId).delete andThen
			(for {
				testCases <- PersistenceSchema.testSuiteHasTestCases.filter(_.testsuite === testSuiteId).map(_.testcase).result
				_ <- DBIO.seq(testCases.map(testCase => {
					testResultManager.updateForDeletedTestCase(testCase) andThen
						testCaseManager.removeActorLinksForTestCase(testCase) andThen
						PersistenceSchema.testCaseCoversOptions.filter(_.testcase === testCase).delete andThen
						PersistenceSchema.testSuiteHasTestCases.filter(_.testcase === testCase).delete andThen
						PersistenceSchema.testCases.filter(_.id === testCase).delete
				}): _*)
			} yield()) andThen
			(for {
				testSuite <- PersistenceSchema.testSuites.filter(_.id === testSuiteId).result.head
				_ <- {
					onSuccessCalls += (() => {
						repositoryUtils.undeployTestSuite(testSuite.domain, testSuite.filename)
					})
					DBIO.successful(())
				}
			} yield testSuite) andThen
			PersistenceSchema.testSuites.filter(_.id === testSuiteId).delete
	}


	def deleteDomainParameters(domainId: Long, onSuccessCalls: ListBuffer[() => _]) = {
		(for {
			ids <- PersistenceSchema.domainParameters.filter(_.domain === domainId).map(_.id).result
			_ <- DBIO.seq(ids.map(id => deleteDomainParameter(domainId, id, onSuccessCalls)): _*)
		} yield()).transactionally
	}

	def deleteSpecificationByDomain(domainId: Long, onSuccessCalls: mutable.ListBuffer[() => _]) = {
		val action = (for {
			ids <- PersistenceSchema.specifications.filter(_.domain === domainId).map(_.id).result
			_ <- DBIO.seq(ids.map(id => delete(id, onSuccessCalls)): _*)
		} yield ()).transactionally
		action
	}

	private def deleteTransactionByDomain(domainId: Long) = {
		PersistenceSchema.transactions.filter(_.domain === domainId).delete
	}

	def deleteDomainInternal(domain: Long, onSuccessCalls: ListBuffer[() => _]): DBIO[_] = {
		for {
			_ <- removeDomainFromCommunities(domain, onSuccessCalls)
			_ <- deleteSpecificationByDomain(domain, onSuccessCalls)
			sharedTestSuiteIds <- PersistenceSchema.testSuites.filter(_.domain === domain).map(_.id).result
			_ <- DBIO.seq(sharedTestSuiteIds.map(undeployTestSuite(_, onSuccessCalls)): _*)
			_ <- deleteTransactionByDomain(domain)
			_ <- testResultManager.updateForDeletedDomain(domain)
			_ <- deleteDomainParameters(domain, onSuccessCalls)
			_ <- PersistenceSchema.domains.filter(_.id === domain).delete
			_ <- {
				onSuccessCalls += (() => {
					repositoryUtils.deleteDomainTestSuiteFolder(domain)
				})
				DBIO.successful(())
			}
		} yield ()
	}

	def deleteDomain(domain: Long): Unit = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val action = deleteDomainInternal(domain, onSuccessCalls)
		exec(
			dbActionFinalisation(Some(onSuccessCalls), None, action)
			.transactionally
		)
	}

	def getSpecifications(ids: Option[List[Long]] = None, domainIds: Option[List[Long]] = None): List[Specifications] = {
		exec(
			PersistenceSchema.specifications
				.filterOpt(ids)((q, ids) => q.id inSet ids)
				.filterOpt(domainIds)((q, domainIds) => q.domain inSet domainIds)
				.sortBy(_.shortname.asc)
				.result
				.map(_.toList)
		)
	}

  def getSpecifications(domain:Long): List[Specifications] = {
			val specs = PersistenceSchema.specifications.filter(_.domain === domain)
			  	.sortBy(_.shortname.asc)
					.result
  				.map(_.toList)
			exec(specs)
  }

	def createSpecificationsInternal(specification: Specifications): DBIO[Long] = {
		createSpecificationsInternal(specification, checkApiKeyUniqueness = false)
	}

	def createSpecificationsInternal(specification: Specifications, checkApiKeyUniqueness: Boolean): DBIO[Long] = {
		for {
			replaceApiKey <- if (checkApiKeyUniqueness) {
				PersistenceSchema.specifications.filter(_.apiKey === specification.apiKey).exists.result
			} else {
				DBIO.successful(false)
			}
			newSpecId <- {
				val specToUse = if (replaceApiKey) specification.withApiKey(CryptoUtil.generateApiKey()) else specification
				PersistenceSchema.specifications.returning(PersistenceSchema.specifications.map(_.id)) += specToUse
			}
		} yield newSpecId
	}

	def createSpecifications(specification: Specifications) = {
		exec(createSpecificationsInternal(specification).transactionally)
	}

	def createActorWrapper(actor: Actors, specificationId: Long) = {
		exec(createActor(actor, specificationId).transactionally)
	}
	def createActor(actor: Actors, specificationId: Long): DBIO[Long] = {
		createActor(actor, specificationId, checkApiKeyUniqueness = false)
	}

	def createActor(actor: Actors, specificationId: Long, checkApiKeyUniqueness: Boolean): DBIO[Long] = {
		for {
			replaceApiKey <- if (checkApiKeyUniqueness) {
				PersistenceSchema.actors.filter(_.apiKey === actor.apiKey).exists.result
			} else {
				DBIO.successful(false)
			}
			savedActorId <- {
				val actorToUse = if (replaceApiKey) actor.withApiKey(CryptoUtil.generateApiKey()) else actor
				PersistenceSchema.actors.returning(PersistenceSchema.actors.map(_.id)) += actorToUse
			}
			_ <- {
				val actions = new ListBuffer[DBIO[_]]()
				actions += (PersistenceSchema.specificationHasActors += (specificationId, savedActorId))
				if (actor.default.isDefined && actor.default.get) {
					// Ensure no other default actors are defined.
					actions += actorManager.setOtherActorsAsNonDefault(savedActorId, specificationId)
				}
				DBIO.seq(actions.toList.map(a => a): _*)
			}
		} yield savedActorId
	}

	def searchActors(domainIds: Option[List[Long]], specificationIds: Option[List[Long]]): List[Actor] = {
		exec(
			PersistenceSchema.actors
				.join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
				.filterOpt(domainIds)((q, ids) => q._1.domain inSet ids)
				.filterOpt(specificationIds)((q, ids) => q._2.specId inSet ids)
				.sortBy(_._1.actorId.asc)
				.map(x => (x._1, x._2.specId))
				.result
		).map(x => new Actor(x._1, null, null, x._2)).toList
	}

	def getActorIdsOfSpecifications(specIds:List[Long]): Map[Long, Set[String]] = {
		val results = exec(
			PersistenceSchema.actors
			.join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
		  .filter(_._2.specId inSet specIds)
		  .map(x => (x._1.actorId, x._2.specId))
		  .result
		  .map(_.toList)
		)
		val specMap = mutable.Map[Long, mutable.Set[String]]()
		results.foreach { result =>
			var actorIdSet = specMap.get(result._2)
			if (actorIdSet.isEmpty) {
				actorIdSet = Some(mutable.Set[String]())
				specMap += (result._2 -> actorIdSet.get)
			}
			actorIdSet.get += result._1
		}
		// Add empty sets for spec IDs with no results.
		specIds.foreach { specId =>
			if (!specMap.contains(specId)) {
				specMap += (specId -> mutable.Set[String]())
			}
		}
		specMap.iterator.toMap.map(x => (x._1, x._2.toSet))
	}

  def getActorsWithSpecificationId(actorIds:Option[List[Long]], specIds:Option[List[Long]]): List[Actor] = {
		exec(
			PersistenceSchema.actors
				.join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
				.filterOpt(actorIds)((q, ids) => q._1.id inSet ids)
				.filterOpt(specIds)((q, ids) => q._2.specId inSet ids)
				.sortBy(_._1.actorId.asc)
				.map(x => (x._1, x._2.specId))
				.result
		).map(x => new Actor(x._1, null, null, x._2)).toList
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

	def getEndpointsCaseForActor(actorId: Long): List[Endpoints] = {
		exec(PersistenceSchema.endpoints.filter(_.actor === actorId).sortBy(_.name.asc).result).toList
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
			val parameters = exec(PersistenceSchema.parameters.filter(_.endpoint === caseObject.id).sortBy(x => (x.displayOrder.asc, x.name.asc)).result.map(_.toList))
			endpoints += new Endpoint(caseObject, actor, parameters)
		}
		endpoints.toList
	}

	def getParameters(ids: Option[List[Long]]): List[models.Parameters] = {
		val q = ids match {
			case Some(ids) => PersistenceSchema.parameters.filter(_.id inSet ids)
			case None => PersistenceSchema.parameters
		}
		exec(q.sortBy(x => (x.displayOrder.asc, x.name.asc)).result.map(_.toList))
	}

	def getEndpointParameters(endpointId: Long): List[models.Parameters] = {
		exec(
			PersistenceSchema.parameters.filter(_.endpoint === endpointId)
			  .sortBy(x => (x.displayOrder.asc, x.name.asc))
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

	def getCompletedConformanceStatementsForTestSession(systemId: Long, sessionId: String): List[Long] = { // Actor IDs considered as completed.
		exec(
			for {
				relatedActorIds <- PersistenceSchema.conformanceResults
					.filter(_.testsession === sessionId)
					.map(_.actor)
					.result
				conformanceResults <- PersistenceSchema.conformanceResults
					.filter(_.sut === systemId)
					.filter(_.actor inSet relatedActorIds)
					.map(x => (x.actor, x.result))
					.result
				completedActors <- {
					val map = mutable.LinkedHashMap[Long, Boolean]()
					conformanceResults.foreach { actorInfo =>
						val currentIsSuccess = "SUCCESS".equals(actorInfo._2)
						val overallIsSuccess = map.getOrElseUpdate(actorInfo._1, currentIsSuccess)
						if (overallIsSuccess && !currentIsSuccess) {
							map.put(actorInfo._1, currentIsSuccess)
						}
					}
					DBIO.successful(map.filter(_._2).keys.toList)
				}
			} yield completedActors
		)
	}

	def getConformanceStatus(actorId: Long, sutId: Long, testSuiteId: Option[Long]): List[ConformanceStatusItem] = {
		exec(
			PersistenceSchema.conformanceResults
				.join(PersistenceSchema.testCases).on(_.testcase === _.id)
				.join(PersistenceSchema.testSuites).on(_._1.testsuite === _.id)
				.filter(_._1._1.actor === actorId)
				.filter(_._1._1.sut === sutId)
				.filterOpt(testSuiteId)((q, id) => q._1._1.testsuite === id)
				.sortBy(x => (x._2.shortname, x._1._2.testSuiteOrder))
				.map(x => (x._2.id, x._2.shortname, x._2.description, x._2.hasDocumentation, x._1._2.id, x._1._2.shortname, x._1._2.description, x._1._2.hasDocumentation, x._1._1.result, x._1._1.outputMessage, x._1._1.testsession, x._1._1.updateTime))
				.result
				.map(_.toList)
		).map(r => {
			ConformanceStatusItem(r._1, r._2, r._3, r._4, r._5, r._6, r._7, r._8, r._9, r._10, r._11, r._12)
		})
	}

	def getSpecificationIdForTestCaseFromConformanceStatements(testCaseId: Long): Option[Long] = {
		val spec = exec(PersistenceSchema.conformanceResults.filter(_.testcase === testCaseId).map(c => {c.spec}).result.headOption)
		spec
	}

	def getSpecificationIdForTestSuiteFromConformanceStatements(testSuiteId: Long): Option[Long] = {
		val spec = exec(PersistenceSchema.conformanceResults.filter(_.testsuite === testSuiteId).map( c => {c.spec}).result.headOption)
		spec
	}

	def getConformanceStatementsFull(domainIds: Option[List[Long]], specIds: Option[List[Long]], actorIds: Option[List[Long]], communityIds: Option[List[Long]], organizationIds: Option[List[Long]], systemIds: Option[List[Long]], orgParameters: Option[Map[Long, Set[String]]], sysParameters: Option[Map[Long, Set[String]]], status: Option[List[String]], updateTimeStart: Option[String], updateTimeEnd: Option[String], sortColumn: Option[String], sortOrder: Option[String]): List[ConformanceStatementFull] = {
		var query = PersistenceSchema.conformanceResults
			.join(PersistenceSchema.specifications).on(_.spec === _.id)
			.join(PersistenceSchema.actors).on(_._1.actor === _.id)
			.join(PersistenceSchema.domains).on(_._2.domain === _.id)
			.join(PersistenceSchema.systems).on(_._1._1._1.sut === _.id)
			.join(PersistenceSchema.organizations).on(_._2.owner === _.id)
			.join(PersistenceSchema.communities).on(_._2.community === _.id)
  		.join(PersistenceSchema.testSuites).on(_._1._1._1._1._1._1.testsuite === _.id)
  		.join(PersistenceSchema.testCases).on(_._1._1._1._1._1._1._1.testcase === _.id)
			.filterOpt(domainIds)((q, ids) => q._1._1._1._1._1._2.id inSet ids)
			.filterOpt(specIds)((q, ids) => q._1._1._1._1._1._1._1._1.spec inSet ids)
			.filterOpt(actorIds)((q, ids) => q._1._1._1._1._1._1._1._1.actor inSet ids)
			.filterOpt(communityIds)((q, ids) => q._1._1._2.id inSet ids)
			.filterOpt(organisationIdsToUse(organizationIds, orgParameters))((q, ids) => q._1._1._1._2.id inSet ids)
			.filterOpt(systemIdsToUse(systemIds, sysParameters))((q, ids) => q._1._1._1._1._1._1._1._1.sut inSet ids)

		// Apply sorting
		val sortColumnToApply = sortColumn.getOrElse("community")
		val sortOrderToApply = sortOrder.getOrElse("asc")
		query = query.sortBy(x => {
			val community = x._1._1._2.shortname.asc
			val organisation = x._1._1._1._2.shortname.asc
			val system = x._1._1._1._1._2.shortname.asc
			val domain = x._1._1._1._1._1._2.shortname.asc
			val specification = x._1._1._1._1._1._1._1._2.shortname.asc
			val actor = x._1._1._1._1._1._1._2.actorId.asc
			val testSuite = x._1._2.shortname.asc
			val testCase = x._2.shortname.asc
			sortColumnToApply match {
				case "organisation" => (if (sortOrderToApply == "asc") organisation.asc else organisation.desc, community, system, domain, specification, actor, testSuite, testCase)
				case "system" => (if (sortOrderToApply == "asc") system.asc else system.desc, community, organisation, domain, specification, actor, testSuite, testCase)
				case "domain" => (if (sortOrderToApply == "asc") domain.asc else domain.desc, specification, community, organisation, system, actor, testSuite, testCase)
				case "specification" => (if (sortOrderToApply == "asc") specification.asc else specification.desc, community, organisation, system, domain, actor, testSuite, testCase)
				case "actor" => (if (sortOrderToApply == "asc") actor.asc else actor.desc, community, organisation, system, domain, specification, testSuite, testCase)
				case _ => (if (sortOrderToApply == "asc") community.asc else community.desc, organisation, system, domain, specification, actor, testSuite, testCase) // Community (or default)
			}
		})

		val results = exec(query
			.result
  		.map(_.toList))

		val resultBuilder = new ConformanceStatusBuilder[ConformanceStatementFull](recordDetails = true)
		results.foreach { result =>
			val resultCommunities = result._1._1._2
			val resultConfResult = result._1._1._1._1._1._1._1._1
			val resultTestSuite= result._1._2
			val resultTestCase = result._2
			val resultOrganisation = result._1._1._1._2
			val resultSystem = result._1._1._1._1._2
			val resultDomain = result._1._1._1._1._1._2
			val resultActor = result._1._1._1._1._1._1._2
			val resultSpec = result._1._1._1._1._1._1._1._2
			val conformanceStatement = new ConformanceStatementFull(
				resultCommunities.id, resultCommunities.shortname, resultOrganisation.id, resultOrganisation.shortname,
					resultSystem.id, resultSystem.shortname,
					resultDomain.id, resultDomain.shortname, resultDomain.fullname,
					resultActor.id, resultActor.actorId, resultActor.name,
					resultSpec.id, resultSpec.shortname, resultSpec.fullname,
					Some(resultTestSuite.shortname), Some(resultTestCase.shortname), resultTestCase.description,
					resultConfResult.result, resultConfResult.outputMessage, resultConfResult.testsession, resultConfResult.updateTime, 0L, 0L, 0L)
			resultBuilder.addConformanceResult(conformanceStatement)
		}
		resultBuilder.getDetails(Some(new ConformanceStatusBuilder.FilterCriteria(
			dateFromString(updateTimeStart),
			dateFromString(updateTimeEnd),
			status))
		)
	}

	def getConformanceStatements(domainIds: Option[List[Long]], specIds: Option[List[Long]], actorIds: Option[List[Long]], communityIds: Option[List[Long]], organizationIds: Option[List[Long]], systemIds: Option[List[Long]], orgParameters: Option[Map[Long, Set[String]]], sysParameters: Option[Map[Long, Set[String]]], status: Option[List[String]], updateTimeStart: Option[String], updateTimeEnd: Option[String], sortColumn: Option[String], sortOrder: Option[String]): List[ConformanceStatementFull] = {
		var query = PersistenceSchema.conformanceResults
			.join(PersistenceSchema.specifications).on(_.spec === _.id)
			.join(PersistenceSchema.actors).on(_._1.actor === _.id)
			.join(PersistenceSchema.domains).on(_._2.domain === _.id)
			.join(PersistenceSchema.systems).on(_._1._1._1.sut === _.id)
  		.join(PersistenceSchema.organizations).on(_._2.owner === _.id)
  		.join(PersistenceSchema.communities).on(_._2.community === _.id)
			.filterOpt(domainIds)((q, ids) => q._1._1._1._2.id inSet ids)
			.filterOpt(specIds)((q, ids) => q._1._1._1._1._1._1.spec inSet ids)
			.filterOpt(actorIds)((q, ids) => q._1._1._1._1._1._1.actor inSet ids)
			.filterOpt(communityIds)((q, ids) => q._2.id inSet ids)
			.filterOpt(organisationIdsToUse(organizationIds, orgParameters))((q, ids) => q._1._2.id inSet ids)
			.filterOpt(systemIdsToUse(systemIds, sysParameters))((q, ids) => q._1._1._1._1._1._1.sut inSet ids)

		// Apply sorting
		val sortColumnToApply = sortColumn.getOrElse("community")
		val sortOrderToApply = sortOrder.getOrElse("asc")
		query = query.sortBy(x => {
			val community = x._2.shortname.asc
			val organisation = x._1._2.shortname.asc
			val system = x._1._1._2.shortname.asc
			val domain = x._1._1._1._2.shortname.asc
			val specification = x._1._1._1._1._1._2.shortname.asc
			val actor = x._1._1._1._1._2.actorId.asc
			sortColumnToApply match {
				case "organisation" => (if (sortOrderToApply == "asc") organisation.asc else organisation.desc, community, system, domain, specification, actor)
				case "system" => (if (sortOrderToApply == "asc") system.asc else system.desc, community, organisation, domain, specification, actor)
				case "domain" => (if (sortOrderToApply == "asc") domain.asc else domain.desc, specification, community, organisation, system, actor)
				case "specification" => (if (sortOrderToApply == "asc") specification.asc else specification.desc, community, organisation, system, domain, actor)
				case "actor" => (if (sortOrderToApply == "asc") actor.asc else actor.desc, community, organisation, system, domain, specification)
				case _ => (if (sortOrderToApply == "asc") community.asc else community.desc, organisation, system, domain, specification, actor) // Community (or default)
			}
		})

		val results = exec(query.result.map(_.toList))
		val resultBuilder = new ConformanceStatusBuilder[ConformanceStatementFull](recordDetails = false)
		results.foreach { result =>
			val resultConfResult = result._1._1._1._1._1._1
			val resultCommunity = result._2
			val resultOrganisation = result._1._2
			val resultSystem = result._1._1._2
			val resultDomain = result._1._1._1._2
			val resultSpecification = result._1._1._1._1._1._2
			val resultActor = result._1._1._1._1._2
			val statement = new ConformanceStatementFull(
				resultCommunity.id, resultCommunity.shortname, resultOrganisation.id, resultOrganisation.shortname,
				resultSystem.id, resultSystem.shortname,
				resultDomain.id, resultDomain.shortname, resultDomain.fullname,
				resultActor.id, resultActor.actorId, resultActor.name,
				resultSpecification.id, resultSpecification.shortname, resultSpecification.fullname,
				None, None, None, resultConfResult.result, None, resultConfResult.testsession, resultConfResult.updateTime,
				0L, 0L, 0L)
			resultBuilder.addConformanceResult(statement)
		}
		resultBuilder.getOverview(Some(new ConformanceStatusBuilder.FilterCriteria(
			dateFromString(updateTimeStart),
			dateFromString(updateTimeEnd),
			status))
		)
	}

	private def dateFromString(dateStr: Option[String]): Option[Date] = {
		if (dateStr.isEmpty) {
			None
		} else {
			Some(new SimpleDateFormat(Constants.FilterDateFormat).parse(dateStr.get))
		}
	}

	def getConformanceCertificateSettingsWrapper(communityId: Long): Option[ConformanceCertificates] = {
		exec(getConformanceCertificateSettings(communityId))
	}

	def getConformanceCertificateSettings(communityId: Long) = {
		PersistenceSchema.conformanceCertificates.filter(_.community === communityId).result.headOption
	}

	def updateConformanceCertificateSettingsInternal(conformanceCertificate: ConformanceCertificates, updatePasswords: Boolean, removeKeystore: Boolean) = {
		for {
			existingSettings <- getConformanceCertificateSettings(conformanceCertificate.community)
			_ <- {
				var actions = ListBuffer[DBIO[_]]()
				if (existingSettings.isDefined) {
					if (removeKeystore) {
						val q = for {c <- PersistenceSchema.conformanceCertificates if c.id === existingSettings.get.id} yield (
							c.message, c.title, c.includeMessage, c.includeTestStatus, c.includeTestCases, c.includeDetails,
							c.includeSignature, c.keystoreFile, c.keystoreType, c.keystorePassword, c.keyPassword
						)
						actions += q.update(
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
						if (conformanceCertificate.keystoreFile.isDefined) {
							val q = for {c <- PersistenceSchema.conformanceCertificates if c.id === existingSettings.get.id} yield c.keystoreFile
							actions += q.update(conformanceCertificate.keystoreFile)
						}
						if (updatePasswords) {
							val q = for {c <- PersistenceSchema.conformanceCertificates if c.id === existingSettings.get.id} yield (
								c.message, c.title, c.includeMessage, c.includeTestStatus, c.includeTestCases, c.includeDetails,
								c.includeSignature, c.keystoreType, c.keystorePassword, c.keyPassword
							)
							var keystorePasswordToUpdate = conformanceCertificate.keystorePassword
							if (keystorePasswordToUpdate.isDefined) {
								keystorePasswordToUpdate = Some(MimeUtil.encryptString(keystorePasswordToUpdate.get))
							}
							var keyPasswordToUpdate = conformanceCertificate.keyPassword
							if (keyPasswordToUpdate.isDefined) {
								keyPasswordToUpdate = Some(MimeUtil.encryptString(keyPasswordToUpdate.get))
							}
							actions += q.update(
								conformanceCertificate.message,
								conformanceCertificate.title,
								conformanceCertificate.includeMessage,
								conformanceCertificate.includeTestStatus,
								conformanceCertificate.includeTestCases,
								conformanceCertificate.includeDetails,
								conformanceCertificate.includeSignature,
								conformanceCertificate.keystoreType,
								keystorePasswordToUpdate,
								keyPasswordToUpdate
							)
						} else {
							val q = for {c <- PersistenceSchema.conformanceCertificates if c.id === existingSettings.get.id} yield (
								c.message, c.title, c.includeMessage, c.includeTestStatus, c.includeTestCases, c.includeDetails,
								c.includeSignature, c.keystoreType
							)
							actions += q.update(
								conformanceCertificate.message,
								conformanceCertificate.title,
								conformanceCertificate.includeMessage,
								conformanceCertificate.includeTestStatus,
								conformanceCertificate.includeTestCases,
								conformanceCertificate.includeDetails,
								conformanceCertificate.includeSignature,
								conformanceCertificate.keystoreType
							)
						}
					}
				} else {
					actions += (PersistenceSchema.insertConformanceCertificate += conformanceCertificate)
				}
				toDBIO(actions)
			}
		} yield ()
	}

	def updateConformanceCertificateSettings(conformanceCertificate: ConformanceCertificates, updatePasswords: Boolean, removeKeystore: Boolean) = {
		exec(updateConformanceCertificateSettingsInternal(conformanceCertificate, updatePasswords, removeKeystore).transactionally)
	}

  def deleteConformanceCertificateSettings(communityId: Long) = {
    PersistenceSchema.conformanceCertificates.filter(_.community === communityId).delete
  }

	def getSystemConfigurationStatus(systemId: Long, actorId: Long): List[SystemConfigurationEndpoint] = {
		val configuredParameters = exec(PersistenceSchema.configs
			.join(PersistenceSchema.parameters).on(_.parameter === _.id)
			.join(PersistenceSchema.endpoints).on(_._2.endpoint === _.id)
			.filter(_._1._1.system === systemId)
			.filter(_._2.actor === actorId)
			.map(x => (
				x._1._2.id, // Parameter ID
				x._1._1.value, // Config value
				x._1._1.contentType, // Content type
			)).result).toList
		val configuredParametersMap = mutable.Map[Long, (String, Option[String])]()
		configuredParameters.foreach{ config =>
			configuredParametersMap += (config._1 -> (config._2, config._3))
		}
		val expectedEndpoints = getEndpointsCaseForActor(actorId)
		val endpointList: List[SystemConfigurationEndpoint] = expectedEndpoints.map(expectedEndpoint => {
			val expectedParameters = getEndpointParameters(expectedEndpoint.id)
			var parameterList: Option[List[SystemConfigurationParameter]] = None
			if (expectedParameters.nonEmpty) {
				parameterList = Some(expectedParameters.map(expectedParameter => {
					val parameterValue = configuredParametersMap.get(expectedParameter.id)
					var config: Option[Configs] = None
					if (parameterValue.isDefined) {
						config = Some(Configs(systemId, expectedParameter.id, expectedParameter.endpoint, parameterValue.get._1, parameterValue.get._2))
					}
					val parameterStatus = new SystemConfigurationParameter(expectedParameter, config.isDefined, config)
					parameterStatus
				}))
			}
			val endpointStatus = new SystemConfigurationEndpoint(expectedEndpoint, parameterList)
			endpointStatus
		})
		endpointList
	}

	private def removeDomainFromCommunities(domainId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
		for {
			communityIds <- PersistenceSchema.communities.filter(_.domain === domainId).map(x => x.id).result
			_ <- {
				val actions = ListBuffer[DBIO[_]]()
				communityIds.foreach { communityId =>
					// Delete conformance statements
					actions += deleteConformanceStatementsForDomainAndCommunity(communityId, domainId, onSuccess)
					// Set domain of community to null
					actions += (for {x <- PersistenceSchema.communities if x.domain === domainId} yield x.domain).update(None)
				}
				toDBIO(actions)
			}
		} yield ()
	}

	def deleteConformanceStatementsForDomainAndCommunity(domainId: Long, communityId: Long, onSuccess: mutable.ListBuffer[() => _]) = {
		val action = for {
			actorIds <- PersistenceSchema.actors.filter(_.domain === domainId).map(x => x.id).result.map(_.toList)
			systemIds <- PersistenceSchema.systems.join(PersistenceSchema.organizations).on(_.owner === _.id).filter(_._2.community === communityId).map(x => x._1.id).result.map(_.toList)
			_ <- {
				val actions = ListBuffer[DBIO[_]]()
				systemIds.foreach { systemId =>
					actions += systemManager.deleteConformanceStatements(systemId, actorIds, onSuccess)
				}
				toDBIO(actions)
			}
		} yield ()
		action
	}

	private[managers] def organisationIdsToUse(organisationIds: Option[Iterable[Long]], orgParameters: Option[Map[Long, Set[String]]]): Option[Iterable[Long]] = {
		var matchingIds: Option[Iterable[Long]] = None
		if (organisationIds.isDefined) {
			matchingIds = Some(organisationIds.get)
		}
		if (orgParameters.isDefined) {
			orgParameters.get.foreach { entry =>
				matchingIds = Some(organisationIdsForParameterValues(matchingIds, entry._1, entry._2))
				if (matchingIds.get.isEmpty) {
					// No matching IDs. Return immediately without checking other parameters.
					return Some(Set[Long]())
				}
			}
		}
		matchingIds
	}

	private def organisationIdsForParameterValues(organisationIds: Option[Iterable[Long]], parameterId: Long, values: Iterable[String]): Set[Long] = {
		exec(
			PersistenceSchema.organisationParameterValues
				.filterOpt(organisationIds)((table, ids) => table.organisation inSet ids)
				.filter(_.parameter === parameterId)
				.filter(_.value inSet values)
				.map(x => x.organisation)
				.result
		).toSet
	}

	private[managers] def systemIdsToUse(systemIds: Option[Iterable[Long]], sysParameters: Option[Map[Long, Set[String]]]): Option[Iterable[Long]] = {
		var matchingIds: Option[Iterable[Long]] = None
		if (systemIds.isDefined) {
			matchingIds = Some(systemIds.get)
		}
		if (sysParameters.isDefined) {
			sysParameters.get.foreach { entry =>
				matchingIds = Some(systemIdsForParameterValues(matchingIds, entry._1, entry._2))
				if (matchingIds.get.isEmpty) {
					// No matching IDs. Return immediately without checking other parameters.
					return Some(Set[Long]())
				}
			}
		}
		matchingIds
	}

	private def systemIdsForParameterValues(systemIds: Option[Iterable[Long]], parameterId: Long, values: Iterable[String]): Set[Long] = {
		exec(
			PersistenceSchema.systemParameterValues
				.filterOpt(systemIds)((table, ids) => table.system inSet ids)
				.filter(_.parameter === parameterId)
				.filter(_.value inSet values)
				.map(x => x.system)
				.result
		).toSet
	}

	def getStatementParametersByCommunityId(communityId: Long): List[StatementParameterMinimal] = {
		val results = exec(
			for {
				domainId <- PersistenceSchema.communities.filter(_.id === communityId).map(_.domain).result.head
				parameters <- {
					if (domainId.isDefined) {
						PersistenceSchema.parameters
							.join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
							.join(PersistenceSchema.actors).on(_._2.actor === _.id)
							.filter(_._2.domain === domainId.get)
							.sortBy(_._1._1.name.asc)
							.map(x => (x._1._1.id, x._1._1.name, x._1._1.testKey, x._1._1.kind))
							.result
					} else {
						DBIO.successful(Seq.empty)
					}
				}
			} yield parameters
		)
		// Keep only uniquely named parameters.
		val addedNames = new mutable.HashSet[String]()
		val parametersToUse = new ListBuffer[StatementParameterMinimal]()
		results.foreach { param =>
			if (!addedNames.contains(param._3)) {
				parametersToUse += StatementParameterMinimal(param._1, param._2, param._3, param._4)
				addedNames += param._3
			}
		}
		parametersToUse.toList
	}

}
