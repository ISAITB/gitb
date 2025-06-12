package managers

import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by serbay on 10/22/14.
 */
abstract class BaseManager @Inject() (dbConfigProvider: DatabaseConfigProvider)
																		 (implicit ec: ExecutionContext) {

	val dbConfig = dbConfigProvider.get[JdbcProfile]
	val DB: dbConfig.profile.backend.JdbcDatabaseDef = dbConfig.db

	protected def toDBIO(actions: Iterable[DBIO[_]]): DBIO[_] = {
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

	def extractFailureDetails(error: Throwable): List[String] = {
		val messages = new ListBuffer[Option[String]]()
		val handledErrors = new ListBuffer[Throwable]()
		extractFailureDetailsInternal(error, handledErrors, messages)
		messages.filter(_.isDefined).toList.map(_.get)
	}

	@tailrec
	private def extractFailureDetailsInternal(error: Throwable, handledErrors: ListBuffer[Throwable], messages: ListBuffer[Option[String]]): Unit = {
		if (error != null && !handledErrors.contains(error)) {
			handledErrors += error
			messages += Option(error.getMessage)
			extractFailureDetailsInternal(error.getCause, handledErrors, messages)
		}
	}

	protected def loadIfApplicable[T](applicable: Boolean, loader: () => Future[T]): Future[Option[T]] = {
		if (applicable) {
			loader.apply().map(Some(_))
		} else {
			Future.successful(None)
		}
	}


}
