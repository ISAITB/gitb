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

package persistence.db

import models._
import models.snapshot._
import models.theme.Theme
import slick.collection.heterogeneous.HNil
import slick.jdbc.MySQLProfile.api._

import java.sql.Timestamp

object PersistenceSchema {

  /**********************
   *** Primary Tables ***
   **********************/

  class CommunitiesTable(tag: Tag) extends Table[Communities](tag, "Communities") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def supportEmail = column[Option[String]] ("support_email")
    def selfRegType = column[Short]("selfreg_type")
    def selfRegToken = column[Option[String]] ("selfreg_token")
    def selfRegTokenHelpText = column[Option[String]]("selfreg_token_help_text", O.SqlType("TEXT"))
    def selfRegNotification = column[Boolean]("selfreg_notification")
    def interactionNotification = column[Boolean]("interaction_notification")
    def selfRegRestriction = column[Short]("selfreg_restriction")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def selfRegForceTemplateSelection = column[Boolean]("selfreg_force_template")
    def selfRegForceRequiredProperties = column[Boolean]("selfreg_force_properties")
    def allowCertificateDownload = column[Boolean]("allow_certificate_download")
    def allowStatementManagement = column[Boolean]("allow_statement_management")
    def allowSystemManagement = column[Boolean]("allow_system_management")
    def allowPostTestOrganisationUpdates = column[Boolean]("allow_post_test_org_updates")
    def allowPostTestSystemUpdates = column[Boolean]("allow_post_test_sys_updates")
    def allowPostTestStatementUpdates = column[Boolean]("allow_post_test_stm_updates")
    def allowAutomationApi = column[Boolean]("allow_automation_api")
    def allowCommunityView = column[Boolean]("allow_community_view")
    def apiKey = column[String]("api_key")
    def latestStatusLabel = column[Option[String]]("latest_status_label")
    def domain = column[Option[Long]] ("domain")
    def * = (id :: shortname :: fullname :: supportEmail :: selfRegType :: selfRegToken :: selfRegTokenHelpText :: selfRegNotification :: interactionNotification :: description :: selfRegRestriction :: selfRegForceTemplateSelection :: selfRegForceRequiredProperties :: allowCertificateDownload :: allowStatementManagement :: allowSystemManagement :: allowPostTestOrganisationUpdates :: allowPostTestSystemUpdates :: allowPostTestStatementUpdates :: allowAutomationApi :: allowCommunityView :: apiKey :: latestStatusLabel :: domain :: HNil).mapTo[Communities]
  }
  val communities = TableQuery[CommunitiesTable]
  val insertCommunity = communities returning communities.map(_.id)

  class OrganizationsTable(tag: Tag) extends Table[Organizations](tag, "Organizations") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def organizationType = column[Short]("type")
    def adminOrganization = column[Boolean]("admin_organization")
    def landingPage = column[Option[Long]] ("landing_page")
    def legalNotice = column[Option[Long]] ("legal_notice")
    def errorTemplate = column[Option[Long]] ("error_template")
    def template = column[Boolean]("template")
    def templateName = column[Option[String]]("template_name")
    def apiKey = column[Option[String]]("api_key")
    def updateTime = column[Timestamp]("updated_on", O.SqlType("TIMESTAMP"))
    def community = column[Long] ("community")
    def * = (id, shortname, fullname, organizationType, adminOrganization, landingPage, legalNotice, errorTemplate, template, templateName, apiKey, community) <> (Organizations.tupled, Organizations.unapply)
  }
  //get table name etc from organizations.baseTableRow
  val organizations = TableQuery[OrganizationsTable]
  //insert organizations by insertOrganization += organization. This is implemented to get auto-incremented ids
  val insertOrganization = organizations returning organizations.map(_.id)

  class UsersTable(tag: Tag) extends Table[Users](tag, "Users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def email = column[String]("email")
    def password = column[String]("password")
    def onetimePassword = column[Boolean]("onetime_password")
    def role = column[Short]("role")
    def organization = column[Long]("organization")
    def ssoUid = column[Option[String]]("sso_uid")
    def ssoEmail = column[Option[String]]("sso_email")
    def ssoStatus = column[Short]("sso_status")
    def * = (id, name, email, password, onetimePassword, role, organization, ssoUid, ssoEmail, ssoStatus) <> (Users.tupled, Users.unapply)
  }
  val users = TableQuery[UsersTable]
  val insertUser = users returning users.map(_.id)

  class SystemsTable(tag: Tag) extends Table[Systems](tag, "Systems") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def version = column[Option[String]]("version")
    def apiKey = column[String]("api_key")
    def badgeKey = column[String]("badge_key")
    def owner = column[Long]("owner")
    def * = (id, shortname, fullname, description, version, apiKey, badgeKey, owner) <> (Systems.tupled, Systems.unapply)
  }
  val systems = TableQuery[SystemsTable]
  val insertSystem = systems returning systems.map(_.id)

  class DomainsTable(tag: Tag) extends Table[Domain](tag, "Domains") {
	  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
  	def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def reportMetadata = column[Option[String]]("report_metadata", O.SqlType("TEXT"))
    def apiKey = column[String]("api_key")
    def * = (id, shortname, fullname, description, reportMetadata, apiKey) <> (Domain.tupled, Domain.unapply)
  }
  val domains = TableQuery[DomainsTable]

  class SpecificationsTable(tag: Tag) extends Table[Specifications](tag, "Specifications") {
	  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def reportMetadata = column[Option[String]]("report_metadata", O.SqlType("TEXT"))
    def hidden = column[Boolean]("is_hidden")
    def apiKey = column[String]("api_key")
    def domain = column[Long]("domain")
    def displayOrder = column[Short]("display_order")
    def group = column[Option[Long]]("spec_group")
    def * = (id, shortname, fullname, description, reportMetadata, hidden, apiKey, domain, displayOrder, group) <> (Specifications.tupled, Specifications.unapply)
  }
  val specifications = TableQuery[SpecificationsTable]
  val insertSpecification = specifications returning specifications.map(_.id)

  class ActorsTable(tag: Tag) extends Table[Actors](tag, "Actors") {
    def id      = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def actorId = column[String]("actorId")
    def name    = column[String]("name")
    def desc    = column[Option[String]]("description", O.SqlType("TEXT"))
    def reportMetadata = column[Option[String]]("report_metadata", O.SqlType("TEXT"))
    def default = column[Option[Boolean]]("is_default")
    def hidden = column[Boolean]("is_hidden")
    def displayOrder = column[Option[Short]]("display_order")
    def apiKey = column[String]("api_key")
    def domain  = column[Long]("domain")
    def * = (id, actorId, name, desc, reportMetadata, default, hidden, displayOrder, apiKey, domain) <> (Actors.tupled, Actors.unapply)
  }
  val actors = TableQuery[ActorsTable]
  val insertActor = actors returning actors.map(_.id)

  class EndpointsTable(tag: Tag) extends Table[Endpoints](tag, "Endpoints") {
	  def id    = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name  = column[String]("name")
    def desc  = column[Option[String]]("description", O.SqlType("TEXT"))
    def actor = column[Long]("actor")
    def * = (id, name, desc, actor) <> (Endpoints.tupled, Endpoints.unapply)
  }
  val endpoints = TableQuery[EndpointsTable]
  val insertEndpoint = endpoints returning endpoints.map(_.id)

	class ParametersTable(tag: Tag) extends Table[models.Parameters] (tag, "Parameters") {
		def id    = column[Long]("id", O.PrimaryKey, O.AutoInc)
		def name  = column[String]("name")
    def testKey = column[String]("test_key")
		def desc  = column[Option[String]]("description", O.SqlType("TEXT"))
		def use   = column[String]("use")
		def kind  = column[String]("kind")
    def adminOnly = column[Boolean]("admin_only")
    def notForTests = column[Boolean]("not_for_tests")
    def hidden = column[Boolean]("hidden")
    def allowedValues  = column[Option[String]]("allowed_values", O.SqlType("TEXT"))
    def displayOrder = column[Short]("display_order")
    def dependsOn  = column[Option[String]]("depends_on")
    def dependsOnValue  = column[Option[String]]("depends_on_value")
    def defaultValue  = column[Option[String]]("default_value", O.SqlType("TEXT"))
		def endpoint = column[Long]("endpoint")

		def * = (id, name, testKey, desc, use, kind, adminOnly, notForTests, hidden, allowedValues, displayOrder, dependsOn, dependsOnValue, defaultValue, endpoint) <> (models.Parameters.tupled, models.Parameters.unapply)
	}
	val parameters = TableQuery[ParametersTable]
  val insertParameter = parameters returning parameters.map(_.id)

  class DomainParametersTable(tag: Tag) extends Table[models.DomainParameter] (tag, "DomainParameters") {
    def id    = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name  = column[String]("name")
    def desc  = column[Option[String]]("description", O.SqlType("TEXT"))
    def kind  = column[String]("kind")
    def value = column[Option[String]]("value", O.SqlType("MEDIUMBLOB"))
    def inTests = column[Boolean]("in_tests")
    def contentType  = column[Option[String]]("content_type")
    def domain = column[Long]("domain")
    def * = (id, name, desc, kind, value, inTests, contentType, domain) <> (models.DomainParameter.tupled, models.DomainParameter.unapply)
  }
  val domainParameters = TableQuery[DomainParametersTable]

  class ConformanceResultsTable(tag: Tag) extends Table[models.ConformanceResult] (tag, "ConformanceResults") {
    def id    = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def sut  = column[Long]("sut_id")
    def spec  = column[Long]("spec_id")
    def actor  = column[Long]("actor_id")
    def testsuite  = column[Long]("test_suite_id")
    def testcase  = column[Long]("test_case_id")
    def result  = column[String]("result")
    def outputMessage = column[Option[String]]("output_message", O.SqlType("TEXT"))
    def testsession = column[Option[String]]("test_session_id")
    def updateTime = column[Option[Timestamp]]("update_time", O.SqlType("TIMESTAMP"))
    def * = (id, sut, spec, actor, testsuite, testcase, result, outputMessage, testsession, updateTime) <> (models.ConformanceResult.tupled, models.ConformanceResult.unapply)
  }
  val conformanceResults = TableQuery[ConformanceResultsTable]

  class ConfigurationsTable(tag:Tag) extends Table[Configs] (tag, "Configurations") {
    def system = column[Long] ("system")
	  def parameter = column[Long]("parameter")
	  def endpoint = column[Long] ("endpoint")
	  def value = column[String]("value", O.SqlType("MEDIUMBLOB"))
    def contentType  = column[Option[String]]("content_type")
    def * = (system, parameter, endpoint, value, contentType) <> (Configs.tupled, Configs.unapply)
    def pk = primaryKey("c_pk", (system, parameter, endpoint))
  }
  val configs = TableQuery[ConfigurationsTable]

  class TransactionsTable(tag: Tag) extends Table[Transaction](tag, "Transactions") {
    def shortname = column[String]("sname", O.PrimaryKey)
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def domain = column[Long]("domain")
    def * = (shortname, fullname, description, domain) <> (Transaction.tupled, Transaction.unapply)
  }
  val transactions = TableQuery[TransactionsTable]

  class OptionsTable(tag: Tag) extends Table[Options](tag, "Options") {
	  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def actor = column[Long]("actor")
    def * = (id, shortname, fullname, description, actor) <> (Options.tupled, Options.unapply)
  }
  val options = TableQuery[OptionsTable]

  class TestCaseGroupsTable(tag: Tag) extends Table[TestCaseGroup](tag, "TestCaseGroups") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def identifier = column[String]("identifier")
    def name    = column[Option[String]]("name")
    def description    = column[Option[String]]("description", O.SqlType("TEXT"))
    def testSuite = column[Long]("testsuite")
    def * = (id, identifier, name, description, testSuite) <> (TestCaseGroup.tupled, TestCaseGroup.unapply)
  }
  val testCaseGroups = TableQuery[TestCaseGroupsTable]

  class TestCasesTable(tag: Tag) extends Table[TestCases](tag, "TestCases") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def identifier = column[String]("identifier")
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def version = column[String]("version")
    def authors = column[Option[String]]("authors")
    def originalDate = column[Option[String]]("original_date")
    def modificationDate = column[Option[String]]("modification_date")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def keywords = column[Option[String]]("keywords")
    def testCaseType = column[Short]("type")
	  def path = column[String]("path")
    def targetActors = column[Option[String]]("target_actors")
    def targetOptions = column[Option[String]]("target_options")
    def testSuiteOrder = column[Short]("testsuite_order")
    def hasDocumentation = column[Boolean]("has_documentation")
    def documentation = column[Option[String]]("documentation")
    def isOptional = column[Boolean]("is_optional")
    def isDisabled = column[Boolean]("is_disabled")
    def tags = column[Option[String]]("tags", O.SqlType("TEXT"))
    def specReference = column[Option[String]]("spec_reference")
    def specDescription = column[Option[String]]("spec_description", O.SqlType("TEXT"))
    def specLink = column[Option[String]]("spec_link")
    def group = column[Option[Long]]("testcase_group")
    def * = (id :: shortname :: fullname :: version :: authors :: originalDate :: modificationDate :: description :: keywords :: testCaseType :: path :: targetActors :: targetOptions :: testSuiteOrder :: hasDocumentation :: documentation :: identifier :: isOptional :: isDisabled :: tags :: specReference :: specDescription :: specLink :: group :: HNil).mapTo[TestCases]
  }
  val testCases = TableQuery[TestCasesTable]

	class TestSuitesTable(tag: Tag) extends Table[TestSuites](tag, "TestSuites") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def identifier = column[String]("identifier")
		def shortname = column[String]("sname")
		def fullname = column[String]("fname")
		def version = column[String]("version")
		def authors = column[Option[String]]("authors")
		def originalDate = column[Option[String]]("original_date")
		def modificationDate = column[Option[String]]("modification_date")
		def description = column[Option[String]]("description", O.SqlType("TEXT"))
		def keywords = column[Option[String]]("keywords")
    def filename = column[String]("file_name")
    def hasDocumentation = column[Boolean]("has_documentation")
    def documentation = column[Option[String]]("documentation")
    def definitionPath = column[Option[String]]("definition_path")
    def hidden = column[Boolean]("is_hidden")
    def shared = column[Boolean]("is_shared")
    def domain = column[Long]("domain")
    def specReference = column[Option[String]]("spec_reference")
    def specDescription = column[Option[String]]("spec_description", O.SqlType("TEXT"))
    def specLink = column[Option[String]]("spec_link")
    def * = (id :: shortname :: fullname :: version :: authors :: originalDate :: modificationDate :: description :: keywords :: filename :: hasDocumentation :: documentation :: identifier :: hidden :: shared :: domain :: definitionPath :: specReference :: specDescription :: specLink :: HNil).mapTo[TestSuites]
	}
	val testSuites = TableQuery[TestSuitesTable]

  class TestResultsTable(tag: Tag) extends Table[TestResult](tag, "TestResults") {
    def testSessionId = column[String]("test_session_id", O.PrimaryKey)
	  def sutId = column[Option[Long]]("sut_id")
    def sut = column[Option[String]]("sut")
    def organizationId = column[Option[Long]]("organization_id")
    def organization = column[Option[String]]("organization")
    def communityId = column[Option[Long]]("community_id")
    def community = column[Option[String]]("community")
    def testCaseId = column[Option[Long]]("testcase_id")
    def testCase = column[Option[String]]("testcase")
    def testSuiteId = column[Option[Long]]("testsuite_id")
    def testSuite = column[Option[String]]("testsuite")
    def actorId = column[Option[Long]]("actor_id")
    def actor = column[Option[String]]("actor")
    def specificationId = column[Option[Long]]("specification_id")
    def specification = column[Option[String]]("specification")
    def domainId = column[Option[Long]]("domain_id")
    def domain = column[Option[String]]("domain")
	  def result = column[String]("result")
	  def startTime = column[Timestamp]("start_time")
	  def endTime = column[Option[Timestamp]]("end_time", O.SqlType("TIMESTAMP"))
    def outputMessage = column[Option[String]]("output_message", O.SqlType("TEXT"))
    def * = (testSessionId, sutId, sut, organizationId, organization, communityId, community, testCaseId, testCase, testSuiteId, testSuite, actorId, actor, specificationId, specification, domainId, domain, result, startTime, endTime, outputMessage) <> (TestResult.tupled, TestResult.unapply)
  }
  val testResults = TableQuery[TestResultsTable]

  class TestResultDefinitionsTable(tag: Tag) extends Table[TestResultDefinition](tag, "TestResultDefinitions") {
    def testSessionId = column[String]("test_session_id", O.PrimaryKey)
    def tpl = column[String]("tpl", O.SqlType("MEDIUMBLOB"))
    def * = (testSessionId, tpl) <> (TestResultDefinition.tupled, TestResultDefinition.unapply)
  }
  val testResultDefinitions = TableQuery[TestResultDefinitionsTable]

  class TestStepReports(tag: Tag) extends Table[TestStepResult](tag, "TestStepReports") {
		def testSessionId = column[String]("test_session_id")
		def testStepId = column[String]("test_step_id")
		def result = column[Short]("result")
		def reportPath = column[String]("report_path")

		def * = (testSessionId, testStepId, result, reportPath) <> (TestStepResult.tupled, TestStepResult.unapply)

		def pk = primaryKey("tsr_pk", (testSessionId, testStepId))
	}
	val testStepReports = TableQuery[TestStepReports]

  class TestInteractionsTable(tag: Tag) extends Table[TestInteraction](tag, "TestInteractions") {
    def testSessionId = column[String]("test_session_id")
    def testStepId = column[String]("test_step_id")
    def admin = column[Boolean]("is_admin")
    def createTime = column[Timestamp]("created_on", O.SqlType("TIMESTAMP"))
    def tpl = column[String]("tpl", O.SqlType("TEXT"))
    def * = (testSessionId, testStepId, admin, createTime, tpl) <> (TestInteraction.tupled, TestInteraction.unapply)
    def pk = primaryKey("ti_pk", (testSessionId, testStepId))
  }
  val testInteractions = TableQuery[TestInteractionsTable]

  /*************************
   *** Relational Tables ***
   *************************/

  class SystemHasAdminsTable(tag: Tag) extends Table[(Long, Long)](tag, "SystemHasAdmins") {
    def systemId = column[Long]("sut_id")
    def userId = column[Long]("user_id")
    def * = (systemId, userId)
    def pk = primaryKey("sha1_pk", (systemId, userId))
  }
  val systemHasAdmins = TableQuery[SystemHasAdminsTable]

  class SystemImplementsActors(tag: Tag) extends Table[(Long, Long, Long)] (tag, "SystemImplementsActors") {
    def systemId = column[Long]("sut_id")
    def specId = column[Long]("spec_id")
    def actorId = column[Long]("actor_id")
    def * = (systemId, specId, actorId)
    def pk = primaryKey("sia_pk", (systemId, specId, actorId))
  }
  val systemImplementsActors = TableQuery[SystemImplementsActors]

	class SystemImplementsOptions(tag: Tag) extends Table[(Long, Long)] (tag, "SystemImplementsOptions") {
		def systemId = column[Long]("sut_id")
		def optionId = column[Long]("option_id")

		def * = (systemId, optionId)
		def pk = primaryKey("sio_pk", (systemId, optionId))
	}
	val systemImplementsOptions = TableQuery[SystemImplementsOptions]

  class SpecificationHasActorsTable(tag: Tag) extends Table[(Long, Long)](tag, "SpecificationHasActors") {
    def specId = column[Long]("spec_id")
    def actorId = column[Long]("actor_id")
    def * = (specId, actorId)
    def pk = primaryKey("sha2_pk", (specId, actorId))
  }
  val specificationHasActors = TableQuery[SpecificationHasActorsTable]

  class SpecificationHasTestSuitesTable(tag: Tag) extends Table[(Long, Long)](tag, "SpecificationHasTestSuites") {
    def specId = column[Long]("spec")
    def testSuiteId = column[Long]("testsuite")
    def * = (specId, testSuiteId)
    def pk = primaryKey("shts_pk", (specId, testSuiteId))
  }
  val specificationHasTestSuites = TableQuery[SpecificationHasTestSuitesTable]

  class EndpointSupportsTransactionsTable(tag: Tag) extends Table[(Long, String, String)](tag, "EndpointSupportsTransactions") {
    def actorId = column[Long]("actor")
    def endpoint = column[String]("endpoint")
    def transaction = column[String]("transaction")
    def * = (actorId, endpoint, transaction)
    def pk = primaryKey("est_pk", (actorId, endpoint, transaction))
  }
  val endpointSupportsTransactions = TableQuery[EndpointSupportsTransactionsTable]

	class TestCaseHasActorsTable(tag: Tag) extends Table[(Long, Long, Long, Boolean)](tag, "TestCaseHasActors") {
		def testcase = column[Long]("testcase")
    def specification = column[Long] ("specification")
		def actor = column[Long]("actor")
    def sut = column[Boolean]("sut")

		def * = (testcase, specification, actor, sut)

		def pk = primaryKey("tcha_pk", (testcase, specification, actor))
	}
	val testCaseHasActors = TableQuery[TestCaseHasActorsTable]

	class TestSuiteHasActorsTable(tag: Tag) extends Table[(Long, Long)](tag, "TestSuiteHasActors") {
		def testsuite = column[Long]("testsuite")
		def actor = column[Long]("actor")

		def * = (testsuite, actor)

		def pk = primaryKey("tsha_pk", (testsuite, actor))
	}
	val testSuiteHasActors = TableQuery[TestSuiteHasActorsTable]

	class TestSuiteHasTestCasesTable(tag: Tag) extends Table[(Long, Long)](tag, "TestSuiteHasTestCases") {
		def testsuite = column[Long]("testsuite")
		def testcase = column[Long]("testcase")

		def * = (testsuite, testcase)

		def pk = primaryKey("tshtc_pk", (testsuite, testcase))
	}
	val testSuiteHasTestCases = TableQuery[TestSuiteHasTestCasesTable]

  class TestCaseCoversOptionsTable(tag: Tag) extends Table[(Long, Long)](tag, "TestCaseCoversOptions") {
    def testcase = column[Long]("testcase")
    def option = column[Long]("option")
    def * =  (testcase, option)
    def pk = primaryKey("tcco_pk", (testcase, option))
  }
  val testCaseCoversOptions = TableQuery[TestCaseCoversOptionsTable]

  class LandingPagesTable(tag: Tag) extends Table[LandingPages](tag, "LandingPages") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def content = column[String]("content")
    def default = column[Boolean]("default_flag")
    def community = column[Long]("community")
    def * = (id, name, description, content, default, community) <> (LandingPages.tupled, LandingPages.unapply)
  }
  val landingPages = TableQuery[LandingPagesTable]
  val insertLandingPage = landingPages returning landingPages.map(_.id)

  class SystemConfigurationsTable(tag: Tag) extends Table[SystemConfigurations](tag, "SystemConfigurations") {
    def name = column[String]("name", O.PrimaryKey)
    def parameter = column[Option[String]]("parameter", O.SqlType("TEXT"))
    def description = column[Option[String]]("description")
    def * = (name, parameter, description) <> (SystemConfigurations.tupled, SystemConfigurations.unapply)
  }
  val systemConfigurations = TableQuery[SystemConfigurationsTable]

  class LegalNoticesTable(tag: Tag) extends Table[LegalNotices](tag, "LegalNotices") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def content = column[String]("content")
    def default = column[Boolean]("default_flag")
    def community = column[Long]("community")
    def * = (id, name, description, content, default, community) <> (LegalNotices.tupled, LegalNotices.unapply)
  }
  val legalNotices = TableQuery[LegalNoticesTable]
  val insertLegalNotice = legalNotices returning legalNotices.map(_.id)

  class ErrorTemplatesTable(tag: Tag) extends Table[ErrorTemplates](tag, "ErrorTemplates") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def content = column[String]("content")
    def default = column[Boolean]("default_flag")
    def community = column[Long]("community")
    def * = (id, name, description, content, default, community) <> (ErrorTemplates.tupled, ErrorTemplates.unapply)
  }
  val errorTemplates = TableQuery[ErrorTemplatesTable]
  val insertErrorTemplate = errorTemplates returning errorTemplates.map(_.id)

  class CommunityReportSettingsTable(tag: Tag) extends Table[CommunityReportSettings](tag, "CommunityReportSettings") {
    def reportType = column[Short]("report_type")
    def signPdfs = column[Boolean]("sign_pdf")
    def customPdfs = column[Boolean]("custom_pdf")
    def customPdfsWithCustomXml = column[Boolean]("custom_pdf_with_custom_xml")
    def customPdfService  = column[Option[String]]("custom_pdf_service")
    def community = column[Long]("community")
    def * = (reportType, signPdfs, customPdfs, customPdfsWithCustomXml, customPdfService, community) <> (CommunityReportSettings.tupled, CommunityReportSettings.unapply)
  }
  val communityReportSettings = TableQuery[CommunityReportSettingsTable]

  class ConformanceCertificatesTable(tag: Tag) extends Table[ConformanceCertificate](tag, "ConformanceCertificates") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[Option[String]]("title", O.SqlType("TEXT"))
    def message = column[Option[String]]("message", O.SqlType("TEXT"))
    def includeTitle = column[Boolean]("include_title")
    def includeMessage = column[Boolean]("include_message")
    def includeTestStatus = column[Boolean]("include_test_status")
    def includeTestCases = column[Boolean]("include_test_cases")
    def includeDetails = column[Boolean]("include_details")
    def includeSignature = column[Boolean]("include_signature")
    def includePageNumbers = column[Boolean]("include_page_numbers")
    def community = column[Long]("community")
    def * = (id, title, includeTitle, includeMessage, includeTestStatus, includeTestCases, includeDetails, includeSignature, includePageNumbers, message, community) <> (ConformanceCertificate.tupled, ConformanceCertificate.unapply)
  }
  val conformanceCertificates = TableQuery[ConformanceCertificatesTable]
  val insertConformanceCertificate = conformanceCertificates returning conformanceCertificates.map(_.id)

  class CommunityKeystoresTable(tag: Tag) extends Table[CommunityKeystore](tag, "CommunityKeystores") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def keystoreFile = column[String]("keystore_file", O.SqlType("MEDIUMBLOB"))
    def keystoreType = column[String]("keystore_type", O.SqlType("TEXT"))
    def keystorePassword = column[String]("keystore_pass", O.SqlType("TEXT"))
    def keyPassword = column[String]("key_pass", O.SqlType("TEXT"))
    def community = column[Long]("community")
    def * = (id, keystoreFile, keystoreType, keystorePassword, keyPassword, community) <> (CommunityKeystore.tupled, CommunityKeystore.unapply)
  }
  val communityKeystores = TableQuery[CommunityKeystoresTable]
  val insertCommunityKeystore = communityKeystores returning communityKeystores.map(_.id)

  class ConformanceOverviewCertificatesTable(tag: Tag) extends Table[ConformanceOverviewCertificate](tag, "ConformanceOverviewCertificates") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[Option[String]]("title", O.SqlType("TEXT"))
    def includeTitle = column[Boolean]("include_title")
    def includeMessage = column[Boolean]("include_message")
    def includeStatementStatus = column[Boolean]("include_statement_status")
    def includeStatements = column[Boolean]("include_statements")
    def includeStatementDetails = column[Boolean]("include_statement_details")
    def includeDetails = column[Boolean]("include_details")
    def includeSignature = column[Boolean]("include_signature")
    def includePageNumbers = column[Boolean]("include_page_numbers")
    def enableAllLevel = column[Boolean]("enable_all")
    def enableDomainLevel = column[Boolean]("enable_domain")
    def enableGroupLevel = column[Boolean]("enable_group")
    def enableSpecificationLevel = column[Boolean]("enable_specification")
    def community = column[Long]("community")
    def * = (id :: title :: includeTitle :: includeMessage :: includeStatementStatus :: includeStatements :: includeStatementDetails :: includeDetails :: includeSignature :: includePageNumbers :: enableAllLevel :: enableDomainLevel :: enableGroupLevel :: enableSpecificationLevel :: community :: HNil).mapTo[ConformanceOverviewCertificate]
  }
  val conformanceOverviewCertificates = TableQuery[ConformanceOverviewCertificatesTable]
  val insertConformanceOverviewCertificate = conformanceOverviewCertificates returning conformanceOverviewCertificates.map(_.id)

  class ConformanceOverviewCertificateMessagesTable(tag: Tag) extends Table[ConformanceOverviewCertificateMessage](tag, "ConformanceOverviewCertificateMessages") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def message = column[String]("message", O.SqlType("TEXT"))
    def messageType = column[Short]("message_type")
    def domain = column[Option[Long]]("domain_id")
    def group = column[Option[Long]]("group_id")
    def specification = column[Option[Long]]("specification_id")
    def actor = column[Option[Long]]("actor_id")
    def community = column[Long]("community_id")
    def * = (id, messageType, message, domain, group, specification, actor, community) <> (ConformanceOverviewCertificateMessage.tupled, ConformanceOverviewCertificateMessage.unapply)
  }
  val conformanceOverviewCertificateMessages = TableQuery[ConformanceOverviewCertificateMessagesTable]
  val insertConformanceOverviewCertificateMessage = conformanceOverviewCertificateMessages returning conformanceOverviewCertificateMessages.map(_.id)

  class OrganisationParametersTable(tag: Tag) extends Table[OrganisationParameters](tag, "OrganisationParameters") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def testKey = column[String]("test_key")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def use   = column[String]("use")
    def kind  = column[String]("kind")
    def adminOnly = column[Boolean]("admin_only")
    def notForTests = column[Boolean]("not_for_tests")
    def inExports = column[Boolean]("in_exports")
    def inSelfRegistration = column[Boolean]("in_selfreg")
    def hidden = column[Boolean]("hidden")
    def allowedValues  = column[Option[String]]("allowed_values", O.SqlType("TEXT"))
    def displayOrder = column[Short]("display_order")
    def dependsOn  = column[Option[String]]("depends_on")
    def dependsOnValue  = column[Option[String]]("depends_on_value")
    def defaultValue  = column[Option[String]]("default_value", O.SqlType("TEXT"))
    def community = column[Long]("community")
    def * = (id, name, testKey, description, use, kind, adminOnly, notForTests, inExports, inSelfRegistration, hidden, allowedValues, displayOrder, dependsOn, dependsOnValue, defaultValue, community) <> (OrganisationParameters.tupled, OrganisationParameters.unapply)
  }
  val organisationParameters = TableQuery[OrganisationParametersTable]
  val insertOrganisationParameters = organisationParameters returning organisationParameters.map(_.id)

  class SystemParametersTable(tag: Tag) extends Table[SystemParameters](tag, "SystemParameters") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def testKey = column[String]("test_key")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def use   = column[String]("use")
    def kind  = column[String]("kind")
    def adminOnly = column[Boolean]("admin_only")
    def notForTests = column[Boolean]("not_for_tests")
    def inExports = column[Boolean]("in_exports")
    def hidden = column[Boolean]("hidden")
    def allowedValues  = column[Option[String]]("allowed_values", O.SqlType("TEXT"))
    def displayOrder = column[Short]("display_order")
    def dependsOn  = column[Option[String]]("depends_on")
    def dependsOnValue  = column[Option[String]]("depends_on_value")
    def defaultValue  = column[Option[String]]("default_value", O.SqlType("TEXT"))
    def community = column[Long]("community")
    def * = (id, name, testKey, description, use, kind, adminOnly, notForTests, inExports, hidden, allowedValues, displayOrder, dependsOn, dependsOnValue, defaultValue, community) <> (SystemParameters.tupled, SystemParameters.unapply)
  }
  val systemParameters = TableQuery[SystemParametersTable]
  val insertSystemParameters = systemParameters returning systemParameters.map(_.id)

  class OrganisationParameterValuesTable(tag: Tag) extends Table[OrganisationParameterValues](tag, "OrganisationParameterValues") {
    def organisation = column[Long] ("organisation")
    def parameter = column[Long]("parameter")
    def value = column[String]("value", O.SqlType("MEDIUMBLOB"))
    def contentType  = column[Option[String]]("content_type")
    def * = (organisation, parameter, value, contentType) <> (OrganisationParameterValues.tupled, OrganisationParameterValues.unapply)
    def pk = primaryKey("opv_pk", (organisation, parameter))
  }
  val organisationParameterValues = TableQuery[OrganisationParameterValuesTable]

  class SystemParameterValuesTable(tag: Tag) extends Table[SystemParameterValues](tag, "SystemParameterValues") {
    def system = column[Long] ("system")
    def parameter = column[Long]("parameter")
    def value = column[String]("value", O.SqlType("MEDIUMBLOB"))
    def contentType  = column[Option[String]]("content_type")
    def * = (system, parameter, value, contentType) <> (SystemParameterValues.tupled, SystemParameterValues.unapply)
    def pk = primaryKey("spv_pk", (system, parameter))
  }
  val systemParameterValues = TableQuery[SystemParameterValuesTable]

  class CommunityLabelsTable(tag: Tag) extends Table[CommunityLabels](tag, "CommunityLabels") {
    def community = column[Long] ("community")
    def labelType = column[Short]("label_type")
    def singularForm = column[String]("singular_form")
    def pluralForm = column[String]("plural_form")
    def fixedCase = column[Boolean]("fixed_case")
    def * = (community, labelType, singularForm, pluralForm, fixedCase) <> (CommunityLabels.tupled, CommunityLabels.unapply)
    def pk = primaryKey("cl_pk", (community, labelType))
  }
  val communityLabels = TableQuery[CommunityLabelsTable]

  class TriggersTable(tag: Tag) extends Table[Triggers](tag, "Triggers") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def url = column[String]("url")
    def eventType = column[Short]("event_type")
    def serviceType = column[Short]("service_type")
    def operation = column[Option[String]]("operation")
    def active = column[Boolean]("active")
    def latestResultOk = column[Option[Boolean]]("latest_result_ok")
    def latestResultOutput = column[Option[String]]("latest_result_output", O.SqlType("TEXT"))
    def community = column[Long] ("community")
    def * = (id, name, description, url, eventType, serviceType, operation, active, latestResultOk, latestResultOutput, community) <> (Triggers.tupled, Triggers.unapply)
    def pk = primaryKey("triggers_pk", id)
  }
  val triggers = TableQuery[TriggersTable]
  val insertTriggers = triggers returning triggers.map(_.id)

  class TriggerDataTable(tag: Tag) extends Table[TriggerData](tag, "TriggerData") {
    def dataType = column[Short]("data_type")
    def dataId = column[Long]("data_id")
    def trigger = column[Long] ("trigger")
    def * = (dataType, dataId, trigger) <> (TriggerData.tupled, TriggerData.unapply)
    def pk = primaryKey("cl_pk", (dataType, dataId, trigger))
  }
  val triggerData = TableQuery[TriggerDataTable]

  class TriggerFireExpressionsTable(tag: Tag) extends Table[TriggerFireExpression](tag, "TriggerFireExpressions") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def expression = column[String]("expression")
    def expressionType = column[Short]("expression_type")
    def notMatch = column[Boolean]("not_match")
    def trigger = column[Long] ("trigger")
    def * = (id, expression, expressionType, notMatch, trigger) <> (TriggerFireExpression.tupled, TriggerFireExpression.unapply)
  }
  val triggerFireExpressions = TableQuery[TriggerFireExpressionsTable]

  class CommunityResourcesTable(tag: Tag) extends Table[CommunityResources](tag, "CommunityResources") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def community = column[Long]("community")
    def * = (id, name, description, community) <> (CommunityResources.tupled, CommunityResources.unapply)
  }
  val communityResources = TableQuery[CommunityResourcesTable]
  val insertCommunityResources = communityResources returning communityResources.map(_.id)

  class SpecificationGroupsTable(tag: Tag) extends Table[SpecificationGroups](tag, "SpecificationGroups") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def reportMetadata = column[Option[String]]("report_metadata", O.SqlType("TEXT"))
    def displayOrder = column[Short]("display_order")
    def apiKey = column[String]("api_key")
    def domain = column[Long]("domain")
    def * = (id, shortname, fullname, description, reportMetadata, displayOrder, apiKey, domain) <> (SpecificationGroups.tupled, SpecificationGroups.unapply)
  }
  val specificationGroups = TableQuery[SpecificationGroupsTable]
  val insertSpecificationGroups = specificationGroups returning specificationGroups.map(_.id)

  class ConformanceSnapshotsTable(tag: Tag) extends Table[ConformanceSnapshot](tag, "ConformanceSnapshots") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def label = column[String]("label")
    def publicLabel = column[Option[String]]("public_label")
    def snapshotTime = column[Timestamp]("snapshot_time", O.SqlType("TIMESTAMP"))
    def apiKey = column[String]("api_key")
    def isPublic = column[Boolean]("is_public")
    def community = column[Long]("community")
    def * = (id, label, publicLabel, snapshotTime, apiKey, isPublic, community) <> (ConformanceSnapshot.tupled, ConformanceSnapshot.unapply)
  }
  val conformanceSnapshots = TableQuery[ConformanceSnapshotsTable]
  val insertConformanceSnapshot = conformanceSnapshots returning conformanceSnapshots.map(_.id)

  class ConformanceSnapshotResultsTable(tag: Tag) extends Table[ConformanceSnapshotResult](tag, "ConformanceSnapshotResults") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def organisationId = column[Long] ("organization_id")
    def systemId = column[Long]("sut_id")
    def domainId = column[Long]("domain_id")
    def specificationGroupId = column[Option[Long]]("spec_group_id")
    def specificationId = column[Long]("spec_id")
    def actorId = column[Long]("actor_id")
    def testSuiteId = column[Long]("test_suite_id")
    def testCaseId = column[Long]("test_case_id")
    def testCaseGroupId = column[Option[Long]]("test_case_group_id")
    def testSessionId = column[Option[String]]("test_session_id")
    def result = column[String]("result")
    def outputMessage = column[Option[String]]("output_message", O.SqlType("TEXT"))
    def updateTime = column[Option[Timestamp]]("update_time", O.SqlType("TIMESTAMP"))
    def snapshotId = column[Long]("snapshot_id")
    def * = (id :: snapshotId :: organisationId ::  systemId ::  domainId :: specificationGroupId :: specificationId :: actorId :: testSuiteId :: testCaseId :: testCaseGroupId:: testSessionId :: result :: outputMessage :: updateTime :: HNil).mapTo[ConformanceSnapshotResult]
  }
  val conformanceSnapshotResults = TableQuery[ConformanceSnapshotResultsTable]
  val insertConformanceSnapshotResult = conformanceSnapshotResults returning conformanceSnapshotResults.map(_.id)

  class ConformanceSnapshotTestCasesTable(tag: Tag) extends Table[ConformanceSnapshotTestCase](tag, "ConformanceSnapshotTestCases") {
    def id = column[Long]("id")
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def version = column[String]("version")
    def testSuiteOrder = column[Short]("testsuite_order")
    def identifier = column[String]("identifier")
    def isOptional = column[Boolean]("is_optional")
    def isDisabled = column[Boolean]("is_disabled")
    def tags = column[Option[String]]("tags", O.SqlType("TEXT"))
    def specReference = column[Option[String]]("spec_reference")
    def specDescription = column[Option[String]]("spec_description", O.SqlType("TEXT"))
    def specLink = column[Option[String]]("spec_link")
    def snapshotId = column[Long]("snapshot_id")
    def * = (id :: shortname :: fullname :: description :: version :: testSuiteOrder :: identifier :: isOptional :: isDisabled :: tags :: specReference :: specDescription :: specLink :: snapshotId :: HNil).mapTo[ConformanceSnapshotTestCase]
  }
  val conformanceSnapshotTestCases = TableQuery[ConformanceSnapshotTestCasesTable]

  class ConformanceSnapshotTestCaseGroupsTable(tag: Tag) extends Table[ConformanceSnapshotTestCaseGroup](tag, "ConformanceSnapshotTestCaseGroups") {
    def id = column[Long]("id")
    def identifier = column[String]("identifier")
    def name = column[Option[String]]("name")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def snapshotId = column[Long]("snapshot_id")
    def * = (id :: identifier :: name :: description :: snapshotId :: HNil).mapTo[ConformanceSnapshotTestCaseGroup]
  }
  val conformanceSnapshotTestCaseGroups = TableQuery[ConformanceSnapshotTestCaseGroupsTable]

  class ConformanceSnapshotTestSuitesTable(tag: Tag) extends Table[ConformanceSnapshotTestSuite](tag, "ConformanceSnapshotTestSuites") {
    def id = column[Long]("id")
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def version = column[String]("version")
    def identifier = column[String]("identifier")
    def specReference = column[Option[String]]("spec_reference")
    def specDescription = column[Option[String]]("spec_description", O.SqlType("TEXT"))
    def specLink = column[Option[String]]("spec_link")
    def snapshotId = column[Long]("snapshot_id")
    def * = (id :: shortname :: fullname :: description :: version :: identifier :: specReference :: specDescription :: specLink :: snapshotId :: HNil).mapTo[ConformanceSnapshotTestSuite]
  }
  val conformanceSnapshotTestSuites = TableQuery[ConformanceSnapshotTestSuitesTable]

  class ConformanceSnapshotActorsTable(tag: Tag) extends Table[ConformanceSnapshotActor](tag, "ConformanceSnapshotActors") {
    def id = column[Long]("id")
    def actorId = column[String]("actorId")
    def name = column[String]("name")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def reportMetadata = column[Option[String]]("report_metadata", O.SqlType("TEXT"))
    def visible = column[Boolean]("visible")
    def apiKey = column[String]("api_key")
    def snapshotId = column[Long]("snapshot_id")
    def * = (id :: actorId :: name :: description :: reportMetadata :: visible :: apiKey :: snapshotId :: HNil).mapTo[ConformanceSnapshotActor]
  }
  val conformanceSnapshotActors = TableQuery[ConformanceSnapshotActorsTable]

  class ConformanceSnapshotSpecificationsTable(tag: Tag) extends Table[ConformanceSnapshotSpecification](tag, "ConformanceSnapshotSpecifications") {
    def id = column[Long]("id")
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def reportMetadata = column[Option[String]]("report_metadata", O.SqlType("TEXT"))
    def apiKey = column[String]("api_key")
    def displayOrder = column[Short]("display_order")
    def snapshotId = column[Long]("snapshot_id")
    def * = (id :: shortname :: fullname :: description :: reportMetadata :: apiKey :: displayOrder :: snapshotId :: HNil).mapTo[ConformanceSnapshotSpecification]
  }
  val conformanceSnapshotSpecifications = TableQuery[ConformanceSnapshotSpecificationsTable]

  class ConformanceSnapshotSpecificationGroupsTable(tag: Tag) extends Table[ConformanceSnapshotSpecificationGroup](tag, "ConformanceSnapshotSpecificationGroups") {
    def id = column[Long]("id")
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def reportMetadata = column[Option[String]]("report_metadata", O.SqlType("TEXT"))
    def displayOrder = column[Short]("display_order")
    def snapshotId = column[Long]("snapshot_id")
    def * = (id :: shortname :: fullname :: description :: reportMetadata :: displayOrder :: snapshotId :: HNil).mapTo[ConformanceSnapshotSpecificationGroup]
  }
  val conformanceSnapshotSpecificationGroups = TableQuery[ConformanceSnapshotSpecificationGroupsTable]

  class ConformanceSnapshotDomainsTable(tag: Tag) extends Table[ConformanceSnapshotDomain](tag, "ConformanceSnapshotDomains") {
    def id = column[Long]("id")
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def reportMetadata = column[Option[String]]("report_metadata", O.SqlType("TEXT"))
    def snapshotId = column[Long]("snapshot_id")
    def * = (id :: shortname :: fullname :: description :: reportMetadata :: snapshotId :: HNil).mapTo[ConformanceSnapshotDomain]
  }
  val conformanceSnapshotDomains = TableQuery[ConformanceSnapshotDomainsTable]

  class ConformanceSnapshotSystemsTable(tag: Tag) extends Table[ConformanceSnapshotSystem](tag, "ConformanceSnapshotSystems") {
    def id = column[Long]("id")
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def version = column[Option[String]]("version")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def apiKey = column[String]("api_key")
    def badgeKey = column[String]("badge_key")
    def snapshotId = column[Long]("snapshot_id")
    def * = (id :: shortname :: fullname :: version :: description :: apiKey :: badgeKey :: snapshotId :: HNil).mapTo[ConformanceSnapshotSystem]
  }
  val conformanceSnapshotSystems = TableQuery[ConformanceSnapshotSystemsTable]

  class ConformanceSnapshotOrganisationsTable(tag: Tag) extends Table[ConformanceSnapshotOrganisation](tag, "ConformanceSnapshotOrganisations") {
    def id = column[Long]("id")
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def apiKey = column[Option[String]]("api_key")
    def snapshotId = column[Long]("snapshot_id")
    def * = (id :: shortname :: fullname :: apiKey :: snapshotId :: HNil).mapTo[ConformanceSnapshotOrganisation]
  }
  val conformanceSnapshotOrganisations = TableQuery[ConformanceSnapshotOrganisationsTable]

  class ConformanceSnapshotDomainParameterTable(tag: Tag) extends Table[ConformanceSnapshotDomainParameter](tag, "conformancesnapshotdomainparams") {
    def domainId = column[Long]("domain_id")
    def paramKey = column[String]("param_key")
    def paramValue = column[String]("param_value")
    def snapshotId = column[Long]("snapshot_id")
    def * = (domainId :: paramKey :: paramValue :: snapshotId :: HNil).mapTo[ConformanceSnapshotDomainParameter]
  }
  val conformanceSnapshotDomainParameters = TableQuery[ConformanceSnapshotDomainParameterTable]

  class ConformanceSnapshotOrganisationPropertyTable(tag: Tag) extends Table[ConformanceSnapshotOrganisationProperty](tag, "conformancesnapshotorgparams") {
    def organisationId = column[Long]("org_id")
    def propertyKey = column[String]("param_key")
    def propertyValue = column[String]("param_value")
    def snapshotId = column[Long]("snapshot_id")
    def * = (organisationId :: propertyKey :: propertyValue :: snapshotId :: HNil).mapTo[ConformanceSnapshotOrganisationProperty]
  }
  val conformanceSnapshotOrganisationProperties = TableQuery[ConformanceSnapshotOrganisationPropertyTable]

  class ConformanceSnapshotSystemPropertyTable(tag: Tag) extends Table[ConformanceSnapshotSystemProperty](tag, "conformancesnapshotsysparams") {
    def systemId = column[Long]("sys_id")
    def propertyKey = column[String]("param_key")
    def propertyValue = column[String]("param_value")
    def snapshotId = column[Long]("snapshot_id")
    def * = (systemId :: propertyKey :: propertyValue :: snapshotId :: HNil).mapTo[ConformanceSnapshotSystemProperty]
  }
  val conformanceSnapshotSystemProperties = TableQuery[ConformanceSnapshotSystemPropertyTable]

  class ConformanceSnapshotCertificateMessageTable(tag: Tag) extends Table[ConformanceSnapshotCertificateMessage](tag, "conformancesnapshotcertificatemessages") {
    def message = column[String]("message")
    def snapshotId = column[Long]("snapshot_id")
    def * = (message :: snapshotId :: HNil).mapTo[ConformanceSnapshotCertificateMessage]
  }
  val conformanceSnapshotCertificateMessages = TableQuery[ConformanceSnapshotCertificateMessageTable]

  class ConformanceSnapshotOverviewCertificateMessageTable(tag: Tag) extends Table[ConformanceSnapshotOverviewCertificateMessage](tag, "conformancesnapshotoverviewcertificatemessages") {
    def id = column[Long]("id")
    def message = column[String]("message")
    def messageType = column[Short]("message_type")
    def domainId = column[Option[Long]]("domain_id")
    def groupId = column[Option[Long]]("group_id")
    def specificationId = column[Option[Long]]("specification_id")
    def actorId = column[Option[Long]]("actor_id")
    def snapshotId = column[Long]("snapshot_id")
    def * = (id :: message :: messageType:: domainId :: groupId :: specificationId:: actorId :: snapshotId :: HNil).mapTo[ConformanceSnapshotOverviewCertificateMessage]
  }
  val conformanceSnapshotOverviewCertificateMessages = TableQuery[ConformanceSnapshotOverviewCertificateMessageTable]

  class ThemesTable(tag: Tag) extends Table[Theme](tag, "Themes") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def key = column[String] ("theme_key")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def active = column[Boolean]("active")
    def custom = column[Boolean]("custom")
    def separatorTitleColor = column[String] ("separator_title_color")
    def modalTitleColor = column[String] ("modal_title_color")
    def tableTitleColor = column[String] ("table_title_color")
    def cardTitleColor = column[String] ("card_title_color")
    def pageTitleColor = column[String] ("page_title_color")
    def headingColor = column[String] ("heading_color")
    def tabLinkColor = column[String] ("tab_link_color")
    def footerTextColor = column[String] ("footer_text_color")
    def headerBackgroundColor = column[String] ("header_background_color")
    def headerBorderColor = column[String] ("header_border_color")
    def headerSeparatorColor = column[String] ("header_separator_color")
    def headerLogoPath = column[String] ("header_logo_path")
    def footerBackgroundColor = column[String] ("footer_background_color")
    def footerBorderColor = column[String] ("footer_border_color")
    def footerLogoPath = column[String] ("footer_logo_path")
    def footerLogoDisplay = column[String] ("footer_logo_display")
    def faviconPath = column[String] ("favicon_path")
    def primaryButtonColor = column[String] ("primary_btn_color")
    def primaryButtonLabelColor = column[String] ("primary_btn_label_color")
    def primaryButtonHoverColor = column[String] ("primary_btn_hover_color")
    def primaryButtonActiveColor = column[String] ("primary_btn_active_color")
    def secondaryButtonColor = column[String] ("secondary_btn_color")
    def secondaryButtonLabelColor = column[String] ("secondary_btn_label_color")
    def secondaryButtonHoverColor = column[String] ("secondary_btn_hover_color")
    def secondaryButtonActiveColor = column[String] ("secondary_btn_active_color")
    def * = (id :: key :: description :: active :: custom :: separatorTitleColor :: modalTitleColor :: tableTitleColor :: cardTitleColor :: pageTitleColor :: headingColor :: tabLinkColor :: footerTextColor :: headerBackgroundColor :: headerBorderColor :: headerSeparatorColor :: headerLogoPath :: footerBackgroundColor :: footerBorderColor :: footerLogoPath :: footerLogoDisplay :: faviconPath :: primaryButtonColor :: primaryButtonLabelColor :: primaryButtonHoverColor :: primaryButtonActiveColor :: secondaryButtonColor :: secondaryButtonLabelColor :: secondaryButtonHoverColor :: secondaryButtonActiveColor :: HNil).mapTo[Theme]
  }
  val themes = TableQuery[ThemesTable]
  val insertTheme = themes returning themes.map(_.id)

  class ProcessedArchivesTable(tag: Tag) extends Table[ProcessedArchive](tag, "ProcessedArchives") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def hash = column[String] ("archive_hash")
    def importTime = column[Timestamp]("import_time", O.SqlType("TIMESTAMP"))
    def * = (id :: hash :: importTime :: HNil).mapTo[ProcessedArchive]
  }
  val processedArchives = TableQuery[ProcessedArchivesTable]
  val insertProcessedArchive = processedArchives returning processedArchives.map(_.id)

}
