package managers

import java.io.File
import java.util

import managers.TestCaseManager.removeActorLinksForTestCase
import models.Enums.TestSuiteReplacementChoice.{TestSuiteReplacementChoice, _}
import models.Enums.{TestResultStatus, TestSuiteReplacementChoice}
import models.{TestSuiteUploadResult, _}
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.ObjectUtils
import org.slf4j.{Logger, LoggerFactory}
import persistence.db.PersistenceSchema
import utils.RepositoryUtils
import utils.RepositoryUtils.getTestSuitesPath

import scala.collection.JavaConversions._
import scala.slick.driver.MySQLDriver.simple._


/**
 * Created by serbay on 10/17/14.
 */
object TestSuiteManager extends BaseManager {

	val TEST_SUITES_PATH = "test-suites"

	private final val logger: Logger = LoggerFactory.getLogger("TestSuiteManager")

	def getTestSuitesWithSpecificationId(specification: Long): List[TestSuites] = {
		DB.withSession { implicit session =>
			PersistenceSchema.testSuites
				.filter(_.specification === specification)
				.sortBy(_.shortname.asc)
				.list
		}
	}

	def getTestSuites(ids: Option[List[Long]]): List[TestSuites] = {
		DB.withSession { implicit session =>
			val q = ids match {
				case Some(idList) => {
					PersistenceSchema.testSuites
						.filter(_.id inSet idList)
				}
				case None => {
					PersistenceSchema.testSuites
				}
			}
			q.sortBy(_.shortname.asc)
			 .list
		}
	}

	def getTestSuiteOfTestCaseWrapper(testCaseId: Long): TestSuites = {
		DB.withSession { implicit session =>
			getTestSuiteOfTestCase(testCaseId)
		}
	}

	def getTestSuiteOfTestCase(testCaseId: Long)(implicit session: Session): TestSuites = {
		val query = for {
			testSuite <- PersistenceSchema.testSuites
			testSuiteHasTestCase <- PersistenceSchema.testSuiteHasTestCases if testSuite.id === testSuiteHasTestCase.testsuite
		} yield (testSuite, testSuiteHasTestCase)
		query.filter(_._2.testcase === testCaseId).firstOption.get._1
	}

	def getTestSuitesWithTestCases(): List[TestSuite] = {
		DB.withSession { implicit session =>
			val testSuites = PersistenceSchema.testSuites.sortBy(_.shortname.asc).list

			testSuites map {
				ts:TestSuites =>
					val testCaseIds = PersistenceSchema.testSuiteHasTestCases.filter(_.testsuite === ts.id).map(_.testcase).list
					val testCases = PersistenceSchema.testCases.filter(_.id inSet testCaseIds).list

					new TestSuite(ts, testCases)
			}
		}
	}

	def removeActorLinksForTestSuite(testSuiteId: Long)(implicit session: Session): Unit = {
		PersistenceSchema.testSuiteHasActors
			.filter(_.testsuite === testSuiteId)
			.delete
	}

	def undeployTestSuiteWrapper(testSuiteId: Long): Unit = {
		DB.withTransaction { implicit session =>
			undeployTestSuite(testSuiteId)
		}
	}

	def undeployTestSuite(testSuiteId: Long)(implicit session: Session): Unit = {
		TestResultManager.updateForDeletedTestSuite(testSuiteId)

		val testCases =
			PersistenceSchema.testSuiteHasTestCases
				.filter(_.testsuite === testSuiteId)
				.map(_.testcase).list

		val testSuite =
			PersistenceSchema.testSuites
				.filter(_.id === testSuiteId)
				.firstOption.get

		PersistenceSchema.testSuiteHasActors
			.filter(_.testsuite === testSuiteId)
			.delete

		PersistenceSchema.conformanceResults
			.filter(_.testsuite === testSuiteId)
			.delete

		testCases.foreach { testCase =>
				TestResultManager.updateForDeletedTestCase(testCase)
				removeActorLinksForTestCase(testCase)
				PersistenceSchema.testCaseCoversOptions.filter(_.testcase === testCase).delete
				PersistenceSchema.testSuiteHasTestCases.filter(_.testcase === testCase).delete
				PersistenceSchema.testCases.filter(_.id === testCase).delete
		}

		PersistenceSchema.testSuites
			.filter(_.id === testSuiteId)
			.delete

		RepositoryUtils.undeployTestSuite(testSuite.specification, testSuite.shortname)
	}

	def getTempFolder(): File = {
		new File("/tmp")
	}

	def getPendingFolder(): File = {
		new File(getTempFolder(), "pending")
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

	def deployTestSuiteFromZipFile(specification: Long, tempTestSuiteArchive: File): TestSuiteUploadResult = {
		DB.withTransaction { implicit session =>
			val result = new TestSuiteUploadResult()
			try {
				val testSuite = RepositoryUtils.getTestSuiteFromZip(specification, tempTestSuiteArchive)
				if (testSuite.isDefined && testSuite.get.testCases.isDefined) {
					logger.debug("Extracted test suite [" + testSuite + "] with the test cases [" + testSuite.get.testCases.get.map(_.shortname) + "]")
					if (testSuiteExists(testSuite.get)) {
						// Park the test suite for now and ask user what to do
						FileUtils.moveDirectoryToDirectory(tempTestSuiteArchive.getParentFile, getPendingFolder(), true)
						result.pendingTestSuiteFolderName = tempTestSuiteArchive.getParentFile.getName
					} else {
						result.items.addAll(saveTestSuite(testSuite.get.toCaseObject, null, testSuite.get.actors, testSuite.get.testCases, tempTestSuiteArchive))
						result.success = true
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
	}

	private def getTestSuiteFile(specification: Long, testSuiteName: String): File = {
		new File(getTestSuitesPath(specification), testSuiteName)
	}

	private def testSuiteExists(suite: TestSuite)(implicit session: Session): Boolean = {
		val count = PersistenceSchema.testSuites
			.filter(_.shortname === suite.shortname)
			.filter(_.specification === suite.specification)
			.size.run
		count > 0
	}

	private def getTestSuiteBySpecificationAndName(specId: Long, name: String)(implicit session: Session): TestSuites = {
		val testSuite = PersistenceSchema.testSuites
			.filter(_.shortname === name)
			.filter(_.specification === specId)
			.firstOption.get
		testSuite
	}

	private def replaceTestSuite(suite: TestSuites, testSuiteActors: Option[List[Actor]], testCases: Option[List[TestCases]], tempTestSuiteArchive: File, action: TestSuiteReplacementChoice): util.ArrayList[TestSuiteUploadItemResult] = {
		if (action == DROP_TEST_HISTORY) {
			DB.withTransaction { implicit session =>
				val existingTestSuite = getTestSuiteBySpecificationAndName(suite.specification, suite.shortname)
				undeployTestSuite(existingTestSuite.id)
				saveTestSuite(suite, null, testSuiteActors, testCases, tempTestSuiteArchive)
			}
		} else if (action == KEEP_TEST_HISTORY) {
			DB.withTransaction { implicit session =>
				val existingTestSuite = getTestSuiteBySpecificationAndName(suite.specification, suite.shortname)
				saveTestSuite(suite, existingTestSuite, testSuiteActors, testCases, tempTestSuiteArchive)
			}
		} else {
			throw new IllegalStateException("Unexpected test suite replacement action ["+action+"]");
		}
	}

	private def updateTestSuiteInDb(testSuiteId: Long, newData: TestSuites)(implicit session: Session): Unit = {
		val q1 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.shortname)
		q1.update(newData.shortname)

		val q2 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.fullname)
		q2.update(newData.fullname)

		val q3 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.version)
		q3.update(newData.version)

		val q4 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.authors)
		q4.update(newData.authors)

		val q6 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.keywords)
		q6.update(newData.keywords)

		val q7 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.description)
		q7.update(newData.description)

		TestResultManager.updateForUpdatedTestSuite(testSuiteId, newData.shortname)
	}

	private def theSameActor(one: Actors, two: Actors) = {
		(ObjectUtils.equals(one.name, two.name)
				&& ObjectUtils.equals(one.description, two.description))
	}

	private def theSameEndpoint(one: Endpoint, two: Endpoints) = {
		(ObjectUtils.equals(one.name, two.name)
				&& ObjectUtils.equals(one.desc, two.desc))
	}

	private def theSameParameter(one: models.Parameters, two: models.Parameters) = {
		(ObjectUtils.equals(one.name, two.name)
				&& ObjectUtils.equals(one.desc, two.desc)
				&& ObjectUtils.equals(one.kind, two.kind)
				&& ObjectUtils.equals(one.use, two.use))
	}

	private def theSameTestSuite(one: models.TestSuites, two: models.TestSuites) = {
		(ObjectUtils.equals(one.shortname, two.shortname)
			&& ObjectUtils.equals(one.description, two.description)
			&& ObjectUtils.equals(one.keywords, two.keywords)
			&& ObjectUtils.equals(one.version, two.version)
			&& ObjectUtils.equals(one.fullname, two.fullname)
			&& ObjectUtils.equals(one.authors, two.authors))
	}

	private def theSameTEstCase(one: models.TestCases, two: models.TestCases) = {
		(ObjectUtils.equals(one.path, two.path)
				&& ObjectUtils.equals(one.testCaseType, two.testCaseType)
				&& ObjectUtils.equals(one.keywords, two.keywords)
				&& ObjectUtils.equals(one.description, two.description)
				&& ObjectUtils.equals(one.authors, two.authors)
				&& ObjectUtils.equals(one.version, two.version)
				&& ObjectUtils.equals(one.fullname, two.fullname)
				&& ObjectUtils.equals(one.shortname, two.shortname)
				&& ObjectUtils.equals(one.targetOptions, two.targetOptions)
				&& ObjectUtils.equals(one.targetActors, two.targetActors))
	}

	def isActorReference(actorToSave: Actors) = {
		actorToSave.actorId != null && actorToSave.name == null && !actorToSave.description.isDefined
	}

	private def saveTestSuite(suite: TestSuites, existingSuite: TestSuites, testSuiteActors: Option[List[Actor]], testCases: Option[List[TestCases]], tempTestSuiteArchive: File)(implicit session: Session): util.ArrayList[TestSuiteUploadItemResult] = {
		val result = new util.ArrayList[TestSuiteUploadItemResult]()
		val targetFolder: File = getTestSuiteFile(suite.specification, suite.shortname)
		val backupFolder: File = new File(targetFolder.getParent, targetFolder.getName()+"_BACKUP")
		try {
			if (targetFolder.exists()) {
				if (backupFolder.exists()) {
					FileUtils.deleteDirectory(backupFolder)
				}
				FileUtils.moveDirectory(targetFolder, backupFolder)
			}
			val resourcePaths = RepositoryUtils.extractTestSuiteFilesFromZipToFolder(suite.specification, targetFolder, tempTestSuiteArchive)
			val spec = PersistenceSchema.specifications
				.filter(_.id === suite.specification)
				.firstOption.get

			var savedTestSuiteId: Long = -1
			var testSuiteIsNew = false
			if (existingSuite != null) {
				// Update existing test suite.
				updateTestSuiteInDb(existingSuite.id, suite)
				savedTestSuiteId = existingSuite.id
				// Remove existing actor links (these will be updated later).
				removeActorLinksForTestSuite(existingSuite.id)
				result.add(new TestSuiteUploadItemResult(suite.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_SUITE, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE))
			} else {
				// New test suite.
				savedTestSuiteId = PersistenceSchema.testSuites.returning(PersistenceSchema.testSuites.map(_.id)).insert(suite)
				result.add(new TestSuiteUploadItemResult(suite.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_SUITE, TestSuiteUploadItemResult.ACTION_TYPE_ADD))
				testSuiteIsNew = true
			}

			// Get the (new, existing or referenced) actor IDs resulting from the import.
			val savedActorIds: util.Map[String, Long] = new util.HashMap[String, Long]
			for (testSuiteActor <- testSuiteActors.get) {
				var savedActorId: Long = -1
				val testSuiteActorCase = testSuiteActor.toCaseObject
				val actorToSave = testSuiteActorCase.withDomainId(spec.domain)
				val query = for {
					actor <- PersistenceSchema.actors
					specificationHasActors <- PersistenceSchema.specificationHasActors if specificationHasActors.actorId === actor.id
				} yield (actor, specificationHasActors)
				val savedActor = query
					.filter(_._1.actorId === testSuiteActor.actorId)
					.filter(_._2.specId === suite.specification)
					.list
				if (savedActor.nonEmpty) {
					if (isActorReference(actorToSave) || theSameActor(savedActor.head._1, actorToSave)) {
						result.add(new TestSuiteUploadItemResult(savedActor.head._1.name, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED))
					} else {
						ActorManager.updateActor(savedActor.head._1.id, actorToSave.actorId, actorToSave.name, actorToSave.description)
						result.add(new TestSuiteUploadItemResult(savedActor.head._1.name, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE))
					}
					savedActorId = savedActor.head._1.id
					savedActorIds.put(savedActor.head._1.actorId, savedActorId)
				} else {
					if (isActorReference(actorToSave)) {
						throw new IllegalStateException("Actor reference ["+actorToSave.actorId+"] not found in specification")
					} else {
						// New actor.
						savedActorId = PersistenceSchema.actors
							.returning(PersistenceSchema.actors.map(_.id))
							.insert(actorToSave)
						savedActorIds.put(actorToSave.actorId, savedActorId)
					}
					// Link new actor to specification.
					PersistenceSchema.specificationHasActors.insert((suite.specification, savedActorId))
					result.add(new TestSuiteUploadItemResult(actorToSave.actorId, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_ADD))
				}
				val existingEndpointMap = new util.HashMap[String, Endpoints]()
				for (existingEndpoint <- PersistenceSchema.endpoints.filter(_.actor === savedActorId).list) {
					existingEndpointMap.put(existingEndpoint.name, existingEndpoint)
				}
				// Update endpoints.
				if (testSuiteActor.endpoints.isDefined) {
					// Process endpoints defined in the test suite
					val newEndpoints = testSuiteActor.endpoints.get
					newEndpoints.foreach { endpoint =>
						val existingEndpoint = existingEndpointMap.get(endpoint.name)
						var endpointId: Long = -1
						if (existingEndpoint != null) {
							// Existing endpoint.
							endpointId = existingEndpoint.id
							existingEndpointMap.remove(endpoint.name)
							if (theSameEndpoint(endpoint, existingEndpoint)) {
								result.add(new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]", TestSuiteUploadItemResult.ITEM_TYPE_ENDPOINT, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED))
							} else {
								EndPointManager.updateEndPoint(endpointId, endpoint.name, endpoint.desc)
								result.add(new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]", TestSuiteUploadItemResult.ITEM_TYPE_ENDPOINT, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE))
							}
						} else {
							// New endpoint.
							endpointId = EndPointManager.createEndpoint(endpoint.toCaseObject.copy(actor = savedActorId))
							result.add(new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]", TestSuiteUploadItemResult.ITEM_TYPE_ENDPOINT, TestSuiteUploadItemResult.ACTION_TYPE_ADD))
						}
						val existingParameterMap = new util.HashMap[String, models.Parameters]()
						for (existingParameter <- PersistenceSchema.parameters.filter(_.endpoint === endpointId.longValue()).list) {
							existingParameterMap.put(existingParameter.name, existingParameter)
						}
						if (endpoint.parameters.isDefined) {
							val parameters = endpoint.parameters.get
							parameters.foreach { parameter =>
								val existingParameter = existingParameterMap.get(parameter.name)
								var parameterId: Long = -1
								if (existingParameter != null) {
									// Existing parameter.
									parameterId = existingParameter.id
									existingParameterMap.remove(parameter.name)
									if (theSameParameter(parameter, existingParameter)) {
										result.add(new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]."+parameter.name, TestSuiteUploadItemResult.ITEM_TYPE_PARAMETER, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED))
									} else {
										ParameterManager.updateParameter(parameterId, parameter.name, parameter.desc, parameter.use, parameter.kind)
										result.add(new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]."+parameter.name, TestSuiteUploadItemResult.ITEM_TYPE_PARAMETER, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE))
									}
								} else {
									// New parameter.
									ParameterManager.createParameter(parameter.copy(endpoint = endpointId))
									result.add(new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]."+parameter.name, TestSuiteUploadItemResult.ITEM_TYPE_PARAMETER, TestSuiteUploadItemResult.ACTION_TYPE_ADD))
								}
							}
						}
					}
				}
			}
			// Process test cases.
			val existingTestCaseMap = new util.HashMap[String, java.lang.Long]()
			if (existingSuite != null) {
				// This is an update - check for existing test cases.
				for (existingTestCase <- TestCaseManager.getTestCasesOfTestSuite(savedTestSuiteId, None)) {
					existingTestCaseMap.put(existingTestCase.shortname, existingTestCase.id)
				}
			}
			// First get the map of existing actorIds to systems
			val existingActorToSystemMap = new util.HashMap[Long, util.HashSet[Long]]()
			PersistenceSchema.systemImplementsActors.filter(_.specId === suite.specification).list.foreach { systemToActor =>
				val systemId = systemToActor._1
				var actorId = systemToActor._3
				if (!existingActorToSystemMap.containsKey(actorId)) {
					existingActorToSystemMap.put(actorId, new util.HashSet[Long])
				}
				existingActorToSystemMap.get(actorId).add(systemId)
			}
			// Proceed to process the test cases
			val savedTestCaseIds: util.List[Long] = new util.ArrayList[Long]
			for (testCase <- testCases.get) {
				val testCaseToStore = testCase.withPath(resourcePaths(testCase.shortname))
				var existingTestCaseId = existingTestCaseMap.get(testCase.shortname)
				if (existingTestCaseId != null) {
					// Test case already exists - update.
					existingTestCaseMap.remove(testCase.shortname)
					TestCaseManager.updateTestCase(
						existingTestCaseId, testCaseToStore.shortname, testCaseToStore.fullname,
						testCaseToStore.version, testCaseToStore.authors, testCaseToStore.description,
						testCaseToStore.keywords, testCaseToStore.testCaseType, testCaseToStore.path)

					// Update test case relation to actors.
					val newTestCaseActors = new util.HashSet[Long]
					testCase.targetActors.getOrElse("").split(",").foreach { actorId =>
						newTestCaseActors.add(savedActorIds.get(actorId))
					}
					val existingTestCaseActors = PersistenceSchema.testCaseHasActors.filter(_.testcase === existingTestCaseId.toLong).list
					existingTestCaseActors.foreach { existingTestCaseActor =>
						val existingActorId = existingTestCaseActor._3
						if (!newTestCaseActors.contains(existingActorId)) {
							// The actor is no longer mentioned in the test case - remove
							PersistenceSchema.conformanceResults.filter(_.testcase === existingTestCaseId.toLong).filter(_.actor === existingActorId).delete
							TestCaseManager.removeActorLinkForTestCase(existingTestCaseId, existingActorId)
						} else {
							// Existing actor remains - remove so that we keep track of what's left.
							newTestCaseActors.remove(existingActorId)
						}
					}
					// The remaining entries are the ones that don't already exist - add them.
					for (newTestCaseActor <- newTestCaseActors) {
						PersistenceSchema.testCaseHasActors.insert((existingTestCaseId.longValue(), suite.specification, newTestCaseActor))
						val implementingSystems = existingActorToSystemMap.get(newTestCaseActor)
						if (implementingSystems != null) {
							for (implementingSystem <- implementingSystems) {
								// Check to see if there existing test sessions for this test case.
								val previousResult = PersistenceSchema.testResults.filter(_.sutId === implementingSystem).filter(_.testCaseId === existingTestCaseId.toLong).sortBy(_.endTime.desc).firstOption
								if (previousResult.isDefined) {
									PersistenceSchema.conformanceResults.insert(ConformanceResult(0L, implementingSystem, suite.specification, newTestCaseActor, savedTestSuiteId, existingTestCaseId, previousResult.get.result, Some(previousResult.get.sessionId)))
								} else {
									PersistenceSchema.conformanceResults.insert(ConformanceResult(0L, implementingSystem, suite.specification, newTestCaseActor, savedTestSuiteId, existingTestCaseId, TestResultStatus.UNDEFINED.toString, None))
								}
							}
						}
					}
					result.add(new TestSuiteUploadItemResult(testCaseToStore.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE))
				} else {
					// New test case.
					existingTestCaseId = PersistenceSchema.testCases
						.returning(PersistenceSchema.testCases.map(_.id))
						.insert(testCaseToStore)
					savedTestCaseIds.add(existingTestCaseId)
					result.add(new TestSuiteUploadItemResult(testCaseToStore.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_ADD))
					// Update test case relation to actors.
					testCase.targetActors.getOrElse("").split(",").foreach {
						actorId =>
							val actorInternalId = savedActorIds.get(actorId)
							PersistenceSchema.testCaseHasActors.insert((existingTestCaseId.longValue(), suite.specification, actorInternalId))
							// All conformance results will be new. We need to add these for all systems that implement the actor.
							val implementingSystems = existingActorToSystemMap.get(actorInternalId)
							if (implementingSystems != null) {
								for (implementingSystem <- implementingSystems) {
									PersistenceSchema.conformanceResults.insert(ConformanceResult(0L, implementingSystem, suite.specification, actorInternalId, savedTestSuiteId, existingTestCaseId, TestResultStatus.UNDEFINED.toString, None))
								}
							}
					}
				}
			}
			// Remove test cases not in new test suite.
			for (testCaseEntry <- existingTestCaseMap.entrySet()) {
				TestCaseManager.delete(testCaseEntry.getValue)
				result.add(new TestSuiteUploadItemResult(testCaseEntry.getKey, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_REMOVE))
			}
			// Add new test suite actor links.
			for (actorEntry <- savedActorIds.entrySet()) {
				PersistenceSchema.testSuiteHasActors.insert((savedTestSuiteId, actorEntry.getValue))
			}
			// Update test suite test case links.
			for (testCaseId <- savedTestCaseIds) {
				PersistenceSchema.testSuiteHasTestCases.insert((savedTestSuiteId, testCaseId))
			}
			// Finally, delete the backup folder
			if (backupFolder.exists()) {
				FileUtils.deleteDirectory(backupFolder)
			}
			result
		} catch {
			case e:Exception => {
				session.rollback()
				if (existingSuite == null) {
					// This was a new test suite
					if (targetFolder.exists()) {
						FileUtils.deleteDirectory(targetFolder)
					}
				} else {
					// This was an update.
					if (backupFolder.exists()) {
						FileUtils.deleteDirectory(targetFolder)
						FileUtils.moveDirectory(backupFolder, targetFolder)
					}
				}
				throw e
			}
		}
	}

	def getTestSuitesBySpecificationAndActorAndTestCaseType(specificationId: Long, actorId: Long, testCaseType: Short): List[TestSuite] = {
		DB.withSession { implicit session =>
			val testSuiteIds = PersistenceSchema.testSuiteHasActors
				.filter(_.actor === actorId)
				.map(_.testsuite)
				.list

			val testSuites = PersistenceSchema.testSuites
				.filter(_.id inSet testSuiteIds)
				.filter(_.specification === specificationId)
				.sortBy(_.shortname.asc)
				.list

			testSuites map { testSuite =>
				val testCaseIds = PersistenceSchema.testSuiteHasTestCases
					.filter(_.testsuite === testSuite.id)
					.map(_.testcase)
					.list

				val testCases = PersistenceSchema.testCases
					.filter(_.id inSet testCaseIds)
					.filter(_.testCaseType === testCaseType)
					.list

				new TestSuite(testSuite, testCases)
			}
		}
	}

}