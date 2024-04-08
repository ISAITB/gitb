package persistence.db

import config.Configurations
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets
import java.sql._
import scala.collection.mutable.ListBuffer
import scala.util.Using

object PersistenceLayer {

  private def logger = LoggerFactory.getLogger("PersistenceLayer")

  /**
   * Creates database if not exists. This must be called before applications starts,
   * since creation method uses mysql-java-connector rather than Play!'s DB API
   * which expects an already created database.
   *
   * Methods that use direct SQL commands should be called here, as well.
   */
  def preInitialize(): Unit = {
    createDatabaseIfNotExists()
  }

  /**
   * Creates a database with a given name by using SQL commands.
   */
  private def createDatabaseIfNotExists(): Unit = {
    try {
      // Create database if not exists (connection using root URL)
      Using.resource(DriverManager.getConnection(Configurations.DB_ROOT_URL, Configurations.DB_USER, Configurations.DB_PASSWORD)) { connection =>
        Using.resource(connection.createStatement()) { statement =>
          statement.execute("CREATE DATABASE IF NOT EXISTS " + Configurations.DB_NAME + " DEFAULT CHARACTER SET UTF8")
        }
      }
      if (Configurations.DB_LATEST_DB_BASELINE_SCRIPT.isDefined) {
        // Run the latest baseline script to avoid running individual Flyway migrations (connect to gitb schema)
        Using.resource(DriverManager.getConnection(Configurations.DB_JDBC_URL, Configurations.DB_USER, Configurations.DB_PASSWORD)) { connection =>
          connection.setAutoCommit(true)
          var count = 0
          Using.resource(connection.prepareStatement("SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_schema) = LOWER(?) AND LOWER(table_name) = LOWER(?) LIMIT 1;")) { statement =>
            statement.setString(1, Configurations.DB_NAME)
            statement.setString(2, Configurations.DB_MIGRATION_TABLE)
            Using.resource(statement.executeQuery()) { resultSet =>
              while (resultSet.next()) {
                count = resultSet.getInt(1)
              }
            }
          }
          if (count == 0) {
            // There is no schema version table. Find the latest baseline script and apply it.
            logger.info("Initialising database using baseline script {}...", Configurations.DB_LATEST_DB_BASELINE_SCRIPT.get)
            // This is a very simple implementation to split the SQL file into statements. If the file includes ";" at any other point besides as a statement separator, this should be adapted.
            getBaseLineScriptStatements().foreach { statementContent =>
              Using.resource(connection.createStatement()) { statement =>
                statement.execute(statementContent)
              }
            }
            logger.info("Database initialised.")
          } else {
            logger.info("Database already initialised.")
          }
        }
      }
    } catch {
      case e: Exception  =>
        logger.error("DB initialisation error", e)
        throw new IllegalStateException(e)
    }
  }

  private def getBaseLineScriptStatements(): List[String] = {
    val statements = new ListBuffer[String]()
    Using.resource(Thread.currentThread().getContextClassLoader.getResourceAsStream("db/baseline/"+Configurations.DB_LATEST_DB_BASELINE_SCRIPT.get)) { stream =>
      val statementBuffer = new StringBuilder();
      StringUtils.split(new String(stream.readAllBytes(), StandardCharsets.UTF_8), "\n").map(_.trim).foreach { line =>
        if (!line.isBlank && !line.startsWith("--")) {
          statementBuffer.append(line)
          if (line.endsWith(";")) {
            statements += StringUtils.normalizeSpace(statementBuffer.toString())
            statementBuffer.delete(0, statementBuffer.length())
          }
        }
      }
    }
    statements.toList
  }

}
