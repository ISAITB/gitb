package managers

import scala.slick.driver.MySQLDriver.simple._
import play.api.libs.concurrent.Execution.Implicits._

import models._
import persistence.db.PersistenceSchema
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
 * Created by VWYNGAET on 26/10/2016.
 */
object OrganizationManager extends BaseManager {
  def logger = LoggerFactory.getLogger("OrganizationManager")


  def getOrganizations(): Future[List[Organizations]] = {
    Future {
      DB.withSession { implicit session =>
        //1) Get all organizations
        val organizations = PersistenceSchema.organizations.list
        organizations
      }
    }
  }

}