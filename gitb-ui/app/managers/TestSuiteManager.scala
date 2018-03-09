package managers

import java.io.File
import java.util

import models._
import org.slf4j.{Logger, LoggerFactory}
import persistence.db.PersistenceSchema
import play.api.libs.concurrent.Execution.Implicits._
import utils.RepositoryUtils

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._


/**
 * Created by serbay on 10/17/14.
 */
object TestSuiteManager extends BaseManager {

	val TEST_SUITES_PATH = "test-suites"

	private final val logger: Logger = LoggerFactory.getLogger("TestSuiteManager")

	def getTestSuitesWithSpecificationId(specification: Long): Future[List[TestSuites]] = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.testSuites
					.filter(_.specification === specification)
					.list
			}
		}
	}

	def getTestSuites(ids: Option[List[Long]]): Future[List[TestSuites]] = {
		Future {
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
				q.list
			}
		}
	}

	def getTestSuiteOfTestCase(testCaseId: Long): TestSuites = {
		DB.withSession { implicit session =>
      var query = for {
        testSuite <- PersistenceSchema.testSuites
        testSuiteHasTestCase <- PersistenceSchema.testSuiteHasTestCases if testSuite.id === testSuiteHasTestCase.testsuite
      } yield (testSuite, testSuiteHasTestCase)
      query.filter(_._2.testcase === testCaseId).firstOption.get._1
		}
	}

	def getTestSuitesWithTestCases(): Future[List[TestSuite]] = {
		Future {
			DB.withSession { implicit session =>
				val testSuites = PersistenceSchema.testSuites.list

				testSuites map {
					ts:TestSuites =>
						val testCaseIds = PersistenceSchema.testSuiteHasTestCases.filter(_.testsuite === ts.id).map(_.testcase).list
						val testCases = PersistenceSchema.testCases.filter(_.id inSet testCaseIds).list

						new TestSuite(ts, testCases)
				}
			}
		}
	}

	def undeployTestSuite(testSuiteId: Long): Future[Unit] = {
		Future {
			DB.withTransaction { implicit session =>
				val testCases =
					PersistenceSchema.testSuiteHasTestCases
						.filter(_.testsuite === testSuiteId)
						.map(_.testcase).list

				val testSuite =
					PersistenceSchema.testSuites
						.filter(_.id === testSuiteId)
						.firstOption.get

				PersistenceSchema.testSuites
					.filter(_.id === testSuiteId)
					.delete

				PersistenceSchema.testSuiteHasActors
					.filter(_.testsuite === testSuiteId)
					.delete

				PersistenceSchema.testSuiteHasTestCases
					.filter(_.testsuite === testSuiteId)
					.delete

				PersistenceSchema.testCases
					.filter(_.id inSet testCases)
					.delete

				PersistenceSchema.testCaseCoversOptions
					.filter(_.testcase inSet testCases)
					.delete

				PersistenceSchema.testCaseHasActors
					.filter(_.testcase inSet testCases)
					.delete

				RepositoryUtils.undeployTestSuite(testSuite.specification, testSuite.shortname)
			}
		}
	}

	def deployTestSuiteFromZipFile(specification: Long, file: File): Future[Unit] = {
		Future {
			try {
				val testSuite = RepositoryUtils.getTestSuiteFromZip(specification, file)

				if (testSuite.isDefined && testSuite.get.testCases.isDefined) {
					logger.debug("Extracted test suite [" + testSuite + "] with the test cases [" + testSuite.get.testCases.get.map(_.shortname) + "]")
					val resourcePaths = RepositoryUtils.extractTestSuiteFilesFromZipToFolder(specification, testSuite.get.shortname, file)

					logger.debug("Extracted test suite files [" + resourcePaths + "]")

					saveTestSuite(testSuite.get.toCaseObject, testSuite.get.actors, testSuite.get.testCases, resourcePaths)
				}
			}
			catch {
				case e:Exception => {
					logger.error("An error occurred", e)
				}
			}
		}
	}

	private def saveTestSuite(suite: TestSuites, testSuiteActors: Option[List[Actor]], testCases: Option[List[TestCases]], resourcePaths: Map[String, String]):Boolean = {

		DB.withTransaction { implicit session =>

			try {
				logger.debug("Creating test suite ["+suite.shortname+"]")
				val count = PersistenceSchema.testSuites
					.filter(_.shortname === suite.shortname)
					.filter(_.specification === suite.specification)
					.size.run
				if (count > 0) {
					// Test suite already exists in specification.
					// TODO handle replacement
					return false
				}
				val spec = PersistenceSchema.specifications
					.filter(_.id === suite.specification)
					.firstOption.get

				val id = PersistenceSchema.testSuites.returning(PersistenceSchema.testSuites.map(_.id)).insert(suite)
				val savedTestSuite = suite.copy(id)

				// Get the (new or existing) actor IDs resulting from the import.
				val savedActorIds: util.Map[String, Long] = new util.HashMap[String, Long]
				for (testSuiteActor <- testSuiteActors.get) {
					val testSuiteActorCase = testSuiteActor.toCaseObject
					var query = for {
						actor <- PersistenceSchema.actors
						specificationHasActors <- PersistenceSchema.specificationHasActors if specificationHasActors.actorId === actor.id
					} yield (actor, specificationHasActors)
					val savedActor = query
						.filter(_._1.actorId === testSuiteActor.actorId)
						.filter(_._2.specId === suite.specification)
						.list
					if (savedActor.nonEmpty) {
						// The actor already exists in the specification.
						// TODO check to update the actor's information and endpoints.
						logger.debug("Actor [" + savedActor.head._1.id + "] and specification [" + suite.specification + "] already related.")
						savedActorIds.put(savedActor.head._1.actorId, savedActor.head._1.id)
					} else {
						// New actor.
						logger.debug("Creating actor with name [" + testSuiteActor.name + "] and id  [" + testSuiteActor.actorId + "]")
						val actorToSave = testSuiteActorCase.withDomainId(spec.domain)
						val savedActorId = PersistenceSchema.actors
							.returning(PersistenceSchema.actors.map(_.id))
							.insert(actorToSave)
						savedActorIds.put(actorToSave.actorId, savedActorId)
						if(testSuiteActor.endpoints.isDefined) {
							val endpoints = testSuiteActor.endpoints.get
							endpoints.foreach { endpoint =>
								val endpointId = PersistenceSchema.endpoints
									.returning(PersistenceSchema.endpoints.map(_.id))
									.insert(endpoint.toCaseObject.copy(actor = savedActorId))
								logger.debug("Creating endpoint ["+endpoint.name+"] for actor ["+testSuiteActor.actorId+"]")
								if(endpoint.parameters.isDefined) {
									val parameters = endpoint.parameters.get
									parameters.foreach { parameter =>
										logger.debug("Creating parameter ["+parameter.name+"] for endpoint ["+endpoint.name+"]")
										PersistenceSchema.parameters.insert(parameter.copy(endpoint = endpointId))
									}
								}
							}
						}
						logger.debug("Relating actor [" + id + "] with specification [" + suite.specification + "]")
						PersistenceSchema.specificationHasActors.insert((suite.specification, savedActorId))
					}
				}
				logger.debug("Adding test cases and relating them with actors")
				val savedTestCaseIds: util.List[Long] = new util.ArrayList[Long]
				for (testCase <- testCases.get) {
					val testCaseToStore = testCase.withPath(resourcePaths(testCase.shortname))
					val testCaseId = PersistenceSchema.testCases
						.returning(PersistenceSchema.testCases.map(_.id))
						.insert(testCaseToStore)
					savedTestCaseIds.add(testCaseId)
					testCase.targetActors.getOrElse("").split(",").foreach {
						actorId =>
							val actorInternalId = savedActorIds.get(actorId)
							logger.debug("Establishing relation between testcase ["+(testCaseId, testCase.shortname)+"] and actor ["+(actorInternalId, actorId)+"]")
							PersistenceSchema.testCaseHasActors.insert((testCaseId, suite.specification, actorInternalId))
					}
				}
				logger.debug("Relating test suite with actors")
				for (actorEntry <- savedActorIds.entrySet()) {
					logger.debug("Establishing relation between test suite ["+(savedTestSuite.id, savedTestSuite.shortname)+"] and actor ["+(actorEntry.getKey, actorEntry.getValue)+"]")
					PersistenceSchema.testSuiteHasActors.insert((savedTestSuite.id, actorEntry.getValue))
				}
				logger.debug("Relating test suite with test cases")
				for (testCaseId <- savedTestCaseIds) {
					logger.debug("Establishing relation between test suite ["+(savedTestSuite.id, savedTestSuite.shortname)+"] and test case ["+testCaseId+"]")
					PersistenceSchema.testSuiteHasTestCases.insert((savedTestSuite.id, testCaseId))
				}
				true
			}
			catch {
				case e: Exception => {
					logger.error("An error occurred, rolling back the session", e)
					session.rollback()
					return false
				}
			}
		}
	}

	def getTestSuitesBySpecificationAndActorAndTestCaseType(specificationId: Long, actorId: Long, testCaseType: Short): Future[List[TestSuite]] = {
		Future {
			DB.withSession { implicit session =>
				val testSuiteIds = PersistenceSchema.testSuiteHasActors
					.filter(_.actor === actorId)
					.map(_.testsuite)
					.list

				val testSuites = PersistenceSchema.testSuites
					.filter(_.id inSet testSuiteIds)
					.filter(_.specification === specificationId)
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

}