package managers

import models.{TestCase, TestCases}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.Rep

import javax.inject.{Inject, Singleton}
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
			Rep[Option[String]], Rep[Option[String]], Rep[Short], Rep[Boolean], Rep[String]
		)

	type TestCaseValueTuple = (
		Long, String, String, String,
			Option[String], Option[String], Option[String], Option[String],
			Option[String], Short, String, Long,
			Option[String], Option[String], Short, Boolean, String
		)

	def withoutDocumentation(dbTestCase: PersistenceSchema.TestCasesTable): TestCaseDbTuple = {
		(dbTestCase.id, dbTestCase.shortname, dbTestCase.fullname, dbTestCase.version,
			dbTestCase.authors, dbTestCase.originalDate, dbTestCase.modificationDate, dbTestCase.description,
			dbTestCase.keywords, dbTestCase.testCaseType, dbTestCase.path, dbTestCase.targetSpec,
			dbTestCase.targetActors, dbTestCase.targetOptions, dbTestCase.testSuiteOrder, dbTestCase.hasDocumentation, dbTestCase.identifier)
	}

	def tupleToTestCase(x: TestCaseValueTuple): TestCases = {
		TestCases(x._1, x._2, x._3, x._4, x._5, x._6, x._7, x._8, x._9, x._10, x._11, x._12, x._13, x._14, x._15, x._16, None, x._17)
	}
}

@Singleton
class TestCaseManager @Inject() (testResultManager: TestResultManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

	import dbConfig.profile.api._

	val TEST_CASES_PATH = "test-cases"

	def getTestCaseForIdWrapper(testCaseId:String): Option[TestCase] = {
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

	def getTestCaseWithDocumentation(testCaseId: Long): TestCases = {
		exec(PersistenceSchema.testCases.filter(_.id === testCaseId).result.head)
	}

	def getTestCase(testCaseId:String): Option[TestCase] = {
		try {
			val tc = exec(PersistenceSchema.testCases.filter(_.id === testCaseId.toLong).map(x => TestCaseManager.withoutDocumentation(x)).result.head)
			Some(new TestCase(TestCaseManager.tupleToTestCase(tc)))
		}
		catch {
			case _: Exception => None
		}
	}

	def updateTestCaseWithoutMetadata(testCaseId: Long, path: String, testSuiteOrder: Short, targetActors: String): DBIO[_] = {
		val q1 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.path, t.testSuiteOrder, t.targetActors)
		q1.update(path, testSuiteOrder, Some(targetActors))
	}

	def updateTestCase(testCaseId: Long, identifier: String, shortName: String, fullName: String, version: String, authors: Option[String], description: Option[String], keywords: Option[String], testCaseType: Short, path: String, testSuiteOrder: Short, targetActors: String, hasDocumentation: Boolean, documentation: Option[String]): DBIO[_] = {
		val q1 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.identifier, t.shortname, t.fullname, t.version, t.authors, t.description, t.keywords, t.testCaseType, t.path, t.testSuiteOrder, t.targetActors, t.hasDocumentation, t.documentation)
		q1.update(identifier, shortName, fullName, version, authors, description, keywords, testCaseType, path, testSuiteOrder, Some(targetActors), hasDocumentation, documentation) andThen
		testResultManager.updateForUpdatedTestCase(testCaseId, shortName)
	}

	def delete(testCaseId: Long): DBIO[_] = {
		deleteInternal(testCaseId, skipConformanceResult = false)
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
		DBIO.seq(actions.toList.map(a => a): _*)
	}

	def removeActorLinksForTestCase(testCaseId: Long): DBIO[_] = {
		PersistenceSchema.testCaseHasActors.filter(_.testcase === testCaseId).delete
	}

	def removeActorLinkForTestCase(testCaseId: Long, actorId: Long): DBIO[_] = {
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

	def searchTestCases(domainIds: Option[List[Long]], specificationIds: Option[List[Long]], actorIds: Option[List[Long]], testSuiteIds: Option[List[Long]]): List[TestCases] = {
		val results = exec(
			for {
				allowedTestCasesByActor <- {
					if (actorIds.isDefined) {
						PersistenceSchema.testCaseHasActors.filter(_.actor inSet actorIds.get).map(x => x.testcase).result.map(x => Some(x.toSet))
					} else {
						DBIO.successful(None)
					}
				}
				testCases <- {
					PersistenceSchema.testCases
						.join(PersistenceSchema.specifications).on(_.targetSpec === _.id)
						.join(PersistenceSchema.testSuiteHasTestCases).on(_._1.id === _.testcase)
						.filterOpt(domainIds)((q, ids) => q._1._2.domain inSet ids)
						.filterOpt(specificationIds)((q, ids) => q._1._1.targetSpec inSet ids)
						.filterOpt(testSuiteIds)((q, ids) => q._2.testsuite inSet ids)
						.filterOpt(allowedTestCasesByActor)((q, ids) => q._1._1.id inSet ids)
						.sortBy(_._1._1.shortname.asc)
						.map(x => TestCaseManager.withoutDocumentation(x._1._1))
						.result
						.map(_.toList)
						.map(x => x.map(TestCaseManager.tupleToTestCase))
				}
			} yield testCases
		)
		results
	}
}
