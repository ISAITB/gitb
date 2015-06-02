package managers

import play.api.Play.current
import scala.slick.driver.MySQLDriver.simple._
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Created by serbay on 10/22/14.
 */
abstract class BaseManager {
	def DB = play.api.db.slick.DB
}
