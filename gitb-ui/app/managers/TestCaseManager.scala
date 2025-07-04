/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package managers

import models.{TestCase, TestCases}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.Rep

import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by serbay on 10/16/14.
 */
object TestCaseManager {

	type TestCaseDbTuple = (
		Rep[Long], Rep[String], Rep[String], Rep[String],
			Rep[Option[String]], Rep[Option[String]], Rep[Option[String]], Rep[Option[String]],
			Rep[Option[String]], Rep[Short], Rep[String],
			Rep[Option[String]], Rep[Option[String]], Rep[Short], Rep[Boolean], Rep[String], Rep[Boolean], Rep[Boolean]
		)

	type TestCaseValueTuple = (
		Long, String, String, String,
			Option[String], Option[String], Option[String], Option[String],
			Option[String], Short, String,
			Option[String], Option[String], Short, Boolean, String, Boolean, Boolean
		)

	def withoutDocumentation(dbTestCase: PersistenceSchema.TestCasesTable): TestCaseDbTuple = {
		(dbTestCase.id, dbTestCase.shortname, dbTestCase.fullname, dbTestCase.version,
			dbTestCase.authors, dbTestCase.originalDate, dbTestCase.modificationDate, dbTestCase.description,
			dbTestCase.keywords, dbTestCase.testCaseType, dbTestCase.path,
			dbTestCase.targetActors, dbTestCase.targetOptions, dbTestCase.testSuiteOrder, dbTestCase.hasDocumentation, dbTestCase.identifier,
			dbTestCase.isOptional, dbTestCase.isDisabled
		)
	}

	def tupleToTestCase(x: TestCaseValueTuple): TestCases = {
		TestCases(x._1, x._2, x._3, x._4, x._5, x._6, x._7, x._8, x._9, x._10, x._11, x._12, x._13, x._14, x._15, None, x._16, x._17, x._18)
	}
}

@Singleton
class TestCaseManager @Inject() (testResultManager: TestResultManager,
																 dbConfigProvider: DatabaseConfigProvider)
																(implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

	import dbConfig.profile.api._

	def getDomainOfTestCase(testCaseId: Long): Future[Long] = {
		DB.run(PersistenceSchema.testSuiteHasTestCases
			.join(PersistenceSchema.testSuites).on(_.testsuite === _.id)
			.filter(_._1.testcase === testCaseId)
			.map(_._2.domain)
			.result
			.head)
	}

	def getSpecificationsOfTestCases(testCaseIds: List[Long]): Future[Set[Long]] = {
		DB.run(PersistenceSchema.testSuiteHasTestCases
			.join(PersistenceSchema.specificationHasTestSuites).on(_.testsuite === _.testSuiteId)
			.filter(_._1.testcase inSet testCaseIds)
			.map(_._2.specId)
			.result).map(_.toSet)
	}

	def getTestCaseWithDocumentation(testCaseId: Long): Future[TestCases] = {
		DB.run(PersistenceSchema.testCases.filter(_.id === testCaseId).result.head)
	}

	def getTestCase(testCaseId:String): Future[Option[TestCase]] = {
		DB.run(
			PersistenceSchema.testCases.filter(_.id === testCaseId.toLong).map(x => TestCaseManager.withoutDocumentation(x)).result.headOption
		).map { result =>
			result.map(x => new TestCase(TestCaseManager.tupleToTestCase(x)))
		}
	}

	def updateTestCaseMetadata(testCaseId: Long, name: String, description: Option[String], documentation: Option[String], isOptional: Boolean, isDisabled: Boolean, tags: Option[String], specReference: Option[String], specDescription: Option[String], specLink: Option[String]): Future[Unit] = {
		var hasDocumentationToSet = false
		var documentationToSet: Option[String] = None
		if (documentation.isDefined && !documentation.get.isBlank) {
			hasDocumentationToSet = true
			documentationToSet = documentation
		}
		val q1 = for {
			_ <- PersistenceSchema.testCases
				.filter(_.id === testCaseId)
				.map(t => (t.shortname, t.fullname, t.description, t.documentation, t.hasDocumentation, t.isOptional, t.isDisabled, t.tags, t.specReference, t.specDescription, t.specLink))
				.update(name, name, description, documentationToSet, hasDocumentationToSet, isOptional, isDisabled, tags, specReference, specDescription, specLink)
			_ <- testResultManager.updateForUpdatedTestCase(testCaseId, name)
		} yield ()
		DB.run(q1.transactionally)
	}

	def updateTestCaseWithoutMetadata(testCaseId: Long, path: String, testSuiteOrder: Short, targetActors: String): DBIO[_] = {
		val q1 = for {t <- PersistenceSchema.testCases if t.id === testCaseId} yield (t.path, t.testSuiteOrder, t.targetActors)
		q1.update(path, testSuiteOrder, Some(targetActors))
	}

	def updateTestCase(testCaseId: Long, identifier: String, shortName: String, fullName: String, version: String, authors: Option[String], description: Option[String], keywords: Option[String], testCaseType: Short, path: String, testSuiteOrder: Short, targetActors: String, hasDocumentation: Boolean, documentation: Option[String], isOptional: Boolean, isDisabled: Boolean, tags: Option[String], specReference: Option[String], specDescription: Option[String], specLink: Option[String], group: Option[Long]): DBIO[_] = {
		for {
			_ <- PersistenceSchema.testCases
				.filter(_.id === testCaseId)
				.map(t => (t.identifier, t.shortname, t.fullname, t.version, t.authors, t.description, t.keywords, t.testCaseType, t.path, t.testSuiteOrder, t.targetActors, t.hasDocumentation, t.documentation, t.isOptional, t.isDisabled, t.tags, t.specReference, t.specDescription, t.specLink, t.group))
				.update(identifier, shortName, fullName, version, authors, description, keywords, testCaseType, path, testSuiteOrder, Some(targetActors), hasDocumentation, documentation, isOptional, isDisabled, tags, specReference, specDescription, specLink, group)
			_ <- testResultManager.updateForUpdatedTestCase(testCaseId, shortName)
		} yield ()
	}

	def delete(testCaseId: Long): DBIO[_] = {
		deleteInternal(testCaseId)
	}

	private def deleteInternal(testCaseId: Long): DBIO[_] = {
		val actions = new ListBuffer[DBIO[_]]()
		actions += testResultManager.updateForDeletedTestCase(testCaseId)
		actions += removeActorLinksForTestCase(testCaseId)
		actions += PersistenceSchema.testCaseCoversOptions.filter(_.testcase === testCaseId).delete
		actions += PersistenceSchema.testSuiteHasTestCases.filter(_.testcase === testCaseId).delete
		actions += PersistenceSchema.conformanceSnapshotResults.filter(_.testCaseId === testCaseId).map(_.testCaseId).update(testCaseId * -1)
		actions += PersistenceSchema.conformanceSnapshotTestCases.filter(_.id === testCaseId).map(_.id).update(testCaseId * -1)
		actions += PersistenceSchema.conformanceResults.filter(_.testcase === testCaseId).delete
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

	def getTestCaseDocumentation(testCaseId: Long): Future[Option[String]] = {
		DB.run(PersistenceSchema.testCases.filter(_.id === testCaseId).map(x => x.documentation).result.headOption).map { result =>
			result.flatten
		}
	}

	def searchTestCases(domainIds: Option[List[Long]], specificationIds: Option[List[Long]], specificationGroupIds: Option[List[Long]], actorIds: Option[List[Long]], testSuiteIds: Option[List[Long]]): Future[List[TestCases]] = {
		DB.run(
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
						.join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
						.join(PersistenceSchema.specificationHasTestSuites).on(_._2.testsuite === _.testSuiteId)
						.join(PersistenceSchema.testSuites).on(_._1._2.testsuite === _.id)
						.join(PersistenceSchema.specifications).on(_._1._2.specId === _.id)
						.filterOpt(domainIds)((q, ids) => q._1._2.domain inSet ids)
						.filterOpt(specificationIds)((q, ids) => q._1._1._2.specId inSet ids)
						.filterOpt(specificationGroupIds)((q, ids) => q._2.group inSet ids)
						.filterOpt(testSuiteIds)((q, ids) => q._1._2.id inSet ids)
						.filterOpt(allowedTestCasesByActor)((q, ids) => q._1._1._1._1.id inSet ids)
						.sortBy(_._1._1._1._1.shortname.asc)
						.map(x => TestCaseManager.withoutDocumentation(x._1._1._1._1))
						.result
						.map(_.toList)
						.map(x => x.map(TestCaseManager.tupleToTestCase))
				}
			} yield testCases
		)
	}
}
