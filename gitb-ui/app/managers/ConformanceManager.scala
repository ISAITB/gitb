package managers

import actors.events.ConformanceStatementUpdatedEvent
import com.gitb.tr.TestResultType
import config.Configurations
import managers.ConformanceManager._
import models.Enums.{ConformanceStatementItemType, OrganizationType, UserRole}
import models.snapshot._
import models.statement.{ConformanceItemTreeData, ConformanceStatementResults}
import models.{FileInfo, _}
import org.apache.commons.lang3.StringUtils
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.{Query, Rep}
import utils.TimeUtil.dateFromFilterString
import utils.{CryptoUtil, MimeUtil, RepositoryUtils, TimeUtil}

import java.io.File
import java.sql.Timestamp
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

object ConformanceManager {

	private type ConformanceResultFullDbTuple = (
		(Rep[Long], Rep[String]), // Community
		(Rep[Long], Rep[String]), // Organisation
		(Rep[Long], Rep[String], Rep[String], Rep[Option[String]], Rep[Option[String]]), // System
		(Rep[Long], Rep[String], Rep[String], Rep[Option[String]], Rep[Option[String]]), // Domain
		(Rep[Long], Rep[String], Rep[String], Rep[String], Rep[Option[String]], Rep[Option[String]]), // Actor
		(Rep[Long], Rep[String], Rep[String], Rep[Short], Rep[Option[String]], Rep[Option[String]]), // Specification
		(Rep[String], Rep[Option[String]], Rep[Option[Timestamp]], Rep[Option[String]]), // Result
		(Rep[Option[String]], Rep[Option[String]], Rep[Option[Long]], Rep[Option[Short]], Rep[Option[String]], Rep[Option[String]]), // Specification group
		(Rep[Long], Rep[String], Rep[Option[String]], Rep[Boolean], Rep[Boolean], Rep[Option[String]], Rep[Short], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]], Rep[String]), // Test case
		(Rep[Long], Rep[String], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]], Rep[String]), // Test suite
		(Rep[Option[Long]], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]]) // Test case group
	)
	private type ConformanceResultFullTuple = (
		(Long, String), // Community
		(Long, String), // Organisation
		(Long, String, String, Option[String], Option[String]), // System
		(Long, String, String, Option[String], Option[String]), // Domain
		(Long, String, String, String, Option[String], Option[String]), // Actor
		(Long, String, String, Short, Option[String], Option[String]), // Specification
		(String, Option[String], Option[Timestamp], Option[String]), // Result
		(Option[String], Option[String], Option[Long], Option[Short], Option[String], Option[String]), // Specification group
		(Long, String, Option[String], Boolean, Boolean, Option[String], Short, Option[String], Option[String], Option[String], String), // Test case
		(Long, String, Option[String], Option[String], Option[String], Option[String], String), // Test suite
		(Option[Long], Option[String], Option[String], Option[String]) // Test case group
	)
	private type ConformanceResultFullDbQuery = Query[ConformanceResultFullDbTuple, ConformanceResultFullTuple, Seq]

	private type ConformanceResultDbTuple = (
		(Rep[Long], Rep[String]), // Community
		(Rep[Long], Rep[String]), // Organisation
		(Rep[Long], Rep[String]), // System
		(Rep[Long], Rep[String], Rep[String]), // Domain
		(Rep[Long], Rep[String], Rep[String]), // Actor
		(Rep[Long], Rep[String], Rep[String], Rep[Short]), // Specification
		(Rep[String], Rep[Option[String]], Rep[Option[Timestamp]]), // Result
		(Rep[Option[String]], Rep[Option[String]], Rep[Option[Long]], Rep[Option[Short]]), // Specification group
		(Rep[Boolean], Rep[Boolean], Rep[Option[Long]]) // Test case
	)
	private type ConformanceResultTuple = (
		(Long, String), // Community
		(Long, String), // Organisation
		(Long, String), // System
		(Long, String, String), // Domain
		(Long, String, String), // Actor
		(Long, String, String, Short), // Specification
		(String, Option[String], Option[Timestamp]), // Result
		(Option[String], Option[String], Option[Long], Option[Short]), // Specification group
		(Boolean, Boolean, Option[Long]) // Test case
	)
	private type ConformanceResultDbQuery = Query[ConformanceResultDbTuple, ConformanceResultTuple, Seq]

	private type ConformanceStatusDbTuple = (
		(Rep[Long], Rep[String], Rep[Option[String]], Rep[Boolean], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]]), // Test suite
		(Rep[Long], Rep[String], Rep[Option[String]], Rep[Boolean], Rep[Boolean], Rep[Boolean], Rep[Option[String]], Rep[Short], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]]), // Test case
		(Rep[Option[Long]], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]]), // Test case group
		(Rep[String], Rep[Option[String]], Rep[Option[String]], Rep[Option[Timestamp]]) // Result
	)
	private type ConformanceStatusTuple = (
		(Long, String, Option[String], Boolean, Option[String], Option[String], Option[String]), // Test suite
		(Long, String, Option[String], Boolean, Boolean, Boolean, Option[String], Short, Option[String], Option[String], Option[String]), // Test case
		(Option[Long], Option[String], Option[String], Option[String]), // Test case group
		(String, Option[String], Option[String], Option[Timestamp]) // Result
	)
	private type ConformanceStatusDbQuery = Query[ConformanceStatusDbTuple, ConformanceStatusTuple, Seq]

	private type ConformanceStatementDbTuple = (
			(Rep[Long], Rep[String], Rep[Option[String]]), // Domain
			(Rep[Long], Rep[String], Rep[Short], Rep[Option[String]]), // Specification
			(Rep[Option[Long]], Rep[Option[String]], Rep[Option[Short]], Rep[Option[String]]), // Specification group
			(Rep[Long], Rep[String], Rep[String], Rep[Option[String]]),  // Actor
			(Rep[String], Rep[Option[Timestamp]]), // Result
			(Rep[Boolean], Rep[Boolean], Rep[Option[Long]]) // Test case
		)
	private type ConformanceStatementTuple = (
			(Long, String, Option[String]), // Domain
			(Long, String, Short, Option[String]), // Specification
			(Option[Long], Option[String], Option[Short], Option[String]), // Specification group
			(Long, String, String, Option[String]), // Actor
			(String, Option[Timestamp]), // Result
			(Boolean, Boolean, Option[Long]) // Test case
		)
	private type ConformanceStatementDbQuery = Query[ConformanceStatementDbTuple, ConformanceStatementTuple, Seq]

	private class ResultCounter(var failed: Int, var completed: Int, var undefined: Int)
}

@Singleton
class ConformanceManager @Inject() (repositoryUtil: RepositoryUtils,
																		domainParameterManager: DomainParameterManager,
																		systemManager: SystemManager,
																		endpointManager: EndPointManager,
																		parameterManager: ParameterManager,
																		organisationManager: OrganizationManager,
																		repositoryUtils: RepositoryUtils,
																		triggerHelper: TriggerHelper,
																		communityHelper: CommunityHelper,
																		dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

	import dbConfig.profile.api._

	def getAvailableConformanceStatements(domainId: Option[Long], systemId: Long): (Boolean, Seq[ConformanceStatementItem]) = {
		exec(for {
			// Load the actors for which the system already has statements (these will be later skipped).
			actorIdsInExistingStatements <- PersistenceSchema.systemImplementsActors
				.filter(_.systemId === systemId)
				.map(_.actorId)
				.result
				.map(_.toSet)
			// Load the relevant domains.
			domains <- PersistenceSchema.domains
				.filterOpt(domainId)((q, id) => q.id === id)
				.map(x => (x.id, x.fullname, x.description))
				.sortBy(_._2.asc)
				.result
			// Load the relevant specification groups.
			groups <- PersistenceSchema.specificationGroups
				.filterOpt(domainId)((q, id) => q.domain === id)
				.map(x => (x.id, x.fullname, x.description, x.domain, x.displayOrder))
				.sortBy(_._2.asc)
				.result
			// Load the relevant specifications.
			specifications <- PersistenceSchema.specifications
				.filterOpt(domainId)((q, id) => q.domain === id)
				.filter(_.hidden === false)
				.map(x => (x.id, x.fullname, x.description, x.group, x.domain, x.displayOrder))
				.sortBy(_._2.asc)
				.result
			// Load the domain's relevant actors.
			actors <- PersistenceSchema.actors
				.join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
				.filter(_._1.hidden === false)
				.filterOpt(domainId)((q, id) => q._1.domain === id)
				.map(x => (x._1.id, x._1.name, x._1.desc, x._2.specId, x._1.default))
				.sortBy(_._2.asc)
				.result
			// For the relevant actors check which of them have defined test cases as SUTs.
			actorsWithTestCases <- PersistenceSchema.testCaseHasActors
				.filter(_.actor inSet actors.map(_._1))
				.filter(_.sut)
				.map(_.actor)
				.distinct
				.result
				.map(_.toSet)
			results <- {
				val specificationToVisibleSutActorMap = new mutable.HashMap[Long, mutable.HashSet[Long]]() // Specification ID to actor map (actors that are SUTs and are not set as hidden).
				// Map specifications to actors that are non-visible and SUTs.
				actors.foreach(x => {
					if (actorsWithTestCases.contains(x._1)) {
						var entry = specificationToVisibleSutActorMap.get(x._4)
						if (entry.isEmpty) {
							entry = Some(new mutable.HashSet[Long]())
							specificationToVisibleSutActorMap += (x._4 -> entry.get)
						}
						entry.get += x._1
					}
				})
				val actorMap = new mutable.HashMap[Long, ListBuffer[ConformanceStatementItem]]() // Specification ID to Actor data
				val defaultActorMap = new mutable.HashMap[Long, ConformanceStatementItem]() // Specification ID to default Actor data
				// Map actors using specification ID.
				actors.foreach(x => {
					if (actorsWithTestCases.contains(x._1) && !actorIdsInExistingStatements.contains(x._1)) {
						// Only keep the actors that have test cases in which they are the SUT and for which we don't already have a statement defined.
						// The actor will be shown if there are other SUT actors available for the same specification.
						val showActor = specificationToVisibleSutActorMap(x._4).size > 1
						val item = ConformanceStatementItem(x._1, x._2, x._3, None, ConformanceStatementItemType.ACTOR, None, 0, None, showActor)
						actorMap.getOrElseUpdate(x._4, new ListBuffer[ConformanceStatementItem]).append(item)
						if (x._5.getOrElse(false)) {
							defaultActorMap.put(x._4, item)
						}
					}
				})
				// If we have defaults, force them.
				defaultActorMap.foreach(x => actorMap.put(x._1, ListBuffer(x._2)))
				val specificationInGroupMap = new mutable.HashMap[Long, ListBuffer[ConformanceStatementItem]]() // Specification group ID to Specification data
				val specificationNotInGroupMap = new mutable.HashMap[Long, ListBuffer[ConformanceStatementItem]]() // Domain ID to Specification data
				specifications.foreach(x => {
					val childActors = actorMap.get(x._1).map(_.toList)
					if (childActors.nonEmpty) {
						// Only keep specifications with actors.
						if (x._4.nonEmpty) {
							// Map specifications using group ID.
							specificationInGroupMap.getOrElseUpdate(x._4.get, new ListBuffer[ConformanceStatementItem]).append(ConformanceStatementItem(x._1, x._2, x._3, None, ConformanceStatementItemType.SPECIFICATION, childActors, x._6))
						} else {
							// Map specifications using domain ID.
							specificationNotInGroupMap.getOrElseUpdate(x._5, new ListBuffer[ConformanceStatementItem]).append(ConformanceStatementItem(x._1, x._2, x._3, None, ConformanceStatementItemType.SPECIFICATION, childActors, x._6))
						}
					}
				})
				val specificationGroupMap = new mutable.HashMap[Long, ListBuffer[ConformanceStatementItem]]() // Domain ID to Specification group data
				// Map groups using domain ID.
				groups.foreach(x => {
					val childSpecifications = specificationInGroupMap.get(x._1).map(_.toList)
					if (childSpecifications.nonEmpty) {
						// Only keep groups with (non-empty) specifications.
						specificationGroupMap.getOrElseUpdate(x._4, new ListBuffer[ConformanceStatementItem]).append(ConformanceStatementItem(x._1, x._2, x._3, None, ConformanceStatementItemType.SPECIFICATION_GROUP, childSpecifications, x._5))
					}
				})
				// Process domain(s).
				val domainItems = new ListBuffer[ConformanceStatementItem]
				domains.foreach(x => {
					val children = specificationGroupMap.getOrElse(x._1, ListBuffer.empty) // Add groups to domain.
						.appendAll(specificationNotInGroupMap.getOrElse(x._1, ListBuffer.empty)) // Add specs (not in groups) to domain.
						.sortBy(_.name) // Sort everything by name
						.toList
					if (children.nonEmpty) {
						// Only keep domains with (non-empty) specifications or groups.
						domainItems.append(ConformanceStatementItem(x._1, x._2, x._3, None, ConformanceStatementItemType.DOMAIN, Some(children), 0))
					}
				})
				DBIO.successful((actorIdsInExistingStatements.nonEmpty, domainItems.toList))
			}
		} yield results)
	}

	def getCompletedConformanceStatementsForTestSession(systemId: Long, sessionId: String): Option[Long] = { // Actor ID considered as completed.
		exec(
			for {
				// Make sure we only do subsequent lookups if we have a non-optional, non-disabled test case that succeeded (otherwise the overall status will not have changed to be successful)
				actorIdToCheck <- PersistenceSchema.conformanceResults
					.join(PersistenceSchema.testCases).on(_.testcase === _.id)
					.filter(_._1.testsession === sessionId)
					.filter(_._2.isOptional === false)
					.filter(_._2.isDisabled === false)
					.filter(_._1.result === "SUCCESS")
					.map(_._1.actor)
					.result
					.headOption
				conformanceResults <- {
					if (actorIdToCheck.nonEmpty) {
						PersistenceSchema.conformanceResults
							.join(PersistenceSchema.testCases).on(_.testcase === _.id)
							.filter(_._1.sut === systemId)
							.filter(_._1.actor === actorIdToCheck.get)
							.map(x => (x._1.result, x._2.isOptional, x._2.isDisabled, x._2.group))
							.result
					} else {
						DBIO.successful(Seq.empty)
					}
				}
				completedActor <- {
					if (actorIdToCheck.isDefined && conformanceResults.nonEmpty) {
						val builder = new ConformanceStatusBuilder.ConformanceData[ConformanceStatementResultData](new ConformanceStatementResultData())
						conformanceResults.foreach { resultInfo =>
							builder.addConformanceResult(resultInfo._1, resultInfo._2, resultInfo._3, resultInfo._4)
						}
						if (builder.complete().overallResult.result == "SUCCESS") {
							DBIO.successful(actorIdToCheck)
						} else {
							DBIO.successful(None)
						}
					} else {
						DBIO.successful(None)
					}
				}
			} yield completedActor
		)
	}

	def getConformanceStatus(actorId: Long, sutId: Long, testSuiteId: Option[Long], includeDisabled: Boolean = true, snapshotId: Option[Long] = None): Option[ConformanceStatus] = {
		if (snapshotId.isEmpty && (actorId < 0 || sutId < 0 || testSuiteId.isDefined && testSuiteId.get < 0)) {
			None
		} else {
			val query: ConformanceStatusDbQuery = if (snapshotId.isDefined) {
				PersistenceSchema.conformanceSnapshotResults
					.join(PersistenceSchema.conformanceSnapshotTestCases).on((q, tc) => q.snapshotId === tc.snapshotId && q.testCaseId === tc.id)
					.join(PersistenceSchema.conformanceSnapshotTestSuites).on((q, ts) => q._1.snapshotId === ts.snapshotId && q._1.testSuiteId === ts.id)
					.joinLeft(PersistenceSchema.conformanceSnapshotTestCaseGroups).on((q, tcg) => q._1._1.snapshotId === tcg.snapshotId && q._1._1.testCaseGroupId === tcg.id)
					.filter(_._1._1._1.snapshotId === snapshotId.get)
					.filter(_._1._1._1.actorId === actorId)
					.filter(_._1._1._1.systemId === sutId)
					.filterIf(!includeDisabled)(_._1._1._2.isDisabled === false)
					.filterOpt(testSuiteId)((q, id) => q._1._1._1.testSuiteId === id)
					.map(x => (
						(x._1._2.id, x._1._2.shortname, x._1._2.description, false, x._1._2.specReference, x._1._2.specDescription, x._1._2.specLink), // Test suite
						(x._1._1._2.id, x._1._1._2.shortname, x._1._1._2.description, false, x._1._1._2.isOptional, x._1._1._2.isDisabled, x._1._1._2.tags, x._1._1._2.testSuiteOrder, x._1._1._2.specReference, x._1._1._2.specDescription, x._1._1._2.specLink), // Test case
						(x._2.map(_.id), x._2.map(_.identifier), x._2.map(_.name).flatten, x._2.map(_.description).flatten), // Test case group
						(x._1._1._1.result, x._1._1._1.outputMessage, x._1._1._1.testSessionId, x._1._1._1.updateTime) // Result
					))
			} else {
				PersistenceSchema.conformanceResults
					.join(PersistenceSchema.testCases).on(_.testcase === _.id)
					.join(PersistenceSchema.testSuites).on(_._1.testsuite === _.id)
					.joinLeft(PersistenceSchema.testCaseGroups).on((q, g) => q._1._2.group === g.id)
					.filter(_._1._1._1.actor === actorId)
					.filter(_._1._1._1.sut === sutId)
					.filterIf(!includeDisabled)(_._1._1._2.isDisabled === false)
					.filterOpt(testSuiteId)((q, id) => q._1._1._1.testsuite === id)
					.map(x => (
						(x._1._2.id, x._1._2.shortname, x._1._2.description, x._1._2.hasDocumentation, x._1._2.specReference, x._1._2.specDescription, x._1._2.specLink), // Test suite
						(x._1._1._2.id, x._1._1._2.shortname, x._1._1._2.description, x._1._1._2.hasDocumentation, x._1._1._2.isOptional, x._1._1._2.isDisabled, x._1._1._2.tags, x._1._1._2.testSuiteOrder, x._1._1._2.specReference, x._1._1._2.specDescription, x._1._1._2.specLink), // Test case
						(x._2.map(_.id), x._2.map(_.identifier), x._2.map(_.name).flatten, x._2.map(_.description).flatten), // Test case group
						(x._1._1._1.result, x._1._1._1.outputMessage, x._1._1._1.testsession, x._1._1._1.updateTime) // Result
					))
			}
			val specificationIdsQuery = if (snapshotId.isDefined) {
				PersistenceSchema.conformanceSnapshotResults
					.filter(_.actorId === actorId)
					.filter(_.systemId === sutId)
					.filter(_.snapshotId === snapshotId.get)
					.map(x => (x.actorId, x.domainId)).result.head
			} else {
				PersistenceSchema.specificationHasActors
					.join(PersistenceSchema.specifications).on(_.specId === _.id)
					.filter(_._1.actorId === actorId)
					.map(x => (x._2.id, x._2.domain)).result.head
			}
			val statusItems = exec(
				for {
					results <- query.sortBy(x => (x._1._2, x._2._8)).result
						.map(_.map(r => {
							ConformanceStatusItem(
								testSuiteId = r._1._1, testSuiteName = r._1._2, testSuiteDescription = r._1._3, testSuiteHasDocumentation = r._1._4, testSuiteSpecReference = r._1._5, testSuiteSpecDescription = r._1._6, testSuiteSpecLink = r._1._7,
								testCaseId = r._2._1, testCaseName = r._2._2, testCaseDescription = r._2._3, testCaseHasDocumentation = r._2._4, testCaseSpecReference = r._2._9, testCaseSpecDescription = r._2._10, testCaseSpecLink = r._2._11,
								testCaseGroup = r._3._1.map(TestCaseGroup(_, r._3._2.get, r._3._3, r._3._4, r._1._1)),
								result = r._4._1, outputMessage = r._4._2, sessionId = r._4._3, sessionTime = r._4._4,
								testCaseOptional = r._2._5, testCaseDisabled = r._2._6, testCaseTags = r._2._7
							)
						}))
					specificationIds <- specificationIdsQuery
				} yield (results, specificationIds)
			)
			// Check to see if we have badges. We use the SUCCESS badge as this will always be present if badges are defined.
			val hasBadge = repositoryUtil.getConformanceBadge(statusItems._2._1, Some(actorId), snapshotId, TestResultType.SUCCESS.toString, exactMatch = false, forReport = false).isDefined
			val status = new ConformanceStatus(sutId, statusItems._2._2, statusItems._2._1, actorId, 0, 0, 0, 0, 0, 0, 0, 0, 0, TestResultType.UNDEFINED, None, hasBadge, new ListBuffer[ConformanceTestSuite])
			val testCaseGroups = new mutable.HashMap[Long, ResultCounter]() // Group ID to group status.
			val testSuiteMap = new mutable.LinkedHashMap[Long, ConformanceTestSuite]()
			statusItems._1.foreach { item =>
				val testSuite = if (testSuiteMap.contains(item.testSuiteId)) {
					testSuiteMap(item.testSuiteId)
				} else {
					// New test suite.
					val newTestSuite = new ConformanceTestSuite(item.testSuiteId, item.testSuiteName, item.testSuiteDescription, None, item.testSuiteHasDocumentation, item.testSuiteSpecReference, item.testSuiteSpecDescription, item.testSuiteSpecLink, TestResultType.UNDEFINED, 0, 0, 0, 0, 0, 0, 0, 0, 0, new ListBuffer[ConformanceTestCase], new mutable.HashSet[TestCaseGroup])
					testSuiteMap += (item.testSuiteId -> newTestSuite)
					newTestSuite
				}
				val testCase = new ConformanceTestCase(item.testCaseId, item.testCaseName, item.testCaseDescription, None, item.sessionId, item.sessionTime, item.outputMessage, item.testCaseHasDocumentation, item.testCaseOptional, item.testCaseDisabled, TestResultType.fromValue(item.result), item.testCaseTags, item.testCaseSpecReference, item.testCaseSpecDescription, item.testCaseSpecLink, item.testCaseGroup.map(_.id))
				testSuite.testCases.asInstanceOf[ListBuffer[ConformanceTestCase]].append(testCase)
				if (item.testCaseGroup.isDefined) {
					testSuite.testCaseGroups.asInstanceOf[mutable.HashSet[TestCaseGroup]].add(item.testCaseGroup.get)
				}
				if (!testCase.disabled) {
					// Update time.
					if (testCase.updateTime.isDefined && (status.updateTime.isEmpty || status.updateTime.get.before(testCase.updateTime.get))) {
						status.updateTime = testCase.updateTime
					}
					// Counters and overall status.
					if (testCase.optional) {
						if (testCase.result == TestResultType.SUCCESS) {
							status.completedOptional += 1
							testSuite.completedOptional += 1
						} else if (testCase.result == TestResultType.FAILURE) {
							status.failedOptional += 1
							testSuite.failedOptional += 1
						} else {
							status.undefinedOptional += 1
							testSuite.undefinedOptional += 1
						}
					} else {
						if (testCase.result == TestResultType.SUCCESS) {
							status.completed += 1
							testSuite.completed += 1
							if (item.testCaseGroup.isEmpty) {
								status.completedToConsider += 1
								testSuite.completedToConsider += 1
							} else {
								groupResult(testCaseGroups, item.testCaseGroup.get).completed += 1
							}
						} else if (testCase.result == TestResultType.FAILURE) {
							status.failed += 1
							testSuite.failed += 1
							if (item.testCaseGroup.isEmpty) {
								status.failedToConsider += 1
								testSuite.failedToConsider += 1
							} else {
								groupResult(testCaseGroups, item.testCaseGroup.get).failed += 1
							}
						} else {
							status.undefined += 1
							testSuite.undefined += 1
							if (item.testCaseGroup.isEmpty) {
								status.undefinedToConsider += 1
								testSuite.undefinedToConsider += 1
							} else {
								groupResult(testCaseGroups, item.testCaseGroup.get).undefined += 1
							}
						}
					}
				}
			}
			// Now process the results from test case groups
			if (testCaseGroups.nonEmpty) {
				val processedGroups = mutable.HashSet[Long]()
				testSuiteMap.values.foreach { testSuite =>
					testSuite.testCases.foreach { testCase =>
						if (testCase.group.exists(!processedGroups.contains(_))) {
							val groupResults = testCaseGroups.get(testCase.group.get)
							if (groupResults.isDefined) {
								if (groupResults.get.completed > 0) {
									status.completedToConsider += 1
									testSuite.completedToConsider += 1
								} else if (groupResults.get.failed > 0) {
									status.failedToConsider += 1
									testSuite.failedToConsider += 1
								} else {
									status.undefinedToConsider += 1
									testSuite.undefinedToConsider += 1
								}
							}
							processedGroups += testCase.group.get
						}
					}
					testSuite.result = resultStatus(testSuite.completedToConsider, testSuite.failedToConsider, testSuite.undefinedToConsider)
				}
			}
			// Complete overall results
			status.testSuites = testSuiteMap.values
			status.result = resultStatus(status.completedToConsider, status.failedToConsider, status.undefinedToConsider)
			Some(status)
		}
	}

	private def resultStatus(completed: Long, failed: Long, undefined: Long): TestResultType = {
		if (failed > 0) {
			TestResultType.FAILURE
		} else if (undefined > 0) {
			TestResultType.UNDEFINED
		} else if (completed > 0) {
			TestResultType.SUCCESS
		} else {
			TestResultType.UNDEFINED
		}
	}

	private def groupResult(groupMap: mutable.HashMap[Long, ResultCounter], group: TestCaseGroup): ResultCounter = {
		var groupResults = groupMap.get(group.id)
		if (groupResults.isEmpty) {
			groupResults = Some(new ResultCounter(0, 0, 0))
			groupMap += (group.id -> groupResults.get)
		}
		groupResults.get
	}

	def getSpecificationIdForTestCaseFromConformanceStatements(testCaseId: Long): Option[Long] = {
		exec(PersistenceSchema.conformanceResults.filter(_.testcase === testCaseId).map(c => {c.spec}).result.headOption)
	}

	def getSpecificationIdForTestSuiteFromConformanceStatements(testSuiteId: Long): Option[Long] = {
		exec(PersistenceSchema.conformanceResults.filter(_.testsuite === testSuiteId).map(c => {c.spec}).result.headOption)
	}

	def getConformanceStatementsResultBuilder(domainIds: Option[List[Long]], specIds: Option[List[Long]], specGroupIds: Option[List[Long]], actorIds: Option[List[Long]],
																						communityIds: Option[List[Long]], organizationIds: Option[List[Long]], systemIds: Option[List[Long]],
																						orgParameters: Option[Map[Long, Set[String]]], sysParameters: Option[Map[Long, Set[String]]],
																						sortColumn: Option[String], sortOrder: Option[String],
																						snapshotId: Option[Long], prefixSpecificationNameWithGroup: Boolean): ConformanceStatusBuilder[ConformanceStatementFull] = {
		val query: ConformanceResultFullDbQuery = if (snapshotId.isDefined) {
			PersistenceSchema.conformanceSnapshotResults
				.join(PersistenceSchema.conformanceSnapshots).on(_.snapshotId === _.id)
				.join(PersistenceSchema.conformanceSnapshotSpecifications).on((q, spec) => q._1.snapshotId === spec.snapshotId && q._1.specificationId === spec.id)
				.joinLeft(PersistenceSchema.conformanceSnapshotSpecificationGroups).on((q, sg) => q._1._1.snapshotId === sg.snapshotId && q._1._1.specificationGroupId === sg.id)
				.join(PersistenceSchema.conformanceSnapshotActors).on((q, act) => q._1._1._1.snapshotId === act.snapshotId && q._1._1._1.actorId === act.id)
				.join(PersistenceSchema.conformanceSnapshotDomains).on((q, dom) => q._1._1._1._1.snapshotId === dom.snapshotId && q._1._1._1._1.domainId === dom.id)
				.join(PersistenceSchema.conformanceSnapshotSystems).on((q, sys) => q._1._1._1._1._1.snapshotId === sys.snapshotId && q._1._1._1._1._1.systemId === sys.id)
				.join(PersistenceSchema.conformanceSnapshotOrganisations).on((q, org) => q._1._1._1._1._1._1.snapshotId === org.snapshotId && q._1._1._1._1._1._1.organisationId === org.id)
				.join(PersistenceSchema.communities).on(_._1._1._1._1._1._1._2.community === _.id)
				.join(PersistenceSchema.conformanceSnapshotTestSuites).on((q, ts) => q._1._1._1._1._1._1._1._1.snapshotId === ts.snapshotId && q._1._1._1._1._1._1._1._1.testSuiteId === ts.id)
				.join(PersistenceSchema.conformanceSnapshotTestCases).on((q, tc) => q._1._1._1._1._1._1._1._1._1.snapshotId === tc.snapshotId && q._1._1._1._1._1._1._1._1._1.testCaseId === tc.id)
				.joinLeft(PersistenceSchema.conformanceSnapshotTestCaseGroups).on((q, tcg) => q._1._1._1._1._1._1._1._1._1._1.snapshotId === tcg.snapshotId && q._1._1._1._1._1._1._1._1._1._1.testCaseGroupId === tcg.id)
				.filter(_._1._1._1._1._1._1._1._1._1._1._1.snapshotId === snapshotId.get)
				.filterOpt(domainIds)((q, ids) => q._1._1._1._1._1._1._1._1._1._1._1.domainId inSet ids)
				.filterOpt(specIds)((q, ids) => q._1._1._1._1._1._1._1._1._1._1._1.specificationId inSet ids)
				.filterOpt(specGroupIds)((q, ids) => q._1._1._1._1._1._1._1._1._1._1._1.specificationGroupId inSet ids)
				.filterOpt(actorIds)((q, ids) => q._1._1._1._1._1._1._1._1._1._1._1.actorId inSet ids)
				.filterOpt(communityIds)((q, ids) => q._1._1._1._1._1._1._1._1._1._1._2.community inSet ids)
				.filterOpt(communityHelper.organisationIdsToUse(organizationIds, orgParameters))((q, ids) => q._1._1._1._1._1._1._1._1._1._1._1.organisationId inSet ids)
				.filterOpt(communityHelper.systemIdsToUse(systemIds, sysParameters))((q, ids) => q._1._1._1._1._1._1._1._1._1._1._1.systemId inSet ids)
				.map(x => (
					(x._1._1._1._2.id, x._1._1._1._2.shortname), // 1.1: Community ID, 1.2: Community shortname
					(x._1._1._1._1._2.id, x._1._1._1._1._2.shortname), // 2.1: Organisation ID, 2.2: Organisation shortname
					(x._1._1._1._1._1._2.id, x._1._1._1._1._1._2.shortname, x._1._1._1._1._1._2.badgeKey, x._1._1._1._1._1._2.description, x._1._1._1._1._1._2.version), // 3.1: System ID, 3.2: System shortname, 3.3: System badge key, 3.4: System description, 3.5: System version
					(x._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._2.fullname, x._1._1._1._1._1._1._2.description, x._1._1._1._1._1._1._2.reportMetadata), // 4.1: Domain ID, 4.2: Domain shortname, 4.3: Domain fullname, 4.4: Domain description, 4.5: Domain report metadata
					(x._1._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._1._2.actorId, x._1._1._1._1._1._1._1._2.name, x._1._1._1._1._1._1._1._2.apiKey, x._1._1._1._1._1._1._1._2.description, x._1._1._1._1._1._1._1._2.reportMetadata), // 5.1: Actor ID, 5.2: Actor identifier, 5.3: Actor name, 5.4: Actor API key, 5.5: Actor description, 5.6: Actor report metadata
					(x._1._1._1._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._1._1._1._2.fullname, x._1._1._1._1._1._1._1._1._1._2.displayOrder, x._1._1._1._1._1._1._1._1._1._2.description, x._1._1._1._1._1._1._1._1._1._2.reportMetadata), // 6.1: Specification ID, 6.2: Specification shortname, 6.3: Specification fullname, 6.4: Specification order, 6.5: Specification description, 6.6: Specification report metadata
					(x._1._1._1._1._1._1._1._1._1._1._1.result, x._1._1._1._1._1._1._1._1._1._1._1.testSessionId, x._1._1._1._1._1._1._1._1._1._1._1.updateTime, x._1._1._1._1._1._1._1._1._1._1._1.outputMessage), // 7.1: Result, 7.2: Session ID, 7.3: Update time, 7.4: Output message
					(x._1._1._1._1._1._1._1._1._2.map(_.shortname), x._1._1._1._1._1._1._1._1._2.map(_.fullname), x._1._1._1._1._1._1._1._1._2.map(_.id), x._1._1._1._1._1._1._1._1._2.map(_.displayOrder), x._1._1._1._1._1._1._1._1._2.map(_.description).flatten, x._1._1._1._1._1._1._1._1._2.map(_.reportMetadata).flatten), // 8.1: Specification group shortname, 8.2: Specification group fullname, 8.3: Specification group ID, 8.4: Specification group order, 8.5: Specification group description, 8.6: Specification group report metadata
					(x._1._2.id, x._1._2.shortname, x._1._2.description, x._1._2.isOptional, x._1._2.isDisabled, x._1._2.tags, x._1._2.testSuiteOrder, x._1._2.specReference, x._1._2.specDescription, x._1._2.specLink, x._1._2.version), // 9.1: Test case ID, 9.2: Test case shortname, 9.3: Test case description, 9.4: Test case optional, 9.5: Test case disabled, 9.6: Test case tags, 9.7: Test case order
					(x._1._1._2.id, x._1._1._2.shortname, x._1._1._2.description, x._1._1._2.specReference, x._1._1._2.specDescription, x._1._1._2.specLink, x._1._1._2.version), // 10.1: Test suite ID, 10.2: Test suite shortname, 10.3: Test suite description, 10.4: Test suite spec reference, 10.5: Test suite spec description, 10.6: Test suite spec link
					(x._2.map(_.id), x._2.map(_.identifier), x._2.map(_.name).flatten, x._2.map(_.description).flatten) // 11.1: Test case group ID, 11.2: Test case group identifier, 11.3: Test case group name, 11.4: Test case group description
				))
		} else {
			PersistenceSchema.conformanceResults
				.join(PersistenceSchema.specifications).on(_.spec === _.id)
				.joinLeft(PersistenceSchema.specificationGroups).on(_._2.group === _.id)
				.join(PersistenceSchema.actors).on(_._1._1.actor === _.id)
				.join(PersistenceSchema.domains).on(_._2.domain === _.id)
				.join(PersistenceSchema.systems).on(_._1._1._1._1.sut === _.id)
				.join(PersistenceSchema.organizations).on(_._2.owner === _.id)
				.join(PersistenceSchema.communities).on(_._2.community === _.id)
				.join(PersistenceSchema.testSuites).on(_._1._1._1._1._1._1._1.testsuite === _.id)
				.join(PersistenceSchema.testCases).on(_._1._1._1._1._1._1._1._1.testcase === _.id)
				.joinLeft(PersistenceSchema.testCaseGroups).on(_._2.group === _.id)
				.filterOpt(domainIds)((q, ids) => q._1._1._1._1._1._1._2.id inSet ids)
				.filterOpt(specIds)((q, ids) => q._1._1._1._1._1._1._1._1._1._1.spec inSet ids)
				.filterOpt(specGroupIds)((q, ids) => q._1._1._1._1._1._1._1._1._1._2.group inSet ids)
				.filterOpt(actorIds)((q, ids) => q._1._1._1._1._1._1._1._1._1._1.actor inSet ids)
				.filterOpt(communityIds)((q, ids) => q._1._1._1._2.id inSet ids)
				.filterOpt(communityHelper.organisationIdsToUse(organizationIds, orgParameters))((q, ids) => q._1._1._1._1._2.id inSet ids)
				.filterOpt(communityHelper.systemIdsToUse(systemIds, sysParameters))((q, ids) => q._1._1._1._1._1._1._1._1._1._1.sut inSet ids)
				.map(x => (
					(x._1._1._1._2.id, x._1._1._1._2.shortname), // 1.1: Community ID, 1.2: Community shortname
					(x._1._1._1._1._2.id, x._1._1._1._1._2.shortname), // 2.1: Organisation ID, 2.2: Organisation shortname
					(x._1._1._1._1._1._2.id, x._1._1._1._1._1._2.shortname, x._1._1._1._1._1._2.badgeKey, x._1._1._1._1._1._2.description, x._1._1._1._1._1._2.version), // 3.1: System ID, 3.2: System shortname, 3.3: System badge key, 3.4: System description, 3.5: System version
					(x._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._2.fullname, x._1._1._1._1._1._1._2.description, x._1._1._1._1._1._1._2.reportMetadata), // 4.1: Domain ID, 4.2: Domain shortname, 4.3: Domain fullname, 4.4: Domain description, 4.5: Domain report metadata
					(x._1._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._1._2.actorId, x._1._1._1._1._1._1._1._2.name, x._1._1._1._1._1._1._1._2.apiKey, x._1._1._1._1._1._1._1._2.desc, x._1._1._1._1._1._1._1._2.reportMetadata), // 5.1: Actor ID, 5.2: Actor identifier, 5.3: Actor name, 5.4: Actor API key, 5.5: Actor description, 5.6: Actor report metadata
					(x._1._1._1._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._1._1._1._2.fullname, x._1._1._1._1._1._1._1._1._1._2.displayOrder, x._1._1._1._1._1._1._1._1._1._2.description, x._1._1._1._1._1._1._1._1._1._2.reportMetadata), // 6.1: Specification ID, 6.2: Specification shortname, 6.3: Specification fullname, 6.4: Specification order, 6.5: Specification description, 6.6: Specification report metadata
					(x._1._1._1._1._1._1._1._1._1._1.result, x._1._1._1._1._1._1._1._1._1._1.testsession, x._1._1._1._1._1._1._1._1._1._1.updateTime, x._1._1._1._1._1._1._1._1._1._1.outputMessage), // 7.1: Result, 7.2: Session ID, 7.3: Update time, 7.4: Output message
					(x._1._1._1._1._1._1._1._1._2.map(_.shortname), x._1._1._1._1._1._1._1._1._2.map(_.fullname), x._1._1._1._1._1._1._1._1._2.map(_.id), x._1._1._1._1._1._1._1._1._2.map(_.displayOrder), x._1._1._1._1._1._1._1._1._2.map(_.description).flatten, x._1._1._1._1._1._1._1._1._2.map(_.reportMetadata).flatten), // 8.1: Specification group shortname, 8.2: Specification group fullname, 8.3: Specification group ID, 8.4: Specification group order, 8.5: Specification group description, 8.6: Specification group report metadata
					(x._1._2.id, x._1._2.shortname, x._1._2.description, x._1._2.isOptional, x._1._2.isDisabled, x._1._2.tags, x._1._2.testSuiteOrder, x._1._2.specReference, x._1._2.specDescription, x._1._2.specLink, x._1._2.version), // 9.1: Test case ID, 9.2: Test case shortname, 9.3: Test case description, 9.4: Test case optional, 9.5: Test case disabled, 9.6: Test case tags, 9.7: Test case order
					(x._1._1._2.id, x._1._1._2.shortname, x._1._1._2.description, x._1._1._2.specReference, x._1._1._2.specDescription, x._1._1._2.specLink, x._1._1._2.version), // 10.1: Test suite ID, 10.2: Test suite shortname, 10.3: Test suite description, 10.4: Test suite spec reference, 10.5: Test suite spec description, 10.6: Test suite spec link
					(x._2.map(_.id), x._2.map(_.identifier), x._2.map(_.name).flatten, x._2.map(_.description).flatten) // 11.1: Test case group ID, 11.2: Test case group identifier, 11.3: Test case group name, 11.4: Test case group description
				))
		}
		val sortColumnToApply = sortColumn.getOrElse("community")
		val sortOrderToApply = sortOrder.getOrElse("asc")
		val results = exec(
			// Apply sorting
			query.sortBy(x => {
					val community = x._1._2.asc
					val organisation = x._2._2.asc
					val system = x._3._2.asc
					val domain = x._4._2.asc
					val specification = x._6._2.asc
					val actor = x._5._2.asc
					val specificationGroup = x._8._2.getOrElse("").asc
					val testSuite = x._10._2.asc
					val testCase = x._9._7.asc
					sortColumnToApply match {
						case "organisation" => (if (sortOrderToApply == "asc") organisation.asc else organisation.desc, community, system, domain, specificationGroup, specification, actor, testSuite, testCase)
						case "system" => (if (sortOrderToApply == "asc") system.asc else system.desc, community, organisation, domain, specificationGroup, specification, actor, testSuite, testCase)
						case "domain" => (if (sortOrderToApply == "asc") domain.asc else domain.desc, specificationGroup, specification, community, organisation, system, actor, testSuite, testCase)
						case "specification" => (if (sortOrderToApply == "asc") specificationGroup.asc else specificationGroup.desc, specification, community, organisation, system, domain, actor, testSuite, testCase)
						case "actor" => (if (sortOrderToApply == "asc") actor.asc else actor.desc, community, organisation, system, domain, specificationGroup, specification, testSuite, testCase)
						case _ => (if (sortOrderToApply == "asc") community.asc else community.desc, organisation, system, domain, specificationGroup, specification, actor, testSuite, testCase) // Community (or default)
					}
				})
				.result.map(_.toList)
		)
		val resultBuilder = new ConformanceStatusBuilder[ConformanceStatementFull](recordDetails = true)
		results.foreach { result =>
			var specName = result._6._2
			var specNameFull = result._6._3
			if (prefixSpecificationNameWithGroup) {
				if (result._8._1.nonEmpty) {
					specName = result._8._1.get + " - " + specName
				}
				if (result._8._2.nonEmpty) {
					specNameFull = result._8._2.get + " - " + specNameFull
				}
			}
			val conformanceStatement = new ConformanceStatementFull(
				communityId = result._1._1, communityName = result._1._2, organizationId = result._2._1, organizationName = result._2._2,
				systemId = result._3._1, systemName = result._3._2, systemBadgeKey = result._3._3, systemDescription = result._3._4, systemVersion = result._3._5,
				domainId = result._4._1, domainName = result._4._2, domainNameFull = result._4._3, domainDescription = result._4._4, domainReportMetadata = result._4._5,
				actorId = result._5._1, actorName = result._5._2, actorFull = result._5._3, actorDescription = result._5._5, actorReportMetadata = result._5._6, actorApiKey = result._5._4,
				specificationId = result._6._1, specificationName = specName, specificationNameFull = specNameFull, specificationDescription = result._6._5, specificationReportMetadata = result._6._6, specificationDisplayOrder = result._6._4,
				specificationGroupId = result._8._3, specificationGroupName = result._8._1, specificationGroupDescription = result._8._5, specificationGroupReportMetadata = result._8._6, specificationGroupNameFull = result._8._1, specificationGroupDisplayOrder = result._8._4, specificationGroupOptionName = result._6._2, specificationGroupOptionNameFull = result._6._3,
				testSuiteId = Some(result._10._1), testSuiteName = Some(result._10._2), testSuiteDescription = result._10._3, testSuiteSpecReference = result._10._4, testSuiteSpecDescription = result._10._5, testSuiteSpecLink = result._10._6, testSuiteVersion = result._10._7,
				testCaseId = Some(result._9._1), testCaseName = Some(result._9._2), testCaseDescription = result._9._3,
				testCaseOptional = Some(result._9._4), testCaseDisabled = Some(result._9._5), testCaseTags = result._9._6, testCaseOrder = Some(result._9._7), testCaseSpecReference = result._9._8, testCaseSpecDescription = result._9._9, testCaseSpecLink = result._9._10, testCaseVersion = result._9._11,
				testCaseGroupId = result._11._1, testCaseGroupIdentifier = result._11._2, testCaseGroupName = result._11._3, testCaseGroupDescription = result._11._4,
				result = result._7._1, outputMessage = result._7._4, sessionId = result._7._2, updateTime = result._7._3,
				completedTests = 0L, failedTests = 0L, undefinedTests = 0L,
				completedOptionalTests = 0L, failedOptionalTests = 0L, undefinedOptionalTests = 0L,
				completedTestsToConsider = 0L, failedTestsToConsider = 0L, undefinedTestsToConsider = 0L)
			resultBuilder.addConformanceResult(conformanceStatement, result._9._4, result._9._5, result._11._1)
		}
		resultBuilder
	}

	def getConformanceStatementsFull(domainIds: Option[List[Long]], specIds: Option[List[Long]], specGroupIds: Option[List[Long]], actorIds: Option[List[Long]], communityIds: Option[List[Long]], organizationIds: Option[List[Long]], systemIds: Option[List[Long]], orgParameters: Option[Map[Long, Set[String]]], sysParameters: Option[Map[Long, Set[String]]], status: Option[List[String]], updateTimeStart: Option[String], updateTimeEnd: Option[String], sortColumn: Option[String], sortOrder: Option[String], snapshotId: Option[Long]): List[ConformanceStatementFull] = {
		// Collect results and calculate status.
		val resultBuilder = getConformanceStatementsResultBuilder(domainIds, specIds, specGroupIds, actorIds, communityIds, organizationIds, systemIds, orgParameters, sysParameters, sortColumn, sortOrder, snapshotId, prefixSpecificationNameWithGroup = true)
		// Apply non-DB filtering.
		resultBuilder.getDetails(Some(new ConformanceStatusBuilder.FilterCriteria(
			dateFromFilterString(updateTimeStart),
			dateFromFilterString(updateTimeEnd),
			status))
		)
	}

	def getConformanceStatements(domainIds: Option[List[Long]], specIds: Option[List[Long]], specGroupIds: Option[List[Long]], actorIds: Option[List[Long]],
															 communityIds: Option[List[Long]], organizationIds: Option[List[Long]], systemIds: Option[List[Long]],
															 orgParameters: Option[Map[Long, Set[String]]], sysParameters: Option[Map[Long, Set[String]]],
															 status: Option[List[String]], updateTimeStart: Option[String], updateTimeEnd: Option[String],
															 sortColumn: Option[String], sortOrder: Option[String], snapshotId: Option[Long]): List[ConformanceStatementFull] = {
		val query: ConformanceResultDbQuery = if (snapshotId.isDefined) {
			PersistenceSchema.conformanceSnapshotResults
				.join(PersistenceSchema.conformanceSnapshots).on(_.snapshotId === _.id)
				.join(PersistenceSchema.conformanceSnapshotSpecifications).on((q, spec) => q._1.snapshotId === spec.snapshotId && q._1.specificationId === spec.id)
				.join(PersistenceSchema.conformanceSnapshotActors).on((q, act) => q._1._1.snapshotId === act.snapshotId && q._1._1.actorId === act.id)
				.join(PersistenceSchema.conformanceSnapshotDomains).on((q, dom) => q._1._1._1.snapshotId === dom.snapshotId && q._1._1._1.domainId === dom.id)
				.join(PersistenceSchema.conformanceSnapshotSystems).on((q, sys) => q._1._1._1._1.snapshotId === sys.snapshotId && q._1._1._1._1.systemId === sys.id)
				.join(PersistenceSchema.conformanceSnapshotOrganisations).on((q, org) => q._1._1._1._1._1.snapshotId === org.snapshotId && q._1._1._1._1._1.organisationId === org.id)
				.join(PersistenceSchema.communities).on(_._1._1._1._1._1._2.community === _.id)
				.join(PersistenceSchema.conformanceSnapshotTestCases).on((q, tc) => q._1._1._1._1._1._1._1.snapshotId === tc.snapshotId && q._1._1._1._1._1._1._1.testCaseId === tc.id)
				.joinLeft(PersistenceSchema.conformanceSnapshotSpecificationGroups).on((q, sg) => q._1._1._1._1._1._1._1._1.snapshotId === sg.snapshotId && q._1._1._1._1._1._1._1._1.specificationGroupId === sg.id)
				.filter(_._1._1._1._1._1._1._1._1._1.snapshotId === snapshotId.get)
				.filterOpt(domainIds)((q, ids) => q._1._1._1._1._1._1._1._1._1.domainId inSet ids)
				.filterOpt(specIds)((q, ids) => q._1._1._1._1._1._1._1._1._1.specificationId inSet ids)
				.filterOpt(specGroupIds)((q, ids) => q._1._1._1._1._1._1._1._1._1.specificationGroupId inSet ids)
				.filterOpt(actorIds)((q, ids) => q._1._1._1._1._1._1._1._1._1.actorId inSet ids)
				.filterOpt(communityIds)((q, ids) => q._1._1._1._1._1._1._1._1._2.community inSet ids)
				.filterOpt(communityHelper.organisationIdsToUse(organizationIds, orgParameters))((q, ids) => q._1._1._1._1._1._1._1._1._1.organisationId inSet ids)
				.filterOpt(communityHelper.systemIdsToUse(systemIds, sysParameters))((q, ids) => q._1._1._1._1._1._1._1._1._1.systemId inSet ids)
				.map(x => (
					(x._1._1._2.id, x._1._1._2.shortname), // 1.1: Community ID, 1.2: Community shortname
					(x._1._1._1._2.id, x._1._1._1._2.shortname), // 2.1: Organisation ID, 2.2: Organisation shortname
					(x._1._1._1._1._2.id, x._1._1._1._1._2.shortname), // 3.1: System ID, 3.2: System shortname
					(x._1._1._1._1._1._2.id, x._1._1._1._1._1._2.shortname, x._1._1._1._1._1._2.fullname), // 4.1: Domain ID, 4.2: Domain shortname, 4.3: Domain fullname
					(x._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._2.actorId, x._1._1._1._1._1._1._2.name), // 5.1: Actor ID, 5.2: Actor identifier, 5.3: Actor name
					(x._1._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._1._2.fullname, x._1._1._1._1._1._1._1._2.displayOrder), // 6.1: Specification ID, 6.2: Specification shortname, 6.3: Specification fullname, 6.4: Specification order
					(x._1._1._1._1._1._1._1._1._1.result, x._1._1._1._1._1._1._1._1._1.testSessionId, x._1._1._1._1._1._1._1._1._1.updateTime), // 7.1: Result, 7.2: Session ID, 7.3: Update time
					(x._2.map(_.shortname), x._2.map(_.fullname), x._2.map(_.id), x._2.map(_.displayOrder)), // 8.1: Specification group shortname, 8.2: Specification group fullname, 8.3: Specification group ID, 8.4: Specification group order
					(x._1._2.isOptional, x._1._2.isDisabled, x._1._1._1._1._1._1._1._1._1.testCaseGroupId) // 9.1: Test case optional, 9.2: Test case disabled, 9.3: Test case group ID
				))
		} else {
			PersistenceSchema.conformanceResults
				.join(PersistenceSchema.specifications).on(_.spec === _.id)
				.join(PersistenceSchema.actors).on(_._1.actor === _.id)
				.join(PersistenceSchema.domains).on(_._2.domain === _.id)
				.join(PersistenceSchema.systems).on(_._1._1._1.sut === _.id)
				.join(PersistenceSchema.organizations).on(_._2.owner === _.id)
				.join(PersistenceSchema.communities).on(_._2.community === _.id)
				.join(PersistenceSchema.testCases).on(_._1._1._1._1._1._1.testcase === _.id)
				.joinLeft(PersistenceSchema.specificationGroups).on(_._1._1._1._1._1._1._2.group === _.id)
				.filterOpt(domainIds)((q, ids) => q._1._1._1._1._1._2.id inSet ids)
				.filterOpt(specIds)((q, ids) => q._1._1._1._1._1._1._1._1.spec inSet ids)
				.filterOpt(specGroupIds)((q, ids) => q._1._1._1._1._1._1._1._2.group inSet ids)
				.filterOpt(actorIds)((q, ids) => q._1._1._1._1._1._1._1._1.actor inSet ids)
				.filterOpt(communityIds)((q, ids) => q._1._1._2.id inSet ids)
				.filterOpt(communityHelper.organisationIdsToUse(organizationIds, orgParameters))((q, ids) => q._1._1._1._2.id inSet ids)
				.filterOpt(communityHelper.systemIdsToUse(systemIds, sysParameters))((q, ids) => q._1._1._1._1._1._1._1._1.sut inSet ids)
				.map(x => (
					(x._1._1._2.id, x._1._1._2.shortname), // 1.1: Community ID, 1.2: Community shortname
					(x._1._1._1._2.id, x._1._1._1._2.shortname), // 2.1: Organisation ID, 2.2: Organisation shortname
					(x._1._1._1._1._2.id, x._1._1._1._1._2.shortname), // 3.1: System ID, 3.2: System shortname
					(x._1._1._1._1._1._2.id, x._1._1._1._1._1._2.shortname, x._1._1._1._1._1._2.fullname), // 4.1: Domain ID, 4.2: Domain shortname, 4.3: Domain fullname
					(x._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._2.actorId, x._1._1._1._1._1._1._2.name), // 5.1: Actor ID, 5.2: Actor identifier, 5.3: Actor name
					(x._1._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._1._2.fullname, x._1._1._1._1._1._1._1._2.displayOrder), // 6.1: Specification ID, 6.2: Specification shortname, 6.3: Specification fullname, 6.4: Specification order
					(x._1._1._1._1._1._1._1._1.result, x._1._1._1._1._1._1._1._1.testsession, x._1._1._1._1._1._1._1._1.updateTime), // 7.1: Result, 7.2: Session ID, 7.3: Update time
					(x._2.map(_.shortname), x._2.map(_.fullname), x._2.map(_.id), x._2.map(_.displayOrder)), // 8.1: Specification group shortname, 8.2: Specification group fullname, 8.3: Specification group ID, 8.4: Specification group order
					(x._1._2.isOptional, x._1._2.isDisabled, x._1._2.group) // 9.1: Test case optional, 9.2: Test case disabled, 9.3: Test case group ID
				))
		}
		// Apply sorting
		val sortColumnToApply = sortColumn.getOrElse("community")
		val sortOrderToApply = sortOrder.getOrElse("asc")
		val results = exec(
			query.sortBy(x => {
					val community = x._1._2.asc
					val organisation = x._2._2.asc
					val system = x._3._2.asc
					val domain = x._4._2.asc
					val specification = x._6._2.asc
					val actor = x._5._2.asc
					val specificationGroup = x._8._1.getOrElse("").asc
					sortColumnToApply match {
						case "organisation" => (if (sortOrderToApply == "asc") organisation.asc else organisation.desc, community, system, domain, specificationGroup, specification, actor)
						case "system" => (if (sortOrderToApply == "asc") system.asc else system.desc, community, organisation, domain, specificationGroup, specification, actor)
						case "domain" => (if (sortOrderToApply == "asc") domain.asc else domain.desc, specificationGroup, specification, community, organisation, system, actor)
						case "specification" => (if (sortOrderToApply == "asc") specificationGroup.asc else specificationGroup.desc, specification, community, organisation, system, domain, actor)
						case "actor" => (if (sortOrderToApply == "asc") actor.asc else actor.desc, community, organisation, system, domain, specificationGroup, specification)
						case _ => (if (sortOrderToApply == "asc") community.asc else community.desc, organisation, system, domain, specificationGroup, specification, actor) // Community (or default)
					}
				})
				.result.map(_.toList)
		)
		val resultBuilder = new ConformanceStatusBuilder[ConformanceStatementFull](recordDetails = false)
		results.foreach { result =>
			var specName = result._6._2
			if (result._8._1.nonEmpty) {
				specName = result._8._1.get + " - " + specName
			}
			var specNameFull = result._6._3
			if (result._8._2.nonEmpty) {
				specNameFull = result._8._2.get + " - " + specNameFull
			}
			val conformanceStatement = new ConformanceStatementFull(
				communityId = result._1._1, communityName = result._1._2, organizationId = result._2._1, organizationName = result._2._2,
				systemId = result._3._1, systemName = result._3._2, systemBadgeKey = "", systemDescription = None, systemVersion = None,
				domainId = result._4._1, domainName = result._4._2, domainNameFull = result._4._3, domainDescription = None, domainReportMetadata = None,
				actorId = result._5._1, actorName = result._5._2, actorFull = result._5._3, actorDescription = None, actorReportMetadata = None, actorApiKey = "",
				specificationId = result._6._1, specificationName = specName, specificationNameFull = specNameFull, specificationDescription = None, specificationReportMetadata = None, specificationDisplayOrder = result._6._4,
				specificationGroupId = result._8._3, specificationGroupName = result._8._1, specificationGroupDescription = None, specificationGroupReportMetadata = None, specificationGroupNameFull = result._8._1, specificationGroupDisplayOrder = result._8._4, specificationGroupOptionName = result._6._2, specificationGroupOptionNameFull = result._6._3,
				testSuiteId = None, testSuiteName = None, testSuiteDescription = None, testSuiteSpecReference = None, testSuiteSpecDescription = None, testSuiteSpecLink = None, testSuiteVersion = "",
				testCaseId = None, testCaseName = None, testCaseDescription = None,
				testCaseOptional = Some(result._9._1), testCaseDisabled = Some(result._9._2), testCaseTags = None, testCaseOrder = None, testCaseSpecReference = None, testCaseSpecDescription = None, testCaseSpecLink = None, testCaseVersion = "",
				testCaseGroupId = result._9._3, testCaseGroupIdentifier = None, testCaseGroupName = None, testCaseGroupDescription = None,
				result = result._7._1, outputMessage = None, sessionId = result._7._2, updateTime = result._7._3,
				completedTests = 0L, failedTests = 0L, undefinedTests = 0L,
				completedOptionalTests = 0L, failedOptionalTests = 0L, undefinedOptionalTests = 0L,
				completedTestsToConsider = 0L, failedTestsToConsider = 0L, undefinedTestsToConsider = 0L)
			resultBuilder.addConformanceResult(conformanceStatement, result._9._1, result._9._2, result._9._3)
		}
		resultBuilder.getOverview(Some(new ConformanceStatusBuilder.FilterCriteria(
			dateFromFilterString(updateTimeStart),
			dateFromFilterString(updateTimeEnd),
			status))
		)
	}

	private def valueOrNone(result: Option[String], withValue: Boolean): Option[String] = {
		if (withValue) {
			result
		} else {
			None
		}
	}

	private def toConformanceResult(result: ConformanceStatementTuple, systemId: Long, withDescriptions: Boolean): ConformanceStatement = {
			new ConformanceStatement(
				result._1._1, result._1._2, result._1._2, valueOrNone(result._1._3, withDescriptions), None,
				result._4._1, result._4._2, result._4._3, valueOrNone(result._4._4, withDescriptions), None,
				result._2._1, result._2._2, result._2._2, valueOrNone(result._2._4, withDescriptions), None,
				systemId, result._5._1, result._5._2,
				0L, 0L, 0L,
				0L, 0L, 0L,
				0L, 0L, 0L,
				result._3._1, result._3._2, valueOrNone(result._3._4, withDescriptions), None,
				result._2._3, result._3._3
			)
	}

	def getSystemInfoFromConformanceSnapshot(systemId: Long, snapshotId: Long): System = {
		val result = exec(
			PersistenceSchema.conformanceSnapshotResults
				.join(PersistenceSchema.conformanceSnapshotSystems).on((q, sys) => q.snapshotId === sys.snapshotId && q.systemId === sys.id)
				.join(PersistenceSchema.conformanceSnapshotOrganisations).on((q, org) => q._1.snapshotId === org.snapshotId && q._1.organisationId === org.id)
				.join(PersistenceSchema.conformanceSnapshots).on(_._1._1.snapshotId === _.id)
				.filter(_._2.id === snapshotId)
				.filter(_._1._1._1.systemId === systemId)
			.map(x => (
				x._1._1._2, // System info
				x._1._2, // Organisation info
				x._2.community // Community ID
			)).result.head
		)
		new System(result._1.id, result._1.shortname, result._1.fullname, result._1.description, None, result._1.apiKey, result._1.badgeKey,
			Some(Organizations(result._2.id, result._2.shortname, result._2.fullname, OrganizationType.Vendor.id.toShort, adminOrganization = false, None, None, None, template = false, None, result._2.apiKey, result._3)),
			None
		)
	}

	def getConformanceStatementsForSystem(systemId: Long, actorId: Option[Long] = None, snapshotId: Option[Long] = None, withDescriptions: Boolean = false, withResults: Boolean = true): Iterable[ConformanceStatementItem] = {
		val results =
			exec(for {
			statements <- {
				var query: ConformanceStatementDbQuery = if (snapshotId.isEmpty) {
					// Latest status.
					PersistenceSchema.conformanceResults
						.join(PersistenceSchema.specifications).on(_.spec === _.id)
						.join(PersistenceSchema.actors).on(_._1.actor === _.id)
						.join(PersistenceSchema.domains).on(_._1._2.domain === _.id)
						.join(PersistenceSchema.testCases).on(_._1._1._1.testcase === _.id)
						.joinLeft(PersistenceSchema.specificationGroups).on(_._1._1._1._2.group === _.id)
						.filter(_._1._1._1._1._1.sut === systemId)
						.filterOpt(actorId)((q, id) => q._1._1._1._1._1.actor === id)
						.map(x => (
							(x._1._1._2.id, x._1._1._2.fullname, x._1._1._2.description), // 1.1: Domain ID, 1.2: Domain name, 1.3: Description
							(x._1._1._1._1._2.id, x._1._1._1._1._2.fullname, x._1._1._1._1._2.displayOrder, x._1._1._1._1._2.description), // 2.1: Specification ID, 2.2: Specification name, 2.3: Specification display order, 2.4: Specification description
							(x._1._1._1._1._2.group, x._2.map(_.fullname), x._2.map(_.displayOrder), x._2.map(_.description).flatten), // 3.1: Specification group ID, 3.2: Specification group name, 3.3: Specification group display order, 3.4: Specification group description
							(x._1._1._1._2.id, x._1._1._1._2.actorId, x._1._1._1._2.name, x._1._1._1._2.desc), // 4.1: Actor ID, 4.2: Actor identifier, 4.3: Actor name, 4.4: Actor description
							(x._1._1._1._1._1.result, x._1._1._1._1._1.updateTime), // 5.1: Result, 5.2: Update time
							(x._1._2.isOptional, x._1._2.isDisabled, x._1._2.group) // 6.1: Optional test case, 6.2: Disabled test case, 6.3: Test case group
						))
				} else {
					PersistenceSchema.conformanceSnapshotResults
						.join(PersistenceSchema.conformanceSnapshotSpecifications).on((q, spec) => q.snapshotId === spec.snapshotId && q.specificationId === spec.id)
						.join(PersistenceSchema.conformanceSnapshotActors).on((q, act) => q._1.snapshotId === act.snapshotId && q._1.actorId === act.id)
						.join(PersistenceSchema.conformanceSnapshotDomains).on((q, dom) => q._1._1.snapshotId === dom.snapshotId && q._1._1.domainId === dom.id)
						.join(PersistenceSchema.conformanceSnapshotTestCases).on((q, tc) => q._1._1._1.snapshotId === tc.snapshotId && q._1._1._1.testCaseId === tc.id)
						.joinLeft(PersistenceSchema.conformanceSnapshotSpecificationGroups).on((q, sg) => q._1._1._1._1.snapshotId === sg.snapshotId && q._1._1._1._1.specificationGroupId === sg.id)
						.filter(_._1._1._1._1._1.snapshotId === snapshotId.get)
						.filter(_._1._1._1._1._1.systemId === systemId)
						.filterOpt(actorId)((q, id) => q._1._1._1._1._1.actorId === id)
						.map(x => (
							(x._1._1._2.id, x._1._1._2.fullname, x._1._1._2.description), // 1.1: Domain ID, 1.2: Domain name, 1.3: Description
							(x._1._1._1._1._2.id, x._1._1._1._1._2.fullname, x._1._1._1._1._2.displayOrder, x._1._1._1._1._2.description), // 2.1: Specification ID, 2.2: Specification name, 2.3: Specification display order, 2.4: Specification description
							(x._1._1._1._1._1.specificationGroupId, x._2.map(_.fullname), x._2.map(_.displayOrder), x._2.map(_.description).flatten), // 3.1: Specification group ID, 3.2: Specification group name, 3.3: Specification group display order, 3.4: Specification group description
							(x._1._1._1._2.id, x._1._1._1._2.actorId, x._1._1._1._2.name, x._1._1._1._2.description), // 4.1: Actor ID, 4.2: Actor identifier, 4.3: Actor name, 4.4: Actor description
							(x._1._1._1._1._1.result, x._1._1._1._1._1.updateTime), // 5.1: Result, 5.2: Update time
							(x._1._2.isOptional, x._1._2.isDisabled, x._1._1._1._1._1.testCaseGroupId) // 6.1: Optional test case, 6.2: Disabled test case, 6.3: Test case group
						))
				}
				if (!withResults) {
					query = query.take(1)
				}
				query
					.result
					.map(rawResults => {
						if (withResults) {
							val resultBuilder = new ConformanceStatusBuilder[ConformanceStatement](recordDetails = false)
							rawResults.foreach { result =>
								resultBuilder.addConformanceResult(toConformanceResult(result, systemId, withDescriptions), result._6._1, result._6._2, result._6._3)
							}
							resultBuilder.getOverview(None)
						} else {
							rawResults.map(toConformanceResult(_, systemId, withDescriptions))
						}
					})
			}
			actorIdsToDisplay <- getActorIdsToDisplayInStatements(statements, snapshotId)
		} yield (statements, actorIdsToDisplay))
		createConformanceItemTree(ConformanceItemTreeData(results._1, Some(results._2)), withResults, snapshotId)
	}

	def getActorIdsToDisplayInStatementsWrapper(statements: Iterable[ConformanceStatement], snapshotId: Option[Long]): Set[Long] = {
		exec(getActorIdsToDisplayInStatements(statements, snapshotId))
	}

	private def getActorIdsToDisplayInStatementsByCommunity(communityId: Long): DBIO[Set[Long]] = {
		for {
			communityDomainId <- PersistenceSchema.communities.filter(_.id === communityId).map(_.domain).result.headOption
			actorIds <- PersistenceSchema.testCaseHasActors
				.join(PersistenceSchema.actors).on(_.actor === _.id)
				.filter(_._1.sut === true)
				.filter(_._2.hidden === false)
				.filterOpt(communityDomainId)((q, domainId) => q._2.domain === domainId)
				.map(x => (x._1.specification, x._1.actor))
				.distinct
				.result
				.map { actorResults =>
					// This map gives us the SUT actors IDs per specification.
					val specMap = new mutable.HashMap[Long, mutable.HashSet[Long]]() // Spec ID to set of actor IDs
					actorResults.foreach { actorResult =>
						if (!specMap.contains(actorResult._1)) {
							specMap += (actorResult._1 -> new mutable.HashSet[Long]())
						}
						specMap(actorResult._1).add(actorResult._2)
					}
					// Return the actor IDs of the specifications that have more than one SUT actor.
					val ids = mutable.HashSet[Long]()
					specMap.filter(entry => entry._2.size > 1).values.foreach { idSet =>
						idSet.foreach { id =>
							ids += id
						}
					}
					ids.toSet
				}
		} yield actorIds
	}

	private def getActorIdsToDisplayInStatements(statements: Iterable[ConformanceStatement], snapshotId: Option[Long]): DBIO[Set[Long]] = {
		if (snapshotId.isDefined) {
			PersistenceSchema.conformanceSnapshotActors
				.filter(_.snapshotId === snapshotId.get)
				.filter(_.visible === true)
				.map(_.id)
				.result
				.map(_.toSet)
		} else {
			PersistenceSchema.testCaseHasActors
				.join(PersistenceSchema.actors).on(_.actor === _.id)
				.filter(_._1.specification inSet statements.map(_.specificationId))
				.filter(_._1.sut === true)
				.filter(_._2.hidden === false)
				.map(x => (x._1.specification, x._1.actor))
				.distinct
				.result
				.map { actorResults =>
					// This map gives us the SUT actors IDs per specification.
					val specMap = new mutable.HashMap[Long, mutable.HashSet[Long]]() // Spec ID to set of actor IDs
					actorResults.foreach { actorResult =>
						if (!specMap.contains(actorResult._1)) {
							specMap += (actorResult._1 -> new mutable.HashSet[Long]())
						}
						specMap(actorResult._1).add(actorResult._2)
					}
					// Check to see if the actor IDs we loaded previously have also other SUT actors.
					// If yes these are actors we will want to display.
					val actorIdsToDisplay = statements.filter { statement =>
						specMap(statement.specificationId).size > 1 // We have more than one actor as a SUT
					}.map(_.actorId)
					actorIdsToDisplay.toSet
				}
		}
	}

	def createConformanceItemTree(data: ConformanceItemTreeData, withResults: Boolean, snapshotId: Option[Long], testSuiteMapper: Option[ConformanceStatement => List[ConformanceTestSuite]] = None): List[ConformanceStatementItem] = {
		val actorIdsToDisplay = data.actorIdsToDisplay.getOrElse(exec(getActorIdsToDisplayInStatements(data.statements, snapshotId)))
		// Convert to hierarchical item structure.
		val domainMap = new mutable.HashMap[Long, (ConformanceStatementItem,
			mutable.HashMap[Long, (ConformanceStatementItem, mutable.HashMap[Long, (ConformanceStatementItem, ListBuffer[ConformanceStatementItem])])], // Specification groups pointing to specifications
			mutable.HashMap[Long, (ConformanceStatementItem, ListBuffer[ConformanceStatementItem])], // Specifications not in groups
			)]()
		data.statements.foreach { statement =>
			val actorResults = if (withResults) {
				Some(ConformanceStatementResults(
					statement.updateTime,
					statement.completedTests, statement.failedTests, statement.undefinedTests,
					statement.completedOptionalTests, statement.failedOptionalTests, statement.undefinedOptionalTests,
					statement.completedTestsToConsider, statement.failedTestsToConsider, statement.undefinedTestsToConsider,
					// If a test suite mapper is defined we are interested in also recording the test suites and test cases per statement. This will be handled by the mapper.
					testSuiteMapper.map(_.apply(statement))
				))
			} else {
				None
			}
			val actorItem = ConformanceStatementItem(statement.actorId, statement.actorFull, statement.actorDescription, statement.actorReportMetadata, ConformanceStatementItemType.ACTOR, None, 0,
				actorResults, actorIdsToDisplay.contains(statement.actorId)
			)
			if (!domainMap.contains(statement.domainId)) {
				domainMap += (statement.domainId -> (ConformanceStatementItem(statement.domainId, statement.domainNameFull, statement.domainDescription, statement.domainReportMetadata, ConformanceStatementItemType.DOMAIN, None, 0), new mutable.HashMap(), new mutable.HashMap()))
			}
			val domainEntry = domainMap(statement.domainId)
			if (statement.specificationGroupId.isDefined) {
				// Specification group
				if (!domainEntry._2.contains(statement.specificationGroupId.get)) {
					// Record the specification group
					domainEntry._2 += (statement.specificationGroupId.get -> (
						ConformanceStatementItem(statement.specificationGroupId.get, statement.specificationGroupName.get, statement.specificationGroupDescription, statement.specificationGroupReportMetadata, ConformanceStatementItemType.SPECIFICATION_GROUP, None, statement.specificationGroupDisplayOrder.getOrElse(0)),
						new mutable.HashMap()
					)
						)
				}
				val specificationGroupEntry = domainEntry._2(statement.specificationGroupId.get)
				if (!specificationGroupEntry._2.contains(statement.specificationId)) {
					// Record the specification
					specificationGroupEntry._2 += (statement.specificationId -> (ConformanceStatementItem(statement.specificationId, statement.specificationNameFull, statement.specificationDescription, statement.specificationReportMetadata, ConformanceStatementItemType.SPECIFICATION, None, statement.specificationDisplayOrder), new ListBuffer))
				}
				// Record the actor
				val specificationEntry = specificationGroupEntry._2(statement.specificationId)
				specificationEntry._2 += actorItem
			} else {
				// Specification not in group
				if (!domainEntry._3.contains(statement.specificationId)) {
					domainEntry._3 += (statement.specificationId -> (ConformanceStatementItem(statement.specificationId, statement.specificationNameFull, statement.specificationDescription, statement.specificationReportMetadata, ConformanceStatementItemType.SPECIFICATION, None, statement.specificationDisplayOrder), new ListBuffer))
				}
				// Record the actor
				val specificationEntry = domainEntry._3(statement.specificationId)
				specificationEntry._2 += actorItem
			}
		}
		// Now convert the collected children to their final form.
		val domains = domainMap.values.map { domainEntry =>
			val specificationGroups = domainEntry._2.values.map { specificationGroupEntry =>
				val specifications = specificationGroupEntry._2.values.map { specificationEntry =>
					specificationEntry._1.withChildren(specificationEntry._2.toList)
				}
				specificationGroupEntry._1.withChildren(specifications.toSeq)
			}
			val specifications = domainEntry._3.values.map { specificationEntry =>
				specificationEntry._1.withChildren(specificationEntry._2.toList)
			}
			domainEntry._1.withChildren(specificationGroups.toSeq ++ specifications.toSeq)
		}
		sortConformanceStatementItems(domains.toList).toList
	}

	private def sortConformanceStatementItems(items: Seq[ConformanceStatementItem]): Seq[ConformanceStatementItem] = {
		val sortedItems = new ListBuffer[ConformanceStatementItem]()
		items.foreach { item =>
			if (item.items.isDefined && item.items.get.nonEmpty) {
				sortedItems += item.withChildren(sortConformanceStatementItems(item.items.get))
			} else {
				sortedItems += item
			}
		}
		sortedItems.sortWith((a, b) => {
			(a.displayOrder < b.displayOrder) || (a.displayOrder == b.displayOrder && a.name.compareTo(b.name) < 0)
		}).toSeq
	}

	def updateStatementConfiguration(userId: Long, systemId: Long, actorId: Long,
																	 organisationPropertyValues: Option[List[OrganisationParameterValues]],
																	 systemPropertyValues: Option[List[SystemParameterValues]],
																	 statementPropertyValues: Option[List[Configs]],
																	 organisationPropertyFiles: Map[Long, FileInfo],
																	 systemPropertyFiles: Map[Long, FileInfo],
																	 statementPropertyFiles: Map[Long, FileInfo]
																	): Unit = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val dbAction = for {
			// Check if the user is the admin (to see if she can update admin-only properties).
			isAdmin <- PersistenceSchema.users.filter(_.id === userId).map(x => x.role).result.head.map(x => {
				x == UserRole.CommunityAdmin.id.toShort || x == UserRole.SystemAdmin.id.toShort
			})
			// Retrieve community ID.
			systemIds <- PersistenceSchema.systems
				.join(PersistenceSchema.organizations).on(_.owner === _.id)
				.filter(_._1.id === systemId)
				.map(x => (x._1.id, x._1.owner, x._2.community)) // (system ID, organisation ID, community ID)
				.result
				.head
			// Update organisation properties.
			_ <- {
				if (organisationPropertyValues.isDefined) {
					organisationManager.saveOrganisationParameterValues(systemIds._2, systemIds._3, isAdmin, organisationPropertyValues.get, organisationPropertyFiles, onSuccessCalls)
				} else {
					DBIO.successful(())
				}
			}
			// Update system properties.
			_ <- {
				if (systemPropertyValues.isDefined) {
					systemManager.saveSystemParameterValues(systemId, systemIds._3, isAdmin, systemPropertyValues.get, systemPropertyFiles, onSuccessCalls)
				} else {
					DBIO.successful(())
				}
			}
			// Update statement properties.
			_ <- {
				if (statementPropertyValues.isDefined) {
					saveStatementParameterValues(systemId, actorId, isAdmin, statementPropertyValues.get, statementPropertyFiles, onSuccessCalls)
				} else {
					DBIO.successful(())
				}
			}
		} yield systemIds._3 // communityID
		val communityId = exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
		triggerHelper.publishTriggerEvent(new ConformanceStatementUpdatedEvent(communityId, systemId, actorId))

	}

	private def saveStatementParameterValues(systemId: Long, actorId: Long, isAdmin: Boolean, values: List[Configs], files: Map[Long, FileInfo], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
		for {
			providedParameters <- {
				val tempMap = new mutable.HashMap[Long, Configs]()
				values.foreach{ v =>
					tempMap += (v.parameter -> v)
				}
				DBIO.successful(tempMap.toMap)
			}
			// Load parameter definitions for the actor
			parameterDefinitions <- PersistenceSchema.parameters
				.join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
				.filter(_._2.actor === actorId)
				.map(_._1)
				.result
			// Make updates
			_ <- {
				val actions = new ListBuffer[DBIO[_]]()
				parameterDefinitions.foreach { parameterDefinition =>
					if ((!parameterDefinition.adminOnly && !parameterDefinition.hidden) || isAdmin) {
						val matchedProvidedParameter = providedParameters.get(parameterDefinition.id)
						if (matchedProvidedParameter.isDefined) {
							// Create or update
							if (parameterDefinition.kind != "SECRET" || (parameterDefinition.kind == "SECRET" && matchedProvidedParameter.get.value != "")) {
								// Special case: No update for secret parameters that are defined but not updated.
								var valueToSet = matchedProvidedParameter.get.value
								var existingBinaryNotUpdated = false
								var contentTypeToSet: Option[String] = None
								if (parameterDefinition.kind == "SECRET") {
									// Encrypt secret value at rest.
									valueToSet = MimeUtil.encryptString(valueToSet)
								} else if (parameterDefinition.kind == "BINARY") {
									// Store file.
									if (files.contains(parameterDefinition.id)) {
										contentTypeToSet = files(parameterDefinition.id).contentType
										onSuccessCalls += (() => repositoryUtils.setStatementParameterFile(parameterDefinition.id, systemId, files(parameterDefinition.id).file))
									} else {
										existingBinaryNotUpdated = true
									}
								}
								if (!existingBinaryNotUpdated) {
									actions += PersistenceSchema.configs.filter(_.parameter === parameterDefinition.id).filter(_.system === systemId).delete
									actions += (PersistenceSchema.configs += Configs(systemId, matchedProvidedParameter.get.parameter, parameterDefinition.endpoint, valueToSet, contentTypeToSet))
								}
							}
						} else {
							// Delete existing (if present)
							onSuccessCalls += (() => repositoryUtils.deleteStatementParameterFile(parameterDefinition.id, systemId))
							actions += PersistenceSchema.configs.filter(_.parameter === parameterDefinition.id).filter(_.system === systemId).delete
						}
					}
				}
				if (actions.nonEmpty) {
					DBIO.seq(actions.toList.map(a => a): _*)
				} else {
					DBIO.successful(())
				}
			}
		} yield ()
	}

	def getStatementParameterValues(systemId: Long, actorId: Long): List[ParametersWithValue] = {
		exec(PersistenceSchema.parameters
			.join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
			.joinLeft(PersistenceSchema.configs).on((p, v) => p._1.id === v.parameter && v.system === systemId)
			.filter(_._1._2.actor === actorId)
			.sortBy(x => (x._1._2.id.asc, x._1._1.displayOrder.asc, x._1._1.name.asc))
			.map(x => (x._1._1, x._2))
			.result
		).toList.map(r => new ParametersWithValue(r._1, r._2))
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
		val expectedEndpoints = endpointManager.getEndpointsCaseForActor(actorId)
		val endpointList: List[SystemConfigurationEndpoint] = expectedEndpoints.map(expectedEndpoint => {
			val expectedParameters = parameterManager.getEndpointParameters(expectedEndpoint.id)
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

	def deleteConformanceStatementsForDomainAndCommunity(domainId: Long, communityId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
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

	def createConformanceSnapshot(communityId: Long, label: String, publicLabel: Option[String], isPublic: Boolean): ConformanceSnapshot = {
		val snapshotTime = TimeUtil.getCurrentTimestamp()
		val apiKey = CryptoUtil.generateApiKey()
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val dbAction = for {
			// Load statement data
			communityResults <- {
				PersistenceSchema.conformanceResults
					.join(PersistenceSchema.specifications).on(_.spec === _.id)
					.joinLeft(PersistenceSchema.specificationGroups).on(_._2.group === _.id)
					.join(PersistenceSchema.actors).on(_._1._1.actor === _.id)
					.join(PersistenceSchema.domains).on(_._2.domain === _.id)
					.join(PersistenceSchema.systems).on(_._1._1._1._1.sut === _.id)
					.join(PersistenceSchema.organizations).on(_._2.owner === _.id)
					.join(PersistenceSchema.testSuites).on(_._1._1._1._1._1._1.testsuite === _.id)
					.join(PersistenceSchema.testCases).on(_._1._1._1._1._1._1._1.testcase === _.id)
					.joinLeft(PersistenceSchema.testCaseGroups).on(_._2.group === _.id)
					.filter(_._1._1._1._2.community === communityId)
					.filter(_._1._1._1._2.adminOrganization === false)
					.map(x => (
						(x._1._1._1._2.id, x._1._1._1._2.shortname, x._1._1._1._2.fullname, x._1._1._1._2.apiKey), // 1. Organisation
						(x._1._1._1._1._2.id, x._1._1._1._1._2.shortname, x._1._1._1._1._2.fullname, x._1._1._1._1._2.description, x._1._1._1._1._2.apiKey, x._1._1._1._1._2.badgeKey, x._1._1._1._1._2.version), // 2. System
						(x._1._1._1._1._1._2.id, x._1._1._1._1._1._2.shortname, x._1._1._1._1._1._2.fullname, x._1._1._1._1._1._2.description, x._1._1._1._1._1._2.reportMetadata), // 3. Domain
						(x._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._2.actorId, x._1._1._1._1._1._1._2.name, x._1._1._1._1._1._1._2.desc, x._1._1._1._1._1._1._2.apiKey, x._1._1._1._1._1._1._2.reportMetadata), // 4. Actor
						(x._1._1._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._1._1._2.fullname, x._1._1._1._1._1._1._1._1._2.description, x._1._1._1._1._1._1._1._1._2.apiKey, x._1._1._1._1._1._1._1._1._2.displayOrder, x._1._1._1._1._1._1._1._1._2.reportMetadata), // 5. Specification
						(x._1._1._1._1._1._1._1._2.map(_.id), x._1._1._1._1._1._1._1._2.map(_.shortname), x._1._1._1._1._1._1._1._2.map(_.fullname), x._1._1._1._1._1._1._1._2.map(_.description), x._1._1._1._1._1._1._1._2.map(_.displayOrder), x._1._1._1._1._1._1._1._2.map(_.reportMetadata)), // 6. Specification group
						(x._1._2.id, x._1._2.shortname, x._1._2.fullname, x._1._2.description, x._1._2.testSuiteOrder, x._1._2.identifier, x._1._2.isOptional, x._1._2.isDisabled, x._1._2.tags, x._1._2.specReference, x._1._2.specDescription, x._1._2.specLink, x._1._2.version), // 7. Test case
						(x._1._1._2.id, x._1._1._2.shortname, x._1._1._2.fullname, x._1._1._2.description, x._1._1._2.identifier, x._1._1._2.specReference, x._1._1._2.specDescription, x._1._1._2.specLink, x._1._1._2.version), // 8. Test suite
						(x._1._1._1._1._1._1._1._1._1.result, x._1._1._1._1._1._1._1._1._1.testsession, x._1._1._1._1._1._1._1._1._1.updateTime, x._1._1._1._1._1._1._1._1._1.outputMessage), // 9. Result
						(x._2.map(_.id), x._2.map(_.identifier), x._2.map(_.name).flatten, x._2.map(_.description).flatten) // 10. Test case group
					))
					.result
			}
			// Create snapshot
			snapshotId <- PersistenceSchema.insertConformanceSnapshot += ConformanceSnapshot(0L, label, publicLabel, snapshotTime, apiKey, isPublic, communityId)
			// Determine which actor IDs should be displayed when viewing conformance statements.
			visibleActorsIds <- getActorIdsToDisplayInStatementsByCommunity(communityId)
			// Populate snapshot
			_ <- {
				val dbActions = new ListBuffer[DBIO[_]]
				// Keep already processed data IDs in sets to avoid double processing errors.
				val addedOrganisations = new mutable.HashSet[Long]()
				val addedSystems = new mutable.HashSet[Long]()
				val addedDomains = new mutable.HashSet[Long]()
				val addedActors = new mutable.HashSet[Long]()
				val addedSpecifications = new mutable.HashSet[Long]()
				val addedSpecificationGroups = new mutable.HashSet[Long]()
				val addedTestCases = new mutable.HashSet[Long]()
				val addedTestCaseGroups = new mutable.HashSet[Long]()
				val addedTestSuites = new mutable.HashSet[Long]()
				communityResults.foreach { result =>
					// 1. Organisation
					dbActions += addIfNotProcessed(addedOrganisations, result._1._1, () => PersistenceSchema.conformanceSnapshotOrganisations += ConformanceSnapshotOrganisation(id = result._1._1, shortname = result._1._2, fullname = result._1._3, apiKey = result._1._4, snapshotId = snapshotId))
					// 2. System
					dbActions += addIfNotProcessed(addedSystems, result._2._1, () => PersistenceSchema.conformanceSnapshotSystems += ConformanceSnapshotSystem(id = result._2._1, shortname = result._2._2, fullname = result._2._3, version = result._2._7, description = result._2._4, apiKey = result._2._5, badgeKey = result._2._6, snapshotId = snapshotId))
					// 3. Domain
					dbActions += addIfNotProcessed(addedDomains, result._3._1, () => PersistenceSchema.conformanceSnapshotDomains += ConformanceSnapshotDomain(id = result._3._1, shortname = result._3._2, fullname = result._3._3, description = result._3._4, reportMetadata = result._3._5, snapshotId = snapshotId))
					// 4. Actor
					dbActions += addIfNotProcessed(addedActors, result._4._1, () => PersistenceSchema.conformanceSnapshotActors += ConformanceSnapshotActor(id = result._4._1, actorId = result._4._2, name = result._4._3, description = result._4._4, reportMetadata = result._4._6, visible = visibleActorsIds.contains(result._4._1), apiKey = result._4._5, snapshotId = snapshotId))
					// 5. Specification
					dbActions += addIfNotProcessed(addedSpecifications, result._5._1, () => PersistenceSchema.conformanceSnapshotSpecifications += ConformanceSnapshotSpecification(id = result._5._1, shortname = result._5._2, fullname = result._5._3, description = result._5._4, reportMetadata = result._5._7, apiKey = result._5._5, displayOrder = result._5._6, snapshotId = snapshotId))
					// 6. Specification group
					if (result._6._1.isDefined) {
						dbActions += addIfNotProcessed(addedSpecificationGroups, result._6._1.get, () => PersistenceSchema.conformanceSnapshotSpecificationGroups += ConformanceSnapshotSpecificationGroup(id = result._6._1.get, shortname = result._6._2.get, fullname = result._6._3.get, description = result._6._4.flatten, reportMetadata = result._6._6.flatten, displayOrder = result._6._5.get, snapshotId = snapshotId))
					}
					// 7. Test case
					dbActions += addIfNotProcessed(addedTestCases, result._7._1, () => PersistenceSchema.conformanceSnapshotTestCases += ConformanceSnapshotTestCase(id = result._7._1, shortname = result._7._2, fullname = result._7._3, description = result._7._4, version = result._7._13, testSuiteOrder = result._7._5, identifier = result._7._6, isOptional = result._7._7, isDisabled = result._7._8, tags = result._7._9, specReference = result._7._10, specDescription = result._7._11, specLink = result._7._12, snapshotId = snapshotId))
					// 8. Test case group
					if (result._10._1.isDefined) {
						dbActions += addIfNotProcessed(addedTestCaseGroups, result._10._1.get, () => PersistenceSchema.conformanceSnapshotTestCaseGroups += ConformanceSnapshotTestCaseGroup(id = result._10._1.get, identifier = result._10._2.get, name = result._10._3, description = result._10._4, snapshotId = snapshotId))
					}
					// 9. Test suite
					dbActions += addIfNotProcessed(addedTestSuites, result._8._1, () => PersistenceSchema.conformanceSnapshotTestSuites += ConformanceSnapshotTestSuite(id = result._8._1, shortname = result._8._2, fullname = result._8._3, description = result._8._4, version = result._8._9, identifier = result._8._5, specReference = result._8._6, specDescription = result._8._7, specLink = result._8._8, snapshotId = snapshotId))
					// 10. Result
					dbActions += (PersistenceSchema.insertConformanceSnapshotResult += ConformanceSnapshotResult(
						id = 0L, snapshotId = snapshotId, organisationId = result._1._1, systemId = result._2._1, domainId = result._3._1,
						actorId = result._4._1, specId = result._5._1, specGroupId = result._6._1,
						testCaseId = result._7._1, testCaseGroupId = result._10._1, testSuiteId = result._8._1,
						result = result._9._1, testSession = result._9._2, updateTime = result._9._3, outputMessage = result._9._4)
					)
				}
				toDBIO(dbActions)
			}
			// Copy conformance and conformance overview certificate settings
			_ <- copyConformanceCertificateSettingsToSnapshot(snapshotId, communityId)
			// Copy badges
			_ <- {
				onSuccessCalls += (() => {
					communityResults.map(_._5._1).toSet.foreach { specificationId =>
						repositoryUtil.addBadgesToConformanceSnapshot(specificationId, snapshotId)
					}
				})
				DBIO.successful(())
			}
		} yield snapshotId
		val snapshotId = exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
		ConformanceSnapshot(snapshotId, label, publicLabel, snapshotTime, apiKey, isPublic, communityId)
	}

	private def includesParameterPlaceholder(message: Option[String], messages: Iterable[ConformanceOverviewCertificateMessage], placeholderPrefix: String): Boolean = {
		(message.isDefined && message.get.contains(placeholderPrefix)) || (messages.nonEmpty && messages.exists(_.message.contains(placeholderPrefix)))
	}

	private def copyConformanceCertificateSettingsToSnapshot(snapshotId: Long, communityId: Long): DBIO[_] = {
		for {
			messageForStatementCertificates <- PersistenceSchema.conformanceCertificates.filter(_.community === communityId).filter(_.includeMessage === true).map(_.message).result.headOption
			messagesForOverviewCertificates <- PersistenceSchema.conformanceOverviewCertificateMessages.filter(_.community === communityId).result
			_ <- {
				val dbActions = new ListBuffer[DBIO[_]]
				val messageForStatementCertificatesToCheck = messageForStatementCertificates.flatten
				if (messageForStatementCertificatesToCheck.isDefined || messagesForOverviewCertificates.nonEmpty) {
					// We might have references to domain/org/sys parameters in messages - copy them to the snapshot
					if (includesParameterPlaceholder(messageForStatementCertificatesToCheck, messagesForOverviewCertificates, Constants.PlaceholderDomain+"{")) {
						dbActions += createSnapshotDomainParameterValues(communityId, snapshotId)
					}
					if (includesParameterPlaceholder(messageForStatementCertificatesToCheck, messagesForOverviewCertificates, Constants.PlaceholderOrganisation+"{")) {
						dbActions += createSnapshotOrganisationParameterValues(communityId, snapshotId)
					}
					if (includesParameterPlaceholder(messageForStatementCertificatesToCheck, messagesForOverviewCertificates, Constants.PlaceholderSystem+"{")) {
						dbActions += createSnapshotSystemParameterValues(communityId, snapshotId)
					}
					if (messageForStatementCertificatesToCheck.isDefined) {
						dbActions += (PersistenceSchema.conformanceSnapshotCertificateMessages += ConformanceSnapshotCertificateMessage(messageForStatementCertificates.get.get, snapshotId))
					}
					if (messagesForOverviewCertificates.nonEmpty) {
						messagesForOverviewCertificates.foreach { message =>
							dbActions += (PersistenceSchema.conformanceSnapshotOverviewCertificateMessages += ConformanceSnapshotOverviewCertificateMessage(
								0L, message.message, message.messageType, message.domain, message.group, message.specification, message.actor, snapshotId
							))
						}
					}
				}
				toDBIO(dbActions)
			}
		} yield ()
	}

	private def addIfNotProcessed(processedSet: mutable.HashSet[Long], id: Long, action: () => DBIO[Any]) = {
		if (processedSet.contains(id)) {
			DBIO.successful(())
		} else {
			processedSet.add(id)
			action.apply()
		}
	}

	private def getLatestConformanceStatusLabelInternal(communityId: Long): DBIO[Option[String]] = {
		PersistenceSchema.communities.filter(_.id === communityId).map(_.latestStatusLabel).result.head
	}

	def getLatestConformanceStatusLabel(communityId: Long): Option[String] = {
		exec(getLatestConformanceStatusLabelInternal(communityId))
	}

	def setLatestConformanceStatusLabel(communityId: Long, label: Option[String]): Unit = {
		exec(PersistenceSchema.communities.filter(_.id === communityId).map(_.latestStatusLabel).update(label).transactionally)
	}

	def getConformanceSnapshotsWithLatest(community: Long, onlyPublic: Boolean): (Seq[ConformanceSnapshot], Option[String]) = {
		exec(for {
			snapshots <- getConformanceSnapshotsInternal(community, onlyPublic)
			latestLabel <- getLatestConformanceStatusLabelInternal(community)
		} yield (snapshots, latestLabel))
	}

	def getPublicSnapshotLabel(communityId: Long, snapshotId: Option[Long]): Option[String] = {
		if (snapshotId.isEmpty) {
			getLatestConformanceStatusLabel(communityId)
		} else {
			exec(PersistenceSchema.conformanceSnapshots
				.filter(_.id === snapshotId.get)
				.filter(_.isPublic === true)
				.map(_.publicLabel)
				.result.headOption).flatten
		}
	}

	private def getConformanceSnapshotsInternal(community: Long, onlyPublic: Boolean): DBIO[Seq[ConformanceSnapshot]] = {
		PersistenceSchema.conformanceSnapshots
			.filter(_.community === community)
			.filterIf(onlyPublic)(_.isPublic === true)
			.sortBy(_.snapshotTime.desc)
			.result
	}

	def getConformanceSnapshots(community: Long, onlyPublic: Boolean): List[ConformanceSnapshot] = {
		exec(getConformanceSnapshotsInternal(community, onlyPublic)).toList
	}

	def getConformanceSnapshot(snapshot: Long): ConformanceSnapshot = {
		exec(PersistenceSchema.conformanceSnapshots
			.filter(_.id === snapshot)
			.result
			.head)
	}

	def existsInConformanceSnapshot(snapshot: Long, systemId: Long, organisationId: Long): Boolean = {
		exec(PersistenceSchema
			.conformanceSnapshotResults
			.filter(_.snapshotId === snapshot)
			.filter(_.systemId === systemId)
			.filter(_.organisationId === organisationId)
			.exists
			.result)
	}

	def editConformanceSnapshot(snapshot: Long, label: String, publicLabel: Option[String], isPublic: Boolean): Unit = {
		exec(PersistenceSchema.conformanceSnapshots
			.filter(_.id === snapshot)
			.map(x => (x.label, x.publicLabel, x.isPublic))
			.update((label, publicLabel, isPublic))
			.transactionally)
	}

	def deleteConformanceSnapshotsOfCommunity(community: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
		for {
			snapshotIds <- PersistenceSchema.conformanceSnapshots.filter(_.community === community).map(_.id).result
			_ <- {
				val dbActions = new ListBuffer[DBIO[_]]
				snapshotIds.foreach { snapshotId =>
					dbActions += deleteConformanceSnapshotInternal(snapshotId, onSuccessCalls)
				}
				toDBIO(dbActions)
			}
		} yield ()
	}

	def deleteConformanceSnapshot(snapshot: Long): Unit = {
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val dbAction = deleteConformanceSnapshotInternal(snapshot, onSuccessCalls)
		exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
	}

	private def deleteConformanceSnapshotInternal(snapshot: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
		for {
			// Delete results.
			_ <- PersistenceSchema.conformanceSnapshotResults.filter(_.snapshotId === snapshot).delete
			// Delete reference data.
			_ <- PersistenceSchema.conformanceSnapshotTestCaseGroups.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotTestCases.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotTestSuites.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotActors.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotSpecifications.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotSpecificationGroups.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotDomains.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotSystems.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotOrganisations.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotDomainParameters.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotOrganisationProperties.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotSystemProperties.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotCertificateMessages.filter(_.snapshotId === snapshot).delete
			_ <- PersistenceSchema.conformanceSnapshotOverviewCertificateMessages.filter(_.snapshotId === snapshot).delete
			// Delete snapshot.
			_ <- PersistenceSchema.conformanceSnapshots.filter(_.id === snapshot).delete
			// Delete badges.
			_ <- {
				onSuccessCalls += (() => repositoryUtil.deleteSnapshotBadges(snapshot))
				DBIO.successful(())
			}
		} yield ()
	}

	def getConformanceBadge(systemKey: String, actorKey: String, snapshotKey: Option[String], forReport: Boolean): Option[File] = {
		val query = if (snapshotKey.isDefined) {
			// Query conformance snapshot.
			for {
				snapshotId <- PersistenceSchema.conformanceSnapshots.filter(_.apiKey === snapshotKey.get).map(_.id).result.headOption
				statementIds <- {
					if (snapshotId.isDefined) {
						PersistenceSchema.conformanceSnapshotResults
							.join(PersistenceSchema.conformanceSnapshotSystems).on((q, sys) => q.snapshotId === sys.snapshotId && q.systemId === sys.id)
							.join(PersistenceSchema.conformanceSnapshotActors).on((q, act) => q._1.snapshotId === act.snapshotId && q._1.actorId === act.id)
							.filter(_._1._1.snapshotId === snapshotId.get)
							.filter(_._1._2.badgeKey === systemKey)
							.filter(_._2.apiKey === actorKey)
							.map(x => (x._1._1.systemId, x._1._1.specificationId, x._1._1.actorId))
							.result
							.headOption
					} else {
						DBIO.successful(None)
					}
				}
			} yield (statementIds.map(_._1), statementIds.map(_._2), statementIds.map(_._3), snapshotId)
		} else {
			// Query latest conformance status.
			for {
				systemId <- PersistenceSchema.systems.filter(_.badgeKey === systemKey).map(_.id).result.headOption
				specIds <- {
					if (systemId.isDefined) {
						PersistenceSchema.actors.filter(_.apiKey === actorKey)
							.join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
							.map(x => (x._2.specId, x._2.actorId))
							.result
							.headOption
					} else {
						DBIO.successful(None)
					}
				}
				snapshotId <- DBIO.successful(None)
			} yield (systemId, specIds.map(_._1), specIds.map(_._2), snapshotId)
		}
		val ids: (Option[Long], Option[Long], Option[Long], Option[Long]) = exec(query) // 1: System ID, 2: Specification ID, 3: Actor ID, 4: Snapshot ID
		val systemId = ids._1
		val specificationId = ids._2
		val actorId = ids._3
		val snapshotId = ids._4
		if (systemId.isDefined && specificationId.isDefined && actorId.isDefined && (snapshotKey.isEmpty || snapshotId.isDefined)) {
			val status = getConformanceStatus(actorId.get, systemId.get, testSuiteId = None, includeDisabled = false, snapshotId)
			if (status.isDefined) {
				repositoryUtil.getConformanceBadge(specificationId.get, actorId, snapshotId, status = status.get.result.value(), exactMatch = false, forReport)
			} else {
				None
			}
		} else {
			None
		}
	}

	def getConformanceBadgeByIds(systemId: Long, actorId: Long, snapshotId: Option[Long], forReport: Boolean): Option[File] = {
		val status = getConformanceStatus(actorId, systemId, testSuiteId = None, includeDisabled = false, snapshotId)
		if (status.isDefined) {
			val specificationId = if (snapshotId.isDefined) {
				exec(
					PersistenceSchema.conformanceSnapshotResults
						.filter(_.systemId === systemId)
						.filter(_.actorId === actorId)
						.filter(_.snapshotId === snapshotId)
						.map(_.specificationId)
						.result
						.head
				)
			} else {
				exec(PersistenceSchema.specificationHasActors.filter(_.actorId === actorId).map(_.specId).result.head)
			}
			repositoryUtil.getConformanceBadge(specificationId, Some(actorId), snapshotId, status = status.get.result.value(), exactMatch = false, forReport)
		} else {
			None
		}
	}

	def getConformanceBadgeUrl(systemId: Long, actorId: Long, snapshotId: Option[Long]): Option[String] = {
		val query = if (snapshotId.isDefined) {
			// Snapshot.
			PersistenceSchema.conformanceSnapshotResults
				.join(PersistenceSchema.conformanceSnapshots).on(_.snapshotId === _.id)
				.join(PersistenceSchema.conformanceSnapshotSystems).on((q, sys) => q._1.snapshotId === sys.snapshotId && q._1.systemId === sys.id)
				.join(PersistenceSchema.conformanceSnapshotActors).on((q, act) => q._1._1.snapshotId === act.snapshotId && q._1._1.actorId === act.id)
				.filter(_._1._1._1.systemId === systemId)
				.filter(_._1._1._1.actorId === actorId)
				.filter(_._1._1._2.id === snapshotId.get)
				.map(x => (x._1._2.badgeKey, x._2.apiKey, x._1._1._2.apiKey))
				.result
				.headOption
				.map(_.map(x => (x._1, x._2, Some(x._3))))
		} else {
			// Latest status.
			PersistenceSchema.conformanceResults
				.join(PersistenceSchema.systems).on(_.sut === _.id)
				.join(PersistenceSchema.actors).on(_._1.actor === _.id)
				.filter(_._1._1.sut === systemId)
				.filter(_._1._1.actor === actorId)
				.map(x => (x._1._2.badgeKey, x._2.apiKey))
				.result
				.headOption
				.map(_.map(x => (x._1, x._2, None)))
		}
		val keys = exec(query)
		if (keys.isDefined) {
			Some(
				StringUtils.appendIfMissing(Configurations.TESTBED_HOME_LINK, "/") // Base URL
					+ "badge/" // Badge API prefix
					+ keys.get._1 // System key
					+ "/" + keys.get._2 // Actor key
					+ keys.get._3.map(x => "/" + x).getOrElse("") // Snapshot key (optional)
			)
		} else {
			None
		}
	}

	def getSnapshotDomainParameters(snapshotId: Long): Iterable[ConformanceSnapshotDomainParameter] = {
		exec(PersistenceSchema.conformanceSnapshotDomainParameters.filter(_.snapshotId === snapshotId).result)
	}

	def getSnapshotOrganisationProperties(snapshotId: Long, organisationId: Long): Iterable[ConformanceSnapshotOrganisationProperty] = {
		exec(PersistenceSchema.conformanceSnapshotOrganisationProperties.filter(_.snapshotId === snapshotId).filter(_.organisationId === organisationId).result)
	}

	def getSnapshotSystemProperties(snapshotId: Long, systemId: Long): Iterable[ConformanceSnapshotSystemProperty] = {
		exec(PersistenceSchema.conformanceSnapshotSystemProperties.filter(_.snapshotId === snapshotId).filter(_.systemId === systemId).result)
	}

	private def createSnapshotDomainParameterValues(communityId: Long, snapshotId: Long): DBIO[_] = {
		for {
			params <- domainParameterManager.getDomainParametersByCommunityIdInternal(communityId, onlySimple = true, loadValues = true)
			_ <- {
				val actions = ListBuffer[DBIO[_]]()
				params.foreach { param =>
					if (param.value.isDefined) {
						actions += (PersistenceSchema.conformanceSnapshotDomainParameters += ConformanceSnapshotDomainParameter(
							param.domain, param.name, param.value.get, snapshotId
						))
					}
				}
				toDBIO(actions)
			}
		} yield ()
	}

	private def createSnapshotOrganisationParameterValues(communityId: Long, snapshotId: Long): DBIO[_] = {
		for {
			properties <- PersistenceSchema.organisationParameterValues
				.join(PersistenceSchema.organisationParameters).on(_.parameter === _.id)
				.filter(_._2.community === communityId)
				.filter(_._2.kind === "SIMPLE")
				.result
			_ <- {
				val actions = ListBuffer[DBIO[_]]()
				properties.foreach { property =>
					actions += (PersistenceSchema.conformanceSnapshotOrganisationProperties += ConformanceSnapshotOrganisationProperty(
						property._1.organisation, property._2.testKey, property._1.value, snapshotId
					))
				}
				toDBIO(actions)
			}
		} yield ()
	}

	private def createSnapshotSystemParameterValues(communityId: Long, snapshotId: Long): DBIO[_] = {
		for {
			properties <- PersistenceSchema.systemParameterValues
				.join(PersistenceSchema.systemParameters).on(_.parameter === _.id)
				.filter(_._2.community === communityId)
				.filter(_._2.kind === "SIMPLE")
				.result
			_ <- {
				val actions = ListBuffer[DBIO[_]]()
				properties.foreach { property =>
					actions += (PersistenceSchema.conformanceSnapshotSystemProperties += ConformanceSnapshotSystemProperty(
						property._1.system, property._2.testKey, property._1.value, snapshotId
					))
				}
				toDBIO(actions)
			}
		} yield ()
	}

}
