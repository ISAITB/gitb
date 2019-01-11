package managers

import java.util

import models.Enums.TestResultStatus
import models.{TestCase, TestCases}
import persistence.db.PersistenceSchema

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by serbay on 10/16/14.
 */
object TestCaseManager extends BaseManager {

	import dbConfig.driver.api._

	val TEST_CASES_PATH = "test-cases"

	def createTestCase(testCase: TestCases) = {
		exec((PersistenceSchema.testCases += testCase).transactionally)
	}

	def getTestCaseForIdWrapper(testCaseId:String) = {
		getTestCaseForId(testCaseId)
	}

	def getTestCaseForId(testCaseId:String) = {
		try {
			val tc = exec(PersistenceSchema.testCases.filter(_.id === testCaseId.toLong).result.head)
			Some(new TestCase(tc))
		}
		catch {
			case e: Exception => None
		}
	}

	def getTestCasesOfTestSuiteWrapper(testSuiteId: Long, testCaseType: Option[Short]): util.List[TestCases] = {
		getTestCasesOfTestSuite(testSuiteId, testCaseType)
	}

	def getTestCasesOfTestSuite(testSuiteId: Long, testCaseType: Option[Short]): util.List[TestCases] = {
		var query = PersistenceSchema.testCases
			.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
		query = query
			.filter(_._2.testsuite === testSuiteId)
		if (testCaseType.isDefined)
			query = query
				.filter(_._1.testCaseType === testCaseType.get)
		val results = exec(query.result.map(_.toList))

		val testCases = new util.ArrayList[TestCases]()
		for (result <- results) {
			testCases.add(result._1)
		}
		testCases
	}

	def getTestCase(testCaseId:String) = {
		try {
			val tc = exec(PersistenceSchema.testCases.filter(_.id === testCaseId.toLong).result.head)
			Some(new TestCase(tc))
		}
		catch {
			case e: Exception => None
		}
	}

	def getTestCases(actor:Long, spec:Long, optionIds: Option[List[Long]], testCaseType:Short) : List[TestCase] = {
		val actorTestCaseTuples = exec(PersistenceSchema.testCaseHasActors
													.filter(_.specification === spec)
													.filter(_.actor === actor)
													.map(_.testcase)
													.result
  												.map(_.toList))

		val optionTestCaseTuples = optionIds match {
			case Some(ids) => exec(PersistenceSchema.testCaseCoversOptions
													.filter(_.option inSet ids)
													.map(_.testcase)
													.result
													.map(_.toList))
			case None => List()
		}

		val ids = actorTestCaseTuples union optionTestCaseTuples

		val testCases = exec(PersistenceSchema.testCases.filter(_.id inSet ids).filter(_.testCaseType === testCaseType).result.map(_.toList))

		toTestCaseList(testCases)
	}

	def getLastExecutionResultsForTestCases(sutId: Long, testCaseIds: List[Long]) = {
		testCaseIds map { testCaseId =>
			val testCaseResult = {
				val testCaseResultStr = exec(PersistenceSchema.testResults
					.filter(_.sutId === sutId)
					.filter(_.testCaseId === testCaseId)
					.filter(_.endTime isDefined)
					.sortBy(_.endTime.desc)
					.map(_.result)
					.result
					.headOption)

				testCaseResultStr match {
					case Some(result) => TestResultStatus.withName(result)
					case None => TestResultStatus.UNDEFINED
				}
			}

			testCaseResult
			}
	}

	def getTestCasesHavingActors(actorIds: List[Long]): util.HashMap[Long, ListBuffer[TestCases]] = {
		var query = PersistenceSchema.testCaseHasActors
  			.join(PersistenceSchema.testCases).on(_.testcase === _.id)
		query = query.filter(_._1.actor inSet actorIds)

		val results = exec(query.result.map(_.toList))
		val testCaseSet = new util.HashMap[Long, ListBuffer[TestCases]]()
		results.foreach { result =>
			if (!testCaseSet.containsKey(result._1._3)) {
				testCaseSet.put(result._1._3, ListBuffer[TestCases]())
			}
			testCaseSet.get(result._1._3) += result._2
		}
		testCaseSet
	}

	def getTestCases(ids: Option[List[Long]]): List[TestCases] = {
		val q = ids match {
			case Some(idList) => {
				PersistenceSchema.testCases
					.filter(_.id inSet idList)
			}
			case None => {
				PersistenceSchema.testCases
			}
		}
		exec(q.sortBy(_.shortname.asc).result.map(_.toList))
	}

	private def toTestCaseList(testCases:List[TestCases]) = {
		testCases map {
			tc:TestCases =>
				val actorIds = tc.targetActors.getOrElse("").split(",")
				val optionIds = tc.targetOptions.getOrElse("").split(",")

				val actors = {
					if(actorIds.nonEmpty) {
						Some(exec(PersistenceSchema.actors.filter(_.actorId inSet actorIds).result.map(_.toList)))
					} else {
						None
					}
				}

				val options = {
					if(optionIds.nonEmpty) {
						Some(exec(PersistenceSchema.options.filter(_.shortname inSet optionIds).result.map(_.toList)))
					} else {
						None
					}
				}

				new TestCase(tc, actors, options)
		}
	}

	def updateTestCase(testCaseId: Long, shortName: String, fullName: String, version: String, authors: Option[String], description: Option[String], keywords: Option[String], testCaseType: Short, path: String) = {
		val q1 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.shortname, t.fullname, t.version, t.authors, t.description, t.keywords, t.testCaseType, t.path)
		q1.update(shortName, fullName, version, authors, description, keywords, testCaseType, path) andThen
		TestResultManager.updateForUpdatedTestCase(testCaseId, shortName)
	}

	def delete(testCaseId: Long) = {
		deleteInternal(testCaseId, false)
	}

	private def deleteInternal(testCaseId: Long, skipConformanceResult: Boolean): DBIO[_] = {
		val actions = new ListBuffer[DBIO[_]]()
		actions += TestResultManager.updateForDeletedTestCase(testCaseId)
		actions += removeActorLinksForTestCase(testCaseId)
		actions += PersistenceSchema.testCaseCoversOptions.filter(_.testcase === testCaseId).delete
		actions += PersistenceSchema.testSuiteHasTestCases.filter(_.testcase === testCaseId).delete
		if (!skipConformanceResult) {
			actions += PersistenceSchema.conformanceResults.filter(_.testcase === testCaseId).delete
		}
		actions += PersistenceSchema.testCases.filter(_.id === testCaseId).delete
		DBIO.seq(actions.map(a => a): _*)
	}

	def removeActorLinksForTestCase(testCaseId: Long) = {
		PersistenceSchema.testCaseHasActors.filter(_.testcase === testCaseId).delete
	}

	def removeActorLinkForTestCase(testCaseId: Long, actorId: Long) = {
		PersistenceSchema.testCaseHasActors
			.filter(_.testcase === testCaseId)
			.filter(_.actor === actorId)
			.delete
	}

}
