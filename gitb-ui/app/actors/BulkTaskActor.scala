/*
 * Copyright (C) 2026 European Union
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

package actors

import actors.BulkTaskActor.{TaskComplete, logger}
import actors.events.obsolete.{DeleteAllObsoleteSessions, DeleteObsoleteSessionsForCommunity, DeleteObsoleteSessionsForOrganisation}
import managers.TestResultManager
import org.apache.pekko.actor.{Actor, Stash}
import org.slf4j.LoggerFactory

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object BulkTaskActor {
  val actorName = "bulk-task-actor"
  private val logger = LoggerFactory.getLogger(classOf[BulkTaskActor])
  private case object TaskComplete
}

/*
 * Actor that executes bulk tasks one at a time.
 */
class BulkTaskActor @Inject() (testResultManager: TestResultManager) extends Actor with Stash {

  implicit private val ec: ExecutionContext = context.dispatcher

  private def idle: Receive = {
    case _: DeleteAllObsoleteSessions               => startTask(handleDeleteAllObsoleteSessions())
    case msg: DeleteObsoleteSessionsForCommunity    => startTask(handleDeleteObsoleteSessionsForCommunity(msg))
    case msg: DeleteObsoleteSessionsForOrganisation => startTask(handleDeleteObsoleteSessionsForOrganisation(msg))
    case msg => logger.warn("Unexpected event type received [{}]", msg.getClass.getName)
  }

  private def busy: Receive = {
    case TaskComplete =>
      // Un-stash all the pending messages now that we can process them
      unstashAll()
      context.become(idle)
    case _ => stash() // Park messages received while busy to make sure we process them one by one.
  }

  override def receive: Receive = idle

  private def startTask(task: Future[_]): Unit = {
    context.become(busy)
    task.onComplete {
      case Success(_) => self ! TaskComplete
      case Failure(exception) =>
        logger.error("Unexpected error while executing task", exception)
        self ! TaskComplete
    }
  }

  private def handleDeleteAllObsoleteSessions(): Future[Unit] = {
    testResultManager.deleteAllObsoleteTestResults().map { count =>
      logger.info("Deleted {} obsolete test sessions", count)
    }
  }

  private def handleDeleteObsoleteSessionsForCommunity(msg: DeleteObsoleteSessionsForCommunity): Future[Unit] = {
    testResultManager.deleteObsoleteTestResultsForCommunityWrapper(msg.communityId).map { count =>
      logger.info("Deleted {} obsolete test sessions for community {}", count, msg.communityId)
    }
  }

  private def handleDeleteObsoleteSessionsForOrganisation(msg: DeleteObsoleteSessionsForOrganisation): Future[Unit] = {
    testResultManager.deleteObsoleteTestResultsForOrganisationWrapper(msg.organisationId).map { count =>
      logger.info("Deleted {} obsolete test sessions for organisation {}", count, msg.organisationId)
    }
  }

}
