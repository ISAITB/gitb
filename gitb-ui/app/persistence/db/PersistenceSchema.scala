package persistence.db

import models._
import java.sql.Timestamp

import slick.jdbc.MySQLProfile.api._

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
    def selfregNotification = column[Boolean]("selfreg_notification")
    def domain = column[Option[Long]] ("domain")
    def * = (id, shortname, fullname, supportEmail, selfRegType, selfRegToken, selfregNotification, domain) <> (Communities.tupled, Communities.unapply)
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
    def community = column[Long] ("community")
    def * = (id, shortname, fullname, organizationType, adminOrganization, landingPage, legalNotice, errorTemplate, template, templateName, community) <> (Organizations.tupled, Organizations.unapply)
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
    //def fk = foreignKey("users_fk", organization, Organizations)(_.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    //def idx1 = index("users_idx_1", email, unique = true)
  }
  val users = TableQuery[UsersTable]
  val insertUser = users returning users.map(_.id)

  class SystemsTable(tag: Tag) extends Table[Systems](tag, "Systems") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def version = column[String]("version")
    def owner = column[Long]("owner")
    def * = (id, shortname, fullname, description, version, owner) <> (Systems.tupled, Systems.unapply)
    //def fk = foreignKey("systems_fk", owner, Organizations)(_.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val systems = TableQuery[SystemsTable]
  val insertSystem = systems returning systems.map(_.id)

  class DomainsTable(tag: Tag) extends Table[Domain](tag, "Domains") {
	  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
	def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def * = (id, shortname, fullname, description) <> (Domain.tupled, Domain.unapply)
  }
  val domains = TableQuery[DomainsTable]

  class SpecificationsTable(tag: Tag) extends Table[Specifications](tag, "Specifications") {
	def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def urls = column[Option[String]]("urls") //comma seperated
    def diagram = column[Option[String]]("diagram")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def stype = column[Short]("type")
    def hidden = column[Boolean]("is_hidden")
    def domain = column[Long]("domain")
    def * = (id, shortname, fullname, urls, diagram, description, stype, hidden, domain) <> (Specifications.tupled, Specifications.unapply)
    //def fk = foreignKey("specs_fk", domain, Domains)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val specifications = TableQuery[SpecificationsTable]

  class ActorsTable(tag: Tag) extends Table[Actors](tag, "Actors") {
    def id      = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def actorId = column[String]("actorId")
    def name    = column[String]("name")
    def desc    = column[Option[String]]("description", O.SqlType("TEXT"))
    def default = column[Option[Boolean]]("is_default")
    def hidden = column[Boolean]("is_hidden")
    def displayOrder = column[Option[Short]]("display_order")
    def domain  = column[Long]("domain")
    def * = (id, actorId, name, desc, default, hidden, displayOrder, domain) <> (Actors.tupled, Actors.unapply)
    def actorIdUniqueIdx = index("actors_aid_unq_idx", actorId, unique = true)
    //def fk = foreignKey("actors_fk", domain, Domains)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val actors = TableQuery[ActorsTable]

  class EndpointsTable(tag: Tag) extends Table[Endpoints](tag, "Endpoints") {
	  def id    = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name  = column[String]("name")
    def desc  = column[Option[String]]("description", O.SqlType("TEXT"))
    def actor = column[Long]("actor")
    def * = (id, name, desc, actor) <> (Endpoints.tupled, Endpoints.unapply)
    def endpointActorUniqueIdx = index("endp_act_unq_idx", (name, actor), unique = true)
    //def fk = foreignKey("endpoints_fk", actor, Actors)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val endpoints = TableQuery[EndpointsTable]

	class ParametersTable(tag: Tag) extends Table[models.Parameters] (tag, "Parameters") {
		def id    = column[Long]("id", O.PrimaryKey, O.AutoInc)
		def name  = column[String]("name")
		def desc  = column[Option[String]]("description", O.SqlType("TEXT"))
		def use   = column[String]("use")
		def kind  = column[String]("kind")
    def adminOnly = column[Boolean]("admin_only")
    def notForTests = column[Boolean]("not_for_tests")
		def endpoint = column[Long]("endpoint")

		def * = (id, name, desc, use, kind, adminOnly, notForTests, endpoint) <> (models.Parameters.tupled, models.Parameters.unapply)
	}
	val parameters = TableQuery[ParametersTable]

  class DomainParametersTable(tag: Tag) extends Table[models.DomainParameter] (tag, "DomainParameters") {
    def id    = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name  = column[String]("name")
    def desc  = column[Option[String]]("description", O.SqlType("TEXT"))
    def kind  = column[String]("kind")
    def value = column[Option[String]]("value", O.SqlType("MEDIUMBLOB"))
    def domain = column[Long]("domain")
    def * = (id, name, desc, kind, value, domain) <> (models.DomainParameter.tupled, models.DomainParameter.unapply)
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
    def testsession = column[Option[String]]("test_session_id")
    def * = (id, sut, spec, actor, testsuite, testcase, result, testsession) <> (models.ConformanceResult.tupled, models.ConformanceResult.unapply)
  }
  val conformanceResults = TableQuery[ConformanceResultsTable]

  class ConfigurationsTable(tag:Tag) extends Table[Configs] (tag, "Configurations") {
    def system = column[Long] ("system")
	  def parameter = column[Long]("parameter")
	  def endpoint = column[Long] ("endpoint")
	  def value = column[String]("value", O.SqlType("MEDIUMBLOB"))
    def * = (system, parameter, endpoint, value) <> (Configs.tupled, Configs.unapply)
    def pk = primaryKey("c_pk", (system, parameter, endpoint))
  }
  val configs = TableQuery[ConfigurationsTable]

  class TransactionsTable(tag: Tag) extends Table[Transaction](tag, "Transactions") {
    def shortname = column[String]("sname", O.PrimaryKey)
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def domain = column[Long]("domain")
    def * = (shortname, fullname, description, domain) <> (Transaction.tupled, Transaction.unapply)
    //def fk = foreignKey("tx_fk", shortname, Domains)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val transactions = TableQuery[TransactionsTable]

  class OptionsTable(tag: Tag) extends Table[Options](tag, "Options") {
	  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.SqlType("TEXT"))
    def actor = column[Long]("actor")
    def * = (id, shortname, fullname, description, actor) <> (Options.tupled, Options.unapply)
    //def fk = foreignKey("options_fk", shortname, Domains)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val options = TableQuery[OptionsTable]

  class TestCasesTable(tag: Tag) extends Table[TestCases](tag, "TestCases") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
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
	  def targetSpec = column[Long]("target_spec")
    def targetActors = column[Option[String]]("target_actors")
    def targetOptions = column[Option[String]]("target_options")
    def testSuiteOrder = column[Short]("testsuite_order")
	  def * = (id, shortname, fullname, version, authors, originalDate, modificationDate, description, keywords, testCaseType, path, targetSpec, targetActors, targetOptions, testSuiteOrder) <> (TestCases.tupled, TestCases.unapply)
    def shortNameVersionUniqueIdx = index("tc_sn_vsn_idx", (shortname, version), unique = true)
    //def fk1 = foreignKey("tc_fk_1", targetSpec, Specifications)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    //def fk2 = foreignKey("tc_fk_2", targetActor, Actors)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val testCases = TableQuery[TestCasesTable]

	class TestSuitesTable(tag: Tag) extends Table[TestSuites](tag, "TestSuites") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
		def shortname = column[String]("sname")
		def fullname = column[String]("fname")
		def version = column[String]("version")
		def authors = column[Option[String]]("authors")
		def originalDate = column[Option[String]]("original_date")
		def modificationDate = column[Option[String]]("modification_date")
		def description = column[Option[String]]("description", O.SqlType("TEXT"))
		def keywords = column[Option[String]]("keywords")
    def specification = column[Long]("specification")
    def filename = column[String]("file_name")
		def * = (id, shortname, fullname, version, authors, originalDate, modificationDate, description, keywords, specification, filename) <> (TestSuites.tupled, TestSuites.unapply)
    def shortNameVersionUniqueIdx = index("ts_sn_vsn_idx", (shortname, version), unique = true)
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
	  def tpl = column[String]("tpl", O.SqlType("BLOB"))
    def * = (testSessionId, sutId, sut, organizationId, organization, communityId, community, testCaseId, testCase, testSuiteId, testSuite, actorId, actor, specificationId, specification, domainId, domain, result, startTime, endTime, tpl) <> (TestResult.tupled, TestResult.unapply)
    //def fk1 = foreignKey("tr_fk_1", sutId, Systems)(_.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    //def fk2 = foreignKey("tr_fk_2", testcase, TestCases)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    //def fk3 = foreignKey("tr_fk_3", sutVersion, Systems)(_.version, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val testResults = TableQuery[TestResultsTable]

	class TestStepReports(tag: Tag) extends Table[TestStepResult](tag, "TestStepReports") {
		def testSessionId = column[String]("test_session_id")
		def testStepId = column[String]("test_step_id")
		def result = column[Short]("result")
		def reportPath = column[String]("report_path")

		def * = (testSessionId, testStepId, result, reportPath) <> (TestStepResult.tupled, TestStepResult.unapply)

		def pk = primaryKey("tsr_pk", (testSessionId, testStepId))
	}
	val testStepReports = TableQuery[TestStepReports]

  /*************************
   *** Relational Tables ***
   *************************/

  class SystemHasAdminsTable(tag: Tag) extends Table[(Long, Long)](tag, "SystemHasAdmins") {
    def systemId = column[Long]("sut_id")
    def userId = column[Long]("user_id")
    def * = (systemId, userId)
    def pk = primaryKey("sha1_pk", (systemId, userId))
    //def fk1 = foreignKey("sha_fk_1", systemId, Systems)(_.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    //def fk2 = foreignKey("sha_fk_2", systemId, Users)(_.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
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
    //def fk1 = foreignKey("shac_fk_1", specName, Specifications)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    //def fk2 = foreignKey("shac_fk_2", actorName, Actors)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val specificationHasActors = TableQuery[SpecificationHasActorsTable]

  class EndpointSupportsTransactionsTable(tag: Tag) extends Table[(Long, String, String)](tag, "EndpointSupportsTransactions") {
    def actorId = column[Long]("actor")
    def endpoint = column[String]("endpoint")
    def transaction = column[String]("transaction")
    def * = (actorId, endpoint, transaction)
    def pk = primaryKey("est_pk", (actorId, endpoint, transaction))
    //def fk1 = foreignKey("est_fk_1", actor, Endpoints)(_.actor, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    //def fk2 = foreignKey("est_fk_2", endpoint, Endpoints)(_.name, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    //def fk3 = foreignKey("est_fk_3", transaction, Transactions)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
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
    //def fk1 = foreignKey("tco_fk_1", testcase, TestCases)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    //def fk2 = foreignKey("tco_fk_2", option, Options)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
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
    def parameter = column[Option[String]]("parameter")
    def description = column[Option[String]]("description")
    def * = (name, parameter, description) <> (SystemConfigurations.tupled, SystemConfigurations.unapply)
  }
  val systemConfigurations = TableQuery[SystemConfigurationsTable]
  val insertSystemConfiguration = systemConfigurations returning systemConfigurations.map(_.name)

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

  class ConformanceCertificatesTable(tag: Tag) extends Table[ConformanceCertificates](tag, "ConformanceCertificates") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[Option[String]]("title", O.SqlType("TEXT"))
    def message = column[Option[String]]("message", O.SqlType("TEXT"))
    def includeMessage = column[Boolean]("include_message")
    def includeTestStatus = column[Boolean]("include_test_status")
    def includeTestCases = column[Boolean]("include_test_cases")
    def includeDetails = column[Boolean]("include_details")
    def includeSignature = column[Boolean]("include_signature")
    def keystoreFile = column[Option[String]]("keystore_file", O.SqlType("MEDIUMBLOB"))
    def keystoreType = column[Option[String]]("keystore_type", O.SqlType("TEXT"))
    def keystorePassword = column[Option[String]]("keystore_pass", O.SqlType("TEXT"))
    def keyPassword = column[Option[String]]("key_pass", O.SqlType("TEXT"))
    def community = column[Long]("community")
    def * = (id, title, message, includeMessage, includeTestStatus, includeTestCases, includeDetails, includeSignature, keystoreFile, keystoreType, keystorePassword, keyPassword, community) <> (ConformanceCertificates.tupled, ConformanceCertificates.unapply)
  }
  val conformanceCertificates = TableQuery[ConformanceCertificatesTable]
  val insertConformanceCertificate = conformanceCertificates returning conformanceCertificates.map(_.id)

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
    def community = column[Long]("community")
    def * = (id, name, testKey, description, use, kind, adminOnly, notForTests, inExports, community) <> (OrganisationParameters.tupled, OrganisationParameters.unapply)
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
    def community = column[Long]("community")
    def * = (id, name, testKey, description, use, kind, adminOnly, notForTests, inExports, community) <> (SystemParameters.tupled, SystemParameters.unapply)
  }
  val systemParameters = TableQuery[SystemParametersTable]
  val insertSystemParameters = systemParameters returning systemParameters.map(_.id)

  class OrganisationParameterValuesTable(tag: Tag) extends Table[OrganisationParameterValues](tag, "OrganisationParameterValues") {
    def organisation = column[Long] ("organisation")
    def parameter = column[Long]("parameter")
    def value = column[String]("value", O.SqlType("MEDIUMBLOB"))
    def * = (organisation, parameter, value) <> (OrganisationParameterValues.tupled, OrganisationParameterValues.unapply)
    def pk = primaryKey("opv_pk", (organisation, parameter))
  }
  val organisationParameterValues = TableQuery[OrganisationParameterValuesTable]

  class SystemParameterValuesTable(tag: Tag) extends Table[SystemParameterValues](tag, "SystemParameterValues") {
    def system = column[Long] ("system")
    def parameter = column[Long]("parameter")
    def value = column[String]("value", O.SqlType("MEDIUMBLOB"))
    def * = (system, parameter, value) <> (SystemParameterValues.tupled, SystemParameterValues.unapply)
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

}
