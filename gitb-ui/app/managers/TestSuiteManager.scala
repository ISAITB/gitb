package managers

import com.gitb.tr.{TAR, TestResultType}
import managers.testsuite.{TestSuitePaths, TestSuiteSaveResult}
import models.Enums.TestSuiteReplacementChoice.{PROCEED, TestSuiteReplacementChoice}
import models.Enums.{TestCaseUploadMatchType, TestResultStatus, TestSuiteReplacementChoice}
import models._
import models.automation.{TestSuiteDeployRequest, TestSuiteLinkRequest, TestSuiteLinkResponseSpecification, TestSuiteUnlinkRequest}
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.Rep
import utils.tdlvalidator.tdl.{FileSource, TestSuiteValidationAdapter}
import utils.{CryptoUtil, RepositoryUtils, ZipArchiver}

import java.io.File
import java.nio.file.{Path, Paths}
import java.util
import java.util.Objects
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsJava, SetHasAsScala}

object TestSuiteManager {

	val TEST_SUITES_PATH = "test-suites"

	type TestSuiteDbTuple = (
			Rep[Long], Rep[String], Rep[String], Rep[String],
			Rep[Option[String]], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]],
			Rep[Option[String]], Rep[Long], Rep[String], Rep[Boolean], Rep[Boolean], Rep[String], Rep[Option[String]]
		)

	type TestSuiteValueTuple = (
		Long, String, String, String,
			Option[String], Option[String], Option[String], Option[String],
			Option[String], Long, String, Boolean, Boolean, String, Option[String]
		)

	private def withoutDocumentation(dbTestSuite: PersistenceSchema.TestSuitesTable): TestSuiteDbTuple = {
		(dbTestSuite.id, dbTestSuite.shortname, dbTestSuite.fullname, dbTestSuite.version,
			dbTestSuite.authors, dbTestSuite.originalDate, dbTestSuite.modificationDate, dbTestSuite.description,
			dbTestSuite.keywords, dbTestSuite.domain, dbTestSuite.filename, dbTestSuite.hasDocumentation, dbTestSuite.shared, dbTestSuite.identifier, dbTestSuite.definitionPath)
	}

	private def tupleToTestSuite(x: TestSuiteValueTuple): TestSuites = {
		TestSuites(x._1, x._2, x._3, x._4, x._5, x._6, x._7, x._8, x._9, x._11, x._12, None, x._14, hidden = false, x._13, x._10, x._15)
	}

}

@Singleton
class TestSuiteManager @Inject() (testResultManager: TestResultManager, actorManager: ActorManager, conformanceManager: ConformanceManager, endPointManager: EndPointManager, testCaseManager: TestCaseManager, parameterManager: ParameterManager, repositoryUtils: RepositoryUtils, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

	import dbConfig.profile.api._

	private final val logger: Logger = LoggerFactory.getLogger("TestSuiteManager")

	def getById(testSuiteId: Long): Option[TestSuites] = {
		exec(PersistenceSchema.testSuites.filter(_.id === testSuiteId).map(TestSuiteManager.withoutDocumentation).result.headOption).map(TestSuiteManager.tupleToTestSuite)
	}

	def getTestSuiteInfoByApiKeys(communityApiKey: String, specificationApiKey: Option[String], testSuiteIdentifier: String): Option[(Long, Long, Boolean)] = { // Test suite ID, domain ID and shared flag
		exec(for {
			communityDomainId <- {
				PersistenceSchema.communities.filter(_.apiKey === communityApiKey).map(_.domain).result.headOption
			}
			relevantDomainId <- {
				if (communityDomainId.isDefined) {
					DBIO.successful(Some(communityDomainId.get))
				} else {
					DBIO.successful(None)
				}
			}
			specificationId <- {
				if (relevantDomainId.isDefined && specificationApiKey.isDefined) {
					PersistenceSchema.specifications
						.filter(_.apiKey === specificationApiKey.get)
						.filter(_.domain === relevantDomainId.get)
						.map(_.id).result.headOption
				} else {
					DBIO.successful(None)
				}
			}
			testSuiteInfo <- {
				if (specificationId.isDefined) {
					// Non-shared test suite.
					PersistenceSchema.testSuites
						.join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
						.filter(_._2.specId === specificationId.get)
						.filter(_._1.identifier === testSuiteIdentifier)
						.map(x => (x._1.id, x._1.domain, x._1.shared))
						.result.headOption
				} else if (specificationApiKey.isEmpty && relevantDomainId.isDefined) {
					// This must be a shared test suite.
					PersistenceSchema.testSuites
						.filter(_.identifier === testSuiteIdentifier)
						.filter(_.domain === relevantDomainId.get)
						.filter(_.shared)
						.map(x => (x.id, x.domain, x.shared))
						.result.headOption
				} else {
					DBIO.successful(None)
				}
			}
		} yield testSuiteInfo)
	}

	def getSharedTestSuitedWithDomainId(domain: Long): List[TestSuites] = {
		exec(PersistenceSchema.testSuites
			.filter(_.domain === domain)
			.filter(_.shared === true)
			.sortBy(_.shortname.asc)
			.map(TestSuiteManager.withoutDocumentation)
			.result.map(_.toList)
		).map(TestSuiteManager.tupleToTestSuite)
	}

	def getTestSuitesWithSpecificationId(specification: Long): List[TestSuites] = {
		exec(PersistenceSchema.testSuites
			.join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
			.filter(_._2.specId === specification)
			.sortBy(_._1.shortname.asc)
			.map(x => TestSuiteManager.withoutDocumentation(x._1))
			.result.map(_.toList))
			.map(TestSuiteManager.tupleToTestSuite)
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

	def getTestSuiteOfTestCaseInternal(testCaseId: Long): DBIO[TestSuites] = {
		PersistenceSchema.testSuites
			.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testsuite)
			.filter(_._2.testcase === testCaseId)
			.map(x => TestSuiteManager.withoutDocumentation(x._1))
			.result
			.head
			.map(TestSuiteManager.tupleToTestSuite)
	}

	def getTestSuiteOfTestCase(testCaseId: Long): TestSuites = {
		exec(getTestSuiteOfTestCaseInternal(testCaseId))
	}

	def getTestSuiteWithTestCases(testSuiteId: Long): TestSuite = {
		exec(getTestSuiteWithTestCasesInternal(testSuiteId))
	}

	private def getTestSuiteWithTestCasesInternal(testSuiteId: Long): DBIO[TestSuite] = {
		for {
			testSuite <- PersistenceSchema.testSuites
				.filter(_.id === testSuiteId)
				.result.head
			testCases <- PersistenceSchema.testCases
				.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
				.filter(_._2.testsuite === testSuiteId)
				.sortBy(_._1.shortname.asc)
				.map(x => TestCaseManager.withoutDocumentation(x._1))
				.result.map(_.toList)
			result <- {
				DBIO.successful(new TestSuite(testSuite, testCases.map(TestCaseManager.tupleToTestCase)))
			}
		} yield result
	}

	def getSpecificationById(specId: Long): Specifications = {
		val spec = exec(PersistenceSchema.specifications.filter(_.id === specId).result.head)
		spec
	}

	def cancelPendingTestSuiteActions(pendingTestSuiteIdentifier: String, domainId: Long, sharedTestSuite: Boolean): TestSuiteUploadResult = {
		applyPendingTestSuiteActions(pendingTestSuiteIdentifier, TestSuiteReplacementChoice.CANCEL, domainId, sharedTestSuite, List.empty)
	}

	def completePendingTestSuiteActions(pendingTestSuiteIdentifier: String, domainId: Long, sharedTestSuite: Boolean, actions: List[TestSuiteDeploymentAction]): TestSuiteUploadResult = {
		applyPendingTestSuiteActions(pendingTestSuiteIdentifier, TestSuiteReplacementChoice.PROCEED, domainId, sharedTestSuite, actions)
	}

	private def processTestSuite(suite: TestSuite, testSuiteActors: Option[List[Actor]], testCases: Option[List[TestCases]], tempTestSuiteArchive: Option[File], actions: List[TestSuiteDeploymentAction], onSuccessCalls: mutable.ListBuffer[() => _], onFailureCalls: mutable.ListBuffer[() => _]): DBIO[List[TestSuiteUploadItemResult]] = {
		for {
			// Save the shared test suite if needed.
			savedSharedTestSuite <- {
				val overallAction = actions.find(_.specification.isEmpty)
				if (overallAction.isDefined && overallAction.get.sharedTestSuite) {
					// Clean-up operation in case of error.
					onFailureCalls += (() => {
						val testSuiteFolder = repositoryUtils.getTestSuitePath(suite.domain, suite.filename)
						if (testSuiteFolder.exists()) {
							FileUtils.deleteDirectory(testSuiteFolder)
						}
					})
					for {
						existingTestSuiteInfo <- getSharedTestSuiteInfoByDomainAndIdentifier(suite.domain, suite.identifier)
						testSuitePathsToUse <- DBIO.successful(createTestSuitePaths(suite.domain, suite.filename, tempTestSuiteArchive.get))
						sharedTestSuiteResult <- {
							val existingTestSuiteId = if (existingTestSuiteInfo.isDefined) {
								Some(existingTestSuiteInfo.get._1)
							} else {
								None
							}
							suite.definitionPath = testSuitePathsToUse.testSuiteDefinitionPath
							stepSaveTestSuiteAndTestCases(suite.toCaseObject, existingTestSuiteId, testCases, testSuitePathsToUse, actions.find(_.specification.isEmpty).get)
						}
						_ <- {
							onSuccessCalls += (() => {
								if (existingTestSuiteInfo.isDefined) {
									val existingFolder = repositoryUtils.getTestSuitePath(suite.domain, existingTestSuiteInfo.get._2)
									if (existingFolder.exists()) {
										FileUtils.deleteDirectory(existingFolder)
									}
								}
							})
							DBIO.successful(())
						}
					} yield Some(sharedTestSuiteResult)
				} else {
					DBIO.successful(None)
				}
			}
			actionsToConsider <- if (savedSharedTestSuite.isEmpty) {
				DBIO.successful(actions)
			} else {
				// For a shared test suite locate all currently linked actors to make sure their conformance statements are updated.
				for {
					linkedSpecifications <- PersistenceSchema.specificationHasTestSuites
						.filter(_.testSuiteId === savedSharedTestSuite.get.testSuite.id)
						.map(_.specId)
						.result
					linkedSpecificationActions <- {
						// Convert list to map for faster lookups.
						val actionMap = new mutable.HashMap[Long, TestSuiteDeploymentAction]()
						actions.filter(_.specification.isDefined).foreach { action =>
							actionMap += (action.specification.get -> action)
						}
						val actionsToUse = new ListBuffer[TestSuiteDeploymentAction]()
						linkedSpecifications.foreach { linkedSpecification =>
							val action = actionMap.getOrElse(linkedSpecification, new TestSuiteDeploymentAction(
								Some(linkedSpecification),
								TestSuiteReplacementChoice.PROCEED,
								false,
								Some(false),
								true,
								None
							))
							actionsToUse += action
						}
						DBIO.successful(actionsToUse.toList)
					}
				} yield linkedSpecificationActions
			}
			// Apply specification-related updates.
			results <- saveTestSuiteForSpecifications(savedSharedTestSuite, actionsToConsider, suite, testSuiteActors, testCases, tempTestSuiteArchive, onSuccessCalls, onFailureCalls)
		} yield results
	}

	private def saveTestSuiteForSpecifications(savedSharedTestSuite: Option[TestSuiteSaveResult], specActions: List[TestSuiteDeploymentAction], suite: TestSuite, testSuiteActors: Option[List[Actor]], testCases: Option[List[TestCases]], tempTestSuiteArchive: Option[File], onSuccessCalls: mutable.ListBuffer[() => _], onFailureCalls: mutable.ListBuffer[() => _]): DBIO[List[TestSuiteUploadItemResult]] = {
		for {
			results <- {
				val dbActions = ListBuffer[DBIO[List[TestSuiteUploadItemResult]]]()
				specActions.filter(_.specification.isDefined).foreach { specAction =>
					if (specAction.action == TestSuiteReplacementChoice.PROCEED) {
						dbActions += (for {
							existingTestSuite <- if (savedSharedTestSuite.isDefined) {
								DBIO.successful(None)
							} else {
								getTestSuiteBySpecificationAndIdentifier(specAction.specification.get, suite.identifier)
							}
							result <- saveTestSuite(savedSharedTestSuite, specAction.specification.get, suite.toCaseObject, existingTestSuite, specAction, testSuiteActors, testCases, tempTestSuiteArchive, onSuccessCalls, onFailureCalls)
						} yield result._2)
					}
				}
				mergeActionsWithResults(dbActions.toList)
			}
		} yield results
	}

	private def applyPendingTestSuiteActions(pendingTestSuiteIdentifier: String, overallAction: TestSuiteReplacementChoice, domainId: Long, sharedTestSuite: Boolean, actions: List[TestSuiteDeploymentAction]): TestSuiteUploadResult = {
		val result = new TestSuiteUploadResult()
		val pendingTestSuiteFolder = new File(repositoryUtils.getPendingFolder(), pendingTestSuiteIdentifier)
		try {
			if (overallAction == TestSuiteReplacementChoice.PROCEED) {
				if (pendingTestSuiteFolder.exists()) {
					val pendingFiles = pendingTestSuiteFolder.listFiles()
					if (pendingFiles.length == 1) {
						val testSuite = repositoryUtils.getTestSuiteFromZip(domainId, None, pendingFiles.head)
						// Sanity check
						if (testSuite.isDefined && testSuite.get.testCases.isDefined) {
							testSuite.get.shared = sharedTestSuite
							val onSuccessCalls = mutable.ListBuffer[() => _]()
							val onFailureCalls = mutable.ListBuffer[() => _]()
							val dbAction = processTestSuite(testSuite.get, testSuite.get.actors, testSuite.get.testCases, Some(pendingFiles.head), actions, onSuccessCalls, onFailureCalls)
							import scala.jdk.CollectionConverters._
							result.items.addAll(exec(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), dbAction).transactionally).asJavaCollection)
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

	private def validateTestSuite(domain: Option[Long], specifications: Option[List[Long]], tempTestSuiteArchive: File): TAR = {
		// Domain parameters.
		val domainId = if (domain.isDefined) {
			domain.get
		} else if (specifications.isDefined && specifications.get.nonEmpty) {
			exec(getDomainIdForSpecification(specifications.get.head))
		} else {
			throw new IllegalArgumentException("Either the domain ID or a specification ID must be provided")
		}
		val parameterSet = conformanceManager.getDomainParameters(domainId).map(p => p.name).toSet
		// Actor IDs (if multiple specs these are ones in common).
		var actorIdSet: Option[mutable.Set[String]] = None
		if (specifications.isDefined) {
			conformanceManager.getActorIdsOfSpecifications(specifications.get).values.foreach { specActorIds =>
				if (actorIdSet.isEmpty) {
					actorIdSet = Some(mutable.Set[String]())
					actorIdSet.get ++= specActorIds
				} else {
					actorIdSet = Some(actorIdSet.get & specActorIds)
				}
			}
		}
		import scala.jdk.CollectionConverters._
		val report = TestSuiteValidationAdapter.getInstance().doValidation(new FileSource(tempTestSuiteArchive), actorIdSet.getOrElse(mutable.Set[String]()).toSet.asJava, parameterSet.asJava, repositoryUtils.getTmpValidationFolder().getAbsolutePath)
		report
	}

	private def testSuiteDefinesExistingActors(specification: Long, actorIdentifiers: List[String]): DBIO[Int] = {
		PersistenceSchema.actors
		  .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
		  .filter(_._2.specId === specification)
		  .filter(_._1.actorId inSet actorIdentifiers)
			.size
			.result
	}

	def deployTestSuiteFromApi(domainId: Long, specificationId: Option[Long], deployRequest: TestSuiteDeployRequest, tempTestSuiteArchive: File): TestSuiteUploadResult = {
		val result = new TestSuiteUploadResult()
		val specificationIds = if (specificationId.isDefined) {
			Some(List(specificationId.get))
		} else {
			None
		}
		result.validationReport = validateTestSuite(Some(domainId), specificationIds, tempTestSuiteArchive)
		val hasErrors = result.validationReport.getCounters.getNrOfErrors.longValue() > 0
		val hasWarnings = result.validationReport.getCounters.getNrOfWarnings.longValue() > 0
		if (!hasErrors && (!hasWarnings || deployRequest.ignoreWarnings)) {
			// Go ahead
			val testSuite = repositoryUtils.getTestSuiteFromZip(domainId, specificationId, tempTestSuiteArchive, completeParse = true)
			if (testSuite.isDefined) {
				// Apply update approach settings based on input parameters and the metadata from the test suite archive.
				val deployRequestToUse = applyUpdateMetadataToDeployRequest(testSuite.get, deployRequest)
				testSuite.get.shared = deployRequestToUse.sharedTestSuite
				if (!deployRequestToUse.sharedTestSuite) {
					// Check to see if we have an update case that must be skipped.
					val sharedTestSuiteExists = exec(checkTestSuiteExists(List(specificationId.get), testSuite.get, Some(true))).nonEmpty
					if (sharedTestSuiteExists) {
						result.existsForSpecs = Some(List((specificationId.get, true)))
						result.needsConfirmation = true
					}
				}
				if (!result.needsConfirmation) {
					val onSuccessCalls = mutable.ListBuffer[() => _]()
					val onFailureCalls = mutable.ListBuffer[() => _]()
					val testCaseChoices = if (testSuite.get.testCases.isDefined) {
						Some(testSuite.get.testCases.get.map { testCase =>
							// If we have specific settings for the test case use them. Otherwise consider the test suite overall defaults.
							deployRequestToUse.testCaseUpdates.getOrElse(testCase.identifier, new TestCaseDeploymentAction(testCase.identifier, deployRequestToUse.updateSpecification, deployRequestToUse.replaceTestHistory))
						})
					} else {
						None
					}
					val updateSpecification = deployRequestToUse.updateSpecification.getOrElse(false)
					val updateActions = List(new TestSuiteDeploymentAction(specificationId, TestSuiteReplacementChoice.PROCEED, updateTestSuite = updateSpecification, updateActors = Some(updateSpecification), deployRequestToUse.sharedTestSuite, testCaseUpdates = testCaseChoices))
					val action = processTestSuite(testSuite.get, testSuite.get.actors, testSuite.get.testCases, Some(tempTestSuiteArchive), updateActions, onSuccessCalls, onFailureCalls)
					import scala.jdk.CollectionConverters._
					result.items.addAll(exec(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), action).transactionally).asJavaCollection)
					result.success = true
				}
			} else {
				throw new IllegalArgumentException("Test suite could not be read from archive")
			}
		} else {
			result.needsConfirmation = true
		}
		result
	}

	private def applyUpdateMetadataToDeployRequest(testSuiteDefinition: TestSuite, deployRequest: TestSuiteDeployRequest): TestSuiteDeployRequest = {
		// Test suite settings.
		val settingsToUse = if (deployRequest.updateSpecification.isEmpty || deployRequest.replaceTestHistory.isEmpty) {
			val updateSpecification = if (deployRequest.updateSpecification.isDefined) {
				deployRequest.updateSpecification.get
			} else {
				testSuiteDefinition.updateApproach.exists(_.isUpdateMetadata)
			}
			val resetTestHistory = if (deployRequest.replaceTestHistory.isDefined) {
				deployRequest.replaceTestHistory.get
			} else {
				testSuiteDefinition.updateApproach.exists(_.isResetTestHistory)
			}
			deployRequest.withTestSuiteUpdateMetadata(resetTestHistory, updateSpecification)
		} else {
			deployRequest
		}
		// Test case settings.
		if (testSuiteDefinition.testCaseUpdateApproach.isDefined) {
			settingsToUse.testCaseUpdates.foreach { testCaseInfo =>
				if (testCaseInfo._2.resetTestHistory.isEmpty) {
					testCaseInfo._2.resetTestHistory = Some(testSuiteDefinition.testCaseUpdateApproach.get.get(testCaseInfo._1).exists(_.isResetTestHistory))
				}
				if (testCaseInfo._2.updateDefinition.isEmpty) {
					testCaseInfo._2.updateDefinition = Some(testSuiteDefinition.testCaseUpdateApproach.get.get(testCaseInfo._1).exists(_.isUpdateMetadata))
				}
			}
		}
		settingsToUse
	}

	def deployTestSuiteFromZipFile(domainId: Long, specifications: Option[List[Long]], sharedTestSuite: Boolean, tempTestSuiteArchive: File): TestSuiteUploadResult = {
		val result = new TestSuiteUploadResult()
		try {
			result.validationReport = validateTestSuite(Some(domainId), specifications, tempTestSuiteArchive)
			if (result.validationReport.getResult == TestResultType.SUCCESS) {
				val hasErrors = result.validationReport.getCounters.getNrOfErrors.intValue() > 0
				val hasReportItems = hasErrors ||
					(result.validationReport.getCounters.getNrOfWarnings.intValue() > 0) ||
					(result.validationReport.getCounters.getNrOfAssertions.intValue() > 0)
				// If we have messages we don't need to make a full parse (i.e. test cases and documentation).
				val specificationIdToUse = specifications.getOrElse(List()).headOption
				val testSuite = repositoryUtils.getTestSuiteFromZip(domainId, specificationIdToUse, tempTestSuiteArchive, !hasErrors)
				if (testSuite.isDefined) {
					result.updateMetadata = testSuite.get.updateApproach.exists(_.isUpdateMetadata)
					result.updateSpecification = testSuite.get.updateApproach.exists(_.isUpdateSpecification)
					var definedActorsIdentifiers: Option[List[String]] = None
					if (testSuite.get.actors.isDefined) {
						definedActorsIdentifiers = Some(testSuite.get.actors.get.map(actor => actor.actorId))
					}
					if (hasReportItems) {
						result.needsConfirmation = true
					}
					if (sharedTestSuite) {
						// Lookup a matching test suite and its test cases.
						val lookupResult = exec(
							for {
								testSuiteId <- PersistenceSchema.testSuites
									.filter(_.identifier === testSuite.get.identifier)
									.filter(_.domain === domainId)
									.filter(_.shared === true)
									.map(_.id)
									.result
									.headOption
								existingTestCases <- if (testSuiteId.isDefined) {
									getExistingTestCasesForTestSuite(testSuiteId.get)
								} else {
									DBIO.successful(List.empty)
								}
								existingSpecifications <- if (testSuiteId.isDefined) {
									PersistenceSchema.specificationHasTestSuites
										.filter(_.testSuiteId === testSuiteId.get)
										.map(_.specId)
										.result
										.map(_.toList)
								} else {
									DBIO.successful(List.empty)
								}
							} yield (testSuiteId, existingTestCases, existingSpecifications)
						)
						result.sharedTestSuiteId = lookupResult._1
						result.sharedTestCases = Some(getTestCaseUploadStatus(testSuite.get, lookupResult._2))
						if (result.sharedTestSuiteId.isDefined) {
							result.needsConfirmation = true
						}
						val linkedSpecifications = new ListBuffer[(Long, Boolean)]
						lookupResult._3.foreach { specId =>
							linkedSpecifications += ((specId, true))
						}
						result.existsForSpecs = Some(linkedSpecifications.toList)
					} else {
						// Check for existing specification data, test suites and test cases.
						val existingTestSuites = exec(checkTestSuiteExists(specifications.get, testSuite.get, None))
						val specsWithExistingTestSuite = new ListBuffer[(Long, Boolean)]()
						val specsWithMatchingData = new ListBuffer[Long]()
						val specTestCases = new mutable.HashMap[Long, List[TestSuiteUploadTestCase]]()
						if (specifications.isDefined) {
							specifications.get.foreach { specification =>
								val existingTestSuiteInfo = existingTestSuites.get(specification)
								val checkTestCases = existingTestSuiteInfo.isDefined && !existingTestSuiteInfo.get._2
								val testSuiteCheck = exec(for {
									existingTestCases <- {
										if (checkTestCases) {
											// We match an existing test suite (that is not a shared one).
											getExistingTestCasesForTestSuite(existingTestSuiteInfo.get._1)
										} else {
											// Don't query per specification the test cases for a shared test suite.
											DBIO.successful(List.empty)
										}
									}
									existingActors <- {
										if (definedActorsIdentifiers.isDefined) {
											testSuiteDefinesExistingActors(specification, definedActorsIdentifiers.get)
										} else {
											DBIO.successful(0)
										}
									}
								} yield (existingTestCases, existingActors))
								if (existingTestSuiteInfo.isDefined) {
									// The archive's test suite exists in the DB
									specsWithExistingTestSuite += ((specification, existingTestSuiteInfo.get._2))
									result.needsConfirmation = true
									// Record the information on the included test cases
									if (checkTestCases) {
										specTestCases += (specification -> getTestCaseUploadStatus(testSuite.get, testSuiteCheck._1))
									}
								}
								if (testSuiteCheck._2 > 0) {
									// The archive's test suite defines actors existing in the DB
									specsWithMatchingData += specification
									result.needsConfirmation = true
								}
							}
							if (result.needsConfirmation) {
								if (specsWithExistingTestSuite.nonEmpty) {
									result.existsForSpecs = Some(specsWithExistingTestSuite.toList)
									if (specTestCases.nonEmpty) {
										result.testCases = Some(specTestCases.toMap)
									}
								}
								if (specsWithMatchingData.nonEmpty) {
									result.matchingDataExists = Some(specsWithMatchingData.toList)
								}
							}
						}
					}
					if (result.needsConfirmation) {
						// Park the test suite for now and ask user what to do
						FileUtils.moveDirectoryToDirectory(tempTestSuiteArchive.getParentFile, repositoryUtils.getPendingFolder(), true)
						result.pendingTestSuiteFolderName = tempTestSuiteArchive.getParentFile.getName
					} else {
						// Proceed immediately.
						val onSuccessCalls = mutable.ListBuffer[() => _]()
						val onFailureCalls = mutable.ListBuffer[() => _]()
						testSuite.get.domain = domainId
						testSuite.get.shared = sharedTestSuite
						val actions = if (sharedTestSuite) {
							List(new TestSuiteDeploymentAction(None, PROCEED, updateTestSuite = true, updateActors = None, sharedTestSuite = true, testCaseUpdates = None))
						} else if (specifications.isDefined && specifications.nonEmpty) {
							specifications.get.map { spec =>
								new TestSuiteDeploymentAction(Some(spec), PROCEED, updateTestSuite = true, updateActors = Some(true), sharedTestSuite = false, testCaseUpdates = None)
							}
						} else {
							throw new IllegalArgumentException("Test suite not shared and no specifications provided")
						}
						val dbAction = processTestSuite(testSuite.get, testSuite.get.actors, testSuite.get.testCases, Some(tempTestSuiteArchive), actions, onSuccessCalls, onFailureCalls)
						import scala.jdk.CollectionConverters._
						result.items.addAll(exec(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), dbAction).transactionally).asJavaCollection)
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

	private def getTestCaseUploadStatus(testSuiteDefinition: TestSuite, testCasesInDb: List[(Long, String, String)]): List[TestSuiteUploadTestCase] = {
		// (Test case ID, test case identifier, test case name)
		val testCasesInDB = new mutable.HashMap[String, String]() // Map of identifiers to names
		testCasesInDb.foreach { testCase =>
			testCasesInDB += (testCase._2 -> testCase._3)
		}
		val testCases = new ListBuffer[TestSuiteUploadTestCase]()
		if (testSuiteDefinition.testCases.isDefined) {
			testSuiteDefinition.testCases.get.foreach { archiveTestCase =>
				val matchedTestCaseInDB = testCasesInDB.remove(archiveTestCase.identifier)
				if (matchedTestCaseInDB.isDefined) {
					val updateMetadata = testSuiteDefinition.testCaseUpdateApproach.isDefined && testSuiteDefinition.testCaseUpdateApproach.get.get(archiveTestCase.identifier).exists(_.isUpdateMetadata)
					val resetTestHistory = testSuiteDefinition.testCaseUpdateApproach.isDefined && testSuiteDefinition.testCaseUpdateApproach.get.get(archiveTestCase.identifier).exists(_.isResetTestHistory)
					testCases += new TestSuiteUploadTestCase(archiveTestCase.identifier, matchedTestCaseInDB.get, TestCaseUploadMatchType.IN_ARCHIVE_AND_DB, updateMetadata, resetTestHistory)
				} else {
					testCases += new TestSuiteUploadTestCase(archiveTestCase.identifier, archiveTestCase.fullname, TestCaseUploadMatchType.IN_ARCHIVE_ONLY)
				}
			}
			// Remaining ones are only in the DB
			testCasesInDB.foreach { dbTestCase =>
				testCases += new TestSuiteUploadTestCase(dbTestCase._1, dbTestCase._2, TestCaseUploadMatchType.IN_DB_ONLY)
			}
		}
		testCases.toList
	}

	private def getDomainIdForSpecification(specification: Long): DBIO[Long] = {
		PersistenceSchema.specifications.filter(_.id === specification).map(_.domain).result.head
	}

	private def checkTestSuiteExists(specificationIds: List[Long], suite: TestSuite, shared: Option[Boolean]): DBIO[Map[Long, (Long, Boolean)]] = { // Specification ID to test suite ID and shared flag
		for {
			testSuiteInfo <- PersistenceSchema.testSuites
				.join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
				.filter(_._1.identifier === suite.identifier)
				.filter(_._2.specId inSet specificationIds)
				.filterOpt(shared)((q, isShared) => q._1.shared === isShared)
				.map(x => (x._2.specId, x._2.testSuiteId, x._1.shared))
				.result
			results <- {
				val map = new mutable.HashMap[Long, (Long, Boolean)]()
				testSuiteInfo.foreach { info =>
					map += (info._1 -> (info._2, info._3))
				}
				DBIO.successful(map.toMap)
			}
		} yield results
	}

	private def getSharedTestSuiteInfoByDomainAndIdentifier(domainId: Long, identifier: String): DBIO[Option[(Long, String)]] = {
		PersistenceSchema.testSuites
			.filter(_.identifier === identifier)
			.filter(_.domain === domainId)
			.filter(_.shared === true)
			.map(x => (x.id, x.filename))
			.result
			.headOption
	}

	private def getTestSuiteBySpecificationAndIdentifier(specId: Long, identifier: String): DBIO[Option[TestSuites]] = {
		for {
			testSuite <- PersistenceSchema.testSuites
				.join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
				.filter(_._1.identifier === identifier)
				.filter(_._2.specId === specId)
				.map(x => TestSuiteManager.withoutDocumentation(x._1))
				.result
				.headOption
				.map(x => if (x.isDefined) {
					Some(TestSuiteManager.tupleToTestSuite(x.get))
				} else {
					None
				})
		} yield testSuite
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

	private def updateTestSuiteInDbWithoutMetadata(testSuiteId: Long, newData: TestSuites): DBIO[_] = {
		val q1 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.filename, t.hidden)
		q1.update(newData.filename, newData.hidden)
	}

	private def updateTestSuiteInDb(testSuiteId: Long, newData: TestSuites): DBIO[_] = {
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
				&& Objects.equals(one.testKey, two.testKey)
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

	private def isActorReference(actorToSave: Actors): Boolean = {
		actorToSave.actorId != null && actorToSave.name == null && actorToSave.description.isEmpty
	}

	def stepSaveTestSuiteAndTestCases(suite: TestSuites, existingSuiteId: Option[Long], testCases: Option[List[TestCases]], testSuitePaths: TestSuitePaths, updateActions: TestSuiteDeploymentAction): DBIO[TestSuiteSaveResult] = {
		for {
			// Save the test suite (insert or update) and return the identifier to use.
			testSuiteId <- stepSaveTestSuite(suite, existingSuiteId, updateActions)
			// Save the test cases.
			stepSaveTestCases <- stepSaveTestCases(testSuiteId, existingSuiteId.isDefined, testCases, testSuitePaths.testCasePaths, updateActions)
		} yield TestSuiteSaveResult(suite.withId(testSuiteId), stepSaveTestCases._1, stepSaveTestCases._2, testSuitePaths)
	}

	private def stepSaveTestSuite(suite: TestSuites, existingSuiteId: Option[Long], updateActions: TestSuiteDeploymentAction): DBIO[Long] = {
		for {
			// Test suite update.
			testSuiteId <- if (existingSuiteId.isDefined) {
				// Update existing test suite.
				(if (updateActions.updateTestSuite) {
					updateTestSuiteInDb(existingSuiteId.get, suite)
				} else {
					updateTestSuiteInDbWithoutMetadata(existingSuiteId.get, suite)
				}) andThen DBIO.successful(existingSuiteId.get)
			} else {
				PersistenceSchema.testSuites.returning(PersistenceSchema.testSuites.map(_.id)) += suite
			}
		} yield testSuiteId
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

	private def stepSaveActors(specificationId: Long, testSuiteActors: Option[List[Actor]], domainId: Long, updateActions: TestSuiteDeploymentAction, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[(util.Map[String, Long], List[TestSuiteUploadItemResult])] = {
		for {
			// Lookup the existing actors for the specification in question.
			specificationActors <- PersistenceSchema.actors
				.join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
				.filter(_._2.specId === specificationId)
				.map(_._1)
				.result
				.map(_.toList)
			results <- {
				val actions = new ListBuffer[DBIO[List[TestSuiteUploadItemResult]]]()
				val savedActorIds: util.Map[String, Long] = new util.HashMap[String, Long]
				for (testSuiteActor <- testSuiteActors.get) {
					val result = new ListBuffer[TestSuiteUploadItemResult]()
					var updateAction: Option[DBIO[_]] = None
					var savedActorId: DBIO[Long] = null
					val actorToSave = testSuiteActor.toCaseObject(CryptoUtil.generateApiKey(), domainId)
					val existingActor = lookupActor(actorToSave, specificationActors)
					var savedActorStringId: String = null
					if (existingActor.isDefined) {
						if (!updateActions.updateActors.get || isActorReference(actorToSave) || theSameActor(existingActor.get, actorToSave)) {
							result += new TestSuiteUploadItemResult(existingActor.get.name, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED, specificationId)
						} else {
							updateAction = Some(actorManager.updateActor(existingActor.get.id, actorToSave.actorId, actorToSave.name, actorToSave.description, actorToSave.default, actorToSave.hidden, actorToSave.displayOrder, specificationId, None, checkApiKeyUniqueness = false))
							result += new TestSuiteUploadItemResult(existingActor.get.name, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE, specificationId)
						}
						savedActorId = DBIO.successful(existingActor.get.id)
						savedActorStringId = existingActor.get.actorId
					} else {
						if (isActorReference(actorToSave)) {
							throw new IllegalStateException("Actor reference [" + actorToSave.actorId + "] not found in specification")
						} else {
							// New actor.
							savedActorId = conformanceManager.createActor(actorToSave, specificationId)
							savedActorStringId = actorToSave.actorId
						}
						result += new TestSuiteUploadItemResult(actorToSave.actorId, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_ADD, specificationId)
					}
					val combinedAction = (updateAction.getOrElse(DBIO.successful(())) andThen savedActorId).flatMap(id => {
						savedActorIds.put(savedActorStringId, id)
						for {
							existingEndpoints <- PersistenceSchema.endpoints.filter(_.actor === id).result.map(_.toList)
							endpointResults <- stepSaveEndpoints(specificationId, id, testSuiteActor, actorToSave, existingEndpoints, updateActions, onSuccessCalls)
						} yield endpointResults
					})
					actions += mergeActionsWithResults(combinedAction, DBIO.successful(result.toList))
				}
				// Group together all actions from the for loop and aggregate their results
				val finalAction = {
					if (actions.nonEmpty) {
						mergeActionsWithResults(actions.toList)
					} else {
						DBIO.successful(List[TestSuiteUploadItemResult]())
					}
				}
				// Get the (new, existing or referenced) actor IDs resulting from the import.
				finalAction.flatMap(collectedResults => {
					DBIO.successful((savedActorIds, collectedResults))
				})
			}
		} yield results
	}

	private def stepSaveEndpoints(specificationId: Long, savedActorId: Long, testSuiteActor: Actor, actorToSave: Actors, existingEndpoints: List[Endpoints], updateActions: TestSuiteDeploymentAction, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[List[TestSuiteUploadItemResult]] = {
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
					if (!updateActions.updateActors.get || theSameEndpoint(endpoint, existingEndpoint)) {
						result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]", TestSuiteUploadItemResult.ITEM_TYPE_ENDPOINT, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED, specificationId)
					} else {
						updateAction = Some(endPointManager.updateEndPoint(existingEndpoint.id, endpoint.name, endpoint.desc))
						result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]", TestSuiteUploadItemResult.ITEM_TYPE_ENDPOINT, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE, specificationId)
					}
				} else {
					// New endpoint.
					endpointId = endPointManager.createEndpoint(endpoint.toCaseObject.copy(actor = savedActorId))
					result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]", TestSuiteUploadItemResult.ITEM_TYPE_ENDPOINT, TestSuiteUploadItemResult.ACTION_TYPE_ADD, specificationId)
				}
				val combinedAction = (updateAction.getOrElse(DBIO.successful(())) andThen endpointId).flatMap(id => {
					for {
						existingEndpointParameters <- PersistenceSchema.parameters.filter(_.endpoint === id.longValue()).result.map(_.toList)
						parameterResults <- stepSaveParameters(specificationId, id, endpoint, actorToSave, existingEndpointParameters, updateActions, onSuccessCalls)
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

	private def stepSaveParameters(specificationId: Long, endpointId: Long, endpoint: Endpoint, actorToSave: Actors, existingEndpointParameters: List[models.Parameters], updateActions: TestSuiteDeploymentAction, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[List[TestSuiteUploadItemResult]] = {
		val actions = new ListBuffer[DBIO[_]]()
		val result = new ListBuffer[TestSuiteUploadItemResult]()
		val existingParameterMap = new util.HashMap[String, models.Parameters]()
		for (existingParameter <- existingEndpointParameters) {
			existingParameterMap.put(existingParameter.testKey, existingParameter)
		}
		if (endpoint.parameters.isDefined) {
			val parameters = endpoint.parameters.get
			parameters.foreach { parameter =>
				var action: Option[DBIO[_]] = None
				val existingParameter = existingParameterMap.get(parameter.testKey)
				var parameterId: Long = -1
				if (existingParameter != null) {
					// Existing parameter.
					parameterId = existingParameter.id
					existingParameterMap.remove(parameter.testKey)
					if (!updateActions.updateActors.get || theSameParameter(parameter, existingParameter)) {
						result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]."+parameter.testKey, TestSuiteUploadItemResult.ITEM_TYPE_PARAMETER, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED, specificationId)
					} else {
						action = Some(parameterManager.updateParameter(parameterId, parameter.name, parameter.testKey, parameter.desc, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue, parameter.defaultValue, onSuccessCalls))
						result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]."+parameter.testKey, TestSuiteUploadItemResult.ITEM_TYPE_PARAMETER, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE, specificationId)
					}
				} else {
					// New parameter.
					action = Some(parameterManager.createParameter(parameter.withEndpoint(endpointId, Some(0))))
					result += new TestSuiteUploadItemResult(actorToSave.actorId+"["+endpoint.name+"]."+parameter.testKey, TestSuiteUploadItemResult.ITEM_TYPE_PARAMETER, TestSuiteUploadItemResult.ACTION_TYPE_ADD, specificationId)
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

	private def stepSaveTestCases(savedTestSuiteId: Long, testSuiteExists: Boolean, testCases: Option[List[TestCases]], resourcePaths: Map[String, String], updateActions: TestSuiteDeploymentAction): DBIO[(Map[String, (Long, Boolean)], List[String])] = {
		for {
			// Lookup the existing test cases for the test suite (if it already exists).
			existingTestCasesForTestSuite <- if (testSuiteExists) {
				getExistingTestCasesForTestSuite(savedTestSuiteId)
			} else {
				DBIO.successful(List[(Long, String, String)]())
			}
			// Place the existing test cases in a map for further processing.
			existingTestCaseMap <- getExistingTestCaseMap(existingTestCasesForTestSuite)
			// Insert or update the test cases.
			testCaseIds <- {
				var combinedAction: DBIO[mutable.HashMap[String, (Long, Boolean)]] = DBIO.successful(mutable.HashMap.empty) // Test case identifier to (Test case ID and isNew flag)
				for (testCase <- testCases.get) {
					val testCaseToStore = testCase.withPath(resourcePaths(testCase.identifier))
					val existingTestCaseInfo = existingTestCaseMap.get(testCase.identifier)
					if (existingTestCaseInfo.isDefined) {
						val existingTestCaseId: Long = existingTestCaseInfo.get._1
						// Test case already exists - update.
						existingTestCaseMap.remove(testCase.identifier)
						// Update test case metadata
						if (updateActions.updateTestCaseMetadata(testCase.identifier)) {
							combinedAction = combinedAction.flatMap { savedTestCaseIds =>
								testCaseManager.updateTestCase(
									existingTestCaseId, testCaseToStore.identifier, testCaseToStore.shortname, testCaseToStore.fullname,
									testCaseToStore.version, testCaseToStore.authors, testCaseToStore.description,
									testCaseToStore.keywords, testCaseToStore.testCaseType, testCaseToStore.path, testCaseToStore.testSuiteOrder,
									testCaseToStore.targetActors.get, testCaseToStore.documentation.isDefined, testCaseToStore.documentation,
									testCaseToStore.isOptional, testCaseToStore.isDisabled
								) andThen DBIO.successful(savedTestCaseIds += (testCaseToStore.identifier -> (existingTestCaseId, false)))
							}
						} else {
							combinedAction = combinedAction.flatMap { savedTestCaseIds =>
								testCaseManager.updateTestCaseWithoutMetadata(
									existingTestCaseId, testCaseToStore.path, testCaseToStore.testSuiteOrder, testCaseToStore.targetActors.get
								) andThen DBIO.successful(savedTestCaseIds += (testCaseToStore.identifier -> (existingTestCaseId, false)))
							}
						}
						// Update test history
						if (updateActions.resetTestCaseHistory(testCase.identifier)) {
							combinedAction = for {
								results <- combinedAction
								_ <- testResultManager.updateForDeletedTestCase(existingTestCaseId) andThen
									PersistenceSchema.conformanceResults
										.filter(_.testcase === existingTestCaseId)
										.map(c => (c.testsession, c.result, c.outputMessage, c.updateTime))
										.update(None, TestResultStatus.UNDEFINED.toString, None, None)
							} yield results
						}
					} else {
						// New test case.
						combinedAction = combinedAction.flatMap { savedTestCaseIds =>
							(PersistenceSchema.testCases.returning(PersistenceSchema.testCases.map(_.id)) += testCaseToStore).flatMap { newTestCaseId =>
								DBIO.successful(savedTestCaseIds += (testCaseToStore.identifier -> (newTestCaseId, true)))
							}
						}
					}
				}
				combinedAction
			}
			// Return the newly added test case IDs.
			newTestCaseIds <- DBIO.successful(testCaseIds.filter(_._2._2).map(_._2._1).toList)
			// Remove the test cases that are no longer in the test suite.
			_ <- stepRemoveTestCases(existingTestCaseMap)
			// Update the test case links for the test suite.
			_ <- stepUpdateTestSuiteTestCaseLinks(savedTestSuiteId, newTestCaseIds)
			// Collect the names of the test cases that were removed.
			removedTestCaseNames <- DBIO.successful(existingTestCaseMap.map(_._2._2).toList)
		} yield (testCaseIds.toMap, removedTestCaseNames)
	}

	def stepUpdateTestSuiteSpecificationLinks(specificationId: Long, testSuiteId: Long, testCases: Option[List[TestCases]], savedActorIds: util.Map[String, Long], savedTestCaseIds: Map[String, (Long, Boolean)]): DBIO[_] = {
		for {
			// Add link between specification and test suite if needed.
			_ <- for {
				testSuiteExistsForSpecification <- PersistenceSchema.specificationHasTestSuites.filter(_.testSuiteId === testSuiteId).filter(_.specId === specificationId).result.headOption
				_ <- if (testSuiteExistsForSpecification.isEmpty) {
					PersistenceSchema.specificationHasTestSuites += (specificationId, testSuiteId)
				} else {
					DBIO.successful(())
				}
			} yield ()
			// Lookup the map of systems to actors for the specification
			systemActors <- getSystemActors(specificationId)
			// Create a map of actors to systems.
			existingActorToSystemMap <- getExistingActorToSystemMap(systemActors)
			// Update test case actor links.
			_ <- {
				var combinedAction: DBIO[_] = DBIO.successful(())
				testCases.get.foreach { testCase =>
					val testCaseId = savedTestCaseIds(testCase.identifier)._1
					if (savedTestCaseIds(testCase.identifier)._2) {
						// New test case - Update test case relation to actors.
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
								combinedAction = combinedAction andThen
									(PersistenceSchema.testCaseHasActors += (testCaseId, specificationId, actorInternalId, isSut))
								// All conformance results will be new. We need to add these for all systems that implement the actor.
								if (isSut) {
									val implementingSystems = existingActorToSystemMap.get(actorInternalId)
									if (implementingSystems != null) {
										for (implementingSystem <- implementingSystems.asScala) {
											combinedAction = combinedAction andThen
												(PersistenceSchema.conformanceResults += ConformanceResult(0L, implementingSystem, specificationId, actorInternalId, testSuiteId, testCaseId, TestResultStatus.UNDEFINED.toString, None, None, None))
										}
									}
								}
							}
						}
					} else {
						// Updated test case - Update test case relation to actors.
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
								existingTestCaseActors <- PersistenceSchema.testCaseHasActors
									.filter(_.testcase === testCaseId)
									.filter(_.actor inSet savedActorIds.values().asScala)
									.result.map(_.toList)
								_ <- {
									val actions = new ListBuffer[DBIO[_]]()
									existingTestCaseActors.map(existingTestCaseActor => {
										val existingActorId = existingTestCaseActor._3
										val existingMarkedAsSut = existingTestCaseActor._4
										if (!actorsToFurtherProcess.contains(existingActorId)) {
											// The actor is no longer mentioned in the test case - remove
											actions += PersistenceSchema.conformanceResults.filter(_.testcase === testCaseId).filter(_.actor === existingActorId).delete
											actions += testCaseManager.removeActorLinkForTestCase(testCaseId, existingActorId)
										} else {
											// Update the actor role if needed.
											if ((existingActorId == sutActor && !existingMarkedAsSut)
												|| (existingActorId != sutActor && existingMarkedAsSut)) {
												// Update the role of the actor.
												val query = for {t <- PersistenceSchema.testCaseHasActors if t.testcase === existingTestCaseActor._1 && t.specification === existingTestCaseActor._2 && t.actor === existingActorId} yield t.sut
												actions += query.update(existingActorId == sutActor)
												if (existingActorId != sutActor) {
													// The actor is no longer the SUT. Remove the conformance results for it.
													actions += PersistenceSchema.conformanceResults.filter(_.testcase === testCaseId).filter(_.actor === existingActorId).delete
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
											action = PersistenceSchema.testCaseHasActors += (testCaseId, specificationId, newTestCaseActor, newTestCaseActor == sutActor)
										}
										val implementingSystems = existingActorToSystemMap.get(newTestCaseActor)
										if (implementingSystems != null) {
											implementingSystems.asScala.foreach { implementingSystem: Long =>
												// Check to see if there are existing test sessions for this test case.
												action = action andThen (for {
													previousResult <- PersistenceSchema.testResults.filter(_.sutId === implementingSystem).filter(_.testCaseId === testCaseId).sortBy(_.endTime.desc).result.headOption
													_ <- {
														if (previousResult.isDefined) {
															PersistenceSchema.conformanceResults += ConformanceResult(0L, implementingSystem, specificationId, newTestCaseActor, testSuiteId, testCaseId, previousResult.get.result, previousResult.get.outputMessage, Some(previousResult.get.sessionId), Some(previousResult.get.endTime.getOrElse(previousResult.get.startTime)))
														} else {
															PersistenceSchema.conformanceResults += ConformanceResult(0L, implementingSystem, specificationId, newTestCaseActor, testSuiteId, testCaseId, TestResultStatus.UNDEFINED.toString, None, None, None)
														}
													}
												} yield ())
											}
										}
										action
									})
									DBIO.seq(actions.toList: _*)
								}
							} yield ())
					}
				}
				combinedAction
			}
			// Update test suite actor links.
			_ <- stepUpdateTestSuiteActorLinks(testSuiteId, savedActorIds, specificationId)
		} yield ()
	}

	private def stepRemoveTestCases(existingTestCaseMap: mutable.HashMap[String, (java.lang.Long, String)]): DBIO[_] = {
		// Remove test cases not in new test suite.
		val actions = new ListBuffer[DBIO[_]]()
		existingTestCaseMap.foreach { testCaseEntry =>
			actions += testCaseManager.delete(testCaseEntry._2._1)

		}
		DBIO.seq(actions.toList: _*)
	}

	private def stepUpdateTestSuiteActorLinks(testSuiteId: Long, savedActorIds: util.Map[String, Long], specificationId: Long): DBIO[_] = {
		for {
			specificationActorIds <- PersistenceSchema.specificationHasActors.filter(_.specId === specificationId).map(_.actorId).result
			// Remove all previous actor links.
			_ <- PersistenceSchema.testSuiteHasActors.filter(_.testsuite === testSuiteId).filter(_.actor inSet specificationActorIds).delete
			// Add new test suite actor links.
			_ <- {
				val actions = new ListBuffer[DBIO[_]]()
				savedActorIds.entrySet().forEach { actorEntry =>
					actions += (PersistenceSchema.testSuiteHasActors += (testSuiteId, actorEntry.getValue))
				}
				toDBIO(actions)
			}
		} yield ()
	}

	private def stepUpdateTestSuiteTestCaseLinks(savedTestSuiteId: Long, savedTestCaseIds: List[Long]): DBIO[_] = {
		// Update test suite test case links.
		val actions = new ListBuffer[DBIO[_]]()
		savedTestCaseIds.foreach { testCaseId =>
			actions += (PersistenceSchema.testSuiteHasTestCases += (savedTestSuiteId, testCaseId))
		}
		DBIO.seq(actions.toList: _*)
	}

	private def getSystemActors(specificationId: Long) = {
		PersistenceSchema.systemImplementsActors.filter(_.specId === specificationId).result.map(_.toList)
	}

	private def getExistingActorToSystemMap(systemActors: List[(Long, Long, Long)]) = {
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

	private def getExistingTestCasesForTestSuite(testSuiteId: Long): DBIO[List[(Long, String, String)]] = {
		PersistenceSchema.testCases
			.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
			.filter(_._2.testsuite === testSuiteId)
			.map(r => (r._1.id, r._1.identifier, r._1.fullname))
			.result
			.map(_.toList)
	}

	private def getExistingTestCaseMap(existingTestCasesForTestSuite: List[(Long, String, String)]): DBIO[mutable.HashMap[String, (java.lang.Long, String)]] = {
		// Process test cases.
		val existingTestCaseMap = new mutable.HashMap[String, (java.lang.Long, String)]()
		if (existingTestCasesForTestSuite.nonEmpty) {
			// This is an update - check for existing test cases.
			for (existingTestCase <- existingTestCasesForTestSuite) {
				existingTestCaseMap += (existingTestCase._2 -> (existingTestCase._1, existingTestCase._3))
			}
		}
		DBIO.successful(existingTestCaseMap)
	}

	private def createTestSuitePaths(domainId: Long, testSuiteFolderName: String, tempTestSuiteArchive: File) = {
		val targetFolder: File = repositoryUtils.getTestSuitePath(domainId, testSuiteFolderName)
		val resourcePaths = repositoryUtils.extractTestSuiteFilesFromZipToFolder(targetFolder, tempTestSuiteArchive)
		TestSuitePaths(targetFolder, resourcePaths._1, resourcePaths._2)
	}

	private def saveTestSuite(sharedTestSuiteInfo: Option[TestSuiteSaveResult], specificationId: Long, tempSuite: TestSuites, existingSuite: Option[TestSuites], updateActions: TestSuiteDeploymentAction, testSuiteActors: Option[List[Actor]], testCases: Option[List[TestCases]], tempTestSuiteArchive: Option[File], onSuccessCalls: mutable.ListBuffer[() => _], onFailureCalls: mutable.ListBuffer[() => _]): DBIO[(TestSuiteSaveResult, List[TestSuiteUploadItemResult])] = {
		// Reuse a previously saved test suite (if applicable).
		var suite = if (sharedTestSuiteInfo.isDefined) {
			sharedTestSuiteInfo.get.testSuite
		} else {
			// Create a new test suite folder name.
			tempSuite.withFileName(repositoryUtils.generateTestSuiteFileName())
		}
		// Reuse the previously saved files (if applicable).
		val testSuitePathsToUse = if (sharedTestSuiteInfo.isDefined) {
			sharedTestSuiteInfo.get.pathInfo
		} else {
			if (tempTestSuiteArchive.isEmpty) {
				throw new IllegalArgumentException("The test suite archive path was expected")
			}
			createTestSuitePaths(suite.domain, suite.filename, tempTestSuiteArchive.get)
		}
		suite = suite.withDefinitionPath(testSuitePathsToUse.testSuiteDefinitionPath)
    var existingFolder: File = null
		var testSuiteExists = false
		var existingTestSuiteId: Option[Long] = None
    if (existingSuite.isDefined) {
      // Update case.
			testSuiteExists = true
			existingTestSuiteId = Some(existingSuite.get.id)
      existingFolder = repositoryUtils.getTestSuitePath(suite.domain, existingSuite.get.filename)
    }
		val action = for {
			// Save the test suite and test cases.
			stepSaveTestSuite <- {
				if (sharedTestSuiteInfo.isDefined) {
					// Skip saving the test suite and use the previously saved information.
					DBIO.successful(sharedTestSuiteInfo.get)
				} else {
					// Save the test suite.
					stepSaveTestSuiteAndTestCases(suite, existingTestSuiteId, testCases, testSuitePathsToUse, updateActions)
				}
			}
			// Process the actors (inserting and saving them as needed) and return their ID information for further processing.
			saveActorsStep <- stepSaveActors(specificationId, testSuiteActors, suite.domain, updateActions, onSuccessCalls)
			// Make all actor and specification updates for the test suite and its test cases.
			_ <- stepUpdateTestSuiteSpecificationLinks(specificationId, stepSaveTestSuite.testSuite.id, testCases, saveActorsStep._1, stepSaveTestSuite.updatedTestCases)
			// Record status updates.
			statusResults <- {
				val results = new ListBuffer[TestSuiteUploadItemResult]()
				// Test suite.
				if (sharedTestSuiteInfo.isDefined && sharedTestSuiteInfo.get.isLinked) {
					results += new TestSuiteUploadItemResult(suite.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_SUITE, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED, specificationId)
				} else {
					if (testSuiteExists) {
						results += new TestSuiteUploadItemResult(suite.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_SUITE, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE, specificationId)
					} else {
						results += new TestSuiteUploadItemResult(suite.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_SUITE, TestSuiteUploadItemResult.ACTION_TYPE_ADD, specificationId)
					}
				}
				// Test case additions and updates.
				testCases.get.foreach { testCase =>
					if (sharedTestSuiteInfo.isDefined && sharedTestSuiteInfo.get.isLinked) {
						results += new TestSuiteUploadItemResult(testCase.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_UNCHANGED, specificationId)
					} else {
						if (stepSaveTestSuite.updatedTestCases(testCase.identifier)._2) {
							results += new TestSuiteUploadItemResult(testCase.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_ADD, specificationId)
						} else {
							results += new TestSuiteUploadItemResult(testCase.shortname, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE, specificationId)
						}
					}
				}
				// Test case removals.
				stepSaveTestSuite.deletedTestCaseNames.foreach { removedTestCaseName =>
					results += new TestSuiteUploadItemResult(removedTestCaseName, TestSuiteUploadItemResult.ITEM_TYPE_TEST_CASE, TestSuiteUploadItemResult.ACTION_TYPE_REMOVE, specificationId)
				}
				DBIO.successful(results.toList)
			}
		} yield (stepSaveTestSuite, saveActorsStep._2, statusResults)
		action.flatMap(results => {
			// Finally, delete the backup folder
			onSuccessCalls += (() => {
				if (existingFolder != null && existingFolder.exists() && existingSuite.isDefined && !existingSuite.get.shared) {
					// We don't deleting the existing folder if this is a shared test suite.
					FileUtils.deleteDirectory(existingFolder)
				}
			})
			DBIO.successful((results._1, results._2 ++ results._3))
		}).cleanUp(error => {
			if (error.isDefined) {
				onFailureCalls += (() => {
					// Cleanup operations in case an error occurred (we only do this here for non-shared test suites.
					if (sharedTestSuiteInfo.isEmpty && testSuitePathsToUse.testSuiteFolder.exists()) {
						FileUtils.deleteDirectory(testSuitePathsToUse.testSuiteFolder)
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

	def extractTestSuite(testSuite: TestSuites, testSuiteOutputPath: Option[Path]): Path = {
		val testSuiteFolder = repositoryUtils.getTestSuitePath(testSuite.domain, testSuite.filename)
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

	def searchTestSuites(domainIds: Option[List[Long]], specificationIds: Option[List[Long]], specificationGroupIds: Option[List[Long]], actorIds: Option[List[Long]]): Iterable[TestSuite] = {
		val results = exec(
			for {
				// See if due to provided actor IDs we need to consider specific specification IDs
				actorSpecIds <- {
					if (actorIds.isEmpty) {
						DBIO.successful(None)
					} else {
						PersistenceSchema.specificationHasActors.filter(_.actorId inSet actorIds.get).map(x => x.specId).result.map(x => Some(x))
					}
				}
				// Combine the specs from actors with the provided spec IDs to get the final set of specs to apply
				specIdsToUse <- {
					if (actorSpecIds.isEmpty) {
						DBIO.successful(specificationIds)
					} else if (specificationIds.isDefined) {
						DBIO.successful(Some((actorSpecIds.get ++ specificationIds.get).toSet))
					} else {
						DBIO.successful(None)
					}
				}
				// Query to retrieve the test suites (without their documentation)
				testSuitesTuples <- PersistenceSchema.testSuites
					.join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
					.join(PersistenceSchema.specifications).on(_._2.specId === _.id)
					.filter(_._1._1.hidden === false)
					.filterOpt(domainIds)((q, ids) => q._1._1.domain inSet ids)
					.filterOpt(specIdsToUse)((q, ids) => q._1._2.specId inSet ids)
					.filterOpt(specificationGroupIds)((q, ids) => q._2.group inSet ids)
					.sortBy(_._1._1.shortname.asc)
					.map(x => (TestSuiteManager.withoutDocumentation(x._1._1), x._1._2.specId))
					.result
				// Map the specification IDs
				testSuiteTuplesWithSpecs <- {
					val testSuiteToSpecificationMap = new mutable.LinkedHashMap[Long, (TestSuiteManager.TestSuiteValueTuple, ListBuffer[Long])]()
					testSuitesTuples.foreach { tuple =>
						val entry = testSuiteToSpecificationMap.getOrElseUpdate(tuple._1._1, (tuple._1, new ListBuffer[Long]()))
						entry._2 += tuple._2
					}
					DBIO.successful(testSuiteToSpecificationMap.values)
				}
				// Use the already returned test suite IDs to load their corresponding test cases
				testCaseTuplesWithTestSuiteId <- {
					val testSuiteIds = testSuiteTuplesWithSpecs.map(_._1._1)
					PersistenceSchema.testCases
						.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
						.filter(_._2.testsuite inSet testSuiteIds)
						.map(x => (TestCaseManager.withoutDocumentation(x._1), x._2.testsuite))
						.result
				}
				// Transform the test case tuples to case objects
				testCaseCaseObjectsWithTestSuiteId <- DBIO.successful(testCaseTuplesWithTestSuiteId.map(x => (TestCaseManager.tupleToTestCase(x._1), x._2)))
				// Map the retrieved test suites to their retrieved test cases to return the final result
				testSuites <- {
					// Map out test cases
					val testSuiteToTestCaseMap = new mutable.HashMap[Long, ListBuffer[TestCases]]()
					testCaseCaseObjectsWithTestSuiteId.foreach { x =>
						var list = testSuiteToTestCaseMap.get(x._2)
						if (list.isEmpty) {
							list = Some(new ListBuffer[TestCases])
							testSuiteToTestCaseMap += (x._2 -> list.get)
						}
						list.get += x._1
					}
					// Construct final results
					val results = testSuiteTuplesWithSpecs.map(x => {
						val testSuiteCaseObject = TestSuiteManager.tupleToTestSuite(x._1)
						val testSuite = new TestSuite(testSuiteCaseObject, testSuiteToTestCaseMap.getOrElse(testSuiteCaseObject.id, new ListBuffer[TestCases]).toList)
						testSuite.specifications = Some(x._2.toList)
						testSuite
					})
					DBIO.successful(results)
				}
			} yield testSuites
		)
		results
	}

	def prepareSharedTestSuiteLink(testSuiteId: Long, specificationIds: List[Long]): TestSuiteUploadResult = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val onFailureCalls = mutable.ListBuffer[() => _]()
		val dbAction = for {
			// Parse shared test suite.
			testSuiteWithActors <- loadTestSuiteAndActorsFromDefinition(testSuiteId)
			// See if we have any specifications with existing actor information matching that of the test suite.
			specificationsWithMatchingActors <- PersistenceSchema.actors
				.join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
				.filter(_._2.specId inSet specificationIds)
				.filter(_._1.actorId inSet testSuiteWithActors.actors.getOrElse(List.empty).map(_.actorId))
				.map(_._2.specId)
				.result
				.map(_.toSet)
			// See if we have any specifications with an existing test suite with the same identifier as the one we are linking.
			specificationsWithMatchingTestSuite <- PersistenceSchema.testSuites
				.join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
				.filter(_._2.specId inSet specificationIds)
				.filter(_._1.identifier === testSuiteWithActors.identifier)
				.filter(_._1.id =!= testSuiteWithActors.id)
				.map(x => (x._2.specId, x._1.shared))
				.result
				.map(_.toSet)
			result <- {
				if (specificationsWithMatchingActors.isEmpty && specificationsWithMatchingTestSuite.isEmpty) {
					// We can directly proceed.
					val actions = specificationIds.map { specId =>
						new TestSuiteDeploymentAction(Some(specId), TestSuiteReplacementChoice.PROCEED, updateTestSuite = false, updateActors = Some(false), sharedTestSuite = false, None)
					}
					for {
						linkResults <- linkSharedTestSuite(Some(testSuiteWithActors), testSuiteId, actions, onSuccessCalls, onFailureCalls)
						overallResult <- {
							val testSuiteResult = new TestSuiteUploadResult()
							testSuiteResult.items.addAll(linkResults.asJavaCollection)
							testSuiteResult.needsConfirmation = false
							testSuiteResult.success = true
							DBIO.successful(testSuiteResult)
						}
					} yield overallResult
				} else {
					// We need confirmations.
					val result = new TestSuiteUploadResult()
					result.matchingDataExists = Some(specificationsWithMatchingActors.toList)
					result.existsForSpecs = Some(specificationsWithMatchingTestSuite.toList)
					result.needsConfirmation = true
					DBIO.successful(result)
				}
			}
		} yield result
		exec(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), dbAction).transactionally)
	}

	private def loadTestSuiteAndActorsFromDefinition(testSuiteId: Long): DBIO[TestSuite] = {
		for {
			loadedTestSuite <- getTestSuiteWithTestCasesInternal(testSuiteId)
			loadedTestSuiteWithActors <- {
				if (loadedTestSuite.definitionPath.isEmpty) {
					throw new IllegalStateException("The definition path must be defined for shared test suite")
				}
				val testSuiteDefinition = repositoryUtils.getTestSuite(repositoryUtils.getTestSuiteDefinitionFile(loadedTestSuite.domain, loadedTestSuite.filename, loadedTestSuite.definitionPath.get))
				loadedTestSuite.actors = Some(repositoryUtils.testSuiteActorInfo(testSuiteDefinition))
				DBIO.successful(loadedTestSuite)
			}
		} yield loadedTestSuiteWithActors
	}

	private def getSpecificationIdsFromApiKeys(domainId: Long, apiKeys: List[String]): DBIO[Map[Long, String]] = {
		PersistenceSchema.specifications
			.filter(_.apiKey inSet apiKeys)
			.filter(_.domain === domainId)
			.map(x => (x.id, x.apiKey))
			.result
			.map(_.toMap)
	}

	def linkSharedTestSuiteFromApi(testSuiteId: Long, domainId: Long, input: TestSuiteLinkRequest): List[TestSuiteLinkResponseSpecification] = {
		// Problem cases:
		// 1. Specification linked to different test suite with same identifier
		// 2. Specification already linked to test suite.
		// 3. Specification not found.
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val onFailureCalls = mutable.ListBuffer[() => _]()
		val dbAction = for {
			// Find specifications IDs.
			specificationMap <- getSpecificationIdsFromApiKeys(domainId, input.specifications.map(_.identifier))
			// Specifications already linked to this test suite.
			specsLinkedToSameTestSuite <- PersistenceSchema.specificationHasTestSuites
				.filter(_.testSuiteId === testSuiteId)
				.filter(_.specId inSet specificationMap.keys)
				.map(_.specId)
				.result
			// Specifications already linked to a different test suite with the same identifier.
			specsLinkedToMatchingOtherTestSuite <- PersistenceSchema.specificationHasTestSuites
				.join(PersistenceSchema.testSuites).on(_.testSuiteId === _.id)
				.filter(_._1.specId inSet specificationMap.keys)
				.filter(_._1.testSuiteId =!= testSuiteId)
				.filter(_._2.identifier === input.testSuite)
				.map(_._1.specId)
				.result
			// Remaining specifications.
			targetSpecs <- {
				val targetSpecIds = specificationMap.keys.to(ListBuffer)
				targetSpecIds --= specsLinkedToSameTestSuite
				targetSpecIds --= specsLinkedToMatchingOtherTestSuite
				DBIO.successful(targetSpecIds)
			}
			// Do the update.
			_ <- {
				val specActions = targetSpecs.map { specId =>
					val apiKey = specificationMap(specId)
					val inputSpec = input.specifications.find(x => x.identifier == apiKey)
					val updateMetadata = if (inputSpec.isDefined) {
						inputSpec.get.update
					} else {
						false
					}
					new TestSuiteDeploymentAction(Some(specId), TestSuiteReplacementChoice.PROCEED, updateTestSuite = false, Some(updateMetadata), sharedTestSuite = false, None)
				}
				linkSharedTestSuite(None, testSuiteId, specActions.toList, onSuccessCalls, onFailureCalls)
			}
			// Record the results.
			result <- {
				val specResults = new ListBuffer[TestSuiteLinkResponseSpecification]()
				val pendingKeys = new mutable.HashSet[String]()
				input.specifications.foreach { inputSpec =>
					pendingKeys += inputSpec.identifier
				}
				// Specifications that were linked.
				targetSpecs.foreach { id =>
					val key = specificationMap(id)
					specResults += TestSuiteLinkResponseSpecification(key, linked = true, None)
					pendingKeys -= key
				}
				// Specifications that were skipped because they were already linked to the test suite.
				specsLinkedToSameTestSuite.foreach { id =>
					val key = specificationMap(id)
					specResults += TestSuiteLinkResponseSpecification(specificationMap(id), linked = false, Some("Specification already linked to this test suite."))
					pendingKeys -= key
				}
				// Specifications that were skipped because they were linked to another test suite.
				specsLinkedToMatchingOtherTestSuite.foreach { id =>
					val key = specificationMap(id)
					specResults += TestSuiteLinkResponseSpecification(specificationMap(id), linked = false, Some("Specification linked to another suite with the same identifier."))
					pendingKeys -= key
				}
				// Specifications that were skipped because they were not found.
				pendingKeys.foreach { key =>
					specResults += TestSuiteLinkResponseSpecification(key, linked = false, Some("Specification not found."))
				}
				DBIO.successful(specResults.toList)
			}
		} yield result
		exec(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), dbAction).transactionally)
	}

	def unlinkSharedTestSuiteFromApi(testSuiteId: Long, domainId: Long, input: TestSuiteUnlinkRequest): Unit = {
		exec(for {
			// Get the specification IDs corresponding to the specification API keys.
			specificationMap <- getSpecificationIdsFromApiKeys(domainId, input.specifications)
			// Get the specification IDs that are actually linked to the test suite.
			targetSpecificationIds <- PersistenceSchema.specificationHasTestSuites
				.filter(_.testSuiteId === testSuiteId)
				.filter(_.specId inSet specificationMap.keys)
				.map(_.specId)
				.result
			// Do the unlinking.
			_ <- unlinkSharedTestSuiteInternal(testSuiteId, targetSpecificationIds.toList)
		} yield ())
	}

	def linkSharedTestSuiteWrapper(testSuiteId: Long, specActions: List[TestSuiteDeploymentAction]): TestSuiteUploadResult = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val onFailureCalls = mutable.ListBuffer[() => _]()
		val dbAction = linkSharedTestSuite(None, testSuiteId, specActions, onSuccessCalls, onFailureCalls)
		val resultItems = exec(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), dbAction).transactionally)
		val result = new TestSuiteUploadResult()
		result.success = true
		result.needsConfirmation = false
		result.items.addAll(resultItems.asJavaCollection)
		result
	}

	def linkSharedTestSuite(testSuiteWithActors: Option[TestSuite], testSuiteId: Long, specActions: List[TestSuiteDeploymentAction], onSuccessCalls: mutable.ListBuffer[() => _], onFailureCalls: mutable.ListBuffer[() => _]): DBIO[List[TestSuiteUploadItemResult]] = {
		for {
			testSuiteToUse <- if (testSuiteWithActors.isDefined) {
				DBIO.successful(testSuiteWithActors.get)
			} else {
				loadTestSuiteAndActorsFromDefinition(testSuiteId)
			}
			results <- {
				// Prepare test case path information.
				val testCaseUpdates = new mutable.HashMap[String, (Long, Boolean)]()
				val testCasePaths = new mutable.HashMap[String, String]()
				testSuiteToUse.testCases.get.foreach { testCase =>
					testCaseUpdates += (testCase.identifier -> (testCase.id, false))
					testCasePaths += (testCase.identifier -> testCase.path)
				}
				// Prepare overall test suite path information.
				val testSuitePaths = TestSuitePaths(
					repositoryUtils.getTestSuitePath(testSuiteToUse.domain, testSuiteToUse.filename),
					testSuiteToUse.definitionPath,
					testCasePaths.toMap
				)
				val testSuiteInfo = TestSuiteSaveResult(testSuiteToUse.toCaseObject, testCaseUpdates.toMap, List.empty, testSuitePaths, isLinked = true)
				// Proceed.
				saveTestSuiteForSpecifications(Some(testSuiteInfo), specActions, testSuiteToUse, testSuiteToUse.actors, testSuiteToUse.testCases, None, onSuccessCalls, onFailureCalls)
			}
		} yield results
	}

	def unlinkSharedTestSuiteInternal(testSuiteId: Long, specificationIds: List[Long]): DBIO[_] = {
		for {
			// Get actor IDs for specifications.
			actorIds <- PersistenceSchema.specificationHasActors.filter(_.specId inSet specificationIds).map(_.actorId).result
			// Get test case IDs for test suite.
			testCaseIds <- PersistenceSchema.testSuiteHasTestCases.filter(_.testsuite === testSuiteId).map(_.testcase).result
			// Delete conformance statement results.
			_ <- PersistenceSchema.conformanceResults
				.filter(_.testsuite === testSuiteId)
				.filter(_.spec inSet specificationIds)
				.delete
			// Delete test suite link to actors.
			_ <- PersistenceSchema.testSuiteHasActors
				.filter(_.testsuite === testSuiteId)
				.filter(_.actor inSet actorIds)
				.delete
			// Delete test case links to actors.
			_ <- PersistenceSchema.testCaseHasActors
				.filter(_.specification inSet specificationIds)
				.filter(_.testcase inSet testCaseIds)
				.delete
			// Delete test suite link to specifications.
			_ <- PersistenceSchema.specificationHasTestSuites
				.filter(_.testSuiteId === testSuiteId)
				.filter(_.specId inSet specificationIds)
				.delete
		} yield ()
	}

	def unlinkSharedTestSuite(testSuiteId: Long, specificationIds: List[Long]): Unit = {
		exec(unlinkSharedTestSuiteInternal(testSuiteId, specificationIds))
	}

	def getLinkedSpecifications(testSuiteId: Long): Seq[Specifications] = {
		val specificationIds = exec(
			PersistenceSchema.specificationHasTestSuites.filter(_.testSuiteId === testSuiteId).map(_.specId).result
		)
		conformanceManager.getSpecifications(Some(specificationIds), None, withGroups = true)
	}

}