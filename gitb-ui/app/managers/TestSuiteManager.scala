/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package managers

import com.gitb.core.TestCaseType
import com.gitb.tr.{TAR, TestResultType}
import exceptions.{ErrorCodes, UserException}
import managers.TestSuiteManager.{TestCaseForTestSuite, TestCasesForTestSuiteWithActorCount, TestSuiteSharedInfo}
import managers.testsuite.{TestSuitePaths, TestSuiteSaveResult}
import models.Enums.TestSuiteReplacementChoice.{PROCEED, TestSuiteReplacementChoice}
import models.Enums.{TestCaseUploadMatchType, TestResultStatus, TestSuiteReplacementChoice}
import models._
import models.automation._
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
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.{CollectionHasAsScala, SetHasAsScala}

object TestSuiteManager {

	val TEST_SUITES_PATH = "test-suites"

	private type TestSuiteDbTuple = (
			Rep[Long], Rep[String], Rep[String], Rep[String],
			Rep[Option[String]], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]],
			Rep[Option[String]], Rep[Long], Rep[String], Rep[Boolean], Rep[Boolean], Rep[String], Rep[Option[String]],
			Rep[Option[String]], Rep[Option[String]], Rep[Option[String]]
		)

	private type TestSuiteValueTuple = (
		Long, String, String, String,
			Option[String], Option[String], Option[String], Option[String],
			Option[String], Long, String, Boolean, Boolean, String, Option[String],
			Option[String], Option[String], Option[String]
		)

	private def withoutDocumentation(dbTestSuite: PersistenceSchema.TestSuitesTable): TestSuiteDbTuple = {
		(dbTestSuite.id, dbTestSuite.shortname, dbTestSuite.fullname, dbTestSuite.version,
			dbTestSuite.authors, dbTestSuite.originalDate, dbTestSuite.modificationDate, dbTestSuite.description,
			dbTestSuite.keywords, dbTestSuite.domain, dbTestSuite.filename, dbTestSuite.hasDocumentation, dbTestSuite.shared, dbTestSuite.identifier, dbTestSuite.definitionPath,
			dbTestSuite.specReference, dbTestSuite.specDescription, dbTestSuite.specLink
		)
	}

	private def tupleToTestSuite(x: TestSuiteValueTuple): TestSuites = {
		TestSuites(x._1, x._2, x._3, x._4, x._5, x._6, x._7, x._8, x._9, x._11, x._12, None, x._14, hidden = false, x._13, x._10, x._15, x._16, x._17, x._18)
	}

	private case class TestCaseForTestSuite(id: Long, identifier: String, fullName: String)
	private case class TestCasesForTestSuiteWithActorCount(specification: Long, testCases: List[TestCaseForTestSuite], actorCount: Int)
	private case class TestSuiteSharedInfo(id: Long, shared: Boolean)

}

@Singleton
class TestSuiteManager @Inject() (domainParameterManager: DomainParameterManager,
																	testResultManager: TestResultManager,
																	actorManager: ActorManager,
																	endPointManager: EndPointManager,
																	testCaseManager: TestCaseManager,
																	parameterManager: ParameterManager,
																	repositoryUtils: RepositoryUtils,
																	dbConfigProvider: DatabaseConfigProvider)
																 (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

	import dbConfig.profile.api._

	private final val logger: Logger = LoggerFactory.getLogger("TestSuiteManager")

	def getById(testSuiteId: Long): Future[Option[TestSuites]] = {
		DB.run(getByIdInternal(testSuiteId))
	}

	def getByIdInternal(testSuiteId: Long): DBIO[Option[TestSuites]] = {
		PersistenceSchema.testSuites
			.filter(_.id === testSuiteId)
			.map(TestSuiteManager.withoutDocumentation)
			.result
			.headOption
			.map { x =>
				x.map(TestSuiteManager.tupleToTestSuite)
			}
	}

	def getTestSuiteDomain(testSuiteId: Long): Future[Long] = {
		DB.run(
			PersistenceSchema.testSuites
				.filter(_.id === testSuiteId)
				.map(_.domain)
				.result
				.head
		)
	}

	def getTestSuiteInfoByApiKeys(communityApiKey: String, specificationApiKey: Option[String], testSuiteIdentifier: String): Future[Option[(Long, Long, Boolean)]] = { // Test suite ID, domain ID and shared flag
		DB.run(for {
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

	def searchSharedTestSuitesWithDomainId(filter: Option[String], page: Long, limit: Long, domain: Long): Future[SearchResult[TestSuites]] = {
		val query = PersistenceSchema.testSuites
			.filter(_.domain === domain)
			.filter(_.shared === true)
			.filterOpt(filter)((table, filterValue) => {
        val filterValueToUse = toLowercaseLikeParameter(filterValue)
				table.identifier.toLowerCase.like(filterValueToUse) || table.shortname.toLowerCase.like(filterValueToUse) || table.description.getOrElse("").toLowerCase.like(filterValueToUse)
			})
			.sortBy(_.shortname.asc)
			.map(TestSuiteManager.withoutDocumentation)
		DB.run {
			for {
				results <- query.drop((page - 1) * limit).take(limit).result.map(x => x.map(TestSuiteManager.tupleToTestSuite))
				resultCount <- query.size.result
			} yield SearchResult(results, resultCount)
		}
	}

	def getSharedTestSuitesWithDomainId(domain: Long): Future[List[TestSuites]] = {
		DB.run(PersistenceSchema.testSuites
			.filter(_.domain === domain)
			.filter(_.shared === true)
			.sortBy(_.shortname.asc)
			.map(TestSuiteManager.withoutDocumentation)
			.result.map(_.toList)
		).map { results =>
			results.map(TestSuiteManager.tupleToTestSuite)
		}
	}

	def getSharedTestSuitesWithSpecificationId(specification: Long): Future[Iterable[TestSuites]] = {
		DB.run {
			PersistenceSchema.testSuites
				.join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
				.filter(_._2.specId === specification)
				.filter(_._1.shared === true)
				.sortBy(_._1.shortname.asc)
				.map(x => TestSuiteManager.withoutDocumentation(x._1))
				.result
				.map(x => x.map(TestSuiteManager.tupleToTestSuite))
		}
	}

	def getTestSuitesWithSpecificationId(filter: Option[String], page: Long, limit: Long, specification: Long): Future[SearchResult[TestSuites]] = {
		val query = PersistenceSchema.testSuites
			.join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
			.filter(_._2.specId === specification)
			.filterOpt(filter)((table, filterValue) => {
        val filterValueToUse = toLowercaseLikeParameter(filterValue)
				table._1.identifier.toLowerCase.like(filterValueToUse) || table._1.shortname.toLowerCase.like(filterValueToUse) || table._1.description.getOrElse("").toLowerCase.like(filterValueToUse)
			})
			.sortBy(_._1.shortname.asc)
			.map(x => TestSuiteManager.withoutDocumentation(x._1))
		DB.run {
			for {
				results <- query.drop((page - 1) * limit).take(limit).result.map(x => x.map(TestSuiteManager.tupleToTestSuite))
				resultCount <- query.size.result
			} yield SearchResult(results, resultCount)
		}
	}

	def getTestSuites(ids: Option[List[Long]]): Future[List[TestSuites]] = {
		val q = ids match {
			case Some(idList) =>
				PersistenceSchema.testSuites
					.filter(_.id inSet idList)
			case None =>
				PersistenceSchema.testSuites
		}
		DB.run(
			q.sortBy(_.shortname.asc)
				.map(TestSuiteManager.withoutDocumentation)
				.result
				.map(_.toList)
		).map { result =>
			result.map(TestSuiteManager.tupleToTestSuite)
		}
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

	def getTestSuiteOfTestCase(testCaseId: Long): Future[TestSuites] = {
		DB.run(getTestSuiteOfTestCaseInternal(testCaseId))
	}

	def getTestSuiteTestCasesWithPaging(testSuiteId: Long, filterText: Option[String], page: Long, limit: Long): Future[SearchResult[TestCases]] = {
		val query = PersistenceSchema.testCases
			.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
			.filter(_._2.testsuite === testSuiteId)
			.filterOpt(toLowercaseLikeParameter(filterText))((q, text) => q._1.shortname.toLowerCase.like(text))
			.sortBy(_._1.testSuiteOrder)
			.map(x => (x._1.id, x._1.identifier, x._1.shortname, x._1.fullname, x._1.description,
				x._1.group, x._1.isOptional, x._1.isDisabled, x._1.tags, x._1.testSuiteOrder,
				x._1.hasDocumentation, x._1.specReference, x._1.specDescription, x._1.specLink)
			)
		DB.run {
			for {
				results <- query.drop((page - 1) * limit)
					.take(limit)
					.result.map { results =>
						results.map { data =>
							TestCases(
								id = data._1, shortname = data._3, fullname = data._4, version = "",
								authors = None, originalDate = None, modificationDate = None,
								description = data._5, keywords = None, testCaseType = TestCaseType.CONFORMANCE.ordinal().toShort,
								path = "", testSuiteOrder = data._10, hasDocumentation = data._11, documentation = None,
								identifier = data._2, isOptional = data._7, isDisabled = data._8, tags = data._9,
								specReference = data._12, specDescription = data._13, specLink = data._14,
								group = data._6
							)
						}
					}
				resultCount <- query.size.result
			} yield SearchResult(results, resultCount)
		}
	}

	def getTestSuiteWithTestCaseData(testSuiteId: Long): Future[TestSuite] = {
		DB.run(
			for {
				testCaseGroups <- PersistenceSchema.testCaseGroups.filter(_.testSuite === testSuiteId).result
				testSuite <- PersistenceSchema.testSuites
					.filter(_.id === testSuiteId)
					.result
					.head
					.map { result =>
						new TestSuite(
							testSuite = result,
							actors = None,
							testCases = None,
							testCaseGroups = Some(testCaseGroups.toList)
						)
					}
			} yield testSuite
		)
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

	def cancelPendingTestSuiteActions(pendingTestSuiteIdentifier: String, domainId: Long, sharedTestSuite: Boolean): Future[TestSuiteUploadResult] = {
		applyPendingTestSuiteActions(pendingTestSuiteIdentifier, TestSuiteReplacementChoice.CANCEL, domainId, sharedTestSuite, List.empty)
	}

	def completePendingTestSuiteActions(pendingTestSuiteIdentifier: String, domainId: Long, sharedTestSuite: Boolean, actions: List[TestSuiteDeploymentAction]): Future[TestSuiteUploadResult] = {
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
							stepSaveTestSuiteAndTestCases(suite.toCaseObject, existingTestSuiteId, testCases, testSuitePathsToUse, suite.testCaseGroups, actions.find(_.specification.isEmpty).get)
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
							result <- saveTestSuite(savedSharedTestSuite, specAction.specification.get, suite, existingTestSuite, specAction, testSuiteActors, testCases, tempTestSuiteArchive, onSuccessCalls, onFailureCalls)
						} yield result._2)
					}
				}
				mergeActionsWithResults(dbActions.toList)
			}
		} yield results
	}

	private def applyPendingTestSuiteActions(pendingTestSuiteIdentifier: String, overallAction: TestSuiteReplacementChoice, domainId: Long, sharedTestSuite: Boolean, actions: List[TestSuiteDeploymentAction]): Future[TestSuiteUploadResult] = {
		val pendingTestSuiteFolder = new File(repositoryUtils.getPendingFolder(), pendingTestSuiteIdentifier)
		val task = for {
			result <- {
				if (overallAction == TestSuiteReplacementChoice.PROCEED) {
					if (pendingTestSuiteFolder.exists()) {
						val pendingFiles = pendingTestSuiteFolder.listFiles()
						if (pendingFiles.length == 1) {
							repositoryUtils.getTestSuiteFromZip(domainId, None, pendingFiles.head).flatMap { testSuite =>
								// Sanity check
								if (testSuite.isDefined && testSuite.get.testCases.isDefined) {
									testSuite.get.shared = sharedTestSuite
									val onSuccessCalls = mutable.ListBuffer[() => _]()
									val onFailureCalls = mutable.ListBuffer[() => _]()
									val dbAction = processTestSuite(testSuite.get, testSuite.get.actors, testSuite.get.testCases, Some(pendingFiles.head), actions, onSuccessCalls, onFailureCalls)
									DB.run(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), dbAction).transactionally).map { items =>
										TestSuiteUploadResult.success(items)
									}
								} else {
									Future.successful {
										TestSuiteUploadResult.failure("The pending test suite archive [" + pendingTestSuiteIdentifier + "] was corrupted ")
									}
								}
							}
						} else {
							Future.successful {
								TestSuiteUploadResult.failure("A single pending test suite archive was expected but found " + pendingFiles.length)
							}
						}
					} else {
						Future.successful {
							TestSuiteUploadResult.failure("The pending test suite ["+pendingTestSuiteIdentifier+"] could not be located")
						}
					}
				} else {
					Future.successful {
						TestSuiteUploadResult.success()
					}
				}
			}
		} yield result
		task.recover {
			case e:Exception =>
				logger.error("An error occurred", e)
				TestSuiteUploadResult.failure(e.getMessage)
		}.andThen { _ =>
			// Delete temporary folder (if exists)
			FileUtils.deleteDirectory(pendingTestSuiteFolder)
		}
	}

	private def validateTestSuite(domain: Option[Long], specifications: Option[List[Long]], tempTestSuiteArchive: File): Future[TAR] = {
		for {
			// Domain parameters.
			domainId <- {
				if (domain.isDefined) {
					Future.successful(domain.get)
				} else if (specifications.isDefined && specifications.get.nonEmpty) {
					DB.run(getDomainIdForSpecification(specifications.get.head))
				} else {
					throw new IllegalArgumentException("Either the domain ID or a specification ID must be provided")
				}
			}
			parameterSet <- domainParameterManager.getDomainParameters(domainId).map(_.map(p => p.name).toSet)
			// Actor IDs (if multiple specs these are ones in common).
			actorIdSet <- {
				if (specifications.isDefined) {
					actorManager.getActorIdsOfSpecifications(specifications.get).map { specActorIds =>
						var actorIdSet: Option[mutable.Set[String]] = None
						specActorIds.values.foreach { ids =>
							if (actorIdSet.isEmpty) {
								actorIdSet = Some(mutable.Set[String]())
								actorIdSet.get ++= ids
							} else {
								actorIdSet = Some(actorIdSet.get & ids)
							}
						}
						actorIdSet.map(_.toSet)
					}
				} else {
					Future.successful(None)
				}
			}
			report <- {
				import scala.jdk.CollectionConverters._
				val report = TestSuiteValidationAdapter.getInstance().doValidation(new FileSource(tempTestSuiteArchive), actorIdSet.getOrElse(mutable.Set[String]()).toSet.asJava, parameterSet.asJava, repositoryUtils.getTmpValidationFolder().getAbsolutePath)
				Future.successful(report)
			}
		} yield report
	}

	private def testSuiteDefinesExistingActors(specification: Long, actorIdentifiers: List[String]): DBIO[Int] = {
		PersistenceSchema.actors
		  .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
		  .filter(_._2.specId === specification)
		  .filter(_._1.actorId inSet actorIdentifiers)
			.size
			.result
	}

	def deployTestSuiteFromApi(domainId: Long, specificationId: Option[Long], deployRequest: TestSuiteDeployRequest, tempTestSuiteArchive: File): Future[TestSuiteUploadResultWithApiKeys] = {
		val specificationIds = if (specificationId.isDefined) {
			Some(List(specificationId.get))
		} else {
			None
		}
		for {
			validationReport <- validateTestSuite(Some(domainId), specificationIds, tempTestSuiteArchive)
			needsConfirmation <- {
				val hasErrors = validationReport.getCounters.getNrOfErrors.longValue() > 0
				val hasWarnings = validationReport.getCounters.getNrOfWarnings.longValue() > 0
				Future.successful(hasErrors || (hasWarnings && !deployRequest.ignoreWarnings))
			}
			result <- {
				if (needsConfirmation) {
					Future.successful {
						TestSuiteUploadResultWithApiKeys(result = TestSuiteUploadResult.confirm(validationReport))
					}
				} else {
					// Go ahead
					repositoryUtils.getTestSuiteFromZip(domainId, specificationId, tempTestSuiteArchive, completeParse = true).flatMap { testSuite =>
						if (testSuite.isDefined) {
							// Apply update approach settings based on input parameters and the metadata from the test suite archive.
							val deployRequestToUse = applyUpdateMetadataToDeployRequest(testSuite.get, deployRequest)
							testSuite.get.shared = deployRequestToUse.sharedTestSuite
							val sharedTestSuiteCheck = if (!deployRequestToUse.sharedTestSuite) {
								// Check to see if we have an update case that must be skipped.
								DB.run(checkTestSuiteExists(List(specificationId.get), testSuite.get, Some(true))).map(_.nonEmpty).map { sharedTestSuiteExists =>
									if (sharedTestSuiteExists) {
										Some(TestSuiteUploadResult.sharedTestSuiteExists(List((specificationId.get, true)), validationReport))
									} else {
										None
									}
								}
							} else {
								Future.successful(None)
							}
							sharedTestSuiteCheck.flatMap { result =>
								if (result.isEmpty) {
									for {
										uploadResult <- {
											val onSuccessCalls = mutable.ListBuffer[() => _]()
											val onFailureCalls = mutable.ListBuffer[() => _]()
											val testCaseChoices = if (testSuite.get.testCases.isDefined) {
												Some(testSuite.get.testCases.get.map { testCase =>
													// If we have specific settings for the test case use them. Otherwise, consider the test suite overall defaults.
													deployRequestToUse.testCaseUpdates.getOrElse(testCase.identifier, new TestCaseDeploymentAction(testCase.identifier, deployRequestToUse.updateSpecification, deployRequestToUse.replaceTestHistory))
												})
											} else {
												None
											}
											val updateSpecification = deployRequestToUse.updateSpecification.getOrElse(false)
											val updateActions = List(new TestSuiteDeploymentAction(specificationId, TestSuiteReplacementChoice.PROCEED, updateTestSuite = updateSpecification, updateActors = Some(updateSpecification), deployRequestToUse.sharedTestSuite, testCaseUpdates = testCaseChoices))
											val action = processTestSuite(testSuite.get, testSuite.get.actors, testSuite.get.testCases, Some(tempTestSuiteArchive), updateActions, onSuccessCalls, onFailureCalls)
											DB.run(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), action).transactionally).map { items =>
												TestSuiteUploadResult.success(items, validationReport)
											}
										}
										// Lookup the API key information to return.
										resultWithIdentifiers <- {
											collectSpecificationIdentifiers(testSuite.get.domain, specificationIds, testSuite.get.identifier).map { specIdentifiers =>
												TestSuiteUploadResultWithApiKeys(
													testSuiteIdentifier = Some(testSuite.get.identifier),
													testCaseIdentifiers = testSuite.get.testCases.map(_.map(_.identifier)),
													specifications = specIdentifiers,
													result = uploadResult
												)
											}
										}
									} yield resultWithIdentifiers
								} else {
									Future.successful {
										TestSuiteUploadResultWithApiKeys(result = result.get)
									}
								}
							}
						} else {
							throw new IllegalArgumentException("Test suite could not be read from archive")
						}
					}
				}
			}
		} yield result
	}

	private def collectActorIdentifiersWrapper(specificationId: Long, testSuiteId: Long): Future[List[KeyValueRequired]] = {
		DB.run(collectActorIdentifiers(specificationId, testSuiteId))
	}

	private def collectActorIdentifiers(specificationId: Long, testSuiteId: Long): DBIO[List[KeyValueRequired]] = {
		PersistenceSchema.actors
			.join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
			.join(PersistenceSchema.testSuiteHasActors).on(_._2.actorId === _.actor)
			.filter(_._1._2.specId === specificationId)
			.filter(_._2.testsuite === testSuiteId)
			.map(x => (x._1._1.actorId, x._1._1.apiKey))
			.result
			.map(_.map(x => KeyValueRequired(x._1, x._2)).toList)
	}

	private def collectSpecificationIdentifiers(domainId: Long, specificationIds: Option[List[Long]], testSuiteIdentifier: String): Future[Option[List[SpecificationActorApiKeys]]] = {
		for {
			// Get the specification IDs to lookup.
			specificationIdsToUse <- {
				if (specificationIds.isDefined) {
					// This is a deployment for a specification.
					Future.successful(specificationIds)
				} else {
					// This is a deployment for a shared test suite. It could already be linked to specifications.
					DB.run(
						PersistenceSchema.testSuites
							.join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
							.filter(_._1.domain === domainId)
							.filter(_._1.identifier === testSuiteIdentifier)
							.filter(_._1.shared === true)
							.map(_._2.specId)
							.result
					).map(Some(_))
				}
			}
			identifiers <- {
				if (specificationIdsToUse.exists(_.nonEmpty)) {
					val specTasks = specificationIdsToUse.get.map { specId =>
						DB.run(
							for {
								specificationInfo <- PersistenceSchema.specifications
									.filter(_.id === specId)
									.map(x => (x.fullname, x.apiKey))
									.result
									.head
								testSuiteId <- PersistenceSchema.testSuites
									.join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
									.filter(_._1.identifier === testSuiteIdentifier)
									.filter(_._2.specId === specId)
									.map(_._1.id)
									.result
									.head
								actorInfo <- collectActorIdentifiers(specId, testSuiteId)
							} yield (specificationInfo, actorInfo)
						).map { specResults =>
							val specKeys = new SpecificationActorApiKeys
							specKeys.specificationName = specResults._1._1
							specKeys.specificationApiKey = specResults._1._2
							if (specResults._2.nonEmpty) {
								specKeys.actors = Some(specResults._2)
							} else {
								specKeys.actors = None
							}
							specKeys
						}
					}
					Future.sequence(specTasks).map { specsKeys =>
						val specs = new ListBuffer[SpecificationActorApiKeys]
						specsKeys.foreach { specKeys =>
							specs += specKeys
						}
						if (specs.nonEmpty) {
							Some(specs.toList)
						} else {
							None
						}
					}
				} else {
					Future.successful(None)
				}
			}
		} yield identifiers
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
			testSuiteDefinition.testCaseUpdateApproach.get.foreach { updateInfoFromDefinition =>
				val updateInfoFromRequest  = settingsToUse.testCaseUpdates.get(updateInfoFromDefinition._1)
				if (updateInfoFromRequest.isEmpty) {
					// No update info found in request.
					settingsToUse.testCaseUpdates += (updateInfoFromDefinition._1 -> new TestCaseDeploymentAction(
						updateInfoFromDefinition._1,
						Some(updateInfoFromDefinition._2.isUpdateMetadata),
						Some(updateInfoFromDefinition._2.isResetTestHistory)
					))
				} else {
					// We have update info in the request, but we may not have for the specific settings in question.
					if (updateInfoFromRequest.get.updateDefinition.isEmpty) {
						updateInfoFromRequest.get.updateDefinition = Some(updateInfoFromDefinition._2.isUpdateMetadata)
					}
					if (updateInfoFromRequest.get.resetTestHistory.isEmpty) {
						updateInfoFromRequest.get.resetTestHistory = Some(updateInfoFromDefinition._2.isResetTestHistory)
					}
				}
			}
		}
		settingsToUse
	}

	def deployTestSuiteFromZipFile(domainId: Long, specifications: Option[List[Long]], sharedTestSuite: Boolean, tempTestSuiteArchive: File): Future[TestSuiteUploadResult] = {
		val task = for {
			validationReport <- validateTestSuite(Some(domainId), specifications, tempTestSuiteArchive)
			result <- {
				if (validationReport.getResult == TestResultType.SUCCESS) {
					val hasErrors = validationReport.getCounters.getNrOfErrors.intValue() > 0
					val hasReportItems = hasErrors ||
						(validationReport.getCounters.getNrOfWarnings.intValue() > 0) ||
						(validationReport.getCounters.getNrOfAssertions.intValue() > 0)
					// If we have messages we don't need to make a full parse (i.e. test cases and documentation).
					val specificationIdToUse = specifications.getOrElse(List()).headOption
					repositoryUtils.getTestSuiteFromZip(domainId, specificationIdToUse, tempTestSuiteArchive, !hasErrors).flatMap { testSuite =>
						if (testSuite.isDefined) {
							val updateMetadata = testSuite.exists(_.updateApproach.exists(_.isUpdateMetadata))
							val updateSpecification = testSuite.exists(_.updateApproach.exists(_.isUpdateSpecification))
							val definedActorsIdentifiers = testSuite.flatMap(_.actors.map(_.map(actor => actor.actorId)))
							if (sharedTestSuite) {
								// Lookup a matching test suite and its test cases.
								DB.run (
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
								).map { lookupResult =>
									val linkedSpecifications = new ListBuffer[(Long, Boolean)]
									lookupResult._3.foreach { specId =>
										linkedSpecifications += ((specId, true))
									}
									TestSuiteUploadResult(
										validationReport = Some(validationReport),
										updateMetadata = updateMetadata,
										updateSpecification = updateSpecification,
										sharedTestSuiteId = lookupResult._1,
										sharedTestCases = Some(getTestCaseUploadStatus(testSuite.get, lookupResult._2)),
										needsConfirmation = hasReportItems || lookupResult._1.isDefined,
										existsForSpecs = Some(linkedSpecifications.toList),
										testSuite = testSuite
									)
								}
							} else {
								// Check for existing specification data, test suites and test cases.
								DB.run(checkTestSuiteExists(specifications.get, testSuite.get, None)).flatMap { existingTestSuites =>
									Future.sequence {
										specifications.get.map { specification =>
											val existingTestSuiteInfo = existingTestSuites.get(specification)
											val checkTestCases = existingTestSuiteInfo.exists(x => !x.shared)
											DB.run(
												for {
													existingTestCases <- {
														if (checkTestCases) {
															// We match an existing test suite (that is not a shared one).
															getExistingTestCasesForTestSuite(existingTestSuiteInfo.get.id)
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
												} yield (existingTestCases, existingActors)
											).map(x => TestCasesForTestSuiteWithActorCount(specification, x._1, x._2))
										}
									}.map { specsResults =>
										val specsWithExistingTestSuite = new ListBuffer[(Long, Boolean)]()
										val specsWithMatchingData = new ListBuffer[Long]()
										val specTestCases = new mutable.HashMap[Long, List[TestSuiteUploadTestCase]]()
										var needsConfirmation = hasReportItems
										specsResults.foreach { specResults =>
											val existingTestSuiteInfo = existingTestSuites.get(specResults.specification)
											val checkTestCases = existingTestSuiteInfo.exists(x => !x.shared)
											if (existingTestSuiteInfo.isDefined) {
												// The archive's test suite exists in the DB
												specsWithExistingTestSuite += ((specResults.specification, existingTestSuiteInfo.get.shared))
												needsConfirmation = true
												// Record the information on the included test cases
												if (checkTestCases) {
													specTestCases += (specResults.specification -> getTestCaseUploadStatus(testSuite.get, specResults.testCases))
												}
											}
											if (specResults.actorCount > 0) {
												// The archive's test suite defines actors existing in the DB
												specsWithMatchingData += specResults.specification
												needsConfirmation = true
											}
										}
										if (needsConfirmation) {
											TestSuiteUploadResult(
												validationReport = Some(validationReport),
												updateMetadata = updateMetadata,
												updateSpecification = updateSpecification,
												needsConfirmation = needsConfirmation,
												existsForSpecs = if (specsWithExistingTestSuite.nonEmpty) Some(specsWithExistingTestSuite.toList) else None,
												testCases = if (specsWithExistingTestSuite.nonEmpty && specTestCases.nonEmpty) Some(specTestCases.toMap) else None,
												matchingDataExists = if (specsWithMatchingData.nonEmpty) Some(specsWithMatchingData.toList) else None,
												testSuite = testSuite
											)
										} else {
											TestSuiteUploadResult(
												validationReport = Some(validationReport),
												updateMetadata = updateMetadata,
												updateSpecification = updateSpecification,
												needsConfirmation = needsConfirmation,
												testSuite = testSuite
											)
										}
									}
								}
							}
						} else {
							Future.successful {
								TestSuiteUploadResult(
									validationReport = Some(validationReport),
									needsConfirmation = hasReportItems
								)
							}
						}
					}
				} else {
					Future.successful {
						TestSuiteUploadResult(
							validationReport = Some(validationReport),
							needsConfirmation = true
						)
					}
				}
			}
			result <- {
				if (result.needsConfirmation) {
					// Park the test suite for now and ask user what to do
					FileUtils.moveDirectoryToDirectory(tempTestSuiteArchive.getParentFile, repositoryUtils.getPendingFolder(), true)
					Future.successful {
						result.withPendingFolder(tempTestSuiteArchive.getParentFile.getName)
					}
				} else {
					// Proceed immediately.
					val onSuccessCalls = mutable.ListBuffer[() => _]()
					val onFailureCalls = mutable.ListBuffer[() => _]()
					val testSuite = result.testSuite.get
					testSuite.domain = domainId
					testSuite.shared = sharedTestSuite
					val actions = if (sharedTestSuite) {
						List(new TestSuiteDeploymentAction(None, PROCEED, updateTestSuite = true, updateActors = None, sharedTestSuite = true, testCaseUpdates = None))
					} else if (specifications.isDefined && specifications.nonEmpty) {
						specifications.get.map { spec =>
							new TestSuiteDeploymentAction(Some(spec), PROCEED, updateTestSuite = true, updateActors = Some(true), sharedTestSuite = false, testCaseUpdates = None)
						}
					} else {
						throw new IllegalArgumentException("Test suite not shared and no specifications provided")
					}
					DB.run(
						processTestSuite(testSuite, testSuite.actors, testSuite.testCases, Some(tempTestSuiteArchive), actions, onSuccessCalls, onFailureCalls)
					).map { items =>
						result.withItems(items)
					}
				}
			}
		} yield result
		task.recover {
			case e:Exception =>
				logger.error("An error occurred", e)
				TestSuiteUploadResult.failure(e.getMessage)
		}.andThen { _ =>
			// Delete temporary folder (if exists)
			FileUtils.deleteDirectory(tempTestSuiteArchive.getParentFile)
		}
	}

	private def getTestCaseUploadStatus(testSuiteDefinition: TestSuite, testCasesInDb: List[TestCaseForTestSuite]): List[TestSuiteUploadTestCase] = {
		// (Test case ID, test case identifier, test case name)
		val testCasesInDB = new mutable.HashMap[String, String]() // Map of identifiers to names
		testCasesInDb.foreach { testCase =>
			testCasesInDB += (testCase.identifier -> testCase.fullName)
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

	private def checkTestSuiteExists(specificationIds: List[Long], suite: TestSuite, shared: Option[Boolean]): DBIO[Map[Long, TestSuiteSharedInfo]] = { // Specification ID to test suite ID and shared flag
		for {
			testSuiteInfo <- PersistenceSchema.testSuites
				.join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
				.filter(_._1.identifier === suite.identifier)
				.filter(_._2.specId inSet specificationIds)
				.filterOpt(shared)((q, isShared) => q._1.shared === isShared)
				.map(x => (x._2.specId, x._2.testSuiteId, x._1.shared))
				.result
			results <- {
				val map = new mutable.HashMap[Long, TestSuiteSharedInfo]()
				testSuiteInfo.foreach { info =>
					map += (info._1 -> TestSuiteSharedInfo(info._2, info._3))
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

	def updateTestSuiteMetadata(testSuiteId: Long, name: String, description: Option[String], documentation: Option[String], version: String, specReference: Option[String], specDescription: Option[String], specLink: Option[String]): Future[Unit] = {
		var hasDocumentationToSet = false
		var documentationToSet: Option[String] = None
		if (documentation.isDefined && !documentation.get.isBlank) {
			hasDocumentationToSet = true
			documentationToSet = documentation
		}
		val q1 = for {
			_ <- PersistenceSchema.testSuites
				.filter(_.id === testSuiteId)
				.map(t => (t.shortname, t.fullname, t.description, t.documentation, t.hasDocumentation, t.version, t.specReference, t.specDescription, t.specLink))
				.update(name, name, description, documentationToSet, hasDocumentationToSet, version, specReference, specDescription, specLink)
			_ <- testResultManager.updateForUpdatedTestSuite(testSuiteId, name)
		} yield ()
		DB.run(q1.transactionally)
	}

	private def updateTestSuiteInDbWithoutMetadata(testSuiteId: Long, newData: TestSuites): DBIO[_] = {
		val q1 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.filename, t.hidden)
		q1.update(newData.filename, newData.hidden)
	}

	private def updateTestSuiteInDb(testSuiteId: Long, newData: TestSuites): DBIO[_] = {
		for {
			_ <- {
				val q1 = for {t <- PersistenceSchema.testSuites if t.id === testSuiteId} yield (t.identifier, t.shortname, t.fullname, t.version, t.authors, t.keywords, t.description, t.filename, t.hasDocumentation, t.documentation, t.hidden, t.specReference, t.specDescription, t.specLink)
				q1.update(newData.identifier, newData.shortname, newData.fullname, newData.version, newData.authors, newData.keywords, newData.description, newData.filename, newData.hasDocumentation, newData.documentation, newData.hidden, newData.specReference, newData.specDescription, newData.specLink)
			}
			_ <- testResultManager.updateForUpdatedTestSuite(testSuiteId, newData.shortname)
		} yield ()
	}

	private def theSameActor(one: Actors, two: Actors): Boolean = {
		(Objects.equals(one.name, two.name)
				&& Objects.equals(one.description, two.description)
				&& Objects.equals(one.reportMetadata, two.reportMetadata)
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

	def stepSaveTestSuiteAndTestCases(suite: TestSuites, existingSuiteId: Option[Long], testCases: Option[List[TestCases]], testSuitePaths: TestSuitePaths, testCaseGroups: Option[List[TestCaseGroup]], updateActions: TestSuiteDeploymentAction): DBIO[TestSuiteSaveResult] = {
		for {
			// Save the test suite (insert or update) and return the identifier to use.
			testSuiteId <- stepSaveTestSuite(suite, existingSuiteId, updateActions)
			// Save test case groups.
			testCaseGroups <- for {
				// Remove previous groupings
				_ <- if (existingSuiteId.isDefined) {
					for {
						existingTestCaseIds <- PersistenceSchema.testSuiteHasTestCases
							.filter(_.testsuite === existingSuiteId.get)
							.map(_.testcase)
							.result
						_ <- PersistenceSchema.testCases.filter(_.id inSet existingTestCaseIds).filter(_.group.isDefined).map(_.group).update(None)
						_ <- PersistenceSchema.testCaseGroups.filter(_.testSuite === existingSuiteId).delete
					} yield ()
				} else {
					DBIO.successful(())
				}
				// Add new test case groups
				testCaseGroups <- if (testCaseGroups.isDefined) {
					var combinedAction: DBIO[Option[mutable.HashMap[Long, Long]]] = DBIO.successful(Some(mutable.HashMap.empty)) // Temp group ID to persisted group ID
					testCaseGroups.get.foreach { group =>
						combinedAction = combinedAction.flatMap { savedGroupIds =>
							(PersistenceSchema.testCaseGroups.returning(PersistenceSchema.testCaseGroups.map(_.id)) += group.withIds(0L, testSuiteId)).flatMap { newGroupId =>
								DBIO.successful(Some(savedGroupIds.get += (group.id -> newGroupId)))
							}
						}
					}
					combinedAction
				} else {
					DBIO.successful(None)
				}
			} yield testCaseGroups
			// Save the test cases.
			stepSaveTestCases <- stepSaveTestCases(testSuiteId, existingSuiteId.isDefined, testCases, testSuitePaths.testCasePaths, testCaseGroups.map(_.toMap), updateActions)
		} yield TestSuiteSaveResult(suite.withId(testSuiteId), stepSaveTestCases._1, stepSaveTestCases._2, testSuitePaths)
	}

	private def stepSaveTestSuite(suite: TestSuites, existingSuiteId: Option[Long], updateActions: TestSuiteDeploymentAction): DBIO[Long] = {
		for {
			// Test suite update.
			testSuiteId <- if (existingSuiteId.isDefined) {
				// Update existing test suite.
				for {
					_ <- {
						if (updateActions.updateTestSuite) {
							updateTestSuiteInDb(existingSuiteId.get, suite)
						} else {
							updateTestSuiteInDbWithoutMetadata(existingSuiteId.get, suite)
						}
					}
					idToUse <- DBIO.successful(existingSuiteId.get)
				} yield idToUse
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
							updateAction = Some(actorManager.updateActor(existingActor.get.id, actorToSave.actorId, actorToSave.name, actorToSave.description, actorToSave.reportMetadata, actorToSave.default, actorToSave.hidden, actorToSave.displayOrder, specificationId, None, checkApiKeyUniqueness = false, None, onSuccessCalls))
							result += new TestSuiteUploadItemResult(existingActor.get.name, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_UPDATE, specificationId)
						}
						savedActorId = DBIO.successful(existingActor.get.id)
						savedActorStringId = existingActor.get.actorId
					} else {
						if (isActorReference(actorToSave)) {
							throw new IllegalStateException("Actor reference [" + actorToSave.actorId + "] not found in specification")
						} else {
							// New actor.
							savedActorId = actorManager.createActor(actorToSave, specificationId, checkApiKeyUniqueness = false, None, onSuccessCalls)
							savedActorStringId = actorToSave.actorId
						}
						result += new TestSuiteUploadItemResult(actorToSave.actorId, TestSuiteUploadItemResult.ITEM_TYPE_ACTOR, TestSuiteUploadItemResult.ACTION_TYPE_ADD, specificationId)
					}
					val combinedAction = for {
						_ <- updateAction.getOrElse(DBIO.successful(()))
						id <- savedActorId
						endpointResults <- {
							savedActorIds.put(savedActorStringId, id)
							for {
								existingEndpoints <- PersistenceSchema.endpoints.filter(_.actor === id).result.map(_.toList)
								endpointResults <- stepSaveEndpoints(specificationId, id, testSuiteActor, actorToSave, existingEndpoints, updateActions, onSuccessCalls)
							} yield endpointResults
						}
					} yield endpointResults
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
				val combinedAction = for {
					_ <- updateAction.getOrElse(DBIO.successful(()))
					id <- endpointId
					parameterResults <- {
						for {
							existingEndpointParameters <- PersistenceSchema.parameters.filter(_.endpoint === id.longValue()).result.map(_.toList)
							parameterResults <- stepSaveParameters(specificationId, id, endpoint, actorToSave, existingEndpointParameters, updateActions, onSuccessCalls)
						} yield parameterResults
					}
				} yield parameterResults
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
						action = Some(parameterManager.updateParameter(parameterId, parameter.name, parameter.testKey, parameter.desc, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue, parameter.defaultValue, None, onSuccessCalls))
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
		combinedAction.map(_ => result.toList)
	}

	private def stepSaveTestCases(savedTestSuiteId: Long, testSuiteExists: Boolean, testCases: Option[List[TestCases]], resourcePaths: Map[String, String], testCaseGroups: Option[Map[Long, Long]], updateActions: TestSuiteDeploymentAction): DBIO[(Map[String, (Long, Boolean)], List[String])] = {
		for {
			// Lookup the existing test cases for the test suite (if it already exists).
			existingTestCasesForTestSuite <- if (testSuiteExists) {
				getExistingTestCasesForTestSuite(savedTestSuiteId)
			} else {
				DBIO.successful(List[TestCaseForTestSuite]())
			}
			// Place the existing test cases in a map for further processing.
			existingTestCaseMap <- getExistingTestCaseMap(existingTestCasesForTestSuite)
			// Insert or update the test cases.
			testCaseIds <- {
				var combinedAction: DBIO[mutable.HashMap[String, (Long, Boolean)]] = DBIO.successful(mutable.HashMap.empty) // Test case identifier to (Test case ID and isNew flag)
				for (testCase <- testCases.get) {
					val testCaseToStore = testCase.withPathAndGroup(resourcePaths(testCase.identifier), testCaseGroups.flatMap(x => testCase.group.flatMap(x.get)))
					val existingTestCaseInfo = existingTestCaseMap.get(testCase.identifier)
					if (existingTestCaseInfo.isDefined) {
						val existingTestCaseId: Long = existingTestCaseInfo.get._1
						// Test case already exists - update.
						existingTestCaseMap.remove(testCase.identifier)
						// Update test case metadata
						if (updateActions.updateTestCaseMetadata(testCase.identifier)) {
							combinedAction = combinedAction.flatMap { savedTestCaseIds =>
								for {
									_ <- testCaseManager.updateTestCase(
										existingTestCaseId, testCaseToStore.identifier, testCaseToStore.shortname, testCaseToStore.fullname,
										testCaseToStore.version, testCaseToStore.authors, testCaseToStore.description,
										testCaseToStore.keywords, testCaseToStore.testCaseType, testCaseToStore.path, testCaseToStore.testSuiteOrder,
										testCaseToStore.targetActors.get, testCaseToStore.documentation.isDefined, testCaseToStore.documentation,
										testCaseToStore.isOptional, testCaseToStore.isDisabled, testCaseToStore.tags,
										testCaseToStore.specReference, testCaseToStore.specDescription, testCaseToStore.specLink,
										testCaseToStore.group)
									savedTestCaseIds <- {
										savedTestCaseIds += (testCaseToStore.identifier -> (existingTestCaseId, false))
										DBIO.successful(savedTestCaseIds)
									}
								} yield savedTestCaseIds
							}
						} else {
							combinedAction = combinedAction.flatMap { savedTestCaseIds =>
								for {
									_ <- testCaseManager.updateTestCaseWithoutMetadata(
										existingTestCaseId, testCaseToStore.path, testCaseToStore.testSuiteOrder, testCaseToStore.targetActors.get)
									savedTestCaseIds <- {
										savedTestCaseIds += (testCaseToStore.identifier -> (existingTestCaseId, false))
										DBIO.successful(savedTestCaseIds)
									}
								} yield savedTestCaseIds
							}
						}
						// Update test history
						if (updateActions.resetTestCaseHistory(testCase.identifier)) {
							combinedAction = for {
								results <- combinedAction
								_ <- testResultManager.updateForDeletedTestCase(existingTestCaseId)
								_ <- PersistenceSchema.conformanceResults
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

	private def getExistingTestCasesForTestSuite(testSuiteId: Long): DBIO[List[TestCaseForTestSuite]] = {
		PersistenceSchema.testCases
			.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
			.filter(_._2.testsuite === testSuiteId)
			.map(r => (r._1.id, r._1.identifier, r._1.fullname))
			.result
			.map(_.map(x => TestCaseForTestSuite(x._1, x._2, x._3)).toList)
	}

	private def getExistingTestCaseMap(existingTestCasesForTestSuite: List[TestCaseForTestSuite]): DBIO[mutable.HashMap[String, (java.lang.Long, String)]] = {
		// Process test cases.
		val existingTestCaseMap = new mutable.HashMap[String, (java.lang.Long, String)]()
		if (existingTestCasesForTestSuite.nonEmpty) {
			// This is an update - check for existing test cases.
			for (existingTestCase <- existingTestCasesForTestSuite) {
				existingTestCaseMap += (existingTestCase.identifier -> (existingTestCase.id, existingTestCase.fullName))
			}
		}
		DBIO.successful(existingTestCaseMap)
	}

	private def createTestSuitePaths(domainId: Long, testSuiteFolderName: String, tempTestSuiteArchive: File) = {
		val targetFolder: File = repositoryUtils.getTestSuitePath(domainId, testSuiteFolderName)
		val resourcePaths = repositoryUtils.extractTestSuiteFilesFromZipToFolder(targetFolder, tempTestSuiteArchive)
		TestSuitePaths(targetFolder, resourcePaths._1, resourcePaths._2)
	}

	private def saveTestSuite(sharedTestSuiteInfo: Option[TestSuiteSaveResult], specificationId: Long, tempSuite: TestSuite, existingSuite: Option[TestSuites], updateActions: TestSuiteDeploymentAction, testSuiteActors: Option[List[Actor]], testCases: Option[List[TestCases]], tempTestSuiteArchive: Option[File], onSuccessCalls: mutable.ListBuffer[() => _], onFailureCalls: mutable.ListBuffer[() => _]): DBIO[(TestSuiteSaveResult, List[TestSuiteUploadItemResult])] = {
		// Reuse a previously saved test suite (if applicable).
		var suite = if (sharedTestSuiteInfo.isDefined) {
			sharedTestSuiteInfo.get.testSuite
		} else {
			// Create a new test suite folder name.
			tempSuite.toCaseObject.withFileName(repositoryUtils.generateTestSuiteFileName())
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
					stepSaveTestSuiteAndTestCases(suite, existingTestSuiteId, testCases, testSuitePathsToUse, tempSuite.testCaseGroups, updateActions)
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
					// Clean-up operations in case an error occurred (we only do this here for non-shared test suites).
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

	def getTestSuiteDocumentation(testSuiteId: Long): Future[Option[String]] = {
		DB.run(PersistenceSchema.testSuites.filter(_.id === testSuiteId).map(x => x.documentation).result.headOption).map(_.flatten)
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

	def searchTestSuites(domainIds: Option[List[Long]], specificationIds: Option[List[Long]], specificationGroupIds: Option[List[Long]], actorIds: Option[List[Long]]): Future[Iterable[TestSuite]] = {
		DB.run(
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
	}

	def prepareSharedTestSuiteLink(testSuiteId: Long, specificationIds: List[Long]): Future[TestSuiteUploadResult] = {
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
					linkSharedTestSuite(Some(testSuiteWithActors), testSuiteId, actions, onSuccessCalls, onFailureCalls).map { linkResults =>
						TestSuiteUploadResult.success(linkResults)
					}
				} else {
					// We need confirmations.
					DBIO.successful {
						TestSuiteUploadResult(
							needsConfirmation = true,
							matchingDataExists = Some(specificationsWithMatchingActors.toList),
							existsForSpecs = Some(specificationsWithMatchingTestSuite.toList)
						)
					}
				}
			}
		} yield result
		DB.run(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), dbAction).transactionally)
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

	def linkSharedTestSuiteFromApi(testSuiteId: Long, domainId: Long, input: TestSuiteLinkRequest): Future[List[TestSuiteLinkResponseSpecification]] = {
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
					specResults += TestSuiteLinkResponseSpecification(key, Some(id), linked = true, None)
					pendingKeys -= key
				}
				// Specifications that were skipped because they were already linked to the test suite.
				specsLinkedToSameTestSuite.foreach { id =>
					val key = specificationMap(id)
					specResults += TestSuiteLinkResponseSpecification(specificationMap(id), Some(id), linked = false, Some("Specification already linked to this test suite."))
					pendingKeys -= key
				}
				// Specifications that were skipped because they were linked to another test suite.
				specsLinkedToMatchingOtherTestSuite.foreach { id =>
					val key = specificationMap(id)
					specResults += TestSuiteLinkResponseSpecification(specificationMap(id), Some(id), linked = false, Some("Specification linked to another suite with the same identifier."))
					pendingKeys -= key
				}
				// Specifications that were skipped because they were not found.
				pendingKeys.foreach { key =>
					specResults += TestSuiteLinkResponseSpecification(key, None, linked = false, Some("Specification not found."))
				}
				DBIO.successful(specResults.toList)
			}
		} yield result
		DB.run(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), dbAction).transactionally).flatMap { results =>
			Future.sequence {
				results.map { result =>
					if (result.specificationId.isDefined && result.linked) {
						collectActorIdentifiersWrapper(result.specificationId.get, testSuiteId).map { identifiers =>
							result.withActorIdentifiers(identifiers)
						}
					} else {
						Future.successful(result)
					}
				}
			}
		}
	}

	def unlinkSharedTestSuiteFromApi(testSuiteId: Long, domainId: Long, input: TestSuiteUnlinkRequest): Future[Unit] = {
		DB.run(
			for {
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
			} yield ()
		)
	}

	def linkSharedTestSuiteWrapper(testSuiteId: Long, specActions: List[TestSuiteDeploymentAction]): Future[TestSuiteUploadResult] = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val onFailureCalls = mutable.ListBuffer[() => _]()
		val dbAction = linkSharedTestSuite(None, testSuiteId, specActions, onSuccessCalls, onFailureCalls)
		DB.run(dbActionFinalisation(Some(onSuccessCalls), Some(onFailureCalls), dbAction).transactionally).map { resultItems =>
			TestSuiteUploadResult.success(resultItems)
		}
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

	def unlinkSharedTestSuiteInternal(testSuiteId: Long, specificationIds: List[Long]): DBIO[Unit] = {
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

	def unlinkSharedTestSuite(testSuiteId: Long, specificationIds: List[Long]): Future[Unit] = {
		DB.run(unlinkSharedTestSuiteInternal(testSuiteId, specificationIds))
	}

	def undeployTestSuiteWrapper(testSuiteId: Long): Future[Unit] = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val action = undeployTestSuite(testSuiteId, onSuccessCalls)
		DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
	}

	def undeployTestSuite(testSuiteId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Unit] = {
		for {
			_ <- testResultManager.updateForDeletedTestSuite(testSuiteId)
			_ <- PersistenceSchema.specificationHasTestSuites.filter(_.testSuiteId === testSuiteId).delete
  		_ <- PersistenceSchema.testSuiteHasActors.filter(_.testsuite === testSuiteId).delete
			_ <- PersistenceSchema.conformanceSnapshotResults.filter(_.testSuiteId === testSuiteId).map(_.testSuiteId).update(testSuiteId * -1)
			_ <- PersistenceSchema.conformanceSnapshotTestSuites.filter(_.id === testSuiteId).map(_.id).update(testSuiteId * -1)
			_ <- PersistenceSchema.conformanceResults.filter(_.testsuite === testSuiteId).delete
			testCases <- PersistenceSchema.testSuiteHasTestCases.filter(_.testsuite === testSuiteId).map(_.testcase).result
			_ <- DBIO.seq(testCases.map(testCase => {
				for {
					_ <- testResultManager.updateForDeletedTestCase(testCase)
					_ <- testCaseManager.removeActorLinksForTestCase(testCase)
					_ <- PersistenceSchema.testCaseCoversOptions.filter(_.testcase === testCase).delete
					_ <- PersistenceSchema.testSuiteHasTestCases.filter(_.testcase === testCase).delete
					_ <- PersistenceSchema.conformanceSnapshotResults.filter(_.testCaseId === testCase).map(_.testCaseId).update(testCase * -1)
					_ <- PersistenceSchema.conformanceSnapshotTestCases.filter(_.id === testCase).map(_.id).update(testCase * -1)
					_ <- PersistenceSchema.testCases.filter(_.id === testCase).delete
				} yield ()
			}): _*)
			_ <- PersistenceSchema.testCaseGroups.filter(_.testSuite === testSuiteId).delete
			testSuite <- PersistenceSchema.testSuites.filter(_.id === testSuiteId).result.head
			_ <- {
				onSuccessCalls += (() => {
					repositoryUtils.undeployTestSuite(testSuite.domain, testSuite.filename)
				})
				DBIO.successful(())
			}
			_ <- PersistenceSchema.testSuites.filter(_.id === testSuiteId).delete
		} yield ()
	}

	def moveTestSuiteToSpecification(testSuiteId: Long, targetSpecificationId: Long): Future[Unit] = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val query = for {
			// Load and check test suite.
			testSuite <- getByIdInternal(testSuiteId).flatMap { x =>
				if (x.isEmpty) {
					// Test suite was not found.
					DBIO.failed(UserException(ErrorCodes.INVALID_REQUEST , "Test suite not found."))
				} else if (x.get.shared) {
					// Test suite must not be shared.
					DBIO.failed(UserException(ErrorCodes.INVALID_REQUEST, "The test suite to move cannot be a shared test suite."))
				} else {
					DBIO.successful(x.get)
				}
			}
			// Get and check target specification.
			currentSpecificationId <- PersistenceSchema.specificationHasTestSuites
				.filter(_.testSuiteId === testSuiteId)
				.map(_.specId)
				.result
				.head
				.flatMap { x =>
					if (targetSpecificationId == x) {
						// The current and target specification IDs must not be the same.
						DBIO.failed(UserException(ErrorCodes.INVALID_REQUEST, "Test suite is already defined in the target specification."))
					} else {
						DBIO.successful(x)
					}
				}
			// Check the target specification for an already defined test suite with the same ID.
			_ <- PersistenceSchema.specificationHasTestSuites
				.join(PersistenceSchema.testSuites).on(_.testSuiteId === _.id)
				.filter(_._1.specId === targetSpecificationId)
				.filter(_._2.identifier === testSuite.identifier)
				.map(x => x._2.shared)
				.result
				.headOption
				.flatMap { x =>
					if (x.isDefined) {
						if (x.get) {
							DBIO.failed(UserException(ErrorCodes.SHARED_TEST_SUITE_EXISTS, "A shared test suite with the same identifier already exists."))
						} else {
							DBIO.failed(UserException(ErrorCodes.TEST_SUITE_EXISTS, "A suite with the same identifier already exists."))
						}
					} else {
						DBIO.successful(())
					}
				}
			/*
			 * All ok - proceed with the updates.
			 */
			// Load a map of the current specification actors (identifier to actor).
			currentSpecificationActors <- PersistenceSchema.testSuiteHasActors
				.join(PersistenceSchema.actors).on(_.actor === _.id)
				.filter(_._1.testsuite === testSuiteId)
				.map(_._2)
				.result
				.map { actors =>
					actors.map(actor => actor.actorId -> actor).toMap
				}
			// Check to see which (if any) of the current actors are not found in the target specification.
			missingTargetActorIdentifiers <- PersistenceSchema.specificationHasActors
				.join(PersistenceSchema.actors).on(_.actorId === _.id)
				.filter(_._1.specId === targetSpecificationId)
				.map(_._2.actorId)
				.result
				.map { targetIdentifiers =>
					currentSpecificationActors.keySet.removedAll(targetIdentifiers.toSet)
				}
			// Add missing actors to target based on source definitions.
			_ <- {
				DBIO.sequence(
					missingTargetActorIdentifiers.toList.map { missingIdentifier =>
						val missingActor = currentSpecificationActors(missingIdentifier).copy(
							id = 0L,
							apiKey = CryptoUtil.generateApiKey()
						)
						actorManager.createActor(missingActor, targetSpecificationId, checkApiKeyUniqueness = false, None, onSuccessCalls)
					}
				)
			}
			// Load a map of the target specification actors (identifier to actor).
			targetSpecificationActors <- PersistenceSchema.specificationHasActors
				.join(PersistenceSchema.actors).on(_.actorId === _.id)
				.filter(_._1.specId === targetSpecificationId)
				.map(_._2)
				.result
				.map { actors =>
					actors.map(actor => actor.actorId -> actor).toMap
				}
			// Get the test case IDs.
			testCaseIds <- PersistenceSchema.testSuiteHasTestCases
				.filter(_.testsuite === testSuiteId)
				.map(_.testcase)
				.result
			// Get the systems that have conformance statements for the target actors (map of actor ID to system IDs).
			conformanceStatementsToUpdate <- PersistenceSchema.systemImplementsActors
				.filter(_.specId === targetSpecificationId)
				.filter(_.actorId inSet targetSpecificationActors.values.map(_.id))
				.map(x => (x.systemId, x.actorId))
				.result
				.map { results =>
					val actorMap = mutable.HashMap[Long, mutable.HashSet[Long]]()
					results.foreach { mapping =>
						actorMap.getOrElseUpdate(mapping._2, mutable.HashSet[Long]()).add(mapping._1)
					}
					actorMap.view.mapValues(_.toSet).toMap
				}
			// Update specification and actor mappings.
			_ <- {
				val actions = new ListBuffer[DBIO[_]]()
				val currentActorIdToActorMap = currentSpecificationActors.values.map(actor => actor.id -> actor).toMap
				currentActorIdToActorMap.foreach { currentActorEntry =>
					val currentActorId = currentActorEntry._1
					val targetActor = targetSpecificationActors(currentActorEntry._2.actorId)
					val targetActorId = targetActor.id
					// Update specification to test suite mapping
					actions += PersistenceSchema.specificationHasTestSuites
						.filter(_.specId === currentSpecificationId)
						.filter(_.testSuiteId === testSuiteId)
						.map(_.specId)
						.update(targetSpecificationId)
					// Update test suite to actor mapping.
					actions += PersistenceSchema.testSuiteHasActors
						.filter(_.actor === currentActorId)
						.filter(_.testsuite === testSuiteId)
						.map(_.actor)
						.update(targetActorId)
					// Update test case to actor mapping.
					actions += PersistenceSchema.testCaseHasActors
						.filter(_.testcase inSet testCaseIds)
						.filter(_.actor === currentActorId)
						.map(x => (x.actor, x.specification))
						.update((targetActorId, targetSpecificationId))
					// Update test results (we don't update the names of the specifications and actors as these are snapshots in time).
					actions += PersistenceSchema.testResults
						.filter(_.actorId === currentActorId)
						.filter(_.testSuiteId === testSuiteId)
						.map(x => (x.specificationId, x.actorId))
						.update((Some(targetSpecificationId), Some(targetActorId)))
					// Update conformance results for systems conforming to the target actors.
					if (conformanceStatementsToUpdate.contains(targetActorId)) {
						conformanceStatementsToUpdate(targetActorId).foreach { systemId =>
							// System that has a conformance statement for the target actor. Update to reflect the additional results.
							actions += PersistenceSchema.conformanceResults
								.filter(_.testcase inSet testCaseIds)
								.filter(_.actor === currentActorId)
								.filter(_.sut === systemId)
								.map(x => (x.actor, x.spec))
								.update((targetActorId, targetSpecificationId))
						}
					}
					// Remove conformance results for the previous actors (the ones to retain have been updated above).
					actions += PersistenceSchema.conformanceResults
						.filter(_.testcase inSet testCaseIds)
						.filter(_.actor === currentActorId)
						.delete
				}
				toDBIO(actions)
			}
			// Clean up any conformance statements that no longer have related conformance results.
			_ <- {
				for {
					// Get the systems for which we still have conformance results for the previous actors.
					systemsWithConformanceResultsForPreviousActors <- PersistenceSchema.conformanceResults
						.filter(_.actor inSet currentSpecificationActors.values.map(_.id))
						.map(_.sut)
						.distinct
						.result
					// Delete the conformance statements for the previous actors for the systems that no longer have conformance results.
					_ <- PersistenceSchema.systemImplementsActors
						.filter(_.actorId inSet currentSpecificationActors.values.map(_.id))
						.filterNot(_.systemId inSet systemsWithConformanceResultsForPreviousActors)
						.delete
				} yield ()
			}
		} yield ()
		DB.run(dbActionFinalisation(Some(onSuccessCalls), None, query).transactionally)
	}

	def convertNonSharedTestSuiteToShared(testSuiteId: Long): Future[Unit] = {
		DB.run(
			for {
				// Load target test suite.
				testSuiteInfo <- PersistenceSchema.testSuites
					.filter(_.id === testSuiteId)
					.map(x => (x.identifier, x.shared, x.domain))
					.result
					.headOption
					.flatMap { result =>
						if (result.isEmpty) {
							DBIO.failed(UserException(ErrorCodes.INVALID_REQUEST , "Test suite not found."))
						} else if (result.exists(_._2)) {
							DBIO.failed(UserException(ErrorCodes.INVALID_REQUEST , "The test suite to convert must not be already a shared one."))
						} else {
							DBIO.successful((result.get._1, result.get._3)) // (Test suite identifier, Domain ID)
						}
					}
				// Make sure that there is no shared test suite with the same identifier in the same domain.
				_testSuiteExists <- PersistenceSchema.testSuites
					.filter(_.identifier === testSuiteInfo._1)
					.filter(_.domain === testSuiteInfo._2)
					.filter(_.shared === true)
					.exists
					.result
					.flatMap { testSuiteExists =>
						if (testSuiteExists) {
							DBIO.failed(UserException(ErrorCodes.SHARED_TEST_SUITE_EXISTS, "A test suite with the same identifier already exists in the same domain."))
						} else {
							DBIO.successful(())
						}
					}
				// All ok - update the shared flag.
				_ <- PersistenceSchema.testSuites
					.filter(_.id === testSuiteId)
					.map(_.shared)
					.update(true)
			} yield ()
		)
	}

	def convertSharedTestSuiteToNonShared(testSuiteId: Long): Future[Long] = {
		DB.run {
			for {
				// Load and check the target test suite.
				_testSuiteInfo <- PersistenceSchema.testSuites
					.filter(_.id === testSuiteId)
					.map(x => (x.identifier, x.shared))
					.result
					.headOption
					.flatMap { result =>
						if (result.isEmpty) {
							DBIO.failed(UserException(ErrorCodes.INVALID_REQUEST , "Test suite not found."))
						} else if (!result.get._2) {
							DBIO.failed(UserException(ErrorCodes.INVALID_REQUEST , "The test suite to convert must be a shared one."))
						} else {
							DBIO.successful(())
						}
					}
				// Load the currently linked specifications.
				linkedSpecification <- PersistenceSchema.specificationHasTestSuites
					.filter(_.testSuiteId === testSuiteId)
					.map(_.specId)
					.result
					.flatMap { linkedSpecifications =>
						val specificationCount = linkedSpecifications.size
						if (specificationCount == 0) {
							DBIO.failed(UserException(ErrorCodes.SHARED_TEST_SUITE_LINKED_TO_NO_SPECIFICATIONS, "The test suite to convert must be linked to the target specification."))
						} else if (specificationCount > 1) {
							DBIO.failed(UserException(ErrorCodes.SHARED_TEST_SUITE_LINKED_TO_MULTIPLE_SPECIFICATIONS, "The test suite to convert must be linked to a single target specification."))
						} else {
							DBIO.successful(linkedSpecifications.head)
						}
					}
				// Proceed.
				_ <- PersistenceSchema.testSuites
					.filter(_.id === testSuiteId)
					.map(_.shared)
					.update(false)
			} yield linkedSpecification
		}
	}

}