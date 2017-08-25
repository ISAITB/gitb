package managers

import java.io.File

import models._
import org.slf4j.{LoggerFactory, Logger}
import persistence.db.PersistenceSchema
import play.api.libs.concurrent.Execution.Implicits._
import utils.RepositoryUtils

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

	def undeployTestSuite(testSuiteId: Long): Future[Unit] = {
		Future {
			DB.withSession { implicit session =>
				try {
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

					RepositoryUtils.undeployTestSuite(testSuite.shortname)
				}
				catch {
					case e: Exception => {
						logger.error("An error occurred", e)

						session.rollback();
					}
				}
			}
		}
	}

	def deployTestSuiteFromZipFile(specification: Long, file: File): Future[Unit] = {
		Future {
			try {
				val testSuite = RepositoryUtils.getTestSuiteFromZip(specification, file)

				if (testSuite.isDefined && testSuite.get.testCases.isDefined) {
					logger.debug("Extracted test suite [" + testSuite + "] with the test cases [" + testSuite.get.testCases.get.map(_.shortname) + "]")
					val resourcePaths = RepositoryUtils.extractTestSuiteFilesFromZipToFolder(testSuite.get.shortname, file)

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

		DB.withSession { implicit session =>

			try {

				val spec = PersistenceSchema.specifications
					.filter(_.id === suite.specification)
					.firstOption.get

				logger.debug("Creating test suite ["+suite.shortname+"]")
				val count = PersistenceSchema.testSuites.filter(_.shortname === suite.shortname).size.run
				if(count > 0) {
					return false
				}
				val id = PersistenceSchema.testSuites.returning(PersistenceSchema.testSuites.map(_.id)).insert(suite)
				val savedTestSuite = suite.copy(id)

				val savedActors = testSuiteActors match {
					case Some(actors) => {
						actors
							.map((actor) => (actor, actor.toCaseObject))
							.map((tuple) => tuple.copy(tuple._1, tuple._2.withDomainId(spec.domain)))
							.map {
								tuple =>
									val savedActor = PersistenceSchema.actors
										.filter(_.actorId === tuple._2.actorId)
										.filter(_.domain === tuple._2.domain)
										.list

									if(savedActor.size > 0) {

                                        val savedSpecificationHasActors = PersistenceSchema.specificationHasActors
                                            .filter(_.specId === suite.specification)
                                            .filter(_.actorId === savedActor.head.id)
                                            .list

                                        if(savedSpecificationHasActors.size == 0) {
                                            logger.debug("Relating actor [" + savedActor.head.id + "] with specification [" + suite.specification + "]")
                                            PersistenceSchema.specificationHasActors.insert((suite.specification, savedActor.head.id))
                                        } else {
                                            logger.debug("Actor [" + savedActor.head.id + "] and specification [" + suite.specification + "] already related.")
                                        }
                                        tuple.copy(tuple._1, savedActor.head)
									} else {
										logger.debug("Creating actor with name [" + tuple._2.name + "] and id  [" + tuple._2.actorId + "]")
										val id = PersistenceSchema.actors
															.returning(PersistenceSchema.actors.map(_.id))
															.insert(tuple._2)

										if(tuple._1.endpoints.isDefined) {
											val endpoints = tuple._1.endpoints.get
											endpoints.foreach { endpoint =>
												val endpointId = PersistenceSchema.endpoints
																					.returning(PersistenceSchema.endpoints.map(_.id))
																					.insert(endpoint.toCaseObject.copy(actor = id))

												logger.debug("Creating endpoint ["+endpoint.name+"] for actor ["+tuple._1.actorId+"]")

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
										PersistenceSchema.specificationHasActors.insert((suite.specification, id))

										tuple.copy(tuple._1, tuple._2.copy(id=id))
									}
							}
					}
					case None => List()
				}

				logger.debug("Relating test cases with actors")
				val savedTestCases = testCases match {
					case Some(testCaseList) => {
						testCaseList
							.map { testCase =>
								testCase.copy(path = resourcePaths.get(testCase.shortname).get)
							}
							.map { testCase =>
								val id = PersistenceSchema.testCases
									.returning(PersistenceSchema.testCases.map(_.id))
									.insert(testCase)

								testCase.copy(id)
							}
							.map { testCase =>
								testCase.targetActors.getOrElse("").split(",").foreach { actorId =>
									val actors = savedActors
										.filter(_._2.actorId == actorId)
										.foreach { tuple =>
											logger.debug("Establishing relation between testcase ["+(testCase.id, testCase.shortname)+"] and actor ["+(tuple._2.id, tuple._2.actorId)+"]")
											PersistenceSchema.testCaseHasActors.insert((testCase.id, suite.specification, tuple._2.id))
										}
								}
								testCase
							}
					}
					case _ => List()
				}

				logger.debug("Relating test suite with actors")
				savedActors.foreach { tuple =>
					logger.debug("Establishing relation between test suite ["+(savedTestSuite.id, savedTestSuite.shortname)+"] and actor ["+(tuple._2.id, tuple._2.actorId)+"]")

					PersistenceSchema.testSuiteHasActors.insert((savedTestSuite.id, tuple._2.id))
				}

				logger.debug("Relating test suite with test cases")
				savedTestCases.foreach { testCase =>
					logger.debug("Establishing relation between test suite ["+(savedTestSuite.id, savedTestSuite.shortname)+"] and test case ["+(testCase.id, testCase.shortname)+"]")

					PersistenceSchema.testSuiteHasTestCases.insert((savedTestSuite.id, testCase.id))
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
					val testCaseId = PersistenceSchema.testSuiteHasTestCases
						.filter(_.testsuite === testSuite.id)
						.map(_.testcase)
						.firstOption
						.get

					val testCases = PersistenceSchema.testCases
						.filter(_.id === testCaseId)
						.filter(_.testCaseType === testCaseType)
						.list

					new TestSuite(testSuite, testCases)
				}
			}
		}
	}

}