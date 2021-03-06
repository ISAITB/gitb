package managers

import java.util

import com.gitb.core.{ActorConfiguration, Configuration}
import javax.inject.{Inject, Singleton}
import models.Enums.{TestResultStatus, TriggerDataType}
import models._
import models.prerequisites.PrerequisiteUtil
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{MimeUtil, RepositoryUtils}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ConformanceManager @Inject() (triggerManager: TriggerManager, actorManager: ActorManager, testResultManager: TestResultManager, testCaseManager: TestCaseManager, repositoryUtils: RepositoryUtils, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  def logger = LoggerFactory.getLogger("ConformanceManager")

	import dbConfig.profile.api._

	/**
	 * Checks if domain exists
	 */
	def checkDomainExists(domainId: Long): Boolean = {
		exec(PersistenceSchema.domains.filter(_.id === domainId).result.headOption).isDefined
	}

  def getSpecificationsBySystem(systemId: Long): List[Specifications] = {
    exec(PersistenceSchema.conformanceResults
      .join(PersistenceSchema.specifications).on(_.spec === _.id)
      .filter(_._1.sut === systemId)
      .map(r => r._2)
			.distinctOn(_.id)
      .sortBy(_.shortname.asc)
      .result.map(_.toList)
    )
  }

	def getActorsWithSpecificationIdBySystem(systemId: Long): List[Actor] = {
		exec(PersistenceSchema.conformanceResults
			.join(PersistenceSchema.actors).on(_.actor === _.id)
			.join(PersistenceSchema.specificationHasActors).on(_._2.id === _.actorId)
			.filter(_._1._1.sut === systemId)
			.map(r => (r._1._2, r._2.specId))
			.distinctOn(_._1.id)
			.sortBy(_._1.actorId.asc)
			.result.map(_.toList)
		).map(x => new Actor(x._1, null, null, x._2))
	}

  def getDomainsBySystem(systemId: Long): List[Domain] = {
    exec(PersistenceSchema.conformanceResults
      .join(PersistenceSchema.specifications).on(_.spec === _.id)
      .join(PersistenceSchema.domains).on(_._2.domain === _.id)
      .filter(_._1._1.sut === systemId)
      .map(r => r._2)
			.distinctOn(_.id)
      .sortBy(_.shortname.asc)
      .result.map(_.toList)
    )
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

	def createDomainInternal(domain:Domain): DBIO[Long] = {
		PersistenceSchema.domains.returning(PersistenceSchema.domains.map(_.id)) += domain
	}

	def createDomain(domain:Domain): Long = {
		exec(createDomainInternal(domain))
	}

	def createDomainParameterInternal(parameter:DomainParameter): DBIO[_] = {
		PersistenceSchema.domainParameters.returning(PersistenceSchema.domainParameters.map(_.id)) += parameter
	}

	def createDomainParameter(parameter:DomainParameter) = {
		exec(createDomainParameterInternal(parameter))
	}

	def updateDomainParameter(parameterId: Long, name: String, description: Option[String], kind: String, value: Option[String], inTests: Boolean) = {
		exec(updateDomainParameterInternal(parameterId, name, description, kind, value, inTests).transactionally)
	}

	def updateDomainParameterInternal(parameterId: Long, name: String, description: Option[String], kind: String, value: Option[String], inTests: Boolean): DBIO[_] = {
		if (value.isDefined) {
			// If there is no value provided this means that we don't want to update this
			val q = for {p <- PersistenceSchema.domainParameters if p.id === parameterId} yield (p.name, p.desc, p.kind, p.inTests, p.value)
			q.update(name, description, kind, inTests, value)
		} else {
			val q = for {p <- PersistenceSchema.domainParameters if p.id === parameterId} yield (p.name, p.desc, p.kind, p.inTests)
			q.update(name, description, kind, inTests)
		}
	}

	def deleteDomainParameterWrapper(domainParameter: Long) = {
		exec(deleteDomainParameter(domainParameter).transactionally)
	}

	def deleteDomainParameter(domainParameter: Long): DBIO[_] = {
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
							.map(_.toList.map(x => DomainParameter(x._1, x._2, None, x._3, None, inTests = false, domainId.get)))
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
					.map(x => (x.id, x.name, x.desc, x.kind, x.inTests))
				.result
			).map(x => DomainParameter(x._1, x._2, x._3, x._4, None, x._5, domainId)).toList
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

	private def getSpecificationById(specId: Long): Specifications = {
		val spec = exec(PersistenceSchema.specifications.filter(_.id === specId).result.head)
		spec
	}

	def delete(specId: Long, onSuccessCalls: mutable.ListBuffer[() => _]) = {
		for {
			ids <- PersistenceSchema.specifications.filter(_.id === specId).map(x => (x.id, x.domain)).result.head
			_ <- testResultManager.updateForDeletedSpecification(specId)
			// Delete also actors from the domain (they are now linked only to specifications
			actorIds <- PersistenceSchema.specificationHasActors.filter(_.specId === specId).map(_.actorId).result
			_ <- DBIO.seq(actorIds.map(id => actorManager.deleteActor(id)): _*)
			_ <- PersistenceSchema.specificationHasActors.filter(_.specId === specId).delete
			testSuiteIds <- PersistenceSchema.testSuites.filter(_.specification === specId).map(_.id).result
			_ <- DBIO.seq(testSuiteIds.map(id => undeployTestSuite(id, onSuccessCalls)): _*)
			_ <- PersistenceSchema.conformanceResults.filter(_.spec === specId).delete
			_ <- PersistenceSchema.specifications.filter(_.id === specId).delete
			_ <- {
				onSuccessCalls += (() => {
					val testSuiteFolder = repositoryUtils.getTestSuitesPath(ids._2, ids._1)
					FileUtils.deleteDirectory(testSuiteFolder)
				})
				DBIO.successful(())
			}
		} yield ()
	}

	def undeployTestSuiteWrapper(testSuiteId: Long) = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val action = undeployTestSuite(testSuiteId, onSuccessCalls)
		exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
	}

	def undeployTestSuite(testSuiteId: Long, onSuccessCalls: mutable.ListBuffer[() => _]) = {
		testResultManager.updateForDeletedTestSuite(testSuiteId) andThen
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
						repositoryUtils.undeployTestSuite(getSpecificationById(testSuite.specification), testSuite.filename)
					})
					DBIO.successful(())
				}
			} yield testSuite) andThen
			PersistenceSchema.testSuites.filter(_.id === testSuiteId).delete
	}


	def deleteDomainParameters(domainId: Long) = {
		(for {
			ids <- PersistenceSchema.domainParameters.filter(_.domain === domainId).map(_.id).result
			_ <- DBIO.seq(ids.map(id => deleteDomainParameter(id)): _*)
		} yield()).transactionally
	}

	def deleteSpecificationByDomain(domainId: Long, onSuccessCalls: mutable.ListBuffer[() => _]) = {
		val action = (for {
			ids <- PersistenceSchema.specifications.filter(_.domain === domainId).map(_.id).result
			_ <- DBIO.seq(ids.map(id => delete(id, onSuccessCalls)): _*)
		} yield ()).transactionally
		action
	}

	def deleteTransactionByDomain(domainId: Long) = {
		PersistenceSchema.transactions.filter(_.domain === domainId).delete
	}

	def deleteDomainInternal(domain: Long, onSuccessCalls: ListBuffer[() => _]): DBIO[_] = {
		removeDomainFromCommunities(domain) andThen
			deleteSpecificationByDomain(domain, onSuccessCalls) andThen
			deleteTransactionByDomain(domain) andThen
			testResultManager.updateForDeletedDomain(domain) andThen
			deleteDomainParameters(domain) andThen
			PersistenceSchema.domains.filter(_.id === domain).delete andThen
			{
				onSuccessCalls += (() => {
					repositoryUtils.deleteDomainTestSuiteFolder(domain)
				})
				DBIO.successful(())
			}
	}

	def deleteDomain(domain: Long) {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val action = deleteDomainInternal(domain, onSuccessCalls)
		exec(
			dbActionFinalisation(Some(onSuccessCalls), None, action)
			.transactionally
		)
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

	def createSpecificationsInternal(specification: Specifications): DBIO[_] = {
		PersistenceSchema.specifications.returning(PersistenceSchema.specifications.map(_.id)) += specification
	}

	def createSpecifications(specification: Specifications) = {
		exec(createSpecificationsInternal(specification).transactionally)
	}

	def createActorWrapper(actor: Actors, specificationId: Long) = {
		exec(createActor(actor, specificationId).transactionally)
	}

	def createActor(actor: Actors, specificationId: Long): DBIO[Long] = {
		for {
			savedActorId <- PersistenceSchema.actors.returning(PersistenceSchema.actors.map(_.id)) += actor
			_ <- {
				val actions = new ListBuffer[DBIO[_]]()
				actions += (PersistenceSchema.specificationHasActors += (specificationId, savedActorId))
				if (actor.default.isDefined && actor.default.get) {
					// Ensure no other default actors are defined.
					actions += actorManager.setOtherActorsAsNonDefault(savedActorId, specificationId)
				}
				DBIO.seq(actions.map(a => a): _*)
			}
		} yield savedActorId
	}

	def getActorsByDomainIdWithSpecificationId(domainId: Long): List[Actor] = {
		var actors: List[Actor] = List()
		exec(
			PersistenceSchema.actors
				.join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
				.filter(_._1.domain === domainId)
			  .sortBy(_._1.actorId.desc)
				.result
		).foreach{ result =>
			actors ::= new Actor(result._1, null, null, result._2._1)
		}
		actors
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
		specMap.toMap.map(x => (x._1, x._2.toSet))
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

	def getConformanceStatus(actorId: Long, sutId: Long, testSuiteId: Option[Long]): List[ConformanceStatusItem] = {
		var query = PersistenceSchema.conformanceResults
  			.join(PersistenceSchema.testCases).on(_.testcase === _.id)
  			.join(PersistenceSchema.testSuites).on(_._1.testsuite === _.id)
		query = query
			.filter(_._1._1.actor === actorId)
			.filter(_._1._1.sut === sutId)
		if (testSuiteId.isDefined) {
			query = query.filter(_._1._1.testsuite === testSuiteId.get)
		}
		val finalQuery = query
			.sortBy(x => (x._2.shortname, x._1._2.testSuiteOrder))
			.map(x => (x._2.id, x._2.shortname, x._2.description, x._2.hasDocumentation, x._1._2.id, x._1._2.shortname, x._1._2.description, x._1._2.hasDocumentation, x._1._1.result, x._1._1.outputMessage, x._1._1.testsession))
		val results = exec(finalQuery.result.map(_.toList)).map(r => {
			ConformanceStatusItem(r._1, r._2, r._3, r._4, r._5, r._6, r._7, r._8, r._9, r._10, r._11)
		})
		results
	}

	def getSpecificationIdForTestCaseFromConformanceStatements(testCaseId: Long): Option[Long] = {
		val spec = exec(PersistenceSchema.conformanceResults.filter(_.testcase === testCaseId).map(c => {c.spec}).result.headOption)
		spec
	}

	def getSpecificationIdForTestSuiteFromConformanceStatements(testSuiteId: Long): Option[Long] = {
		val spec = exec(PersistenceSchema.conformanceResults.filter(_.testsuite === testSuiteId).map( c => {c.spec}).result.headOption)
		spec
	}

	def getConformanceStatementsFull(domainIds: Option[List[Long]], specIds: Option[List[Long]], actorIds: Option[List[Long]], communityIds: Option[List[Long]], organizationIds: Option[List[Long]], systemIds: Option[List[Long]], orgParameters: Option[Map[Long, Set[String]]], sysParameters: Option[Map[Long, Set[String]]]): List[ConformanceStatementFull] = {
		val query = PersistenceSchema.conformanceResults
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
			.sortBy(x => (x._1._1._2.shortname, x._1._1._1._2.shortname, x._1._1._1._1._2.shortname, x._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._2.actorId, x._1._2.shortname, x._2.shortname))

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
			val resultSpec = result._1._1._1._1._1._1._1._2

			val conformanceStatement = ConformanceStatementFull(
				resultCommunities.id, resultCommunities.shortname, resultOrganisation.id, resultOrganisation.shortname,
					resultSystem.id, resultSystem.shortname,
					resultDomain.id, resultDomain.shortname, resultDomain.fullname,
					resultActor.id, resultActor.actorId, resultActor.name,
					resultSpec.id, resultSpec.shortname, resultSpec.fullname,
					Some(resultTestSuite.shortname), Some(resultTestCase.shortname), resultTestCase.description,
					Some(resultConfResult.result), resultConfResult.outputMessage, resultConfResult.testsession, 0L, 0L, 0L)
			statements += conformanceStatement
		}
		statements.toList
	}

	def getConformanceStatements(domainIds: Option[List[Long]], specIds: Option[List[Long]], actorIds: Option[List[Long]], communityIds: Option[List[Long]], organizationIds: Option[List[Long]], systemIds: Option[List[Long]], orgParameters: Option[Map[Long, Set[String]]], sysParameters: Option[Map[Long, Set[String]]]): List[ConformanceStatementFull] = {
		val query = PersistenceSchema.conformanceResults
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
			.sortBy(x => (x._2.shortname, x._1._2.shortname, x._1._1._2.shortname, x._1._1._1._2.shortname, x._1._1._1._1._1._2.shortname, x._1._1._1._1._2.actorId))

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
					None, None, None, resultConfResult.testsession, None, None,
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
		import scala.collection.JavaConverters._
		for (conformanceEntry <- mapAsScalaMap(conformanceMap)) {
			statements += conformanceEntry._2
		}
		statements.toList
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
		} yield ()
	}

	def updateConformanceCertificateSettings(conformanceCertificate: ConformanceCertificates, updatePasswords: Boolean, removeKeystore: Boolean) = {
		exec(updateConformanceCertificateSettingsInternal(conformanceCertificate, updatePasswords, removeKeystore).transactionally)
	}

  def deleteConformanceCertificateSettings(communityId: Long) = {
    PersistenceSchema.conformanceCertificates.filter(_.community === communityId).delete
  }

	def getSystemConfigurationParameters(systemId: Long, actorId: Long): List[ActorConfiguration] = {
		val actor = actorManager.getById(actorId).get
		var parameterData = exec(PersistenceSchema.configs
  		.join(PersistenceSchema.parameters).on(_.parameter === _.id)
			.join(PersistenceSchema.endpoints).on(_._2.endpoint === _.id)
  		.filter(_._1._1.system === systemId)
  		.filter(_._2.actor === actorId)
  		.filter(_._1._2.notForTests === false)
  		.map(x => (
				x._2.name, // Endpoint name
				x._1._2.name, // Parameter name
				x._1._1.value, // Parameter value
				x._1._2.dependsOn, // DependsOn
				x._1._2.dependsOnValue, // DependsOnValue
				x._1._2.kind // Kind
			)).result).map(x => new SystemConfigurationParameterMinimal(x._1, x._2, Some(x._3), x._4, x._5, x._6))

		// Keep only the values that have valid prerequisites defined.
		parameterData = PrerequisiteUtil.withValidPrerequisites(parameterData)

		val actorMap = new util.HashMap[String, ActorConfiguration]()
		parameterData.foreach{ p =>
			var actorConfig = actorMap.get(p.endpointName)
			if (actorConfig == null) {
				actorConfig = new ActorConfiguration()
				actorConfig.setActor(actor.actorId)
				actorConfig.setEndpoint(p.endpointName)
				actorMap.put(p.endpointName, actorConfig)
			}
			val config = new Configuration()
			config.setName(p.parameterName)
			if (p.parameterKind == "SECRET") {
				config.setValue(MimeUtil.decryptString(p.parameterValue.get))
			} else {
				config.setValue(p.parameterValue.get)
			}
			actorConfig.getConfig.add(config)
		}
		import scala.collection.JavaConverters._
		collectionAsScalaIterable(actorMap.values()).toList
	}

	def getSystemConfigurationStatus(systemId: Long, actorId: Long): List[SystemConfigurationEndpoint] = {
		val configuredParameters = exec(PersistenceSchema.configs
			.join(PersistenceSchema.parameters).on(_.parameter === _.id)
			.join(PersistenceSchema.endpoints).on(_._2.endpoint === _.id)
			.filter(_._1._1.system === systemId)
			.filter(_._2.actor === actorId)
			.map(x => (
				x._1._2.id, // Parameter ID
				x._1._1.value // Config value
			)).result).toList
		val configuredParametersMap = new util.HashMap[Long, String]()
		configuredParameters.foreach{ config =>
			configuredParametersMap.put(config._1, config._2)
		}
		val expectedEndpoints = getEndpointsCaseForActor(actorId)
		val endpointList: List[SystemConfigurationEndpoint] = expectedEndpoints.map(expectedEndpoint => {
			val expectedParameters = getEndpointParameters(expectedEndpoint.id)
			var parameterList: Option[List[SystemConfigurationParameter]] = None
			if (expectedParameters.nonEmpty) {
				parameterList = Some(expectedParameters.map(expectedParameter => {
					val parameterValue = configuredParametersMap.get(expectedParameter.id)
					var config: Option[Configs] = None
					if (parameterValue != null) {
						config = Some(Configs(systemId, expectedParameter.id, expectedParameter.endpoint, parameterValue))
					}
					val parameterStatus = new SystemConfigurationParameter(expectedParameter, config.isDefined, config, None, None)
					parameterStatus
				}))
			}
			val endpointStatus = new SystemConfigurationEndpoint(expectedEndpoint, parameterList)
			endpointStatus
		})
		endpointList
	}

	private def removeDomainFromCommunities(domainId: Long): DBIO[_] = {
		for {
			communityIds <- PersistenceSchema.communities.filter(_.domain === domainId).map(x => x.id).result
			_ <- {
				val actions = ListBuffer[DBIO[_]]()
				communityIds.foreach { communityId =>
					// Delete conformance statements
					actions += deleteConformanceStatementsForDomainAndCommunity(communityId, domainId)
					// Set domain of community to null
					actions += (for {x <- PersistenceSchema.communities if x.domain === domainId} yield x.domain).update(None)
				}
				toDBIO(actions)
			}
		} yield ()
	}

	def deleteConformanceStatementsForDomainAndCommunity(domainId: Long, communityId: Long) = {
		val action = for {
			actorIds <- PersistenceSchema.actors.filter(_.domain === domainId).map(x => x.id).result.map(_.toList)
			systemIds <- PersistenceSchema.systems.join(PersistenceSchema.organizations).on(_.owner === _.id).filter(_._2.community === communityId).map(x => x._1.id).result.map(_.toList)
			_ <- {
				PersistenceSchema.systemImplementsActors.filter(_.systemId inSet systemIds).filter(_.actorId inSet actorIds).delete andThen
				PersistenceSchema.conformanceResults.filter(_.sut inSet systemIds).filter(_.actor inSet actorIds).delete
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


}
