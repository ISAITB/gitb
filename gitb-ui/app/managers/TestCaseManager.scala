package managers

import javax.inject.{Inject, Singleton}
import models.{TestCase, TestCases}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.Rep

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by serbay on 10/16/14.
 */
object TestCaseManager {

	type TestCaseDbTuple = (
		Rep[Long], Rep[String], Rep[String], Rep[String],
			Rep[Option[String]], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]],
			Rep[Option[String]], Rep[Short], Rep[String], Rep[Long],
			Rep[Option[String]], Rep[Option[String]], Rep[Short], Rep[Boolean]
		)

	type TestCaseValueTuple = (
		Long, String, String, String,
			Option[String], Option[String], Option[String], Option[String],
			Option[String], Short, String, Long,
			Option[String], Option[String], Short, Boolean
		)

	def withoutDocumentation(dbTestCase: PersistenceSchema.TestCasesTable): TestCaseDbTuple = {
		(dbTestCase.id, dbTestCase.shortname, dbTestCase.fullname, dbTestCase.version,
			dbTestCase.authors, dbTestCase.originalDate, dbTestCase.modificationDate, dbTestCase.description,
			dbTestCase.keywords, dbTestCase.testCaseType, dbTestCase.path, dbTestCase.targetSpec,
			dbTestCase.targetActors, dbTestCase.targetOptions, dbTestCase.testSuiteOrder, dbTestCase.hasDocumentation)
	}

	def tupleToTestCase(x: TestCaseValueTuple) = {
		TestCases(x._1, x._2, x._3, x._4, x._5, x._6, x._7, x._8, x._9, x._10, x._11, x._12, x._13, x._14, x._15, x._16, None)
	}
}

@Singleton
class TestCaseManager @Inject() (testResultManager: TestResultManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

	import dbConfig.profile.api._

	val TEST_CASES_PATH = "test-cases"

	def getTestCaseForIdWrapper(testCaseId:String) = {
		getTestCase(testCaseId)
	}

	def getTestCasesForIds(testCaseIds: List[Long]): List[TestCases] = {
		exec(
			PersistenceSchema.testCases.filter(_.id inSet testCaseIds)
  			.map(x => TestCaseManager.withoutDocumentation(x))
				.result
				.map(_.toList)
		)
		.map(TestCaseManager.tupleToTestCase)
	}

	def getTestCase(testCaseId:String) = {
		try {
			val tc = exec(PersistenceSchema.testCases.filter(_.id === testCaseId.toLong).map(x => TestCaseManager.withoutDocumentation(x)).result.head)
			Some(new TestCase(TestCaseManager.tupleToTestCase(tc)))
		}
		catch {
			case e: Exception => None
		}
	}

	def getAllTestCases(): List[TestCases] = {
		exec(PersistenceSchema.testCases.sortBy(_.shortname.asc).map(x => TestCaseManager.withoutDocumentation(x))
			.result
			.map(_.toList))
			.map(TestCaseManager.tupleToTestCase)
	}

	def getTestCasesForSystem(systemId: Long): List[TestCases] = {
		val query = PersistenceSchema.testCases
  		.join(PersistenceSchema.conformanceResults).on(_.id === _.testcase)
  		.filter(_._2.sut === systemId)
			.sortBy(_._1.shortname.asc)
  		.map(r => TestCaseManager.withoutDocumentation(r._1))
  	exec(query.result.map(_.toList)).map(TestCaseManager.tupleToTestCase)
	}

	def getTestCasesForCommunity(communityId: Long): List[TestCases] = {
		exec(PersistenceSchema.testCases
			.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
			.join(PersistenceSchema.testSuites).on(_._2.testsuite === _.id)
			.join(PersistenceSchema.specifications).on(_._2.specification === _.id)
			.join(PersistenceSchema.communities).on(_._2.domain === _.domain)
			.filter(_._2.id === communityId)
			.sortBy(_._1._1._1._1.shortname.asc)
			.map(r => TestCaseManager.withoutDocumentation(r._1._1._1._1))
			.result.map(_.toList)
		).map(TestCaseManager.tupleToTestCase)
	}

	def updateTestCase(testCaseId: Long, shortName: String, fullName: String, version: String, authors: Option[String], description: Option[String], keywords: Option[String], testCaseType: Short, path: String, testSuiteOrder: Short, targetActors: String, hasDocumentation: Boolean, documentation: Option[String]) = {
		val q1 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.shortname, t.fullname, t.version, t.authors, t.description, t.keywords, t.testCaseType, t.path, t.testSuiteOrder, t.targetActors, t.hasDocumentation, t.documentation)
		q1.update(shortName, fullName, version, authors, description, keywords, testCaseType, path, testSuiteOrder, Some(targetActors), hasDocumentation, documentation) andThen
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

	def getTestCaseDocumentation(testCaseId: Long): Option[String] = {
		val result = exec(PersistenceSchema.testCases.filter(_.id === testCaseId).map(x => x.documentation).result.headOption)
		if (result.isDefined) {
			result.get
		} else {
			None
		}
	}

}
