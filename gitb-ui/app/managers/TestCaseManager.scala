package managers

import java.util

import javax.inject.{Inject, Singleton}
import models.Enums.TestResultStatus
import models.{TestCase, TestCases}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by serbay on 10/16/14.
 */
@Singleton
class TestCaseManager @Inject() (testResultManager: TestResultManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

	import dbConfig.profile.api._

	val TEST_CASES_PATH = "test-cases"

	def createTestCase(testCase: TestCases) = {
		exec((PersistenceSchema.testCases += testCase).transactionally)
	}

	def getTestCaseForIdWrapper(testCaseId:String) = {
		getTestCase(testCaseId)
	}

	def getTestCasesForIds(testCaseIds: List[Long]): List[TestCases] = {
		exec(PersistenceSchema.testCases.filter(_.id inSet testCaseIds).result.map(_.toList))
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

	def getAllTestCases(): List[TestCases] = {
		exec(PersistenceSchema.testCases.sortBy(_.shortname.asc).result.map(_.toList))
	}

	def getTestCasesForSystem(systemId: Long): List[TestCases] = {
		val query = PersistenceSchema.testCases
  		.join(PersistenceSchema.conformanceResults).on(_.id === _.testcase)
  		.filter(_._2.sut === systemId)
			.sortBy(_._1.shortname.asc)
  		.map(r => r._1)
  	exec(query.result.map(_.toList))
	}

	def getTestCasesForCommunity(communityId: Long): List[TestCases] = {
		exec(PersistenceSchema.testCases
			.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
			.join(PersistenceSchema.testSuites).on(_._2.testsuite === _.id)
			.join(PersistenceSchema.specifications).on(_._2.specification === _.id)
			.join(PersistenceSchema.communities).on(_._2.domain === _.domain)
			.filter(_._2.id === communityId)
			.map(r => r._1._1._1._1)
			.sortBy(_.shortname.asc)
			.result.map(_.toList)
		)
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

	def updateTestCase(testCaseId: Long, shortName: String, fullName: String, version: String, authors: Option[String], description: Option[String], keywords: Option[String], testCaseType: Short, path: String, testSuiteOrder: Short, targetActors: String) = {
		val q1 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.shortname, t.fullname, t.version, t.authors, t.description, t.keywords, t.testCaseType, t.path, t.testSuiteOrder, t.targetActors)
		q1.update(shortName, fullName, version, authors, description, keywords, testCaseType, path, testSuiteOrder, Some(targetActors)) andThen
		testResultManager.updateForUpdatedTestCase(testCaseId, shortName)
	}

	def delete(testCaseId: Long) = {
		deleteInternal(testCaseId, false)
	}

	private def deleteInternal(testCaseId: Long, skipConformanceResult: Boolean): DBIO[_] = {
		val actions = new ListBuffer[DBIO[_]]()
		actions += testResultManager.updateForDeletedTestCase(testCaseId)
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
