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

package managers

import config.Configurations
import exceptions.UnauthorizedAccessException
import models.Enums.UserRole
import models.UserTrackingInfo
import org.apache.pekko.{Done, NotUsed}
import org.apache.pekko.actor.{ActorSystem, CoordinatedShutdown}
import org.apache.pekko.pattern.after
import org.apache.pekko.stream.scaladsl.{Source, SourceQueueWithComplete}
import org.apache.pekko.stream.{Materializer, OverflowStrategy}
import org.apache.pekko.util.ByteString
import org.slf4j.LoggerFactory
import persistence.cache.TokenCache
import play.api.libs.json.{JsObject, Json}

import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.{Inject, Singleton}
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object ServerEventManager {

  // The names of the events sent over a server event channel.
  private val EVENT_CONNECTED = "connected"
  private val EVENT_CLOSED = "closed"
  private val EVENT_SESSION = "session"
  private val EVENT_HEARTBEAT = "heartbeat"
  private val EVENT_CONFIGURATION = "configuration"

  // Reasons for which a channel is closed by the server.
  val CLOSE_REASON_LOGOUT = "logout"
  private val CLOSE_REASON_EXPIRED = "expired"

  private val REPLAY_BUFFER_SIZE = 500
  private val QUEUE_SIZE = 512
  private val MAINTENANCE_INTERVAL = 20.seconds
  private val RECONNECT_GRACE_PERIOD_MS = 30000L
  private val RETRY_INTERVAL_MS = 3000

  /**
   * A server event channel, established by a user's browser tab, over which events are pushed to it.
   *
   * A channel outlives the underlying HTTP connection for a short grace period, to allow the browser's automatic
   * reconnection to resume it (via the Last-Event-ID header) without losing events.
   */
  private[managers] class Channel(val id: String, val userId: Long, val accessToken: String,
                                  var organisationId: Long, var communityId: Long, var role: Short) {
    var seq: Long = 0
    val replayBuffer: mutable.ArrayDeque[(Long, String)] = mutable.ArrayDeque[(Long, String)]()
    val testSessions: mutable.Set[String] = mutable.Set[String]()
    var queue: Option[SourceQueueWithComplete[String]] = None
    var bindingId: Long = 0
    var disconnectedSince: Option[Long] = None
  }

  /**
   * Format an event according to the SSE wire format.
   */
  private def formatEvent(id: Option[String], name: String, data: String, retry: Option[Int] = None): String = {
    val builder = new StringBuilder()
    retry.foreach(x => builder.append("retry: ").append(x).append('\n'))
    id.foreach(x => builder.append("id: ").append(x).append('\n'))
    builder.append("event: ").append(name).append('\n')
    // Multi-line data must be sent as multiple data fields.
    data.linesIterator.foreach(line => builder.append("data: ").append(line).append('\n'))
    builder.append('\n')
    builder.toString()
  }

  // The heartbeat is a proper event (not a comment) so that clients can detect a silently dropped connection. It carries
  // no ID, and is therefore never replayed.
  private val HEARTBEAT_FRAME = formatEvent(None, EVENT_HEARTBEAT, "ping")

}

/**
 * Manages the server-sent event (SSE) channels opened by users' browsers, and provides the means to push events to them.
 *
 * Each channel is linked to the user that opened it as well as the user's organisation, community and role, so that
 * events can be targeted at a specific user, an organisation, a community or a set of administrators.
 *
 * This class also tracks which test sessions are being followed through which channels, and which test sessions are
 * active (i.e. headless sessions that have no open channel).
 */
@Singleton
class ServerEventManager @Inject() (actorSystem: ActorSystem,
                                    userManager: UserManager,
                                    testbedClient: TestbedBackendClient)
                                   (implicit ec: ExecutionContext, mat: Materializer) {

  import ServerEventManager._

  private final val logger = LoggerFactory.getLogger(classOf[ServerEventManager])

  private val channels = TrieMap[String, Channel]()
  private val testSessionChannels = TrieMap[String, Set[String]]() // [test session ID -> channel IDs]
  private val activeTestSessions = TrieMap[String, Boolean]()
  private val bindingCounter = new AtomicLong(0) // Distinguishes the successive connections of one channel.

  private val maintenanceTask = actorSystem.scheduler.scheduleWithFixedDelay(MAINTENANCE_INTERVAL, MAINTENANCE_INTERVAL)(() => performMaintenance())

  // End all open streams before the server stops accepting connections (they would otherwise delay shutdown). No close
  // is signalled to clients so that their browsers reconnect once the server is back.
  CoordinatedShutdown(actorSystem).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "complete-server-event-channels") { () =>
    maintenanceTask.cancel()
    channels.values.foreach { channel =>
      channel.synchronized {
        channel.queue.foreach(_.complete())
        channel.queue = None
      }
    }
    Future.successful(Done)
  }

  /*
   * ----- Channel lifecycle -----
   */

  /**
   * Open (or resume) a channel for the given user.
   *
   * @param lastEventId The value of the Last-Event-ID request header (if provided by the browser when reconnecting).
   * @return The stream of raw SSE data to return to the client.
   */
  def openChannel(userId: Long, accessToken: String, lastEventId: Option[String]): Future[Source[ByteString, NotUsed]] = {
    userManager.getUserTrackingInfoById(userId).map { user =>
      if (user.isEmpty) {
        throw UnauthorizedAccessException("User not found")
      }
      bindChannel(user.get, accessToken, lastEventId)
    }
  }

  private def parseLastEventId(lastEventId: Option[String]): Option[(String, Long)] = {
    lastEventId.flatMap { value =>
      val index = value.lastIndexOf(':')
      if (index > 0) {
        value.substring(index + 1).toLongOption.map(seq => (value.substring(0, index), seq))
      } else {
        None
      }
    }
  }

  /**
   * The configuration values sent to clients when a channel is (re)established, so that they can catch up on
   * changes that may have occurred while disconnected (e.g. across a server restart). This is currently limited to
   * the shutdown preparation flag; extend it here as further configuration values need to be kept in sync this way.
   */
  private def currentConfiguration(): JsObject = {
    Json.obj("preparingForShutdown" -> Configurations.PREPARE_FOR_SHUTDOWN)
  }

  private def bindChannel(user: UserTrackingInfo, accessToken: String, lastEventId: Option[String]): Source[ByteString, NotUsed] = {
    val (queue, source) = Source.queue[String](QUEUE_SIZE, OverflowStrategy.dropHead).preMaterialize()
    val bindingId = bindingCounter.incrementAndGet()
    // Try to resume an existing channel (only if it is the user's own and no events were lost).
    val resumed = parseLastEventId(lastEventId).flatMap { case (channelId, lastSeq) =>
      channels.get(channelId).filter(_.userId == user.id).flatMap { channel =>
        channel.synchronized {
          val oldestBuffered = channel.replayBuffer.headOption.map(_._1).getOrElse(channel.seq + 1)
          if (lastSeq <= channel.seq && oldestBuffered <= lastSeq + 1) {
            // Release any previous (stale) connection.
            channel.queue.foreach(_.complete())
            channel.queue = Some(queue)
            channel.bindingId = bindingId
            channel.disconnectedSince = None
            offer(queue, formatEvent(None, EVENT_CONNECTED, Json.obj("channelId" -> channel.id, "resumed" -> true, "configuration" -> currentConfiguration()).toString(), Some(RETRY_INTERVAL_MS)))
            channel.replayBuffer.filter(_._1 > lastSeq).foreach(event => offer(queue, event._2))
            Some(channel)
          } else {
            None
          }
        }
      }
    }
    val channel = resumed.getOrElse {
      // Drop the previous channel if we could not resume it.
      parseLastEventId(lastEventId).foreach { case (channelId, _) =>
        channels.get(channelId).filter(_.userId == user.id).foreach(removeChannel(_, notifyClient = false))
      }
      val newChannel = new Channel(UUID.randomUUID().toString, user.id, accessToken, user.organisationId, user.communityId, user.role)
      newChannel.queue = Some(queue)
      newChannel.bindingId = bindingId
      channels.put(newChannel.id, newChannel)
      // The connected event is sent with sequence 0 so that a reconnection before any other event still resumes the channel.
      offer(queue, formatEvent(Some(s"${newChannel.id}:0"), EVENT_CONNECTED, Json.obj("channelId" -> newChannel.id, "resumed" -> false, "configuration" -> currentConfiguration()).toString(), Some(RETRY_INTERVAL_MS)))
      newChannel
    }
    if (logger.isDebugEnabled) logger.debug("Server event channel [{}] bound for user [{}] (resumed: {})", channel.id, user.id, resumed.isDefined)
    source
      .map(frame => ByteString.fromString(frame, StandardCharsets.UTF_8))
      .watchTermination() { (_, done) =>
        done.onComplete(_ => streamClosed(channel, bindingId))
        NotUsed
      }
  }

  private def streamClosed(channel: Channel, bindingId: Long): Unit = {
    channel.synchronized {
      // Ignore the closing of stale connections that were already replaced.
      if (channel.bindingId == bindingId && channels.contains(channel.id)) {
        channel.queue = None
        channel.disconnectedSince = Some(System.currentTimeMillis())
        if (logger.isDebugEnabled) logger.debug("Server event channel [{}] disconnected", channel.id)
      }
    }
  }

  private def offer(queue: SourceQueueWithComplete[String], frame: String): Unit = {
    // The queue is bounded with the oldest events dropped when full. Failures mean that the stream is already closed.
    queue.offer(frame)
  }

  /**
   * Remove the channel (immediately). Test sessions that are no longer followed by any channel are signalled to the test engine.
   */
  private def removeChannel(channel: Channel, notifyClient: Boolean, closeReason: String = CLOSE_REASON_LOGOUT): Unit = {
    val testSessions = channel.synchronized {
      if (notifyClient) {
        channel.queue.foreach(queue => offer(queue, formatEvent(None, EVENT_CLOSED, Json.obj("reason" -> closeReason).toString())))
      }
      channel.queue.foreach(_.complete())
      channel.queue = None
      channels.remove(channel.id)
      val sessions = channel.testSessions.toList
      channel.testSessions.clear()
      sessions
    }
    testSessions.foreach(unregisterChannelForTestSession(_, channel.id))
    if (logger.isDebugEnabled) logger.debug("Removed server event channel [{}]", channel.id)
  }

  private def performMaintenance(): Unit = {
    try {
      val now = System.currentTimeMillis()
      channels.values.foreach { channel =>
        val (connected, disconnectedSince, followsTestSessions) = channel.synchronized {
          (channel.queue.isDefined, channel.disconnectedSince, channel.testSessions.nonEmpty)
        }
        if (connected) {
          /*
           * Do not extend the user's session as a result of this check. Channels that follow a test session are
           * not closed for inactivity as the user is actively (if passively) using the application.
           */
          if (!followsTestSessions && !TokenCache.isAccessTokenValid(channel.accessToken)) {
            removeChannel(channel, notifyClient = true, CLOSE_REASON_EXPIRED)
          } else {
            channel.synchronized {
              channel.queue.foreach(offer(_, HEARTBEAT_FRAME))
            }
          }
        } else if (disconnectedSince.exists(now - _ > RECONNECT_GRACE_PERIOD_MS)) {
          removeChannel(channel, notifyClient = false)
        }
      }
    } catch {
      case e: Exception => logger.warn("Unexpected error during server event maintenance", e)
    }
  }

  /**
   * Close all channels linked to the given access token (e.g. on logout).
   */
  def closeChannelsForToken(accessToken: String, reason: String): Unit = {
    channels.values.filter(_.accessToken == accessToken).foreach(removeChannel(_, notifyClient = true, reason))
  }

  /**
   * Close all channels of the given user.
   */
  def closeChannelsForUser(userId: Long, reason: String): Unit = {
    channels.values.filter(_.userId == userId).foreach(removeChannel(_, notifyClient = true, reason))
  }

  /**
   * Refresh the organisation, community and role information of the user's open channels (e.g. after an account
   * update). Channels for users that no longer exist are closed.
   */
  def refreshChannelsForUser(userId: Long): Future[Unit] = {
    userManager.getUserTrackingInfoById(userId).map { user =>
      channels.values.filter(_.userId == userId).foreach { channel =>
        if (user.isDefined) {
          channel.synchronized {
            channel.organisationId = user.get.organisationId
            channel.communityId = user.get.communityId
            channel.role = user.get.role
          }
        } else {
          removeChannel(channel, notifyClient = true, CLOSE_REASON_EXPIRED)
        }
      }
    }
  }

  def channelBelongsToUser(channelId: String, userId: Long): Boolean = {
    channels.get(channelId).exists(_.userId == userId)
  }

  /*
   * ----- Sending events -----
   */

  private def emit(channel: Channel, name: String, data: String): Unit = {
    channel.synchronized {
      channel.seq += 1
      val frame = formatEvent(Some(s"${channel.id}:${channel.seq}"), name, data)
      channel.replayBuffer.append((channel.seq, frame))
      while (channel.replayBuffer.size > REPLAY_BUFFER_SIZE) {
        channel.replayBuffer.removeHead()
      }
      channel.queue.foreach(offer(_, frame))
    }
  }

  /**
   * Send an event to all channels matching the given filter.
   *
   * @return The number of channels the event was sent to.
   */
  def send(filter: (Long, Long, Long, Short) => Boolean, name: String, data: String): Int = {
    var count = 0
    channels.values.foreach { channel =>
      if (filter(channel.userId, channel.organisationId, channel.communityId, channel.role)) {
        emit(channel, name, data)
        count += 1
      }
    }
    count
  }

  def sendToChannel(channelId: String, name: String, data: String): Boolean = {
    channels.get(channelId).map(emit(_, name, data)).isDefined
  }

  def sendToUser(userId: Long, name: String, data: String): Int = {
    send((user, _, _, _) => user == userId, name, data)
  }

  def sendToOrganisation(organisationId: Long, name: String, data: String): Int = {
    send((_, organisation, _, _) => organisation == organisationId, name, data)
  }

  /**
   * Send to the users of a community, optionally limited to specific roles.
   */
  def sendToCommunity(communityId: Long, name: String, data: String, roles: Set[Short] = Set.empty): Int = {
    send((_, _, community, role) => community == communityId && (roles.isEmpty || roles.contains(role)), name, data)
  }

  def sendToTestBedAdministrators(name: String, data: String): Int = {
    send((_, _, _, role) => role == UserRole.SystemAdmin.id.toShort, name, data)
  }

  def sendToAll(name: String, data: String): Int = {
    send((_, _, _, _) => true, name, data)
  }

  /**
   * Push updated configuration values (a partial configuration object, using the same keys as [[currentConfiguration]])
   * to all connected channels.
   */
  def sendConfigurationUpdate(update: JsObject): Int = {
    sendToAll(EVENT_CONFIGURATION, update.toString())
  }

  /*
   * ----- Test sessions -----
   */

  /**
   * Have the given channel follow the given test session.
   */
  def subscribeToTestSession(channelId: String, sessionId: String): Boolean = {
    channels.get(channelId).exists { channel =>
      testSessionChannels.synchronized {
        val existingChannelIds = testSessionChannels.getOrElse(sessionId, Set.empty)
        // A test session can only be followed by the channels of a single user (the one that initiated it).
        if (existingChannelIds.exists(id => channels.get(id).exists(_.userId != channel.userId))) {
          false
        } else {
          channel.synchronized {
            channel.testSessions.add(sessionId)
          }
          testSessionChannels.put(sessionId, existingChannelIds + channelId)
          registerActiveTestSession(sessionId)
          true
        }
      }
    }
  }

  /**
   * Stop following the given test session through the given channel.
   */
  def unsubscribeFromTestSession(channelId: String, sessionId: String): Unit = {
    channels.get(channelId).foreach { channel =>
      channel.synchronized {
        channel.testSessions.remove(sessionId)
      }
    }
    unregisterChannelForTestSession(sessionId, channelId)
  }

  private def unregisterChannelForTestSession(sessionId: String, channelId: String): Unit = {
    val lastChannel = testSessionChannels.synchronized {
      val remaining = testSessionChannels.getOrElse(sessionId, Set.empty) - channelId
      if (remaining.isEmpty) {
        testSessionChannels.remove(sessionId)
        true
      } else {
        testSessionChannels.put(sessionId, remaining)
        false
      }
    }
    if (lastChannel) {
      // Ping the test engine - this is needed for cleanup in case a test session has not started yet.
      pingTestEngineForClosedConnection(sessionId)
    }
  }

  private def pingTestEngineForClosedConnection(sessionId: String): Future[Unit] = {
    testbedClient.stop("CONNECTION_CLOSED|" + sessionId).recover {
      case e: Exception => logger.warn("Unable to signal closed connection for test session [" + sessionId + "]", e)
    }
  }

  /**
   * Whether the test session is followed by at least one channel (otherwise it is a headless session).
   */
  def hasTestSessionSubscribers(sessionId: String): Boolean = {
    testSessionChannels.get(sessionId).exists(_.nonEmpty)
  }

  def registerActiveTestSession(sessionId: String): Unit = {
    activeTestSessions.put(sessionId, true)
  }

  def removeActiveTestSession(sessionId: String): Unit = {
    testSessionEnded(sessionId, null)
  }

  def testSessionEnded(sessionId: String, msg: String): Unit = {
    if (msg != null) {
      broadcast(sessionId, msg)
    }
    activeTestSessions.remove(sessionId)
  }

  /**
   * Broadcast the given message (in JSON) to all channels following the given test session (retrying if needed).
   */
  def broadcast(sessionId: String, msg: String): Unit = {
    broadcast(sessionId, msg, retry = true)
  }

  def broadcast(sessionId: String, msg: String, retry: Boolean): Unit = {
    if (retry) {
      broadcastAttempt(sessionId, msg, 1)
    } else {
      broadcastMessage(sessionId, msg)
    }
  }

  private def broadcastAttempt(sessionId: String, msg: String, attempt: Int): Future[Unit] = {
    if (attempt <= 10) {
      if (!broadcastMessage(sessionId, msg)) {
        after(1.seconds, actorSystem.scheduler)(Future.successful(())).flatMap { _ =>
          broadcastAttempt(sessionId, msg, attempt + 1)
        }
      } else {
        Future.unit
      }
    } else {
      logger.warn("Unable to send message for session [" + sessionId + "] after 10 attempts")
      Future.unit
    }
  }

  private def broadcastMessage(sessionId: String, msg: String): Boolean = {
    val channelIds = testSessionChannels.getOrElse(sessionId, Set.empty)
    if (channelIds.nonEmpty) {
      // The message is already JSON. Wrap it with the session ID so that the client can route it.
      val data = "{\"session\":%s,\"message\":%s}".formatted(Json.toJson(sessionId).toString(), msg)
      channelIds.foreach { channelId =>
        channels.get(channelId).foreach(emit(_, EVENT_SESSION, data))
      }
      true
    } else {
      // An active session without a channel is a headless session.
      activeTestSessions.contains(sessionId)
    }
  }

}
