package persistence.db

import java.sql._
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
