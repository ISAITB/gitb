/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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

	protected def toLowercaseLikeParameter(value: Option[String]): Option[String] = {
		value.map(x => s"%${x.toLowerCase}%")
	}

}
