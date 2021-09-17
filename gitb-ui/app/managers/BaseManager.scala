package managers

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by serbay on 10/22/14.
 */
abstract class BaseManager @Inject() (dbConfigProvider: DatabaseConfigProvider) {

	val dbConfig = dbConfigProvider.get[JdbcProfile]
	val DB = dbConfig.db

	final protected def exec[R](a: DBIO[R]): R = {
		Await.result(DB.run(a), Duration.Inf)
	}

	protected def toDBIO(actions: ListBuffer[DBIO[_]]): DBIO[_] = {
		if (actions.nonEmpty) {
			DBIO.seq(actions.toList.map(a => a): _*)
		} else {
			DBIO.successful(())
		}
	}

	protected def dbActionFinalisation[A](onSuccessCalls: Option[mutable.ListBuffer[() => _]], onFailureCalls: Option[mutable.ListBuffer[() => _]], dbAction: DBIO[A]): DBIO[A] = {
		dbAction.flatMap(result => {
			if (onSuccessCalls.isDefined) {
				onSuccessCalls.get.foreach { onSuccess =>
					onSuccess.apply()
				}
			}
			DBIO.successful(result)
		}).cleanUp(error => {
			if (error.isDefined) {
				if (onFailureCalls.isDefined) {
					// Cleanup operations in case an error occurred.
					onFailureCalls.get.foreach { onFailure =>
						onFailure.apply()
					}
				}
				DBIO.failed(error.get)
			} else {
				DBIO.successful(())
			}
		})
	}

}
