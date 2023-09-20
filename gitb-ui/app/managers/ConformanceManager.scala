package managers

import com.gitb.tr.TestResultType
import config.Configurations
import managers.ConformanceManager.{ConformanceResultDbQuery, ConformanceResultFullDbQuery, ConformanceStatusDbQuery}
import models.Enums.ConformanceStatementItemType
import models._
import org.apache.commons.lang3.StringUtils
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.{Query, Rep}
import utils.{CryptoUtil, RepositoryUtils, TimeUtil}
import utils.TimeUtil.dateFromFilterString

import java.io.File
import java.sql.Timestamp
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

object ConformanceManager {

	type ConformanceResultFullDbTuple = (
		(Rep[Long], Rep[String]), // Community
		(Rep[Long], Rep[String]), // Organisation
		(Rep[Long], Rep[String], Rep[String]), // System
		(Rep[Long], Rep[String], Rep[String]), // Domain
		(Rep[Long], Rep[String], Rep[String], Rep[String]), // Actor
		(Rep[Long], Rep[String], Rep[String]), // Specification
		(Rep[String], Rep[Option[String]], Rep[Option[Timestamp]], Rep[Option[String]]), // Result
		(Rep[Option[String]], Rep[Option[String]]), // Specification group
		(Rep[Long], Rep[String], Rep[Option[String]], Rep[Boolean], Rep[Boolean], Rep[Option[String]], Rep[Short]), // Test case
		(Rep[Long], Rep[String], Rep[Option[String]]) // Test suite
	)
	type ConformanceResultFullTuple = (
		(Long, String), // Community
		(Long, String), // Organisation
		(Long, String, String), // System
		(Long, String, String), // Domain
		(Long, String, String, String), // Actor
		(Long, String, String), // Specification
		(String, Option[String], Option[Timestamp], Option[String]), // Result
		(Option[String], Option[String]), // Specification group
		(Long, String, Option[String], Boolean, Boolean, Option[String], Short), // Test case
		(Long, String, Option[String]) // Test suite
	)
	type ConformanceResultFullDbQuery = Query[ConformanceResultFullDbTuple, ConformanceResultFullTuple, Seq]

	type ConformanceResultDbTuple = (
		(Rep[Long], Rep[String]), // Community
		(Rep[Long], Rep[String]), // Organisation
		(Rep[Long], Rep[String]), // System
		(Rep[Long], Rep[String], Rep[String]), // Domain
		(Rep[Long], Rep[String], Rep[String]), // Actor
		(Rep[Long], Rep[String], Rep[String]), // Specification
		(Rep[String], Rep[Option[String]], Rep[Option[Timestamp]]), // Result
		(Rep[Option[String]], Rep[Option[String]]), // Specification group
		(Rep[Boolean], Rep[Boolean]) // Test case
	)
	type ConformanceResultTuple = (
		(Long, String), // Community
		(Long, String), // Organisation
		(Long, String), // System
		(Long, String, String), // Domain
		(Long, String, String), // Actor
		(Long, String, String), // Specification
		(String, Option[String], Option[Timestamp]), // Result
		(Option[String], Option[String]), // Specification group
		(Boolean, Boolean) // Test case
	)
	type ConformanceResultDbQuery = Query[ConformanceResultDbTuple, ConformanceResultTuple, Seq]

	type ConformanceStatusDbTuple = (
		(Rep[Long], Rep[String], Rep[Option[String]], Rep[Boolean]), // Test suite
		(Rep[Long], Rep[String], Rep[Option[String]], Rep[Boolean], Rep[Boolean], Rep[Boolean], Rep[Option[String]], Rep[Short]), // Test case
		(Rep[String], Rep[Option[String]], Rep[Option[String]], Rep[Option[Timestamp]]) // Result
	)
	type ConformanceStatusTuple = (
		(Long, String, Option[String], Boolean), // Test suite
		(Long, String, Option[String], Boolean, Boolean, Boolean, Option[String], Short), // Test case
		(String, Option[String], Option[String], Option[Timestamp]) // Result
	)
	type ConformanceStatusDbQuery = Query[ConformanceStatusDbTuple, ConformanceStatusTuple, Seq]

}

@Singleton
class ConformanceManager @Inject() (repositoryUtil: RepositoryUtils, systemManager: SystemManager, endpointManager: EndPointManager, parameterManager: ParameterManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

	import dbConfig.profile.api._

	def getAvailableConformanceStatements(domainId: Option[Long], systemId: Long): (Boolean, Seq[ConformanceStatementItem]) = {
		exec(for {
			// Load the actors for which the system already has statements (these will be later skipped).
			actorIdsInExistingStatements <- PersistenceSchema.systemImplementsActors
				.filter(_.systemId === systemId)
				.map(_.actorId)
				.result
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
			// Load the relevant actors (excluding ones with existing statements).
			actors <- PersistenceSchema.actors
				.join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
				.filter(_._1.hidden === false)
				.filterNot(_._1.id inSet actorIdsInExistingStatements)
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
				val actorMap = new mutable.HashMap[Long, ListBuffer[ConformanceStatementItem]]() // Specification ID to Actor data
				val defaultActorMap = new mutable.HashMap[Long, ConformanceStatementItem]() // Specification ID to default Actor data
				// Map actors using specification ID.
				actors.foreach(x => {
					if (actorsWithTestCases.contains(x._1)) {
						// Only keep the actors that have test cases in which they are the SUT.
						val item = ConformanceStatementItem(x._1, x._2, x._3, ConformanceStatementItemType.ACTOR, None, 0)
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
							specificationInGroupMap.getOrElseUpdate(x._4.get, new ListBuffer[ConformanceStatementItem]).append(ConformanceStatementItem(x._1, x._2, x._3, ConformanceStatementItemType.SPECIFICATION, childActors, x._6))
						} else {
							// Map specifications using domain ID.
							specificationNotInGroupMap.getOrElseUpdate(x._5, new ListBuffer[ConformanceStatementItem]).append(ConformanceStatementItem(x._1, x._2, x._3, ConformanceStatementItemType.SPECIFICATION, childActors, x._6))
						}
					}
				})
				val specificationGroupMap = new mutable.HashMap[Long, ListBuffer[ConformanceStatementItem]]() // Domain ID to Specification group data
				// Map groups using domain ID.
				groups.foreach(x => {
					val childSpecifications = specificationInGroupMap.get(x._1).map(_.toList)
					if (childSpecifications.nonEmpty) {
						// Only keep groups with (non-empty) specifications.
						specificationGroupMap.getOrElseUpdate(x._4, new ListBuffer[ConformanceStatementItem]).append(ConformanceStatementItem(x._1, x._2, x._3, ConformanceStatementItemType.SPECIFICATION_GROUP, childSpecifications, x._5))
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
						domainItems.append(ConformanceStatementItem(x._1, x._2, x._3, ConformanceStatementItemType.DOMAIN, Some(children), 0))
					}
				})
				DBIO.successful((actorIdsInExistingStatements.nonEmpty, domainItems.toList))
			}
		} yield results)
	}

	def getCompletedConformanceStatementsForTestSession(systemId: Long, sessionId: String): List[Long] = { // Actor IDs considered as completed.
		exec(
			for {
				// Make sure we only do subsequent lookups if we have a non-optional, non-disabled test case
				relatedActorIds <- PersistenceSchema.conformanceResults
					.join(PersistenceSchema.testCases).on(_.testcase === _.id)
					.filter(_._1.testsession === sessionId)
					.filter(_._2.isOptional === false)
					.filter(_._2.isDisabled === false)
					.map(_._1.actor)
					.result
				conformanceResults <- {
					if (relatedActorIds.nonEmpty) {
						PersistenceSchema.conformanceResults
							.filter(_.sut === systemId)
							.filter(_.actor inSet relatedActorIds)
							.map(x => (x.actor, x.result))
							.result
					} else {
						DBIO.successful(Seq.empty)
					}
				}
				completedActors <- {
					if (conformanceResults.nonEmpty) {
						val map = mutable.LinkedHashMap[Long, Boolean]()
						conformanceResults.foreach { actorInfo =>
							val currentIsSuccess = "SUCCESS".equals(actorInfo._2)
							val overallIsSuccess = map.getOrElseUpdate(actorInfo._1, currentIsSuccess)
							if (overallIsSuccess && !currentIsSuccess) {
								map.put(actorInfo._1, currentIsSuccess)
							}
						}
						DBIO.successful(map.filter(_._2).keys.toList)
					} else {
						DBIO.successful(List.empty)
					}
				}
			} yield completedActors
		)
	}

	def getConformanceStatus(actorId: Long, sutId: Long, testSuiteId: Option[Long], includeDisabled: Boolean = true, snapshotId: Option[Long] = None): Option[ConformanceStatus] = {
		if (snapshotId.isEmpty && (actorId < 0 || sutId < 0 || testSuiteId.isDefined && testSuiteId.get < 0)) {
			None
		} else {
			val query: ConformanceStatusDbQuery = if (snapshotId.isDefined) {
				PersistenceSchema.conformanceSnapshotResults
					.filter(_.snapshotId === snapshotId.get)
					.filter(_.actorId === actorId)
					.filter(_.systemId === sutId)
					.filterIf(!includeDisabled)(_.testCaseIsDisabled === false)
					.filterOpt(testSuiteId)((q, id) => q.testSuiteId === id)
					.map(x => (
						(x.testSuiteId, x.testSuite, x.testSuiteDescription, false), // Test suite
						(x.testCaseId, x.testCase, x.testCaseDescription, false, x.testCaseIsOptional, x.testCaseIsDisabled, x.testCaseTags, x.testCaseOrder), // Test case
						(x.result, x.outputMessage, x.testSessionId, x.updateTime) // Result
					))
			} else {
				PersistenceSchema.conformanceResults
					.join(PersistenceSchema.testCases).on(_.testcase === _.id)
					.join(PersistenceSchema.testSuites).on(_._1.testsuite === _.id)
					.filter(_._1._1.actor === actorId)
					.filter(_._1._1.sut === sutId)
					.filterIf(!includeDisabled)(_._1._2.isDisabled === false)
					.filterOpt(testSuiteId)((q, id) => q._1._1.testsuite === id)
					.map(x => (
						(x._2.id, x._2.shortname, x._2.description, x._2.hasDocumentation), // Test suite
						(x._1._2.id, x._1._2.shortname, x._1._2.description, x._1._2.hasDocumentation, x._1._2.isOptional, x._1._2.isDisabled, x._1._2.tags, x._1._2.testSuiteOrder), // Test case
						(x._1._1.result, x._1._1.outputMessage, x._1._1.testsession, x._1._1.updateTime) // Result
					))
			}
			val specificationIdQuery = if (snapshotId.isDefined) {
				PersistenceSchema.conformanceSnapshotResults.filter(_.actorId === actorId).filter(_.systemId === sutId).filter(_.snapshotId === snapshotId.get).map(_.specificationId).result.head
			} else {
				PersistenceSchema.specificationHasActors.filter(_.actorId === actorId).map(_.specId).result.head
			}
			val statusItems = exec(
				for {
					results <- query.sortBy(x => (x._1._2, x._2._8)).result
						.map(_.map(r => {
							ConformanceStatusItem(
								testSuiteId = r._1._1, testSuiteName = r._1._2, testSuiteDescription = r._1._3, testSuiteHasDocumentation = r._1._4,
								testCaseId = r._2._1, testCaseName = r._2._2, testCaseDescription = r._2._3, testCaseHasDocumentation = r._2._4,
								result = r._3._1, outputMessage = r._3._2, sessionId = r._3._3, sessionTime = r._3._4,
								testCaseOptional = r._2._5, testCaseDisabled = r._2._6, testCaseTags = r._2._7
							)
						}))
					specificationId <- specificationIdQuery
				} yield (results, specificationId)
			)
			// Check to see if we have badges. We use the SUCCESS badge as this will always be present if badges are defined.
			val hasBadge = repositoryUtil.getConformanceBadge(statusItems._2, Some(actorId), snapshotId, TestResultType.SUCCESS.toString, exactMatch = false).isDefined
			val status = new ConformanceStatus(0, 0, 0, 0, 0, 0, TestResultType.UNDEFINED, None, hasBadge, new ListBuffer[ConformanceTestSuite])
			val testSuiteMap = new mutable.LinkedHashMap[Long, ConformanceTestSuite]()
			statusItems._1.foreach { item =>
				val testSuite = if (testSuiteMap.contains(item.testSuiteId)) {
					testSuiteMap(item.testSuiteId)
				} else {
					// New test suite.
					val newTestSuite = new ConformanceTestSuite(item.testSuiteId, item.testSuiteName, item.testSuiteDescription, item.testSuiteHasDocumentation, TestResultType.UNDEFINED, 0, 0, 0, 0, 0, 0, new ListBuffer[ConformanceTestCase])
					testSuiteMap += (item.testSuiteId -> newTestSuite)
					newTestSuite
				}
				val testCase = new ConformanceTestCase(item.testCaseId, item.testCaseName, item.testCaseDescription, item.sessionId, item.sessionTime, item.outputMessage, item.testCaseHasDocumentation, item.testCaseOptional, item.testCaseDisabled, TestResultType.fromValue(item.result), item.testCaseTags)
				testSuite.testCases.asInstanceOf[ListBuffer[ConformanceTestCase]].append(testCase)
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
							if (testSuite.failed == 0 && testSuite.undefined == 0) {
								testSuite.result = TestResultType.SUCCESS
							}
							status.completed += 1
							testSuite.completed += 1
						} else if (testCase.result == TestResultType.FAILURE) {
							testSuite.result = TestResultType.FAILURE
							status.failed += 1
							testSuite.failed += 1
						} else {
							if (testSuite.result == TestResultType.SUCCESS) testSuite.result = TestResultType.UNDEFINED
							status.undefined += 1
							testSuite.undefined += 1
						}
					}
				}
			}
			status.testSuites = testSuiteMap.values
			status.result = if (status.failed > 0) {
				TestResultType.FAILURE
			} else if (status.undefined > 0) {
				TestResultType.UNDEFINED
			} else if (status.completed > 0) {
				TestResultType.SUCCESS
			} else {
				TestResultType.UNDEFINED
			}
			Some(status)
		}
	}

	def getSpecificationIdForTestCaseFromConformanceStatements(testCaseId: Long): Option[Long] = {
		val spec = exec(PersistenceSchema.conformanceResults.filter(_.testcase === testCaseId).map(c => {c.spec}).result.headOption)
		spec
	}

	def getSpecificationIdForTestSuiteFromConformanceStatements(testSuiteId: Long): Option[Long] = {
		val spec = exec(PersistenceSchema.conformanceResults.filter(_.testsuite === testSuiteId).map( c => {c.spec}).result.headOption)
		spec
	}

	def getConformanceStatementsFull(domainIds: Option[List[Long]], specIds: Option[List[Long]], specGroupIds: Option[List[Long]], actorIds: Option[List[Long]], communityIds: Option[List[Long]], organizationIds: Option[List[Long]], systemIds: Option[List[Long]], orgParameters: Option[Map[Long, Set[String]]], sysParameters: Option[Map[Long, Set[String]]], status: Option[List[String]], updateTimeStart: Option[String], updateTimeEnd: Option[String], sortColumn: Option[String], sortOrder: Option[String], snapshotId: Option[Long]): List[ConformanceStatementFull] = {
		val query: ConformanceResultFullDbQuery = if (snapshotId.isDefined) {
			PersistenceSchema.conformanceSnapshotResults
				.join(PersistenceSchema.conformanceSnapshots).on(_.snapshotId === _.id)
				.join(PersistenceSchema.communities).on(_._2.community === _.id)
				.filter(_._1._1.snapshotId === snapshotId)
				.filterOpt(domainIds)((q, ids) => q._1._1.domainId inSet ids)
				.filterOpt(specIds)((q, ids) => q._1._1.specificationId inSet ids)
				.filterOpt(specGroupIds)((q, ids) => q._1._1.specificationGroupId inSet ids)
				.filterOpt(actorIds)((q, ids) => q._1._1.actorId inSet ids)
				.filterOpt(organisationIdsToUse(organizationIds, orgParameters))((q, ids) => q._1._1.organisationId inSet ids)
				.filterOpt(systemIdsToUse(systemIds, sysParameters))((q, ids) => q._1._1.systemId inSet ids)
				.map(x => (
					(x._2.id, x._2.shortname), // 1.1: Community ID, 1.2: Community shortname
					(x._1._1.organisationId, x._1._1.organisation), // 2.1: Organisation ID, 2.2: Organisation shortname
					(x._1._1.systemId, x._1._1.system, x._1._1.systemBadgeKey), // 3.1: System ID, 3.2: System shortname, 3.3: System badge key
					(x._1._1.domainId, x._1._1.domain, x._1._1.domain), // 4.1: Domain ID, 4.2: Domain shortname, 4.3: Domain fullname
					(x._1._1.actorId, x._1._1.actor, x._1._1.actor, x._1._1.actorApiKey), // 5.1: Actor ID, 5.2: Actor identifier, 5.3: Actor name, 5.4: Actor API key
					(x._1._1.specificationId, x._1._1.specification, x._1._1.specification), // 6.1: Specification ID, 6.2: Specification shortname, 6.3: Specification fullname
					(x._1._1.result, x._1._1.testSessionId, x._1._1.updateTime, x._1._1.outputMessage), // 7.1: Result, 7.2: Session ID, 7.3: Update time, 7.4: Output message
					(x._1._1.specificationGroup, x._1._1.specificationGroup), // 8.1: Specification group shortname, 8.2: Specification group fullname,
					(x._1._1.testCaseId, x._1._1.testCase, x._1._1.testCaseDescription, x._1._1.testCaseIsOptional, x._1._1.testCaseIsDisabled, x._1._1.testCaseTags, x._1._1.testCaseOrder), // 9.1: Test case ID, 9.2: Test case shortname, 9.3: Test case description, 9.4: Test case optional, 9.5: Test case disabled, 9.6: Test case tags, 9.7: Test case order
					(x._1._1.testSuiteId, x._1._1.testSuite, x._1._1.testSuiteDescription) // 10.1: Test suite ID, 10.2: Test suite shortname, 10.3: Test suite description
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
				.filterOpt(domainIds)((q, ids) => q._1._1._1._1._1._2.id inSet ids)
				.filterOpt(specIds)((q, ids) => q._1._1._1._1._1._1._1._1._1.spec inSet ids)
				.filterOpt(specGroupIds)((q, ids) => q._1._1._1._1._1._1._1._1._2.group inSet ids)
				.filterOpt(actorIds)((q, ids) => q._1._1._1._1._1._1._1._1._1.actor inSet ids)
				.filterOpt(communityIds)((q, ids) => q._1._1._2.id inSet ids)
				.filterOpt(organisationIdsToUse(organizationIds, orgParameters))((q, ids) => q._1._1._1._2.id inSet ids)
				.filterOpt(systemIdsToUse(systemIds, sysParameters))((q, ids) => q._1._1._1._1._1._1._1._1._1.sut inSet ids)
				.map(x => (
					(x._1._1._2.id, x._1._1._2.shortname), // 1.1: Community ID, 1.2: Community shortname
					(x._1._1._1._2.id, x._1._1._1._2.shortname), // 2.1: Organisation ID, 2.2: Organisation shortname
					(x._1._1._1._1._2.id, x._1._1._1._1._2.shortname, x._1._1._1._1._2.badgeKey), // 3.1: System ID, 3.2: System shortname, 3.3: System badge key
					(x._1._1._1._1._1._2.id, x._1._1._1._1._1._2.shortname, x._1._1._1._1._1._2.fullname), // 4.1: Domain ID, 4.2: Domain shortname, 4.3: Domain fullname
					(x._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._2.actorId, x._1._1._1._1._1._1._2.name, x._1._1._1._1._1._1._2.apiKey), // 5.1: Actor ID, 5.2: Actor identifier, 5.3: Actor name, 5.4: Actor API key
					(x._1._1._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._1._1._2.fullname), // 6.1: Specification ID, 6.2: Specification shortname, 6.3: Specification fullname
					(x._1._1._1._1._1._1._1._1._1.result, x._1._1._1._1._1._1._1._1._1.testsession, x._1._1._1._1._1._1._1._1._1.updateTime, x._1._1._1._1._1._1._1._1._1.outputMessage), // 7.1: Result, 7.2: Session ID, 7.3: Update time, 7.4: Output message
					(x._1._1._1._1._1._1._1._2.map(_.shortname), x._1._1._1._1._1._1._1._2.map(_.fullname)), // 8.1: Specification group shortname, 8.2: Specification group fullname,
					(x._2.id, x._2.shortname, x._2.description, x._2.isOptional, x._2.isDisabled, x._2.tags, x._2.testSuiteOrder), // 9.1: Test case ID, 9.2: Test case shortname, 9.3: Test case description, 9.4: Test case optional, 9.5: Test case disabled, 9.6: Test case tags, 9.7: Test case order
					(x._1._2.id, x._1._2.shortname, x._1._2.description) // 10.1: Test suite ID, 10.2: Test suite shortname, 10.3: Test suite description
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
		// Collect results and calculate status.
		val resultBuilder = new ConformanceStatusBuilder[ConformanceStatementFull](recordDetails = true)
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
				systemId = result._3._1, systemName = result._3._2, systemBadgeKey = result._3._3,
				domainId = result._4._1, domainName = result._4._2, domainNameFull = result._4._3,
				actorId = result._5._1, actorName = result._5._2, actorFull = result._5._3, actorApiKey = result._5._4,
				specificationId = result._6._1, specificationName = specName, specificationNameFull = specNameFull,
				specificationGroupName = result._8._1, specificationGroupNameFull = result._8._1, specificationGroupOptionName = result._6._2, specificationGroupOptionNameFull = result._6._3,
				testSuiteId = Some(result._10._1), testSuiteName = Some(result._10._2), testSuiteDescription = result._10._3,
				testCaseId = Some(result._9._1), testCaseName = Some(result._9._2), testCaseDescription = result._9._3,
				testCaseOptional = Some(result._9._4), testCaseDisabled = Some(result._9._5), testCaseTags = result._9._6, testCaseOrder = Some(result._9._7),
				result = result._7._1, outputMessage = result._7._4, sessionId = result._7._2, updateTime = result._7._3,
				completedTests = 0L, failedTests = 0L, undefinedTests = 0L, completedOptionalTests = 0L, failedOptionalTests = 0L, undefinedOptionalTests = 0L)
			resultBuilder.addConformanceResult(conformanceStatement, result._9._4, result._9._5)
		}
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
				.join(PersistenceSchema.communities).on(_._2.community === _.id)
				.filter(_._1._1.snapshotId === snapshotId.get)
				.filterOpt(domainIds)((q, ids) => q._1._1.domainId inSet ids)
				.filterOpt(specIds)((q, ids) => q._1._1.specificationId inSet ids)
				.filterOpt(specGroupIds)((q, ids) => q._1._1.specificationGroupId inSet ids)
				.filterOpt(actorIds)((q, ids) => q._1._1.actorId inSet ids)
				.filterOpt(organisationIdsToUse(organizationIds, orgParameters))((q, ids) => q._1._1.organisationId inSet ids)
				.filterOpt(systemIdsToUse(systemIds, sysParameters))((q, ids) => q._1._1.systemId inSet ids)
				.map(x => (
					(x._2.id, x._2.shortname), // 1.1: Community ID, 1.2: Community shortname
					(x._1._1.organisationId, x._1._1.organisation), // 2.1: Organisation ID, 2.2: Organisation shortname
					(x._1._1.systemId, x._1._1.system), // 3.1: System ID, 3.2: System shortname
					(x._1._1.domainId, x._1._1.domain, x._1._1.domain), // 4.1: Domain ID, 4.2: Domain shortname, 4.3: Domain fullname
					(x._1._1.actorId, x._1._1.actor, x._1._1.actor), // 5.1: Actor ID, 5.2: Actor identifier, 5.3: Actor name
					(x._1._1.specificationId, x._1._1.specification, x._1._1.specification), // 6.1: Specification ID, 6.2: Specification shortname, 6.3: Specification fullname
					(x._1._1.result, x._1._1.testSessionId, x._1._1.updateTime), // 7.1: Result, 7.2: Session ID, 7.3: Update time
					(x._1._1.specificationGroup, x._1._1.specificationGroup), // 8.1: Specification group shortname, 8.2: Specification group fullname,
					(x._1._1.testCaseIsOptional, x._1._1.testCaseIsDisabled) // 9.1: Test case optional, 9.2: Test case disabled
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
				.filterOpt(organisationIdsToUse(organizationIds, orgParameters))((q, ids) => q._1._1._1._2.id inSet ids)
				.filterOpt(systemIdsToUse(systemIds, sysParameters))((q, ids) => q._1._1._1._1._1._1._1._1.sut inSet ids)
				.map(x => (
					(x._1._1._2.id, x._1._1._2.shortname), // 1.1: Community ID, 1.2: Community shortname
					(x._1._1._1._2.id, x._1._1._1._2.shortname), // 2.1: Organisation ID, 2.2: Organisation shortname
					(x._1._1._1._1._2.id, x._1._1._1._1._2.shortname), // 3.1: System ID, 3.2: System shortname
					(x._1._1._1._1._1._2.id, x._1._1._1._1._1._2.shortname, x._1._1._1._1._1._2.fullname), // 4.1: Domain ID, 4.2: Domain shortname, 4.3: Domain fullname
					(x._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._2.actorId, x._1._1._1._1._1._1._2.name), // 5.1: Actor ID, 5.2: Actor identifier, 5.3: Actor name
					(x._1._1._1._1._1._1._1._2.id, x._1._1._1._1._1._1._1._2.shortname, x._1._1._1._1._1._1._1._2.fullname), // 6.1: Specification ID, 6.2: Specification shortname, 6.3: Specification fullname
					(x._1._1._1._1._1._1._1._1.result, x._1._1._1._1._1._1._1._1.testsession, x._1._1._1._1._1._1._1._1.updateTime), // 7.1: Result, 7.2: Session ID, 7.3: Update time
					(x._2.map(_.shortname), x._2.map(_.fullname)), // 8.1: Specification group shortname, 8.2: Specification group fullname,
					(x._1._2.isOptional, x._1._2.isDisabled) // 9.1: Test case optional, 9.2: Test case disabled
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
				systemId = result._3._1, systemName = result._3._2, systemBadgeKey = "",
				domainId = result._4._1, domainName = result._4._2, domainNameFull = result._4._3,
				actorId = result._5._1, actorName = result._5._2, actorFull = result._5._3, actorApiKey = "",
				specificationId = result._6._1, specificationName = specName, specificationNameFull = specNameFull,
				specificationGroupName = result._8._1, specificationGroupNameFull = result._8._1, specificationGroupOptionName = result._6._2, specificationGroupOptionNameFull = result._6._3,
				testSuiteId = None, testSuiteName = None, testSuiteDescription = None,
				testCaseId = None, testCaseName = None, testCaseDescription = None,
				testCaseOptional = Some(result._9._1), testCaseDisabled = Some(result._9._2), testCaseTags = None, testCaseOrder = None,
				result = result._7._1, outputMessage = None, sessionId = result._7._2, updateTime = result._7._3,
				completedTests = 0L, failedTests = 0L, undefinedTests = 0L, completedOptionalTests = 0L, failedOptionalTests = 0L, undefinedOptionalTests = 0L)
			resultBuilder.addConformanceResult(conformanceStatement, result._9._1, result._9._2)
		}
		resultBuilder.getOverview(Some(new ConformanceStatusBuilder.FilterCriteria(
			dateFromFilterString(updateTimeStart),
			dateFromFilterString(updateTimeEnd),
			status))
		)
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

	def createConformanceSnapshot(communityId: Long, label: String): ConformanceSnapshot = {
		val snapshotTime = TimeUtil.getCurrentTimestamp()
		val communityResults = getConformanceStatementsFull(None, None, None, None, Some(List(communityId)), None, None, None, None, None, None, None, None, None, None)
		val apiKey = CryptoUtil.generateApiKey()
		val onSuccessCalls = mutable.ListBuffer[() => _]()
		val dbAction = for {
			// Create snapshot
			snapshotId <- PersistenceSchema.insertConformanceSnapshot += ConformanceSnapshot(0L, label, snapshotTime, apiKey, communityId)
			// Populate snapshot
			_ <- {
				val dbActions = new ListBuffer[DBIO[_]]
				communityResults.foreach { result =>
					dbActions += (PersistenceSchema.insertConformanceSnapshotResult += ConformanceSnapshotResult(
						id = 0L, snapshotId = snapshotId, organisationId = result.organizationId, organisation = result.organizationName,
						systemId = result.systemId, system = result.systemName, systemBadgeKey = result.systemBadgeKey,
						domainId = result.domainId, domain = result.domainName,
						specGroupId = result.specificationGroupId, specGroup = result.specificationGroupName, specGroupDisplayOrder = result.specificationGroupDisplayOrder,
						specId = result.specificationId, spec = result.specificationName, specDisplayOrder = result.specificationDisplayOrder,
						actorId = result.actorId, actor = result.actorName, actorApiKey = result.actorApiKey,
						testSuiteId = result.testSuiteId.get, testSuite = result.testSuiteName.get, testSuiteDescription = result.testSuiteDescription,
						testCaseId = result.testCaseId.get, testCase = result.testCaseName.get, testCaseDescription = result.testCaseDescription, testCaseOrder = result.testCaseOrder.get,
						testCaseOptional = result.testCaseOptional.get, testCaseDisabled = result.testCaseDisabled.get, testCaseTags = result.testCaseTags,
						testSession = result.sessionId, result = result.result, outputMessage = result.outputMessage, updateTime = result.updateTime)
					)
				}
				toDBIO(dbActions)
			}
			// Copy badges
			_ <- {
				onSuccessCalls += (() => {
					communityResults.map(_.specificationId).toSet.foreach { specificationId =>
						repositoryUtil.addBadgesToConformanceSnapshot(specificationId, snapshotId)
					}
				})
				DBIO.successful(())
			}
		} yield snapshotId
		val snapshotId = exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
		ConformanceSnapshot(snapshotId, label, snapshotTime, apiKey, communityId)
	}

	def getConformanceSnapshots(community: Long): List[ConformanceSnapshot] = {
		exec(PersistenceSchema.conformanceSnapshots
			.filter(_.community === community)
			.sortBy(_.snapshotTime.desc)
			.result).toList
	}

	def getConformanceSnapshot(snapshot: Long): ConformanceSnapshot = {
		exec(PersistenceSchema.conformanceSnapshots
			.filter(_.id === snapshot)
			.result
			.head)
	}

	def editConformanceSnapshot(snapshot: Long, label: String): Unit = {
		exec(PersistenceSchema.conformanceSnapshots
			.filter(_.id === snapshot)
			.map(_.label)
			.update(label)
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
			// Delete snapshot.
			_ <- PersistenceSchema.conformanceSnapshots.filter(_.id === snapshot).delete
			// Delete badges.
			_ <- {
				onSuccessCalls += (() => repositoryUtil.deleteSnapshotBadges(snapshot))
				DBIO.successful(())
			}
		} yield ()
	}

	def getConformanceBadge(systemKey: String, actorKey: String, snapshotKey: Option[String]): Option[File] = {
		val query = if (snapshotKey.isDefined) {
			// Query conformance snapshot.
			for {
				statementIds <- PersistenceSchema.conformanceSnapshotResults
					.filter(_.systemBadgeKey === systemKey)
					.filter(_.actorApiKey === actorKey)
					.map(x => (x.systemId, x.specificationId, x.actorId))
					.result
					.headOption
				snapshotId <- PersistenceSchema.conformanceSnapshots.filter(_.apiKey === snapshotKey.get).map(_.id).result.headOption
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
				repositoryUtil.getConformanceBadge(specificationId.get, actorId, snapshotId, status = status.get.result.value(), exactMatch = false)
			} else {
				None
			}
		} else {
			None
		}
	}

	def getConformanceBadgeByIds(systemId: Long, actorId: Long, snapshotId: Option[Long]): Option[File] = {
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
			repositoryUtil.getConformanceBadge(specificationId, Some(actorId), snapshotId, status = status.get.result.value(), exactMatch = false)
		} else {
			None
		}
	}

	def getConformanceBadgeUrl(systemId: Long, actorId: Long, snapshotId: Option[Long]): Option[String] = {
		val query = if (snapshotId.isDefined) {
			// Snapshot.
			PersistenceSchema.conformanceSnapshotResults
				.join(PersistenceSchema.conformanceSnapshots).on(_.snapshotId === _.id)
				.filter(_._1.systemId === systemId)
				.filter(_._1.actorId === actorId)
				.filter(_._1.snapshotId === snapshotId.get)
				.map(x => (x._1.systemBadgeKey, x._1.actorApiKey, x._2.apiKey))
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

}
