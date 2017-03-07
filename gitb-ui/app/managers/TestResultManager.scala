package managers

import scala.slick.driver.MySQLDriver.simple._
import play.api.libs.concurrent.Execution.Implicits._

import models._
import persistence.db.PersistenceSchema
import org.slf4j.LoggerFactory

import scala.concurrent.Future

object TestResultManager extends BaseManager {
  def logger = LoggerFactory.getLogger("TestResultManager")

  /**
   * Gets all running test results
   */
  def getRunningTestResults: Future[List[TestResult]] = {
    Future {
      DB.withSession { implicit session =>
        val results = PersistenceSchema.testResults.filter(_.endTime.isEmpty).list
        results
      }
    }
  }

}
