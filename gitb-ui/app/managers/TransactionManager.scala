package managers

import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

import scala.slick.driver.MySQLDriver.simple._

object TransactionManager extends BaseManager {
  def logger = LoggerFactory.getLogger("TransactionManager")

  def deleteTransactionByDomain(domainId: Long)(implicit session: Session) = {
    PersistenceSchema.transactions.filter(_.domain === domainId).delete
  }

}
