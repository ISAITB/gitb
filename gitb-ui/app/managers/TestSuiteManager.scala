package managers

import java.io.File
import java.nio.file.{Path, Paths}
import java.util
import java.util.Objects

import com.gitb.tr.{TAR, TestResultType}
import javax.inject.{Inject, Singleton}
import models.Enums.TestSuiteReplacementChoice.{TestSuiteReplacementChoice, _}
import models.Enums.{TestResultStatus, TestSuiteReplacementChoice}
import models.{TestSuiteUploadResult, _}
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.Rep
import utils.{RepositoryUtils, ZipArchiver}
import utils.tdlvalidator.tdl.{FileSource, TestSuiteValidationAdapter}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

object TestSuiteManager {

	val TEST_SUITES_PATH = "test-suites"

	type TestSuiteDbTuple = (
			Rep[Long], Rep[String], Rep[String], Rep[String],
			Rep[Option[String]], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]],
			Rep[Option[String]], Rep[Long], Rep[String], Rep[Boolean]
		)

	type TestSuiteValueTuple = (
		Long, String, String, String,
			Option[String], Option[String], Option[String], Option[String],
			Option[String], Long, String, Boolean
		)

	private def withoutDocumentation(dbTestSuite: PersistenceSchema.TestSuitesTable): TestSuiteDbTuple = {
		(dbTestSuite.id, dbTestSuite.shortname, dbTestSuite.fullname, dbTestSuite.version,
			dbTestSuite.authors, dbTestSuite.originalDate, dbTestSuite.modificationDate, dbTestSuite.description,
			dbTestSuite.keywords, dbTestSuite.specification, dbTestSuite.filename, dbTestSuite.hasDocumentation)
	}

	def tupleToTestSuite(x: TestSuiteValueTuple) = {
		TestSuites(x._1, x._2, x._3, x._4, x._5, x._6, x._7, x._8, x._9, x._10, x._11, x._12, None)
	}

}

/**
 * Created by serbay on 10/17/14.
 */
@Singleton
class TestSuiteManager @Inject() (testResultManager: TestResultManager, actorManager: ActorManager, conformanceManager: ConformanceManager, endPointManager: EndPointManager, testCaseManager: TestCaseManager, parameterManager: ParameterManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

	import dbConfig.profile.api._

	private final val logger: Logger = LoggerFactory.getLogger("TestSuiteManager")

	def getById(testSuiteId: Long): Option[TestSuites] = {
		exec(PersistenceSchema.testSuites.filter(_.id === testSuiteId).map(TestSuiteManager.withoutDocumentation).result.headOption).map(TestSuiteManager.tupleToTestSuite)
	}

	def getTestSuitesWithSpecificationId(specification: Long): List[TestSuites] = {
		exec(PersistenceSchema.testSuites
			.filter(_.specification === specification)
			.sortBy(_.shortname.asc)
			.map(TestSuiteManager.withoutDocumentation)
			.result.map(_.toList)).map(TestSuiteManager.tupleToTestSuite)
	}

	def getTestSuites(ids: Option[List[Long]]): List[TestSuites] = {
		val q = ids match {
			case Some(idList) => {
				PersistenceSchema.testSuites
					.filter(_.id inSet idList)
			}
			case None => {
				PersistenceSchema.testSuites
			}
		}
		exec(q.sortBy(_.shortname.asc).map(TestSuiteManager.withoutDocumentation)
		 .result.map(_.toList)).map(TestSuiteManager.tupleToTestSuite)
	}

	def getTestSuiteOfTestCaseWrapper(testCaseId: Long): TestSuites = {
		getTestSuiteOfTestCase(testCaseId)
	}

	def getTestSuiteOfTestCase(testCaseId: Long): TestSuites = {
		val query = PersistenceSchema.testSuites
			.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testsuite)
		val result = exec(query.filter(_._2.testcase === testCaseId).map(x => TestSuiteManager.withoutDocumentation(x._1)).result.head)
		TestSuiteManager.tupleToTestSuite(result)
	}

	def getTestSuitesWithTestCases(): List[TestSuite] = {
		val testSuites = exec(PersistenceSchema.testSuites.sortBy(_.shortname.asc).map(TestSuiteManager.withoutDocumentation).result.map(_.toList)).map(TestSuiteManager.tupleToTestSuite)
		testSuites map {
			ts:TestSuites =>
				val testCaseIds = exec(PersistenceSchema.testSuiteHasTestCases.filter(_.testsuite === ts.id).map(_.testcase).result.map(_.toList))
				val testCases = exec(PersistenceSchema.testCases.filter(_.id inSet testCaseIds).map(TestCaseManager.withoutDocumentation).result.map(_.toList)).map(TestCaseManager.tupleToTestCase)

				new TestSuite(ts, testCases)
		}
	}

	def getTestSuitesWithTestCasesForCommunity(communityId: Long): List[TestSuite] = {
		val testSuites = exec(
			PersistenceSchema.testSuites
				.join(PersistenceSchema.specifications).on(_.specification === _.id)
  			.join(PersistenceSchema.communities).on(_._2.domain === _.domain)
  			.filter(_._2.id === communityId)
				.sortBy(_._1._1.shortname.asc)
  			.map(r => TestSuiteManager.withoutDocumentation(r._1._1))
				.result.map(_.toList)
		).map(TestSuiteManager.tupleToTestSuite)
		testSuites map {
			ts:TestSuites =>
				val testCaseIds = exec(PersistenceSchema.testSuiteHasTestCases.filter(_.testsuite === ts.id).map(_.testcase).result.map(_.toList))
				val testCases = exec(PersistenceSchema.testCases.filter(_.id inSet testCaseIds).map(TestCaseManager.withoutDocumentation).result.map(_.toList)).map(TestCaseManager.tupleToTestCase)

				new TestSuite(ts, testCases)
		}
	}

	def getTestSuitesWithTestCasesForSystem(systemId: Long): List[TestSuite] = {
		val testSuites = exec(
			PersistenceSchema.testSuites
				.join(PersistenceSchema.conformanceResults).on(_.id === _.testsuite)
				.filter(_._2.sut === systemId)
				.distinctOn(_._1.id)
				.sortBy(_._1.shortname.asc)
				.map(r => TestSuiteManager.withoutDocumentation(r._1))
				.result.map(_.toList)
		).map(TestSuiteManager.tupleToTestSuite)
		testSuites map {
			ts:TestSuites =>
				val testCaseIds = exec(PersistenceSchema.testSuiteHasTestCases.filter(_.testsuite === ts.id).map(_.testcase).result.map(_.toList))
				val testCases = exec(PersistenceSchema.testCases.filter(_.id inSet testCaseIds).map(TestCaseManager.withoutDocumentation).result.map(_.toList)).map(TestCaseManager.tupleToTestCase)

				new TestSuite(ts, testCases)
		}
	}

	def removeActorLinksForTestSuite(testSuiteId: Long) = {
		PersistenceSchema.testSuiteHasActors
			.filter(_.testsuite === testSuiteId)
			.delete
	}

	def getSpecificationById(specId: Long): Specifications = {
		val spec = exec(PersistenceSchema.specifications.filter(_.id === specId).result.head)
		spec
	}

	def getTempFolder(): File = {
		new File("/tmp")
	}

	def getPendingFolder(): File = {
		new File(getTempFolder(), "pending")
	}

	def getTmpValidationFolder(): File = {
		new File(getTempFolder(), "ts_validation")
	}

	def applyPendingTestSuiteAction(specification: Long, pendingTestSuiteIdentifier: String, action: TestSuiteReplacementChoice): TestSuiteUploadResult = {
		val result = new TestSuiteUploadResult()
		val pendingTestSuiteFolder = new File(getPendingFolder(), pendingTestSuiteIdentifier)
		try {
			var replace = false
			action match {
				case TestSuiteReplacementChoice.CANCEL => result.success = true
				case TestSuiteReplacementChoice.DROP_TEST_HISTORY | TestSuiteReplacementChoice.KEEP_TEST_HISTORY => replace = true
				case _ => throw new IllegalStateException("Unsupported pending test suite action: "+action)
			}
			if (replace) {
				if (pendingTestSuiteFolder.exists()) {
					val pendingFiles = pendingTestSuiteFolder.listFiles()
					if (pendingFiles.length == 1) {
						val testSuite = RepositoryUtils.getTestSuiteFromZip(specification, pendingFiles(0))
						// Sanity check
						if (testSuite.isDefined && testSuite.get.testCases.isDefined) {
							result.items.addAll(replaceTestSuite(testSuite.get.toCaseObject, testSuite.get.actors, testSuite.get.testCases, pendingFiles(0), action))
							result.success = true
						} else {
							result.errorInformation = "The pending test suite archive ["+pendingTestSuiteIdentifier +"] was corrupted "
						}
					} else {
						result.errorInformation = "A single pending test suite archive was expected but found " + pendingFiles.length
					}
				} else {
					result.errorInformation = "The pending test suite ["+pendingTestSuiteIdentifier+"] could not be located"
				}
			}
		} catch {
			case e:Exception => {
				logger.error("An error occurred", e)
				result.errorInformation = e.getMessage
				result.success = false
			}
		} finally {
			// Delete temporary folder (if exists)
			FileUtils.deleteDirectory(pendingTestSuiteFolder)
		}
		result
	}

	private def validateTestSuite(specification: Long, tempTestSuiteArchive: File): TAR = {
		val actorSet = new util.HashSet[String]()
		actorSet.addAll(conformanceManager.getActorsWithSpecificationId(None, Some(specification)).map(a => a.actorId))

		val parameterSet = new util.HashSet[String]()
		val domain = conformanceManager.getDomainOfSpecification(specification)
		parameterSet.addAll(conformanceManager.getDomainParameters(domain.id).map(p => p.name))

		val report = TestSuiteValidationAdapter.getInstance().doValidation(new FileSource(tempTestSuiteArchive), actorSet, parameterSet, getTmpValidationFolder().getAbsolutePath)
		report
	}

	def deployTestSuiteFromZipFile(specification: Long, tempTestSuiteArchive: File): TestSuiteUploadResult = {
		val result = new TestSuiteUploadResult()
		try {
			result.validationReport =  validateTestSuite(specification, tempTestSuiteArchive)
			if (result.validationReport.getResult == TestResultType.SUCCESS) {
				// We can proceed. Check also if test suite exists.
				val testSuite = RepositoryUtils.getTestSuiteFromZip(specification, tempTestSuiteArchive)
				if (testSuite.isDefined && testSuite.get.testCases.isDefined) {
					logger.debug("Extracted test suite [" + testSuite + "] with the test cases [" + testSuite.get.testCases.get.map(_.shortname) + "]")
					val exists = testSuiteExists(testSuite.get)
					val noWarnings = result.validationReport.getCounters.getNrOfWarnings.intValue() == 0
					result.exists = exists
					if (exists || !noWarnings) {
						// Park the test suite for now and ask user what to do
						FileUtils.moveDirectoryToDirectory(tempTestSuiteArchive.getParentFile, getPendingFolder(), true)
						result.pendingTestSuiteFolderName = tempTestSuiteArchive.getParentFile.getName
					} else {
						val onSuccessCalls = mutable.ListBuffer[() => _]()
						val onFailureCalls = mutable.ListBuffer[() => _]()
						val action = saveTestSuite(testSuite.get.toCaseObject, null, testSuite.get.actors, testSuite.get.testCases, tempTestSuiteArchive, onSuccessCalls, onFailureCalls)
						result.items.addAll(exec(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), action).transactionally))
						result.success = true
					}
				}
			}
		} catch {
			case e:Exception => {
				logger.error("An error occurred", e)
				result.errorInformation = e.getMessage
				result.success = false
			}
		} finally {
			// Delete temporary folder (if exists)
			FileUtils.deleteDirectory(tempTestSuiteArchive.getParentFile)
		}
		result
	}

	def getTestSuiteFile(specification: Long, testSuiteName: String): File = {
		new File(RepositoryUtils.getTestSuitesPath(getSpecificationById(specification)), testSuiteName)
	}

	private def testSuiteExists(suite: TestSuite): Boolean = {
		val count = exec(PersistenceSchema.testSuites
			.filter(_.shortname === suite.shortname)
			.filter(_.specification === suite.specification)
			.size.result)
		count > 0
	}

	private def getTestSuiteBySpecificationAndName(specId: Long, name: String): Option[TestSuites] = {
		val testSuite = exec(PersistenceSchema.testSuites
			.filter(_.shortname === name)
			.filter(_.specification === specId)
			.map(TestSuiteManager.withoutDocumentation)
			.result.headOption)
		if (testSuite.isDefined) {
			Some(TestSuiteManager.tupleToTestSuite(testSuite.get))
		} else {
			None
		}
	}

	private def replaceTestSuite(suite: TestSuites, testSuiteActors: Option[List[Actor]], testCases: Option[List[TestCases]], tempTestSuiteArchive: File, action: TestSuiteReplacementChoice): List[TestSuiteUploadItemResult] = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val onFailureCalls = mutable.ListBuffer[() => _]()
		if (action == DROP_TEST_HISTORY) {
			val existingTestSuite = getTestSuiteBySpecificationAndName(suite.specification, suite.shortname)
			val action = conformanceManager.undeployTestSuite(existingTestSuite.get.id, onSuccessCalls) andThen
				saveTestSuite(suite, null, testSuiteActors, testCases, tempTestSuiteArchive, onSuccessCalls, onFailureCalls)
			exec(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), action).transactionally)
		} else if (action == KEEP_TEST_HISTORY) {
			val existingTestSuite = getTestSuiteBySpecificationAndName(suite.specification, suite.shortname)
			val action = saveTestSuite(suite, existingTestSuite.orNull, testSuiteActors, testCases, tempTestSuiteArchive, onSuccessCalls, onFailureCalls)
			exec(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), action).transactionally)
		} else {
			throw new IllegalStateException("Unexpected test suite replacement action ["+action+"]")
		}
	}

	def updateTestSuiteInDb(testSuiteId: Long, newData: TestSuites) = {
		val q1 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.shortname, t.fullname, t.version, t.authors, t.keywords, t.description, t.filename, t.hasDocumentation, t.documentation)
		q1.update(newData.shortname, newData.fullname, newData.version, newData.authors, newData.keywords, newData.description, newData.filename, newData.hasDocumentation, newData.documentation) andThen
		testResultManager.updateForUpdatedTestSuite(testSuiteId, newData.shortname)
	}

	private def theSameActor(one: Actors, two: Actors) = {
		(Objects.equals(one.name, two.name)
				&& Objects.equals(one.description, two.description)
				&& Objects.equals(one.default, two.default)
        && Objects.equals(one.hidden, two.hidden)
				&& Objects.equals(one.displayOrder, two.displayOrder))
	}

	private def theSameEndpoint(one: Endpoint, two: Endpoints) = {
		(Objects.equals(one.name, two.name)
				&& Objects.equals(one.desc, two.desc))
	}

	private def theSameParameter(one: models.Parameters, two: models.Parameters) = {
		(Objects.equals(one.name, two.name)
				&& Objects.equals(one.desc, two.desc)
				&& Objects.equals(one.kind, two.kind)
				&& Objects.equals(one.use, two.use)
				&& Objects.equals(one.adminOnly, two.adminOnly)
				&& Objects.equals(one.notForTests, two.notForTests))
	}

	def isActorReference(actorToSave: Actors) = {
		actorToSave.actorId != null && actorToSave.name == null && actorToSave.description.isEmpty
	}

	private def stepSaveTestSuite(suite: TestSuites, existingSuite: TestSuites): DBIO[(Long, List[TestSuiteUploadItemResult])] = {
		val actions = new ListBuffer[DBIO[_]]()
		var savedTestSuiteId: DBIO[Long] = null
		val result = new ListBuffer[TestSuiteUploadItemResult]()
		if (existingSuite != null) {
			// Update existing test suite.
			actions += updateTestSuiteInDb(existingSuite.id, suite)
			// Remove existing actor links (these will be updated later).
			actions += removeActorLinksForTestSuite(existingSuite.id)
			result += new TestSuiteUploadItemResult(suite.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_SUITE, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE)
			savedTestSuiteId = DBIO.successful(existingSuite.id)
		} else {
			// New test suite.
			savedTestSuiteId = PersistenceSchema.testSuites.returning(PersistenceSchema.testSuites.map(_.id)) += suite
			result += new TestSuiteUploadItemResult(suite.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_SUITE, TestSuiteUploadItemResult.ACTION_TYPE_ADD)
		}
		var otherActionsComposed: DBIO[_] = null
		if (actions.nonEmpty) {
			otherActionsComposed = DBIO.seq(actions.toList: _*)
		} else {
			otherActionsComposed = DBIO.successful(())
		}
		(otherActionsComposed andThen
		savedTestSuiteId).flatMap(id => DBIO.successful((id, result.toList)))
	}

	private def lookupActor(actorToLookup: Actors, actorsToCheck: List[Actors]): Option[Actors] = {
		actorsToCheck.find(a => a.actorId == actorToLookup.actorId)
	}

	private def mergeActionsWithResults(action1: DBIO[List[TestSuiteUploadItemResult]], action2: DBIO[List[TestSuiteUploadItemResult]]): DBIO[List[TestSuiteUploadItemResult]] = {
		mergeActionsWithResults(List(action1, action2))
	}

	private def mergeActionsWithResults(actions: List[DBIO[List[TestSuiteUploadItemResult]]]): DBIO[List[TestSuiteUploadItemResult]] = {
		DBIO.fold(actions, List[TestSuiteUploadItemResult]()) {
			(aggregated, current) => {
				aggregated ++ current
			}
		}
	}

	private def stepSaveActors(testSuiteActors: Option[List[Actor]], domainId: Long, specificationActors: List[Actors], suite: TestSuites): DBIO[(util.Map[String, Long], List[TestSuiteUploadItemResult])] = {
		// Get the (new, existing or referenced) actor IDs resulting from the import.
		var finalAction: DBIO[List[TestSuiteUploadItemResult]] = DBIO.successful(List[TestSuiteUploadItemResult]())
		val actions = new ListBuffer[DBIO[List[TestSuiteUploadItemResult]]]()
		val savedActorIds: util.Map[String, Long] = new util.HashMap[String, Long]
		for (testSuiteActor <- testSuiteActors.get) {
			val result = new ListBuffer[TestSuiteUploadItemResult]()
			var updateAction: Option[DBIO[_]] = None
			var savedActorId: DBIO[Long] = null
			val testSuiteActorCase = testSuiteActor.toCaseObject
			val actorToSave = testSuiteActorCase.withDomainId(domainId)
			val savedActor = lookupActor(actorToSave, specificationActors)
			var savedActorStringId: String = null

			if (savedActor.isDefined) {
				if (isActorReference(actorToSave) || theSameActor(savedActor.get, actorToSave)) {
					result += new TestSuiteUploadItemResult(savedActor.get.name, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED)
				} else {
					updateAction = Some(actorManager.updateActor(savedActor.get.id, actorToSave.actorId, actorToSave.name, actorToSave.description, actorToSave.default, actorToSave.hidden, actorToSave.displayOrder, suite.specification))
					result += new TestSuiteUploadItemResult(savedActor.get.name, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE)
				}
				savedActorId = DBIO.successful(savedActor.get.id)
				savedActorStringId = savedActor.get.actorId
			} else {
				if (isActorReference(actorToSave)) {
					throw new IllegalStateException("Actor reference [" + actorToSave.actorId + "] not found in specification")
				} else {
					// New actor.
					savedActorId = conformanceManager.createActor(actorToSave, suite.specification)
					savedActorStringId = actorToSave.actorId
				}
				result += new TestSuiteUploadItemResult(actorToSave.actorId, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_ADD)
			}

			val combinedAction = (updateAction.getOrElse(DBIO.successful(())) andThen savedActorId).flatMap(id => {
				savedActorIds.put(savedActorStringId, id)
				for {
					existingEndpoints <- PersistenceSchema.endpoints.filter(_.actor === id).result.map(_.toList)
					endpointResults <- stepSaveEndpoints(id, testSuiteActor, actorToSave, existingEndpoints)
				} yield endpointResults
			})
			actions += mergeActionsWithResults(combinedAction, DBIO.successful(result.toList))
		}
		// Group together all actions from the for loop and aggregate their results
		finalAction = {
			if (actions.nonEmpty) {
				mergeActionsWithResults(actions.toList)
			} else {
				DBIO.successful(List[TestSuiteUploadItemResult]())
			}
		}
		finalAction.flatMap(collectedResults => {
			DBIO.successful((savedActorIds, collectedResults))
		})
	}

	private def stepSaveEndpoints(savedActorId: Long, testSuiteActor: Actor, actorToSave: Actors, existingEndpoints: List[Endpoints]): DBIO[List[TestSuiteUploadItemResult]] = {
		val actions = new ListBuffer[DBIO[List[TestSuiteUploadItemResult]]]()
		val existingEndpointMap = new util.HashMap[String, Endpoints]()
		for (existingEndpoint <- existingEndpoints) {
			existingEndpointMap.put(existingEndpoint.name, existingEndpoint)
		}
		// Update endpoints.
		if (testSuiteActor.endpoints.isDefined) {
			// Process endpoints defined in the test suite
			val newEndpoints = testSuiteActor.endpoints.get
			newEndpoints.foreach { endpoint =>
				val result = new ListBuffer[TestSuiteUploadItemResult]()
				var updateAction: Option[DBIO[_]] = None
				val existingEndpoint = existingEndpointMap.get(endpoint.name)
				var endpointId: DBIO[Long] = null
				if (existingEndpoint != null) {
					// Existing endpoint.
					endpointId = DBIO.successful(existingEndpoint.id)
					existingEndpointMap.remove(endpoint.name)
					if (theSameEndpoint(endpoint, existingEndpoint)) {
						result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]", TestSuiteUploadItemResult.ITEM_TYPE_ENDPOINT, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED)
					} else {
						updateAction = Some(endPointManager.updateEndPoint(existingEndpoint.id, endpoint.name, endpoint.desc))
						result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]", TestSuiteUploadItemResult.ITEM_TYPE_ENDPOINT, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE)
					}
				} else {
					// New endpoint.
					endpointId = endPointManager.createEndpoint(endpoint.toCaseObject.copy(actor = savedActorId))
					result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]", TestSuiteUploadItemResult.ITEM_TYPE_ENDPOINT, TestSuiteUploadItemResult.ACTION_TYPE_ADD)
				}
				val combinedAction = (updateAction.getOrElse(DBIO.successful(())) andThen endpointId).flatMap(id => {
					for {
						existingEndpointParameters <- PersistenceSchema.parameters.filter(_.endpoint === id.longValue()).result.map(_.toList)
						parameterResults <- stepSaveParameters(id, endpoint, actorToSave, existingEndpointParameters)
					} yield parameterResults
				})
				actions += mergeActionsWithResults(combinedAction, DBIO.successful(result.toList))
			}
		}
		if (actions.nonEmpty) {
			mergeActionsWithResults(actions.toList)
		} else {
			DBIO.successful(List[TestSuiteUploadItemResult]())
		}
	}

	private def stepSaveParameters(endpointId: Long, endpoint: Endpoint, actorToSave: Actors, existingEndpointParameters: List[models.Parameters]): DBIO[List[TestSuiteUploadItemResult]] = {
		val actions = new ListBuffer[DBIO[_]]()
		val result = new ListBuffer[TestSuiteUploadItemResult]()
		val existingParameterMap = new util.HashMap[String, models.Parameters]()
		for (existingParameter <- existingEndpointParameters) {
			existingParameterMap.put(existingParameter.name, existingParameter)
		}
		if (endpoint.parameters.isDefined) {
			val parameters = endpoint.parameters.get
			parameters.foreach { parameter =>
				var action: Option[DBIO[_]] = None
				val existingParameter = existingParameterMap.get(parameter.name)
				var parameterId: Long = -1
				if (existingParameter != null) {
					// Existing parameter.
					parameterId = existingParameter.id
					existingParameterMap.remove(parameter.name)
					if (theSameParameter(parameter, existingParameter)) {
						result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]."+parameter.name, TestSuiteUploadItemResult.ITEM_TYPE_PARAMETER, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED)
					} else {
						action = Some(parameterManager.updateParameter(parameterId, parameter.name, parameter.desc, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests))
						result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]."+parameter.name, TestSuiteUploadItemResult.ITEM_TYPE_PARAMETER, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE)
					}
				} else {
					// New parameter.
					action = Some(parameterManager.createParameter(parameter.copy(endpoint = endpointId)))
					result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]."+parameter.name, TestSuiteUploadItemResult.ITEM_TYPE_PARAMETER, TestSuiteUploadItemResult.ACTION_TYPE_ADD)
				}
				actions += action.getOrElse(DBIO.successful(()))
			}
		}
		var combinedAction: DBIO[_] = null
		if (actions.nonEmpty) {
			combinedAction = DBIO.seq(actions.map(a => a): _*)
		} else {
			combinedAction = DBIO.successful(())
		}
		combinedAction andThen DBIO.successful(result.toList)
	}

	def stepProcessTestCases(specificationId: Long, savedTestSuiteId: Long, testCases: Option[List[TestCases]], resourcePaths: Map[String, String], existingTestCaseMap: util.HashMap[String, java.lang.Long], savedActorIds: util.Map[String, Long], existingActorToSystemMap: util.HashMap[Long, util.HashSet[Long]]): DBIO[(util.List[Long], List[TestSuiteUploadItemResult])] = {
		val savedTestCaseIds: util.List[Long] = new util.ArrayList[Long]
		var combinedAction: DBIO[_] = DBIO.successful(())
		val result = new ListBuffer[TestSuiteUploadItemResult]()
		for (testCase <- testCases.get) {
			val testCaseToStore = testCase.withPath(resourcePaths(testCase.shortname))
			val existingTestCaseId = existingTestCaseMap.get(testCase.shortname)
			if (existingTestCaseId != null) {
				// Test case already exists - update.
				existingTestCaseMap.remove(testCase.shortname)
				combinedAction = combinedAction andThen
					testCaseManager.updateTestCase(
						existingTestCaseId, testCaseToStore.shortname, testCaseToStore.fullname,
						testCaseToStore.version, testCaseToStore.authors, testCaseToStore.description,
						testCaseToStore.keywords, testCaseToStore.testCaseType, testCaseToStore.path, testCaseToStore.testSuiteOrder,
						testCaseToStore.targetActors.get, testCaseToStore.documentation.isDefined, testCaseToStore.documentation
					)
				// Update test case relation to actors.
				val actorsToFurtherProcess = new util.HashSet[Long]
				val actorsThatExistAndChangedToSut = new util.HashSet[Long]
				var sutActor: Long = -1
				testCase.targetActors.getOrElse("").split(",").foreach { actorId =>
					var actorIdToSet: String = null
					if (actorId.endsWith("[SUT]")) {
						actorIdToSet = actorId.substring(0, actorId.indexOf("[SUT]"))
						sutActor = savedActorIds.get(actorIdToSet)
					} else {
						actorIdToSet = actorId
					}
					actorsToFurtherProcess.add(savedActorIds.get(actorIdToSet))
				}
				combinedAction = combinedAction andThen
					(for {
					existingTestCaseActors <- PersistenceSchema.testCaseHasActors.filter(_.testcase === existingTestCaseId.toLong).result.map(_.toList)
					_ <- {
						val actions = new ListBuffer[DBIO[_]]()
						existingTestCaseActors.map(existingTestCaseActor => {
							val existingActorId = existingTestCaseActor._3
							val existingMarkedAsSut = existingTestCaseActor._4
							if (!actorsToFurtherProcess.contains(existingActorId)) {
								// The actor is no longer mentioned in the test case - remove
								actions += PersistenceSchema.conformanceResults.filter(_.testcase === existingTestCaseId.toLong).filter(_.actor === existingActorId).delete
								actions += testCaseManager.removeActorLinkForTestCase(existingTestCaseId, existingActorId)
							} else {
								// Update the actor role if needed.
								if ((existingActorId == sutActor && !existingMarkedAsSut)
										|| (existingActorId != sutActor && existingMarkedAsSut)) {
                  // Update the role of the actor.
                  val query = for {t <- PersistenceSchema.testCaseHasActors if t.testcase === existingTestCaseActor._1 && t.specification === existingTestCaseActor._2 && t.actor === existingActorId} yield t.sut
                  actions += query.update(existingActorId == sutActor)
                  if (existingActorId != sutActor) {
                    // The actor is no longer the SUT. Remove the conformance results for it.
                    actions += PersistenceSchema.conformanceResults.filter(_.testcase === existingTestCaseId.toLong).filter(_.actor === existingActorId).delete
                    // No need for further processing for this actor.
                    actorsToFurtherProcess.remove(existingActorId)
                  } else {
                    // The actor was not previously the SUT but is now marked as the SUT. Need to check for updates to conformance results.
                    actorsThatExistAndChangedToSut.add(existingActorId)
                  }
								} else {
                  // Existing actor remains unchanged - remove so that we keep track of what's left.
                  actorsToFurtherProcess.remove(existingActorId)
                }
							}
						})
						if (actions.nonEmpty) {
							DBIO.seq(actions.map(a => a): _*)
						} else {
							DBIO.successful(())
						}
					}
					_ <- {
						val actions = actorsToFurtherProcess.map(newTestCaseActor => {
							var action: DBIO[_] = null
              if (actorsThatExistAndChangedToSut.contains(newTestCaseActor)) {
                action = DBIO.successful(())
              } else {
                action = PersistenceSchema.testCaseHasActors += (existingTestCaseId.longValue(), specificationId, newTestCaseActor, newTestCaseActor == sutActor)
              }
							val implementingSystems = existingActorToSystemMap.get(newTestCaseActor)
							if (implementingSystems != null) {
								for (implementingSystem <- implementingSystems) {
									// Check to see if there are existing test sessions for this test case.
									action = action andThen (for {
										previousResult <- PersistenceSchema.testResults.filter(_.sutId === implementingSystem).filter(_.testCaseId === existingTestCaseId.toLong).sortBy(_.endTime.desc).result.headOption
										_ <- {
											if (previousResult.isDefined) {
												PersistenceSchema.conformanceResults += ConformanceResult(0L, implementingSystem, specificationId, newTestCaseActor, savedTestSuiteId, existingTestCaseId, previousResult.get.result, Some(previousResult.get.sessionId))
											} else {
												PersistenceSchema.conformanceResults += ConformanceResult(0L, implementingSystem, specificationId, newTestCaseActor, savedTestSuiteId, existingTestCaseId, TestResultStatus.UNDEFINED.toString, None)
											}
										}
									} yield())
								}
							}
							action
						})
						DBIO.seq(actions.toList: _*)
					}
				} yield())
				result += new TestSuiteUploadItemResult(testCaseToStore.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE)
			} else {
				// New test case.
				combinedAction = combinedAction andThen
					(for {
					existingTestCaseId <- {
						PersistenceSchema.testCases.returning(PersistenceSchema.testCases.map(_.id)) += testCaseToStore
					}
					_ <- {
						savedTestCaseIds.add(existingTestCaseId)
						DBIO.successful(())
					}
					_ <- {
						var action: DBIO[_] = DBIO.successful(())
						// Update test case relation to actors.
						testCase.targetActors.getOrElse("").split(",").foreach { actorId =>
							var isSut: Boolean = false
							var actorIdToSet: String = null
							if (actorId.endsWith("[SUT]")) {
								actorIdToSet = actorId.substring(0, actorId.indexOf("[SUT]"))
								isSut = true
							} else {
								actorIdToSet = actorId
							}
							if (savedActorIds.containsKey(actorIdToSet)) {
								val actorInternalId = savedActorIds.get(actorIdToSet)
								action = action andThen
									(PersistenceSchema.testCaseHasActors += (existingTestCaseId.longValue(), specificationId, actorInternalId, isSut))
								// All conformance results will be new. We need to add these for all systems that implement the actor.
								if (isSut) {
									val implementingSystems = existingActorToSystemMap.get(actorInternalId)
									if (implementingSystems != null) {
										for (implementingSystem <- implementingSystems) {
											action = action andThen
												(PersistenceSchema.conformanceResults += ConformanceResult(0L, implementingSystem, specificationId, actorInternalId, savedTestSuiteId, existingTestCaseId, TestResultStatus.UNDEFINED.toString, None))
										}
									}
								}
							}
						}
						action
					}
				} yield())
				result += new TestSuiteUploadItemResult(testCaseToStore.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_ADD)
			}
		}
		combinedAction andThen DBIO.successful((savedTestCaseIds, result.toList))
	}

	def stepRemoveTestCases(existingTestCaseMap: util.HashMap[String, java.lang.Long]): DBIO[List[TestSuiteUploadItemResult]] = {
		// Remove test cases not in new test suite.
		val actions = new ListBuffer[DBIO[_]]()
		val results = new ListBuffer[TestSuiteUploadItemResult]()

		for (testCaseEntry <- existingTestCaseMap.entrySet()) {
			actions += testCaseManager.delete(testCaseEntry.getValue)
			results += new TestSuiteUploadItemResult(testCaseEntry.getKey, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_REMOVE)
		}
		DBIO.seq(actions.toList: _*) andThen DBIO.successful(results.toList)
	}

	def stepUpdateTestSuiteActorLinks(savedTestSuiteId: Long, savedActorIds: util.Map[String, Long]): DBIO[_] = {
		// Add new test suite actor links.
		val actions = new ListBuffer[DBIO[_]]()
		for (actorEntry <- savedActorIds.entrySet()) {
				actions += (PersistenceSchema.testSuiteHasActors += (savedTestSuiteId, actorEntry.getValue))
		}
		DBIO.seq(actions.toList: _*)
	}

	def stepUpdateTestSuiteTestCaseLinks(savedTestSuiteId: Long, savedTestCaseIds: util.List[Long]): DBIO[_] = {
		// Update test suite test case links.
		val actions = new ListBuffer[DBIO[_]]()
		for (testCaseId <- savedTestCaseIds) {
			actions += (PersistenceSchema.testSuiteHasTestCases += (savedTestSuiteId, testCaseId))
		}
		DBIO.seq(actions.toList: _*)
	}

	def getSystemActors(specificationId: Long) = {
		PersistenceSchema.systemImplementsActors.filter(_.specId === specificationId).result.map(_.toList)
	}

	def getExistingActorToSystemMap(systemActors: List[(Long, Long, Long)]) = {
		// First get the map of existing actorIds to systems
		val existingActorToSystemMap = new util.HashMap[Long, util.HashSet[Long]]()
		systemActors.foreach { systemToActor =>
			val systemId = systemToActor._1
			val actorId = systemToActor._3
			if (!existingActorToSystemMap.containsKey(actorId)) {
				existingActorToSystemMap.put(actorId, new util.HashSet[Long])
			}
			existingActorToSystemMap.get(actorId).add(systemId)
		}
		DBIO.successful(existingActorToSystemMap)
	}

	def getExistingTestCasesForTestSuite(testSuiteId: Long) = {
		PersistenceSchema.testCases
			.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
			.filter(_._2.testsuite === testSuiteId)
			.map(r => (r._1.id, r._1.shortname))
			.result
			.map(_.toList)
	}

	def getExistingTestCaseMap(existingTestCasesForTestSuite: List[(Long, String)]): DBIO[util.HashMap[String, java.lang.Long]] = {
		// Process test cases.
		val existingTestCaseMap = new util.HashMap[String, java.lang.Long]()
		if (existingTestCasesForTestSuite.nonEmpty) {
			// This is an update - check for existing test cases.
			for (existingTestCase <- existingTestCasesForTestSuite) {
				existingTestCaseMap.put(existingTestCase._2, existingTestCase._1)
			}
		}
		DBIO.successful(existingTestCaseMap)
	}

	/*
	This method follow quite obscure branching because of the asynchronous processing model of Slick.
	To ensure that all actions are processed in the correct sequence we use "for combinators", maps and
	flatmaps that execute preceding actions before processing the following ones. Its very ugly but its
	the only way to do things now that Slick dropped support for implicit sessions.
	 */
	private def saveTestSuite(suite: TestSuites, existingSuite: TestSuites, testSuiteActors: Option[List[Actor]], testCases: Option[List[TestCases]], tempTestSuiteArchive: File, onSuccessCalls: mutable.ListBuffer[() => _], onFailureCalls: mutable.ListBuffer[() => _]): DBIO[List[TestSuiteUploadItemResult]] = {
		val targetFolder: File = getTestSuiteFile(suite.specification, suite.filename)
    var existingFolder: File = null
    if (existingSuite != null) {
      // Update case.
      existingFolder = new File(targetFolder.getParent, existingSuite.filename)
    }
		val resourcePaths = RepositoryUtils.extractTestSuiteFilesFromZipToFolder(suite.specification, targetFolder, tempTestSuiteArchive)
		val action = for {
			// Get the specification of the test suite.
			spec <- PersistenceSchema.specifications.filter(_.id === suite.specification).result.head
			// Save the test suite (insert or update) and return the identifier to use.
			saveTestSuiteStep <- stepSaveTestSuite(suite, existingSuite)
			// Lookup the existing actors for the specification in question.
			specificationActors <- {
				val query = PersistenceSchema.actors
  					.join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
				query
					.filter(_._2.specId === suite.specification)
  				.map(_._1)
					.result
  				.map(_.toList)
			}
			// Process the actors (inserting and saving them as needed) and return their ID information for further processing.
			saveActorsStep <- stepSaveActors(testSuiteActors, spec.domain, specificationActors, suite)
			// Lookup the existing test cases for the test suite (if it already exists).
			existingTestCasesForTestSuite <- {
				if (existingSuite != null) {
					getExistingTestCasesForTestSuite(saveTestSuiteStep._1)
				} else {
					DBIO.successful(List[(Long, String)]())
				}
			}
			// Place the existing test cases in a map for further processing.
			existingTestCaseMap <- getExistingTestCaseMap(existingTestCasesForTestSuite)
			// Lookup the map of systems to actors for the specification
			systemActors <- getSystemActors(suite.specification)
			// Create a map of actors to systems.
			existingActorToSystemMap <- getExistingActorToSystemMap(systemActors)
			// Process the test cases.
			processTestCasesStep <- stepProcessTestCases(spec.id, saveTestSuiteStep._1, testCases, resourcePaths, existingTestCaseMap, saveActorsStep._1, existingActorToSystemMap)
			// Remove the test cases that are no longer in the test suite.
			removeTestCasesStep <- stepRemoveTestCases(existingTestCaseMap)
			// Update the actor links for the  test suite.
			_ <- stepUpdateTestSuiteActorLinks(saveTestSuiteStep._1, saveActorsStep._1)
			// Update the test case links for the test suite.
			_ <- stepUpdateTestSuiteTestCaseLinks(saveTestSuiteStep._1, processTestCasesStep._1)
		} yield(saveTestSuiteStep, saveActorsStep, processTestCasesStep, removeTestCasesStep)
		action.flatMap(results => {
			// Finally, delete the backup folder
			onSuccessCalls += (() => {
				if (existingFolder != null && existingFolder.exists()) {
					FileUtils.deleteDirectory(existingFolder)
				}
			})
			DBIO.successful(results._1._2 ++ results._2._2 ++ results._3._2 ++ results._4)
		}).cleanUp(error => {
			if (error.isDefined) {
				onFailureCalls += (() => {
					// Cleanup operations in case an error occurred.
					if (targetFolder.exists()) {
						FileUtils.deleteDirectory(targetFolder)
					}
				})
				DBIO.failed(error.get)
			} else {
				DBIO.successful(())
			}
		})
	}

	def getTestSuiteDocumentation(testSuiteId: Long): Option[String] = {
		val result = exec(PersistenceSchema.testSuites.filter(_.id === testSuiteId).map(x => x.documentation).result.headOption)
		if (result.isDefined) {
			result.get
		} else {
			None
		}
	}

	def extractTestSuite(testSuite: TestSuites, specification: Specifications, testSuiteOutputPath: Option[Path]): Path = {
		val testSuiteFolder = RepositoryUtils.getTestSuitesResource(specification, testSuite.filename, None)
		var outputPathToUse = testSuiteOutputPath
		if (testSuiteOutputPath.isEmpty) {
			outputPathToUse = Some(Paths.get(
				ReportManager.getTempFolderPath().toFile.getAbsolutePath,
				"test_suite",
				"test_suite."+testSuite.id.toString+"."+System.currentTimeMillis()+".zip"
			))
		}
		new ZipArchiver(testSuiteFolder.toPath, outputPathToUse.get).zip()
		outputPathToUse.get
	}

}