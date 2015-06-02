package persistence.db

import scala.slick.driver.MySQLDriver.simple._
import play.api.db.slick._
import play.api.Play.current

import java.sql._
import scala.slick.jdbc.meta.MTable
import config.Configurations
import org.slf4j.LoggerFactory

object PersistenceLayer {
  def logger = LoggerFactory.getLogger("PersistenceLayer")

  /**
   * Creates database if not exists. This must be called before applications starts,
   * since creation method uses mysql-java-connector rather than Play!'s DB API
   * which expects an already created database.
   *
   * Methods that use direct SQL commands should be called here, as well.
   */
  def preInitialize() = {
    createDatabaseIfNotExists(Configurations.DB_NAME)
  }

  /**
   * Creates database tables if not exist. This must be called after application starts,
   * since it depends on Play!'s DB API which reads configurations like db.default.* and
   * configures DB connection when the application is being started.
   *
   * Methods that use Play!'s DB API should be called here.
   */
  def initialize() = {
    createTablesIfNotExist()
  }

  /**
   * Creates database tables after connecting to it
   */
  def createTablesIfNotExist():Unit = {
    DB withSession { implicit session =>
      val dbTableList = MTable.getTables.list

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.organizations.baseTableRow.tableName))){
        PersistenceSchema.organizations.ddl.create
        logger.info("Organizations table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.users.baseTableRow.tableName))){
        PersistenceSchema.users.ddl.create
        logger.info("Users table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.systems.baseTableRow.tableName))){
        PersistenceSchema.systems.ddl.create
        logger.info("Systems table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.domains.baseTableRow.tableName))){
        PersistenceSchema.domains.ddl.create
        logger.info("Domains table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.specifications.baseTableRow.tableName))){
        PersistenceSchema.specifications.ddl.create
        logger.info("Specifications table craeted...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.actors.baseTableRow.tableName))){
        PersistenceSchema.actors.ddl.create
        logger.info("Actors table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.endpoints.baseTableRow.tableName))){
        PersistenceSchema.endpoints.ddl.create
        logger.info("Endpoints table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.configs.baseTableRow.tableName))){
        PersistenceSchema.configs.ddl.create
        logger.info("Configurations table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.parameters.baseTableRow.tableName))){
        PersistenceSchema.parameters.ddl.create
        logger.info("Parameters table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.transactions.baseTableRow.tableName))){
        PersistenceSchema.transactions.ddl.create
        logger.info("Transactions table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.options.baseTableRow.tableName))){
        PersistenceSchema.options.ddl.create
        logger.info("Options table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.testCases.baseTableRow.tableName))){
        PersistenceSchema.testCases.ddl.create
        logger.info("TestCases table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.testSuites.baseTableRow.tableName))){
        PersistenceSchema.testSuites.ddl.create
        logger.info("TestSuites table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.testResults.baseTableRow.tableName))){
        PersistenceSchema.testResults.ddl.create
        logger.info("TestResults table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.testStepReports.baseTableRow.tableName))){
        PersistenceSchema.testStepReports.ddl.create
        logger.info("TestStepResults table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.systemHasAdmins.baseTableRow.tableName))){
        PersistenceSchema.systemHasAdmins.ddl.create
        logger.info("SystemHasAdmins table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.systemImplementsActors.baseTableRow.tableName))){
        PersistenceSchema.systemImplementsActors.ddl.create
        logger.info("SystemImplementsActors table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.systemImplementsOptions.baseTableRow.tableName))){
        PersistenceSchema.systemImplementsOptions.ddl.create
        logger.info("SystemImplementsOptions table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.specificationHasActors.baseTableRow.tableName))){
        PersistenceSchema.specificationHasActors.ddl.create
        logger.info("SpecificationHasActors table created...")
      }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.endpointSupportsTransactions.baseTableRow.tableName))){
        PersistenceSchema.endpointSupportsTransactions.ddl.create
        logger.info("EndpointSupportsTransactions table created...")
      }

	    if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.testCaseHasActors.baseTableRow.tableName))) {
		    PersistenceSchema.testCaseHasActors.ddl.create
		    logger.info("TestCaseHasActors table created...")
	    }

	    if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.testSuiteHasActors.baseTableRow.tableName))) {
		    PersistenceSchema.testSuiteHasActors.ddl.create
		    logger.info("TestSuiteHasActors table created...")
	    }

	    if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.testSuiteHasTestCases.baseTableRow.tableName))) {
		    PersistenceSchema.testSuiteHasTestCases.ddl.create
		    logger.info("TestSuiteHasTestCases table created...")
	    }

      if(!dbTableList.exists(_.name.name.equalsIgnoreCase(PersistenceSchema.testCaseCoversOptions.baseTableRow.tableName))){
        PersistenceSchema.testCaseCoversOptions.ddl.create
        logger.info("TestCaseCoversOptions table created...")
      }
    }
  }

  /**
   * Creates a database with a given name by using SQL commands.
   * Can be used for creating test databases, as well.
   */
  def createDatabaseIfNotExists(dbName:String):Unit = {
    try{
      Class.forName(Configurations.DB_DRIVER_CLASS)
      val connection = DriverManager.getConnection(Configurations.DB_ROOT_URL, Configurations.DB_USER, Configurations.DB_PASSWORD)
      val statement = connection.createStatement()
      val create = "CREATE DATABASE IF NOT EXISTS " + dbName + " DEFAULT CHARACTER SET UTF8"
      statement.execute(create)
      statement.close()
      connection.close()
    }catch{
      case e: Exception  => {
        logger.info("DBError: " + e.getMessage)
      }
    }
  }

  /**
   * Drops a database with a given name by using SQL commands.
   * "Should" be used for removing "test" databases!
   */
  def dropDatabaseIfExists(dbName:String):Unit = {
    try{
      Class.forName(Configurations.DB_DRIVER_CLASS)
      val connection = DriverManager.getConnection(Configurations.DB_ROOT_URL, Configurations.DB_USER, Configurations.DB_PASSWORD)
      val statement = connection.createStatement()
      val drop = "DROP DATABASE IF EXISTS " + dbName
      statement.execute(drop)
      statement.close()
      connection.close()
    }catch{
      case e: Exception  => {
        logger.info("DBError: " + e.getMessage)
      }
    }
  }
}
