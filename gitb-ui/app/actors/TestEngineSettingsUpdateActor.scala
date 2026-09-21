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

import actors.TestEngineSettingsUpdateActor.{Attempt, PushSettings, Result, logger}
import managers.TestbedBackendClient
import models.TypedActorConfiguration
import org.apache.pekko.actor.{Actor, Cancellable}
import org.slf4j.LoggerFactory

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object TestEngineSettingsUpdateActor {
  val actorName = "test-engine-settings-update-actor"
  private val logger = LoggerFactory.getLogger(classOf[TestEngineSettingsUpdateActor])
  private val maxAttempts = 10
  private val retryDelay = 10.seconds

  /** External trigger requesting that the provided settings be pushed to the test engine. */
  case class PushSettings(config: TypedActorConfiguration)

  /** Internal message used to (re)trigger a push attempt. */
  private case class Attempt(config: TypedActorConfiguration, attemptNo: Int, epoch: Long)

  /** Internal message reporting the outcome of a push attempt. */
  private case class Result(config: TypedActorConfiguration, success: Boolean, attemptNo: Int, epoch: Long)
}

/**
 * Actor used to propagate global setting updates to the test engine (gitb-srv), retrying in case the test engine
 * is temporarily unreachable (e.g. not yet started or restarting).
 * <br>
 * Only one settings push is ever considered current - if a new push is requested while a retry is pending, the
 * pending retry is cancelled and superseded (tracked using an increasing epoch counter).
 */
class TestEngineSettingsUpdateActor @Inject() (testbedBackendClient: TestbedBackendClient) extends Actor {

  implicit private val ec: ExecutionContext = context.dispatcher

  private var epoch: Long = 0L
  private var scheduled: Option[Cancellable] = None

  override def receive: Receive = {
    case PushSettings(config) =>
      epoch += 1
      scheduled.foreach(_.cancel())
      scheduled = None
      attempt(config, 1, epoch)
    case Attempt(config, attemptNo, msgEpoch) =>
      if (msgEpoch == epoch) {
        attempt(config, attemptNo, msgEpoch)
      }
    case Result(config, success, attemptNo, msgEpoch) =>
      if (msgEpoch == epoch) {
        handleResult(config, success, attemptNo, msgEpoch)
      }
  }

  private def attempt(config: TypedActorConfiguration, attemptNo: Int, msgEpoch: Long): Unit = {
    val selfRef = self
    testbedBackendClient.updateSettings(config).onComplete {
      case Success(_) => selfRef ! Result(config, success = true, attemptNo, msgEpoch)
      case Failure(_) => selfRef ! Result(config, success = false, attemptNo, msgEpoch)
    }
  }

  private def handleResult(config: TypedActorConfiguration, success: Boolean, attemptNo: Int, msgEpoch: Long): Unit = {
    if (success) {
      if (attemptNo == 1) {
        logger.info("Test engine callback settings successfully updated in test engine.")
      } else {
        logger.info("Test engine callback settings successfully updated in test engine after {} attempt(s).", attemptNo)
      }
    } else if (attemptNo >= TestEngineSettingsUpdateActor.maxAttempts) {
      logger.warn("Test engine callback settings failed to be updated in test engine after {} attempt(s).", attemptNo)
    } else {
      scheduled = Some(context.system.scheduler.scheduleOnce(
        TestEngineSettingsUpdateActor.retryDelay, self, Attempt(config, attemptNo + 1, msgEpoch)
      ))
    }
  }

}
