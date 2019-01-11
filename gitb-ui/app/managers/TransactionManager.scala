package managers

import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

object TransactionManager extends BaseManager {
  def logger = LoggerFactory.getLogger("TransactionManager")

  import dbConfig.driver.api._

  def deleteTransactionByDomain(domainId: Long) = {
    PersistenceSchema.transactions.filter(_.domain === domainId).delete
  }

}
