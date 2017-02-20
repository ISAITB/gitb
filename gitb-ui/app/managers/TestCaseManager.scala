package managers

import models.Enums.TestResultStatus
import models.{TestCase, TestCases}
import persistence.db.PersistenceSchema
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._

/**
 * Created by serbay on 10/16/14.
 */
object TestCaseManager extends BaseManager {

	val TEST_CASES_PATH = "test-cases"

	def createTestCase(testCase: TestCases) = {
		Future {
			DB.withSession { implicit session =>
				PersistenceSchema.testCases.insert(testCase)
			}
		}
	}


	def getTestCase(testCaseId:String) = {
		Future {
			DB.withSession { implicit session =>
				try {
					val tc = PersistenceSchema.testCases.filter(_.shortname === testCaseId).first

					Some(new TestCase(tc))
				}
				catch {
					case e: Exception => None
				}
			}
		}
	}

	def getTestCases(actor:Long, spec:Long, optionIds: Option[List[Long]], testCaseType:Short) : Future[List[TestCase]] = {
		Future{
			DB.withSession { implicit session =>
				val actorTestCaseTuples = PersistenceSchema.testCaseHasActors
                              .filter(_.specification === spec)
															.filter(_.actor === actor)
                              .map(_.testcase)
															.list

				val optionTestCaseTuples = optionIds match {
					case Some(ids) => PersistenceSchema.testCaseCoversOptions
															.filter(_.option inSet ids)
                              .map(_.testcase)
															.list
					case None => List()
				}

				val ids = actorTestCaseTuples union optionTestCaseTuples

				val testCases = PersistenceSchema.testCases.filter(_.id inSet ids).filter(_.testCaseType === testCaseType).list

				toTestCaseList(testCases)
			}
		}
	}

	def getLastExecutionResultsForTestCases(sutId: Long, testCaseIds: List[Long]) = {
		Future {
			DB.withSession { implicit session =>
				testCaseIds map { testCaseId =>
					val testCaseResult = {
						val testCaseResultStr = PersistenceSchema.testResults
							.filter(_.sutId === sutId)
							.filter(_.testcaseId === testCaseId)
							.filter(_.endTime isDefined)
							.sortBy(_.endTime.desc)
							.map(_.result)
							.firstOption

						testCaseResultStr match {
							case Some(result) => TestResultStatus.withName(result)
							case None => TestResultStatus.UNDEFINED
						}
					}

					testCaseResult
				}
			}
		}
	}

	private def toTestCaseList(testCases:List[TestCases])(implicit session:Session) = {
		testCases map {
			tc:TestCases =>
				val actorIds = tc.targetActors.getOrElse("").split(",")
				val optionIds = tc.targetOptions.getOrElse("").split(",")

				val actors = {
					if(actorIds.nonEmpty) {
						Some(PersistenceSchema.actors.filter(_.actorId inSet actorIds).list)
					} else {
						None
					}
				}

				val options = {
					if(optionIds.nonEmpty) {
						Some(PersistenceSchema.options.filter(_.shortname inSet optionIds).list)
					} else {
						None
					}
				}

				new TestCase(tc, actors, options)
		}
	}
}
