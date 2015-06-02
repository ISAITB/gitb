package persistence.db

import models._
import java.sql.Blob
import scala.slick.driver.MySQLDriver.simple._

object PersistenceSchema {

  /**********************
   *** Primary Tables ***
   **********************/

  class OrganizationsTable(tag: Tag) extends Table[Organizations](tag, "Organizations") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def organizationType = column[Short]("type")
    def * = (id, shortname, fullname, organizationType) <> (Organizations.tupled, Organizations.unapply)
  }
  //get table name etc from organizations.baseTableRow
  val organizations = TableQuery[OrganizationsTable]
  //insert organizations by insertOrganization += organization. This is implemented to get auto-incremented ids
  val insertOrganization = (organizations returning organizations.map(_.id))

  class UsersTable(tag: Tag) extends Table[Users](tag, "Users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def email = column[String]("email")
    def password = column[String]("password")
    def role = column[Short]("role")
    def organization = column[Long]("organization")
    def * = (id, name, email, password, role, organization) <> (Users.tupled, Users.unapply)
    //def fk = foreignKey("users_fk", organization, Organizations)(_.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    //def idx1 = index("users_idx_1", email, unique = true)
  }
  val users = TableQuery[UsersTable]
  val insertUser = (users returning users.map(_.id))

  class SystemsTable(tag: Tag) extends Table[Systems](tag, "Systems") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.Nullable, O.DBType("TEXT"))
    def version = column[String]("version")
    def owner = column[Long]("owner")
    def * = (id, shortname, fullname, description, version, owner) <> (Systems.tupled, Systems.unapply)
    //def fk = foreignKey("systems_fk", owner, Organizations)(_.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val systems = TableQuery[SystemsTable]
  val insertSystem = (systems returning systems.map(_.id))

  class DomainsTable(tag: Tag) extends Table[Domain](tag, "Domains") {
	  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
	def description = column[Option[String]]("description", O.Nullable, O.DBType("TEXT"))
    def * = (id, shortname, fullname, description) <> (Domain.tupled, Domain.unapply)
  }
  val domains = TableQuery[DomainsTable]

  class SpecificationsTable(tag: Tag) extends Table[Specifications](tag, "Specifications") {
	def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def urls = column[Option[String]]("urls") //comma seperated
    def diagram = column[Option[String]]("diagram")
    def description = column[Option[String]]("description", O.Nullable, O.DBType("TEXT"))
    def stype = column[Short]("type")
    def domain = column[Long]("domain")
    def * = (id, shortname, fullname, urls, diagram, description, stype, domain) <> (Specifications.tupled, Specifications.unapply)
    //def fk = foreignKey("specs_fk", domain, Domains)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val specifications = TableQuery[SpecificationsTable]

  class ActorsTable(tag: Tag) extends Table[Actors](tag, "Actors") {
    def id      = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def actorId = column[String]("actorId")
    def name    = column[String]("name")
    def desc    = column[Option[String]]("description", O.Nullable, O.DBType("TEXT"))
    def domain  = column[Long]("domain")
    def * = (id, actorId, name, desc, domain) <> (Actors.tupled, Actors.unapply)
    def actorIdUniqueIdx = index("actors_aid_unq_idx", actorId, unique = true)
    //def fk = foreignKey("actors_fk", domain, Domains)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val actors = TableQuery[ActorsTable]

  class EndpointsTable(tag: Tag) extends Table[Endpoints](tag, "Endpoints") {
	  def id    = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name  = column[String]("name")
    def desc  = column[Option[String]]("description", O.Nullable, O.DBType("TEXT"))
    def actor = column[Long]("actor")
    def * = (id, name, desc, actor) <> (Endpoints.tupled, Endpoints.unapply)
    def endpointActorUniqueIdx = index("endp_act_unq_idx", (name, actor), unique = true)
    //def fk = foreignKey("endpoints_fk", actor, Actors)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val endpoints = TableQuery[EndpointsTable]

	class ParametersTable(tag: Tag) extends Table[models.Parameters] (tag, "Parameters") {
		def id    = column[Long]("id", O.PrimaryKey, O.AutoInc)
		def name  = column[String]("name")
		def desc  = column[Option[String]]("description", O.Nullable, O.DBType("TEXT"))
		def use   = column[String]("use")
		def kind  = column[String]("kind")
		def endpoint = column[Long]("endpoint")

		def * = (id, name, desc, use, kind, endpoint) <> (models.Parameters.tupled, models.Parameters.unapply)
	}
	val parameters = TableQuery[ParametersTable]

  class ConfigurationsTable(tag:Tag) extends Table[Config] (tag, "Configurations") {
    def system = column[Long] ("system")
	  def parameter = column[Long]("parameter")
	  def endpoint = column[Long] ("endpoint")
	  def value = column[String]("value", O.DBType("BLOB"))
    def * = (system, parameter, endpoint, value) <> (Config.tupled, Config.unapply)
    def pk = primaryKey("c_pk", (system, parameter, endpoint))
  }
  val configs = TableQuery[ConfigurationsTable]

  class TransactionsTable(tag: Tag) extends Table[Transaction](tag, "Transactions") {
    def shortname = column[String]("sname", O.PrimaryKey)
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.Nullable, O.DBType("TEXT"))
    def domain = column[Long]("domain")
    def * = (shortname, fullname, description, domain) <> (Transaction.tupled, Transaction.unapply)
    //def fk = foreignKey("tx_fk", shortname, Domains)(_.shortname, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  val transactions = TableQuery[TransactionsTable]

  class OptionsTable(tag: Tag) extends Table[Options](tag, "Options") {
	  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def shortname = column[String]("sname")
    def fullname = column[String]("fname")
    def description = column[Option[String]]("description", O.Nullable, O.DBType("TEXT"))
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
    def description = column[Option[String]]("description", O.Nullable, O.DBType("TEXT"))
    def keywords = column[Option[String]]("keywords")
    def testCaseType = column[Short]("type")
	  def path = column[String]("path")
	  def targetSpec = column[Long]("target_spec")
    def targetActors = column[Option[String]]("target_actors")
    def targetOptions = column[Option[String]]("target_options")
	  def * = (id, shortname, fullname, version, authors, originalDate, modificationDate, description, keywords, testCaseType, path, targetSpec, targetActors, targetOptions) <> (TestCases.tupled, TestCases.unapply)
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
		def description = column[Option[String]]("description", O.Nullable, O.DBType("TEXT"))
		def keywords = column[Option[String]]("keywords")
    def specification = column[Long]("specification")
		def * = (id, shortname, fullname, version, authors, originalDate, modificationDate, description, keywords, specification) <> (TestSuites.tupled, TestSuites.unapply)
    def shortNameVersionUniqueIdx = index("ts_sn_vsn_idx", (shortname, version), unique = true)
	}
	val testSuites = TableQuery[TestSuitesTable]

  class TestResultsTable(tag: Tag) extends Table[TestResult](tag, "TestResults") {
    def testSessionId = column[String]("test_session_id", O.PrimaryKey)
	  def sutId = column[Long]("sut_id")
	  def actorId = column[Long]("actor_id")
	  def testcaseId = column[Long]("testcase_id")
	  def result = column[String]("result")
	  def startTime = column[String]("start_time")
	  def endTime = column[Option[String]]("end_time", O.Nullable)
	  def tpl = column[String]("tpl", O.DBType("BLOB"))
	  def sutVersion = column[Option[String]]("sut_version", O.Nullable) // TODO
    def * = (testSessionId, sutId, actorId, testcaseId, result, startTime, endTime, sutVersion, tpl) <> (TestResult.tupled, TestResult.unapply)
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

	class TestCaseHasActorsTable(tag: Tag) extends Table[(Long, Long, Long)](tag, "TestCaseHasActors") {
		def testcase = column[Long]("testcase")
    def specification = column[Long] ("specification")
		def actor = column[Long]("actor")

		def * = (testcase, specification, actor)

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
}
