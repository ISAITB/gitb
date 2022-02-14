package managers

import java.io.File
import java.nio.file.{Path, Paths}
import java.util
import java.util.Objects
import com.gitb.tr.{TAR, TestResultType}

import javax.inject.{Inject, Singleton}
import models.Enums.TestSuiteReplacementChoice.TestSuiteReplacementChoice
import models.Enums.TestSuiteReplacementChoiceHistory.TestSuiteReplacementChoiceHistory
import models.Enums.TestSuiteReplacementChoiceMetadata.TestSuiteReplacementChoiceMetadata
import models.Enums.{TestResultStatus, TestSuiteReplacementChoice, TestSuiteReplacementChoiceHistory, TestSuiteReplacementChoiceMetadata}
import models.{TestArtifactMetadata, TestSuiteUploadResult, _}
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.Rep
import utils.tdlvalidator.tdl.{FileSource, TestSuiteValidationAdapter}
import utils.{CryptoUtil, RepositoryUtils, ZipArchiver}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.SetHasAsScala

object TestSuiteManager {

	val TEST_SUITES_PATH = "test-suites"

	type TestSuiteDbTuple = (
			Rep[Long], Rep[String], Rep[String], Rep[String],
			Rep[Option[String]], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]],
			Rep[Option[String]], Rep[Long], Rep[String], Rep[Boolean], Rep[String]
		)

	type TestSuiteValueTuple = (
		Long, String, String, String,
			Option[String], Option[String], Option[String], Option[String],
			Option[String], Long, String, Boolean, String
		)

	private def withoutDocumentation(dbTestSuite: PersistenceSchema.TestSuitesTable): TestSuiteDbTuple = {
		(dbTestSuite.id, dbTestSuite.shortname, dbTestSuite.fullname, dbTestSuite.version,
			dbTestSuite.authors, dbTestSuite.originalDate, dbTestSuite.modificationDate, dbTestSuite.description,
			dbTestSuite.keywords, dbTestSuite.specification, dbTestSuite.filename, dbTestSuite.hasDocumentation, dbTestSuite.identifier)
	}

	def tupleToTestSuite(x: TestSuiteValueTuple): TestSuites = {
		TestSuites(x._1, x._2, x._3, x._4, x._5, x._6, x._7, x._8, x._9, x._10, x._11, x._12, None, x._13, hidden = false)
	}

}

/**
 * Created by serbay on 10/17/14.
 */
@Singleton
class TestSuiteManager @Inject() (testResultManager: TestResultManager, actorManager: ActorManager, conformanceManager: ConformanceManager, endPointManager: EndPointManager, testCaseManager: TestCaseManager, parameterManager: ParameterManager, repositoryUtils: RepositoryUtils, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

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
			case Some(idList) =>
				PersistenceSchema.testSuites
					.filter(_.id inSet idList)
			case None =>
				PersistenceSchema.testSuites
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
		val testSuites = exec(PersistenceSchema.testSuites.filter(_.hidden === false).sortBy(_.shortname.asc).map(TestSuiteManager.withoutDocumentation).result.map(_.toList)).map(TestSuiteManager.tupleToTestSuite)
		testSuites map {
			ts:TestSuites =>
				val testCaseIds = exec(PersistenceSchema.testSuiteHasTestCases.filter(_.testsuite === ts.id).map(_.testcase).result.map(_.toList))
				val testCases = exec(PersistenceSchema.testCases.filter(_.id inSet testCaseIds).map(TestCaseManager.withoutDocumentation).result.map(_.toList)).map(TestCaseManager.tupleToTestCase)

				new TestSuite(ts, testCases)
		}
	}

	def getTestSuiteWithTestCases(testSuiteId: Long): TestSuite = {
		val testSuite = exec(
			PersistenceSchema.testSuites
				.filter(_.id === testSuiteId)
			  .result.head
		)
		val testCases = exec(PersistenceSchema.testCases
		  .join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
		  .filter(_._2.testsuite === testSuiteId)
			.sortBy(_._1.shortname.asc)
		  .map(x => TestCaseManager.withoutDocumentation(x._1))
			.result.map(_.toList)
		).map(TestCaseManager.tupleToTestCase)
		new TestSuite(testSuite, testCases)
	}

	def getTestSuitesWithTestCasesForCommunity(communityId: Long): List[TestSuite] = {
		val testSuites = exec(
			PersistenceSchema.testSuites
				.join(PersistenceSchema.specifications).on(_.specification === _.id)
  			.join(PersistenceSchema.communities).on(_._2.domain === _.domain)
				.filter(_._1._1.hidden === false)
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
				.filter(_._1.hidden === false)
				.filter(_._2.sut === systemId)
				.map(r => TestSuiteManager.withoutDocumentation(r._1))
				.distinctOn(_._1) // Test suite ID
				.sortBy(_._2.asc) // Test suite short name
				.result.map(_.toList)
		).map(TestSuiteManager.tupleToTestSuite)
		testSuites map {
			ts:TestSuites =>
				val testCaseIds = exec(PersistenceSchema.testSuiteHasTestCases.filter(_.testsuite === ts.id).map(_.testcase).result.map(_.toList))
				val testCases = exec(PersistenceSchema.testCases.filter(_.id inSet testCaseIds).map(TestCaseManager.withoutDocumentation).result.map(_.toList)).map(TestCaseManager.tupleToTestCase)

				new TestSuite(ts, testCases)
		}
	}

	def removeActorLinksForTestSuite(testSuiteId: Long): DBIO[_] = {
		PersistenceSchema.testSuiteHasActors
			.filter(_.testsuite === testSuiteId)
			.delete
	}

	def getSpecificationById(specId: Long): Specifications = {
		val spec = exec(PersistenceSchema.specifications.filter(_.id === specId).result.head)
		spec
	}

	def cancelPendingTestSuiteActions(pendingTestSuiteIdentifier: String): TestSuiteUploadResult = {
		applyPendingTestSuiteActions(pendingTestSuiteIdentifier, TestSuiteReplacementChoice.CANCEL, null)
	}

	def completePendingTestSuiteActions(pendingTestSuiteIdentifier: String, actionsPerSpec: List[PendingTestSuiteAction]): TestSuiteUploadResult = {
		applyPendingTestSuiteActions(pendingTestSuiteIdentifier, TestSuiteReplacementChoice.PROCEED, actionsPerSpec)
	}

	private def applyPendingTestSuiteActions(pendingTestSuiteIdentifier: String, overallAction: TestSuiteReplacementChoice, actionsPerSpec: List[PendingTestSuiteAction]): TestSuiteUploadResult = {
		val result = new TestSuiteUploadResult()
		val pendingTestSuiteFolder = new File(repositoryUtils.getPendingFolder(), pendingTestSuiteIdentifier)
		try {
			if (overallAction == TestSuiteReplacementChoice.PROCEED) {
				if (pendingTestSuiteFolder.exists()) {
					val pendingFiles = pendingTestSuiteFolder.listFiles()
					if (pendingFiles.length == 1) {
						val testSuite = repositoryUtils.getTestSuiteFromZip(actionsPerSpec.head.specification, pendingFiles.head)
						// Sanity check
						if (testSuite.isDefined && testSuite.get.testCases.isDefined) {
							val onSuccessCalls = mutable.ListBuffer[() => _]()
							val onFailureCalls = mutable.ListBuffer[() => _]()
							val dbActions = new ListBuffer[DBIO[List[TestSuiteUploadItemResult]]]()
							actionsPerSpec.foreach { replacementChoice =>
								if (replacementChoice.action == TestSuiteReplacementChoice.PROCEED) {
									setTestSuiteSpecification(testSuite.get, replacementChoice.specification)
									dbActions += replaceTestSuiteInternal(testSuite.get.toCaseObject, testSuite.get.actors, testSuite.get.testCases, pendingFiles.head, replacementChoice.actionHistory.get, replacementChoice.actionMetadata.get, onSuccessCalls, onFailureCalls)
								}
							}
							val mergedDbAction: DBIO[List[TestSuiteUploadItemResult]] = mergeActionsWithResults(dbActions.toList)
							import scala.jdk.CollectionConverters._
							result.items.addAll(exec(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), mergedDbAction).transactionally).asJavaCollection)
							result.success = true
						} else {
							result.errorInformation = "The pending test suite archive [" + pendingTestSuiteIdentifier + "] was corrupted "
						}
					} else {
						result.errorInformation = "A single pending test suite archive was expected but found " + pendingFiles.length
					}
				} else {
					result.errorInformation = "The pending test suite ["+pendingTestSuiteIdentifier+"] could not be located"
				}
			} else {
				result.success = true
			}
		} catch {
			case e:Exception =>
				logger.error("An error occurred", e)
				result.errorInformation = e.getMessage
				result.success = false
		} finally {
			// Delete temporary folder (if exists)
			FileUtils.deleteDirectory(pendingTestSuiteFolder)
		}
		result
	}

	private def validateTestSuite(specifications: List[Long], tempTestSuiteArchive: File): TAR = {
		// Domain parameters.
		val domain = conformanceManager.getDomainOfSpecification(specifications.head)
		val parameterSet = conformanceManager.getDomainParameters(domain.id).map(p => p.name).toSet
		// Actor IDs (if multiple specs these are ones in common).
		var actorIdSet: Option[mutable.Set[String]] = None
		conformanceManager.getActorIdsOfSpecifications(specifications).values.foreach { specActorIds =>
			if (actorIdSet.isEmpty) {
				actorIdSet = Some(mutable.Set[String]())
				actorIdSet.get ++= specActorIds
			} else {
				actorIdSet = Some(actorIdSet.get & specActorIds)
			}
		}
		import scala.jdk.CollectionConverters._
		val report = TestSuiteValidationAdapter.getInstance().doValidation(new FileSource(tempTestSuiteArchive), actorIdSet.getOrElse(mutable.Set[String]()).toSet.asJava, parameterSet.asJava, repositoryUtils.getTmpValidationFolder().getAbsolutePath)
		report
	}

	private def setTestSuiteSpecification(testSuite: TestSuite, specification: Long): Unit = {
		testSuite.specification = specification
		if (testSuite.testCases.isDefined) {
			testSuite.testCases = Some(
				testSuite.testCases.get.map(tc => tc.withSpecification(specification))
			)
		}
	}

	private def testSuiteDefinesExistingActors(specification: Long, actorIdentifiers: List[String]): Boolean = {
		exec(PersistenceSchema.actors
		  .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
		  .filter(_._2.specId === specification)
		  .filter(_._1.actorId inSet actorIdentifiers)
			  .map(x => x._1.actorId)
			  .result
			  .headOption
		).isDefined
	}

	def deployTestSuiteFromZipFile(specifications: List[Long], tempTestSuiteArchive: File): TestSuiteUploadResult = {
		val result = new TestSuiteUploadResult()
		try {
			result.validationReport = validateTestSuite(specifications, tempTestSuiteArchive)
			if (result.validationReport.getResult == TestResultType.SUCCESS) {
				val hasReportItems = (result.validationReport.getCounters.getNrOfWarnings.intValue() > 0) ||
						(result.validationReport.getCounters.getNrOfErrors.intValue() > 0) ||
						(result.validationReport.getCounters.getNrOfAssertions.intValue() > 0)
				// If we have messages we don't need to make a full parse (i.e. test cases and documentation).
				val testSuite = repositoryUtils.getTestSuiteFromZip(specifications.head, tempTestSuiteArchive, !hasReportItems)
				if (testSuite.isDefined) {
					var definedActorsIdentifiers: Option[List[String]] = None
					if (testSuite.get.actors.isDefined) {
						definedActorsIdentifiers = Some(testSuite.get.actors.get.map(actor => actor.actorId))
					}
					if (hasReportItems) {
						result.needsConfirmation = true
					}
					val specsWithExistingTestSuite = new ListBuffer[Long]()
					val specsWithMatchingData = new ListBuffer[Long]()
					specifications.foreach { specification =>
						setTestSuiteSpecification(testSuite.get, specification)
						val testSuiteExists = checkTestSuiteExists(testSuite.get)
						if (testSuiteExists) {
							specsWithExistingTestSuite += specification
							result.needsConfirmation = true
						}
						if (definedActorsIdentifiers.isDefined) {
							val matchingDataExists = testSuiteDefinesExistingActors(specification, definedActorsIdentifiers.get)
							if (matchingDataExists) {
								specsWithMatchingData += specification
								result.needsConfirmation = true
							}
						}
					}
					if (result.needsConfirmation) {
						if (specsWithExistingTestSuite.nonEmpty) {
							result.existsForSpecs = Some(specsWithExistingTestSuite.toList)
						}
						if (specsWithMatchingData.nonEmpty) {
							result.matchingDataExists = Some(specsWithMatchingData.toList)
						}
						// Park the test suite for now and ask user what to do
						FileUtils.moveDirectoryToDirectory(tempTestSuiteArchive.getParentFile, repositoryUtils.getPendingFolder(), true)
						result.pendingTestSuiteFolderName = tempTestSuiteArchive.getParentFile.getName
					} else {
						// Proceed immediately.
						val onSuccessCalls = mutable.ListBuffer[() => _]()
						val onFailureCalls = mutable.ListBuffer[() => _]()
						val dbActions = new ListBuffer[DBIO[List[TestSuiteUploadItemResult]]]()
						specifications.foreach { specification =>
							setTestSuiteSpecification(testSuite.get, specification)
							dbActions += saveTestSuite(testSuite.get.toCaseObject, null, updateMetadata = true, testSuite.get.actors, testSuite.get.testCases, tempTestSuiteArchive, onSuccessCalls, onFailureCalls)
						}
						val action: DBIO[List[TestSuiteUploadItemResult]] = mergeActionsWithResults(dbActions.toList)
						import scala.jdk.CollectionConverters._
						result.items.addAll(exec(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), action).transactionally).asJavaCollection)
						result.success = true
					}
				}
			} else {
				result.needsConfirmation = true
			}
		} catch {
			case e:Exception =>
				logger.error("An error occurred", e)
				result.errorInformation = e.getMessage
				result.success = false
		} finally {
			// Delete temporary folder (if exists)
			FileUtils.deleteDirectory(tempTestSuiteArchive.getParentFile)
		}
		result
	}

	def getTestSuiteFile(specification: Long, testSuiteName: String): File = {
		new File(repositoryUtils.getTestSuitesPath(getSpecificationById(specification)), testSuiteName)
	}

	private def checkTestSuiteExists(suite: TestSuite): Boolean = {
		val count = exec(PersistenceSchema.testSuites
			.filter(_.identifier === suite.identifier)
			.filter(_.specification === suite.specification)
			.size.result)
		count > 0
	}

	private def getTestSuiteIdBySpecificationAndIdentifier(specId: Long, identifier: String): Option[Long] = {
		exec(PersistenceSchema.testSuites
			.filter(_.identifier === identifier)
			.filter(_.specification === specId)
			.map(_.id)
			.result.headOption)
	}

	private def getTestSuiteBySpecificationAndIdentifier(specId: Long, identifier: String): Option[TestSuites] = {
		val testSuite = exec(PersistenceSchema.testSuites
			.filter(_.identifier === identifier)
			.filter(_.specification === specId)
			.map(TestSuiteManager.withoutDocumentation)
			.result.headOption)
		if (testSuite.isDefined) {
			Some(TestSuiteManager.tupleToTestSuite(testSuite.get))
		} else {
			None
		}
	}

	private def getTestSuiteMetadataForReplacement(testSuiteId: Long): Option[TestArtifactMetadata] = {
		var result: Option[TestArtifactMetadata] = None
		val testSuiteData = exec(PersistenceSchema.testSuites.filter(_.id === testSuiteId).map(x => (x.shortname, x.fullname, x.description, x.documentation)).result.headOption)
		if (testSuiteData.isDefined) {
			result = Some(new TestArtifactMetadata(testSuiteData.get._1, testSuiteData.get._2, testSuiteData.get._3, testSuiteData.get._4))
			val testCaseMap = mutable.Map[String, TestArtifactMetadata]()
			exec(PersistenceSchema.testCases
				.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
				.filter(_._2.testsuite === testSuiteId)
				.map(x => (x._1.identifier, x._1.shortname, x._1.fullname, x._1.description, x._1.documentation))
				.result
			).foreach { x =>
				testCaseMap += (x._1 -> new TestArtifactMetadata(x._2, x._3, x._4, x._5))
			}
			result.get.testCases = testCaseMap.iterator.toMap
		}
		result
	}

	private def applyMetadataToTestSuite(metadata: TestArtifactMetadata, testSuite: TestSuites, testCases: Option[List[TestCases]]): (TestSuites, Option[List[TestCases]]) = {
		val updatedTestSuite = TestSuites(testSuite.id, metadata.shortName, metadata.fullName, testSuite.version, testSuite.authors,
			testSuite.originalDate, testSuite.modificationDate, metadata.description, testSuite.keywords, testSuite.specification,
			testSuite.filename, metadata.documentation.isDefined, metadata.documentation, testSuite.identifier, testCases.isEmpty || testCases.get.isEmpty)
		var updatedTestCases: Option[List[TestCases]] = None
		if (testCases.isDefined) {
			val testCasesList = ListBuffer[TestCases]()
			testCases.get.foreach { testCase =>
				var shortName = testCase.shortname
				var fullName = testCase.fullname
				var description = testCase.description
				var documentation = testCase.documentation
				val testCaseMetadata = metadata.testCases.get(testCase.identifier)
				if (testCaseMetadata.isDefined) {
					shortName = testCaseMetadata.get.shortName
					fullName = testCaseMetadata.get.fullName
					description = testCaseMetadata.get.description
					documentation = testCaseMetadata.get.documentation
				}
				testCasesList += TestCases(testCase.id, shortName, fullName, testCase.version, testCase.authors, testCase.originalDate,
					testCase.modificationDate, description, testCase.keywords, testCase.testCaseType, testCase.path, testCase.targetSpec,
					testCase.targetActors, testCase.targetOptions, testCase.testSuiteOrder, documentation.isDefined, documentation,
					testCase.identifier)
			}
			updatedTestCases = Some(testCasesList.toList)
		}
		(updatedTestSuite, updatedTestCases)
	}

	private def replaceTestSuiteInternal(suite: TestSuites, testSuiteActors: Option[List[Actor]], testCases: Option[List[TestCases]], tempTestSuiteArchive: File, actionHistory: TestSuiteReplacementChoiceHistory, actionMetadata: TestSuiteReplacementChoiceMetadata, onSuccessCalls: mutable.ListBuffer[() => _], onFailureCalls: mutable.ListBuffer[() => _]): DBIO[List[TestSuiteUploadItemResult]] = {
		var action: DBIO[List[TestSuiteUploadItemResult]] = null
		val updateMetadata = actionMetadata == TestSuiteReplacementChoiceMetadata.UPDATE
		if (actionHistory == TestSuiteReplacementChoiceHistory.DROP) {
			val existingTestSuiteId = getTestSuiteIdBySpecificationAndIdentifier(suite.specification, suite.identifier)
			var undeployDbAction: Option[DBIO[_]] = None
			var testSuiteToUse: TestSuites = suite
			var testCasesToUse: Option[List[TestCases]] = testCases
			if (existingTestSuiteId.isDefined) {
				if (!updateMetadata) {
					val metadataToUse = getTestSuiteMetadataForReplacement(existingTestSuiteId.get)
					if (metadataToUse.isDefined) {
						val results = applyMetadataToTestSuite(metadataToUse.get, suite, testCases)
						testSuiteToUse = results._1
						testCasesToUse = results._2
					}
				}
				undeployDbAction = Some(conformanceManager.undeployTestSuite(existingTestSuiteId.get, onSuccessCalls))
			}
			action = undeployDbAction.getOrElse(DBIO.successful(())) andThen
				saveTestSuite(testSuiteToUse, null, updateMetadata, testSuiteActors, testCasesToUse, tempTestSuiteArchive, onSuccessCalls, onFailureCalls)
		} else if (actionHistory == TestSuiteReplacementChoiceHistory.KEEP) {
			val existingTestSuite = getTestSuiteBySpecificationAndIdentifier(suite.specification, suite.identifier)
			action = saveTestSuite(suite, existingTestSuite.orNull, updateMetadata, testSuiteActors, testCases, tempTestSuiteArchive, onSuccessCalls, onFailureCalls)
		} else {
			throw new IllegalStateException("Unexpected test suite replacement action ["+actionHistory+"]")
		}
		action
	}

	def updateTestSuiteMetadata(testSuiteId: Long, name: String, description: Option[String], documentation: Option[String], version: String): Unit = {
		var hasDocumentationToSet = false
		var documentationToSet: Option[String] = None
		if (documentation.isDefined && !documentation.get.isBlank) {
			hasDocumentationToSet = true
			documentationToSet = documentation
		}
		val q1 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.shortname, t.fullname, t.description, t.documentation, t.hasDocumentation, t.version)
		exec(
			q1.update(name, name, description, documentationToSet, hasDocumentationToSet, version) andThen
				testResultManager.updateForUpdatedTestSuite(testSuiteId, name)
				.transactionally
		)
	}

	def updateTestCaseMetadata(testCaseId: Long, name: String, description: Option[String], documentation: Option[String]): Unit = {
		var hasDocumentationToSet = false
		var documentationToSet: Option[String] = None
		if (documentation.isDefined && !documentation.get.isBlank) {
			hasDocumentationToSet = true
			documentationToSet = documentation
		}
		val q1 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.shortname, t.fullname, t.description, t.documentation, t.hasDocumentation)
		exec(
				q1.update(name, name, description, documentationToSet, hasDocumentationToSet) andThen
					testResultManager.updateForUpdatedTestCase(testCaseId, name)
					.transactionally
		)
	}

	def updateTestSuiteInDbWithoutMetadata(testSuiteId: Long, newData: TestSuites): DBIO[_] = {
		val q1 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.filename, t.hidden)
		q1.update(newData.filename, newData.hidden)
	}

	def updateTestSuiteInDb(testSuiteId: Long, newData: TestSuites): DBIO[_] = {
		val q1 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.identifier, t.shortname, t.fullname, t.version, t.authors, t.keywords, t.description, t.filename, t.hasDocumentation, t.documentation, t.hidden)
		q1.update(newData.identifier, newData.shortname, newData.fullname, newData.version, newData.authors, newData.keywords, newData.description, newData.filename, newData.hasDocumentation, newData.documentation, newData.hidden) andThen
			testResultManager.updateForUpdatedTestSuite(testSuiteId, newData.shortname)
	}

	private def theSameActor(one: Actors, two: Actors): Boolean = {
		(Objects.equals(one.name, two.name)
				&& Objects.equals(one.description, two.description)
				&& Objects.equals(one.default, two.default)
        && Objects.equals(one.hidden, two.hidden)
				&& Objects.equals(one.displayOrder, two.displayOrder))
	}

	private def theSameEndpoint(one: Endpoint, two: Endpoints): Boolean = {
		(Objects.equals(one.name, two.name)
				&& Objects.equals(one.desc, two.desc))
	}

	private def theSameParameter(one: models.Parameters, two: models.Parameters): Boolean = {
		(Objects.equals(one.name, two.name)
				&& Objects.equals(one.desc, two.desc)
				&& Objects.equals(one.kind, two.kind)
				&& Objects.equals(one.use, two.use)
				&& Objects.equals(one.adminOnly, two.adminOnly)
				&& Objects.equals(one.notForTests, two.notForTests)
				&& Objects.equals(one.hidden, two.hidden)
				&& Objects.equals(one.allowedValues, two.allowedValues)
				&& Objects.equals(one.displayOrder, two.displayOrder)
				&& Objects.equals(one.dependsOn, two.dependsOn)
				&& Objects.equals(one.dependsOnValue, two.dependsOnValue)
			)
	}

	def isActorReference(actorToSave: Actors): Boolean = {
		actorToSave.actorId != null && actorToSave.name == null && actorToSave.description.isEmpty
	}

	private def stepSaveTestSuite(suite: TestSuites, existingSuite: TestSuites, updateMetadata: Boolean): DBIO[(Long, List[TestSuiteUploadItemResult])] = {
		val actions = new ListBuffer[DBIO[_]]()
		var savedTestSuiteId: DBIO[Long] = null
		val result = new ListBuffer[TestSuiteUploadItemResult]()
		if (existingSuite != null) {
			// Update existing test suite.
			if (updateMetadata) {
				actions += updateTestSuiteInDb(existingSuite.id, suite)
			} else {
				actions += updateTestSuiteInDbWithoutMetadata(existingSuite.id, suite)
			}
			// Remove existing actor links (these will be updated later).
			actions += removeActorLinksForTestSuite(existingSuite.id)
			result += new TestSuiteUploadItemResult(suite.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_SUITE, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE, suite.specification)
			savedTestSuiteId = DBIO.successful(existingSuite.id)
		} else {
			// New test suite.
			savedTestSuiteId = PersistenceSchema.testSuites.returning(PersistenceSchema.testSuites.map(_.id)) += suite
			result += new TestSuiteUploadItemResult(suite.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_SUITE, TestSuiteUploadItemResult.ACTION_TYPE_ADD, suite.specification)
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

	private def stepSaveActors(testSuiteActors: Option[List[Actor]], domainId: Long, specificationActors: List[Actors], suite: TestSuites, updateMetadata: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[(util.Map[String, Long], List[TestSuiteUploadItemResult])] = {
		// Get the (new, existing or referenced) actor IDs resulting from the import.
		var finalAction: DBIO[List[TestSuiteUploadItemResult]] = DBIO.successful(List[TestSuiteUploadItemResult]())
		val actions = new ListBuffer[DBIO[List[TestSuiteUploadItemResult]]]()
		val savedActorIds: util.Map[String, Long] = new util.HashMap[String, Long]
		for (testSuiteActor <- testSuiteActors.get) {
			val result = new ListBuffer[TestSuiteUploadItemResult]()
			var updateAction: Option[DBIO[_]] = None
			var savedActorId: DBIO[Long] = null
			val actorToSave = testSuiteActor.toCaseObject(CryptoUtil.generateApiKey(), domainId)
			val savedActor = lookupActor(actorToSave, specificationActors)
			var savedActorStringId: String = null

			if (savedActor.isDefined) {
				if (!updateMetadata || isActorReference(actorToSave) || theSameActor(savedActor.get, actorToSave)) {
					result += new TestSuiteUploadItemResult(savedActor.get.name, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED, suite.specification)
				} else {
					updateAction = Some(actorManager.updateActor(savedActor.get.id, actorToSave.actorId, actorToSave.name, actorToSave.description, actorToSave.default, actorToSave.hidden, actorToSave.displayOrder, suite.specification, None))
					result += new TestSuiteUploadItemResult(savedActor.get.name, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE, suite.specification)
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
				result += new TestSuiteUploadItemResult(actorToSave.actorId, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_ADD, suite.specification)
			}

			val combinedAction = (updateAction.getOrElse(DBIO.successful(())) andThen savedActorId).flatMap(id => {
				savedActorIds.put(savedActorStringId, id)
				for {
					existingEndpoints <- PersistenceSchema.endpoints.filter(_.actor === id).result.map(_.toList)
					endpointResults <- stepSaveEndpoints(id, testSuiteActor, actorToSave, existingEndpoints, suite, updateMetadata, onSuccessCalls)
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

	private def stepSaveEndpoints(savedActorId: Long, testSuiteActor: Actor, actorToSave: Actors, existingEndpoints: List[Endpoints], suite: TestSuites, updateMetadata: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[List[TestSuiteUploadItemResult]] = {
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
					if (!updateMetadata || theSameEndpoint(endpoint, existingEndpoint)) {
						result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]", TestSuiteUploadItemResult.ITEM_TYPE_ENDPOINT, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED, suite.specification)
					} else {
						updateAction = Some(endPointManager.updateEndPoint(existingEndpoint.id, endpoint.name, endpoint.desc))
						result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]", TestSuiteUploadItemResult.ITEM_TYPE_ENDPOINT, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE, suite.specification)
					}
				} else {
					// New endpoint.
					endpointId = endPointManager.createEndpoint(endpoint.toCaseObject.copy(actor = savedActorId))
					result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]", TestSuiteUploadItemResult.ITEM_TYPE_ENDPOINT, TestSuiteUploadItemResult.ACTION_TYPE_ADD, suite.specification)
				}
				val combinedAction = (updateAction.getOrElse(DBIO.successful(())) andThen endpointId).flatMap(id => {
					for {
						existingEndpointParameters <- PersistenceSchema.parameters.filter(_.endpoint === id.longValue()).result.map(_.toList)
						parameterResults <- stepSaveParameters(id, endpoint, actorToSave, existingEndpointParameters, suite, updateMetadata, onSuccessCalls)
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

	private def stepSaveParameters(endpointId: Long, endpoint: Endpoint, actorToSave: Actors, existingEndpointParameters: List[models.Parameters], suite: TestSuites, updateMetadata: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[List[TestSuiteUploadItemResult]] = {
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
					if (!updateMetadata || theSameParameter(parameter, existingParameter)) {
						result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]."+parameter.name, TestSuiteUploadItemResult.ITEM_TYPE_PARAMETER, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED, suite.specification)
					} else {
						action = Some(parameterManager.updateParameter(parameterId, parameter.name, parameter.desc, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue, onSuccessCalls))
						result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]."+parameter.name, TestSuiteUploadItemResult.ITEM_TYPE_PARAMETER, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE, suite.specification)
					}
				} else {
					// New parameter.
					action = Some(parameterManager.createParameter(parameter.withEndpoint(endpointId, Some(0))))
					result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]."+parameter.name, TestSuiteUploadItemResult.ITEM_TYPE_PARAMETER, TestSuiteUploadItemResult.ACTION_TYPE_ADD, suite.specification)
				}
				actions += action.getOrElse(DBIO.successful(()))
			}
		}
		var combinedAction: DBIO[_] = null
		if (actions.nonEmpty) {
			combinedAction = DBIO.seq(actions.toList.map(a => a): _*)
		} else {
			combinedAction = DBIO.successful(())
		}
		combinedAction andThen DBIO.successful(result.toList)
	}

	def stepProcessTestCases(specificationId: Long, savedTestSuiteId: Long, testCases: Option[List[TestCases]], resourcePaths: Map[String, String], existingTestCaseMap: util.HashMap[String, (java.lang.Long, String)], savedActorIds: util.Map[String, Long], existingActorToSystemMap: util.Map[Long, util.Set[Long]], updateMetadata: Boolean): DBIO[(util.List[Long], List[TestSuiteUploadItemResult])] = {
		val savedTestCaseIds: util.List[Long] = new util.ArrayList[Long]
		var combinedAction: DBIO[_] = DBIO.successful(())
		val result = new ListBuffer[TestSuiteUploadItemResult]()
		for (testCase <- testCases.get) {
			val testCaseToStore = testCase.withPath(resourcePaths(testCase.identifier))
			val existingTestCaseInfo = existingTestCaseMap.get(testCase.identifier)
			if (existingTestCaseInfo != null) {
				val existingTestCaseId: Long = existingTestCaseInfo._1
				// Test case already exists - update.
				existingTestCaseMap.remove(testCase.identifier)
				if (updateMetadata) {
					combinedAction = combinedAction andThen
						testCaseManager.updateTestCase(
							existingTestCaseId, testCaseToStore.identifier, testCaseToStore.shortname, testCaseToStore.fullname,
							testCaseToStore.version, testCaseToStore.authors, testCaseToStore.description,
							testCaseToStore.keywords, testCaseToStore.testCaseType, testCaseToStore.path, testCaseToStore.testSuiteOrder,
							testCaseToStore.targetActors.get, testCaseToStore.documentation.isDefined, testCaseToStore.documentation
						)
				} else {
					combinedAction = combinedAction andThen
						testCaseManager.updateTestCaseWithoutMetadata(existingTestCaseId, testCaseToStore.path, testCaseToStore.testSuiteOrder,
							testCaseToStore.targetActors.get)
				}
				// Update test case relation to actors.
				val actorsToFurtherProcess: util.Set[Long] = new util.HashSet()
				val actorsThatExistAndChangedToSut: util.Set[Long] = new util.HashSet()
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
					existingTestCaseActors <- PersistenceSchema.testCaseHasActors.filter(_.testcase === existingTestCaseId).result.map(_.toList)
					_ <- {
						val actions = new ListBuffer[DBIO[_]]()
						existingTestCaseActors.map(existingTestCaseActor => {
							val existingActorId = existingTestCaseActor._3
							val existingMarkedAsSut = existingTestCaseActor._4
							if (!actorsToFurtherProcess.contains(existingActorId)) {
								// The actor is no longer mentioned in the test case - remove
								actions += PersistenceSchema.conformanceResults.filter(_.testcase === existingTestCaseId).filter(_.actor === existingActorId).delete
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
                    actions += PersistenceSchema.conformanceResults.filter(_.testcase === existingTestCaseId).filter(_.actor === existingActorId).delete
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
							DBIO.seq(actions.toList.map(a => a): _*)
						} else {
							DBIO.successful(())
						}
					}
					_ <- {
						val actions = actorsToFurtherProcess.asScala.map(newTestCaseActor => {
							var action: DBIO[_] = null
              if (actorsThatExistAndChangedToSut.contains(newTestCaseActor)) {
                action = DBIO.successful(())
              } else {
                action = PersistenceSchema.testCaseHasActors += (existingTestCaseId, specificationId, newTestCaseActor, newTestCaseActor == sutActor)
              }
							val implementingSystems = existingActorToSystemMap.get(newTestCaseActor)
							if (implementingSystems != null) {
								implementingSystems.asScala.foreach { implementingSystem: Long =>
									// Check to see if there are existing test sessions for this test case.
									action = action andThen (for {
										previousResult <- PersistenceSchema.testResults.filter(_.sutId === implementingSystem).filter(_.testCaseId === existingTestCaseId).sortBy(_.endTime.desc).result.headOption
										_ <- {
											if (previousResult.isDefined) {
												PersistenceSchema.conformanceResults += ConformanceResult(0L, implementingSystem, specificationId, newTestCaseActor, savedTestSuiteId, existingTestCaseId, previousResult.get.result, previousResult.get.outputMessage, Some(previousResult.get.sessionId), Some(previousResult.get.endTime.getOrElse(previousResult.get.startTime)))
											} else {
												PersistenceSchema.conformanceResults += ConformanceResult(0L, implementingSystem, specificationId, newTestCaseActor, savedTestSuiteId, existingTestCaseId, TestResultStatus.UNDEFINED.toString, None, None, None)
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
				result += new TestSuiteUploadItemResult(testCaseToStore.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE, specificationId)
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
										for (implementingSystem <- implementingSystems.asScala) {
											action = action andThen
												(PersistenceSchema.conformanceResults += ConformanceResult(0L, implementingSystem, specificationId, actorInternalId, savedTestSuiteId, existingTestCaseId, TestResultStatus.UNDEFINED.toString, None, None, None))
										}
									}
								}
							}
						}
						action
					}
				} yield())
				result += new TestSuiteUploadItemResult(testCaseToStore.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_ADD, specificationId)
			}
		}
		combinedAction andThen DBIO.successful((savedTestCaseIds, result.toList))
	}

	def stepRemoveTestCases(existingTestCaseMap: util.HashMap[String, (java.lang.Long, String)], specificationId: Long): DBIO[List[TestSuiteUploadItemResult]] = {
		// Remove test cases not in new test suite.
		val actions = new ListBuffer[DBIO[_]]()
		val results = new ListBuffer[TestSuiteUploadItemResult]()
		existingTestCaseMap.entrySet().forEach { testCaseEntry =>
			actions += testCaseManager.delete(testCaseEntry.getValue._1)
			results += new TestSuiteUploadItemResult(testCaseEntry.getValue._2, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_REMOVE, specificationId)
		}
		DBIO.seq(actions.toList: _*) andThen DBIO.successful(results.toList)
	}

	def stepUpdateTestSuiteActorLinks(savedTestSuiteId: Long, savedActorIds: util.Map[String, Long]): DBIO[_] = {
		// Add new test suite actor links.
		val actions = new ListBuffer[DBIO[_]]()
		savedActorIds.entrySet().forEach { actorEntry =>
			actions += (PersistenceSchema.testSuiteHasActors += (savedTestSuiteId, actorEntry.getValue))
		}
		DBIO.seq(actions.toList: _*)
	}

	def stepUpdateTestSuiteTestCaseLinks(savedTestSuiteId: Long, savedTestCaseIds: util.List[Long]): DBIO[_] = {
		// Update test suite test case links.
		val actions = new ListBuffer[DBIO[_]]()
		savedTestCaseIds.forEach { testCaseId =>
			actions += (PersistenceSchema.testSuiteHasTestCases += (savedTestSuiteId, testCaseId))
		}
		DBIO.seq(actions.toList: _*)
	}

	def getSystemActors(specificationId: Long) = {
		PersistenceSchema.systemImplementsActors.filter(_.specId === specificationId).result.map(_.toList)
	}

	def getExistingActorToSystemMap(systemActors: List[(Long, Long, Long)]) = {
		// First get the map of existing actorIds to systems
		val existingActorToSystemMap: util.Map[Long, util.Set[Long]] = new util.HashMap()
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

	def getExistingTestCasesForTestSuite(testSuiteId: Long): DBIO[List[(Long, String, String)]] = {
		PersistenceSchema.testCases
			.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
			.filter(_._2.testsuite === testSuiteId)
			.map(r => (r._1.id, r._1.identifier, r._1.shortname))
			.result
			.map(_.toList)
	}

	def getExistingTestCaseMap(existingTestCasesForTestSuite: List[(Long, String, String)]): DBIO[util.HashMap[String, (java.lang.Long, String)]] = {
		// Process test cases.
		val existingTestCaseMap = new util.HashMap[String, (java.lang.Long, String)]()
		if (existingTestCasesForTestSuite.nonEmpty) {
			// This is an update - check for existing test cases.
			for (existingTestCase <- existingTestCasesForTestSuite) {
				existingTestCaseMap.put(existingTestCase._2, (existingTestCase._1, existingTestCase._3))
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
	private def saveTestSuite(suite: TestSuites, existingSuite: TestSuites, updateMetadata: Boolean, testSuiteActors: Option[List[Actor]], testCases: Option[List[TestCases]], tempTestSuiteArchive: File, onSuccessCalls: mutable.ListBuffer[() => _], onFailureCalls: mutable.ListBuffer[() => _]): DBIO[List[TestSuiteUploadItemResult]] = {
		val targetFolder: File = getTestSuiteFile(suite.specification, suite.filename)
    var existingFolder: File = null
    if (existingSuite != null) {
      // Update case.
      existingFolder = new File(targetFolder.getParent, existingSuite.filename)
    }
		val resourcePaths = repositoryUtils.extractTestSuiteFilesFromZipToFolder(suite.specification, targetFolder, tempTestSuiteArchive)
		val action = for {
			// Get the specification of the test suite.
			spec <- PersistenceSchema.specifications.filter(_.id === suite.specification).result.head
			// Save the test suite (insert or update) and return the identifier to use.
			saveTestSuiteStep <- stepSaveTestSuite(suite, existingSuite, updateMetadata)
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
			saveActorsStep <- stepSaveActors(testSuiteActors, spec.domain, specificationActors, suite, updateMetadata, onSuccessCalls)
			// Lookup the existing test cases for the test suite (if it already exists).
			existingTestCasesForTestSuite <- {
				if (existingSuite != null) {
					getExistingTestCasesForTestSuite(saveTestSuiteStep._1)
				} else {
					DBIO.successful(List[(Long, String, String)]())
				}
			}
			// Place the existing test cases in a map for further processing.
			existingTestCaseMap <- getExistingTestCaseMap(existingTestCasesForTestSuite)
			// Lookup the map of systems to actors for the specification
			systemActors <- getSystemActors(suite.specification)
			// Create a map of actors to systems.
			existingActorToSystemMap <- getExistingActorToSystemMap(systemActors)
			// Process the test cases.
			processTestCasesStep <- stepProcessTestCases(spec.id, saveTestSuiteStep._1, testCases, resourcePaths, existingTestCaseMap, saveActorsStep._1, existingActorToSystemMap, updateMetadata)
			// Remove the test cases that are no longer in the test suite.
			removeTestCasesStep <- stepRemoveTestCases(existingTestCaseMap, suite.specification)
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
		val testSuiteFolder = repositoryUtils.getTestSuitesResource(specification, testSuite.filename, None)
		var outputPathToUse = testSuiteOutputPath
		if (testSuiteOutputPath.isEmpty) {
			outputPathToUse = Some(Paths.get(
				repositoryUtils.getTempReportFolder().getAbsolutePath,
				"test_suite",
				"test_suite."+testSuite.id.toString+"."+System.currentTimeMillis()+".zip"
			))
		}
		new ZipArchiver(testSuiteFolder.toPath, outputPathToUse.get).zip()
		outputPathToUse.get
	}

}