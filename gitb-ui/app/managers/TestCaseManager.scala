package managers

import java.util

import models.Enums.TestResultStatus
import models.{TestCase, TestCases}
import persistence.db.PersistenceSchema

import scala.collection.mutable.ListBuffer
import scala.slick.driver.MySQLDriver.simple._

/**
 * Created by serbay on 10/16/14.
 */
object TestCaseManager extends BaseManager {

	val TEST_CASES_PATH = "test-cases"

	def createTestCase(testCase: TestCases) = {
		DB.withTransaction { implicit session =>
			PersistenceSchema.testCases.insert(testCase)
		}
	}

	def getTestCaseForIdWrapper(testCaseId:String) = {
		DB.withTransaction { implicit session =>
			getTestCaseForId(testCaseId)
		}
	}

	def getTestCaseForId(testCaseId:String)(implicit session: Session) = {
		try {
			val tc = PersistenceSchema.testCases.filter(_.id === testCaseId.toLong).first
			Some(new TestCase(tc))
		}
		catch {
			case e: Exception => None
		}
	}

	def getTestCasesOfTestSuiteWrapper(testSuiteId: Long, testCaseType: Option[Short]): util.List[TestCases] = {
		DB.withSession { implicit session =>
			getTestCasesOfTestSuite(testSuiteId, testCaseType)
		}
	}

	def getTestCasesOfTestSuite(testSuiteId: Long, testCaseType: Option[Short])(implicit session: Session): util.List[TestCases] = {
		var query = for {
			testCase <- PersistenceSchema.testCases
			testSuiteHasTestCases <- PersistenceSchema.testSuiteHasTestCases if testSuiteHasTestCases.testcase === testCase.id
		} yield (testCase, testSuiteHasTestCases)
		query = query
			.filter(_._2.testsuite === testSuiteId)
		if (testCaseType.isDefined)
			query = query
				.filter(_._1.testCaseType === testCaseType.get)
		val results = query.list

		val testCases = new util.ArrayList[TestCases]()
		for (result <- results) {
			testCases.add(result._1)
		}
		testCases
	}

	def getTestCase(testCaseId:String) = {
		DB.withSession { implicit session =>
			try {
				val tc = PersistenceSchema.testCases.filter(_.id === testCaseId.toLong).first
				Some(new TestCase(tc))
			}
			catch {
				case e: Exception => None
			}
		}
	}

	def getTestCases(actor:Long, spec:Long, optionIds: Option[List[Long]], testCaseType:Short) : List[TestCase] = {
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

	def getLastExecutionResultsForTestCases(sutId: Long, testCaseIds: List[Long]) = {
		DB.withSession { implicit session =>
			testCaseIds map { testCaseId =>
				val testCaseResult = {
					val testCaseResultStr = PersistenceSchema.testResults
						.filter(_.sutId === sutId)
						.filter(_.testCaseId === testCaseId)
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

	def getTestCasesHavingActors(actorIds: List[Long]): util.HashMap[Long, ListBuffer[TestCases]] = {
		DB.withSession { implicit session =>
			var query = for {
					testCaseHasActors <- PersistenceSchema.testCaseHasActors
					testCases <- PersistenceSchema.testCases if testCases.id === testCaseHasActors.testcase
			} yield (testCaseHasActors, testCases)
			query = query.filter(_._1.actor inSet actorIds)

			val results = query.list
			val testCaseSet = new util.HashMap[Long, ListBuffer[TestCases]]()
			results.foreach { result =>
				if (!testCaseSet.containsKey(result._1._3)) {
					testCaseSet.put(result._1._3, ListBuffer[TestCases]())
				}
				testCaseSet.get(result._1._3) += result._2
			}
			testCaseSet
		}
	}

	def getTestCases(ids: Option[List[Long]]): List[TestCases] = {
		DB.withSession { implicit session =>
			val q = ids match {
				case Some(idList) => {
					PersistenceSchema.testCases
						.filter(_.id inSet idList)
				}
				case None => {
					PersistenceSchema.testCases
				}
			}
			q.list
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

	def updateTestCase(testCaseId: Long, shortName: String, fullName: String, version: String, authors: Option[String], description: Option[String], keywords: Option[String], testCaseType: Short, path: String)(implicit session: Session) = {
		val q1 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.shortname)
		q1.update(shortName)

		val q2 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.fullname)
		q2.update(fullName)

		val q3 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.version)
		q3.update(version)

		val q4 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.authors)
		q4.update(authors)

		val q5 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.description)
		q5.update(description)

		val q6 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.keywords)
		q6.update(keywords)

		val q7 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.testCaseType)
		q7.update(testCaseType)

		val q8 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.path)
		q8.update(path)

		TestResultManager.updateForUpdatedTestCase(testCaseId, shortName)
	}

	def delete(testCaseId: Long)(implicit session: Session): Unit = {
		delete(testCaseId, false)
	}

	def delete(testCaseId: Long, skipConformanceResult: Boolean)(implicit session: Session): Unit = {
		TestResultManager.updateForDeletedTestCase(testCaseId)
		removeActorLinksForTestCase(testCaseId)
		PersistenceSchema.testCaseCoversOptions.filter(_.testcase === testCaseId).delete
		PersistenceSchema.testSuiteHasTestCases.filter(_.testcase === testCaseId).delete
		if (!skipConformanceResult) {
			PersistenceSchema.conformanceResults.filter(_.testcase === testCaseId).delete
		}
		PersistenceSchema.testCases.filter(_.id === testCaseId).delete
	}

	def removeActorLinksForTestCase(testCaseId: Long)(implicit session: Session): Unit = {
		PersistenceSchema.testCaseHasActors
			.filter(_.testcase === testCaseId)
			.delete
	}

	def removeActorLinkForTestCase(testCaseId: Long, actorId: Long)(implicit session: Session): Unit = {
		PersistenceSchema.testCaseHasActors
			.filter(_.testcase === testCaseId)
			.filter(_.actor === actorId)
			.delete
	}

}
