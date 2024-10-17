package actors

import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, Props}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object WebSocketActor {

  //references to all the connection handling actors
  //[sessionId -> Actor]
  var webSockets: mutable.Map[String, ActorRef] = TrieMap[String, ActorRef]()
  var activeSessions: Set[String] = Set[String]()

}

@Singleton
class WebSocketActor @Inject() (actorSystem: ActorSystem, testbedClient: managers.TestbedBackendClient) {

  private final val logger = LoggerFactory.getLogger("WebSocketActor")

  def pingTestEngineForClosedConnection(sessionId: String): Unit = {
    testbedClient.stop("CONNECTION_CLOSED|"+sessionId)
  }

  private def broadcastAttempt(sessionId:String, msg:String, attempt: Int): Unit = {
    if (attempt <= 10) {
      if (!broadcastMessage(sessionId, msg)) {
        org.apache.pekko.pattern.after(duration = 1.seconds, using = actorSystem.scheduler) {
          Future.successful(true)
        } andThen {
          case _ => broadcastAttempt(sessionId, msg, attempt+1)
        }
      }
    } else {
      logger.warn("Unable to send message for session ["+sessionId+"] after 10 attempts")
    }
  }

  def broadcast(sessionId:String, msg:String, retry: Boolean):Unit = {
    if (retry) {
      broadcastAttempt(sessionId, msg, 1)
    } else {
      broadcastMessage(sessionId, msg)
    }
  }

  def registerActiveTestSession(sessionId:String): Unit = {
    WebSocketActor.activeSessions.synchronized {
      if (!WebSocketActor.activeSessions.contains(sessionId)) {
        WebSocketActor.activeSessions += sessionId
      }
    }
  }

  def removeActiveTestSession(sessionId:String):Unit = {
    testSessionEnded(sessionId, null)
  }

  def testSessionEnded(sessionId:String, msg: String):Unit = {
    if (msg != null) {
      broadcast(sessionId, msg)
    }
    WebSocketActor.activeSessions.synchronized {
      if (WebSocketActor.activeSessions.contains(sessionId)) {
        WebSocketActor.activeSessions -= sessionId
      }
    }
  }

  /**
   * Broadcasts given msg (in Json) to all clients with the given session
   */
  def broadcast(sessionId:String, msg:String):Unit = {
    broadcast(sessionId, msg, retry = true)
  }

  def broadcastMessage(sessionId:String, msg:String):Boolean = {
    val webSocketInfo = WebSocketActor.webSockets.get(sessionId)
    if (webSocketInfo.isDefined) {
      // Send message to the ActorRef of the session ID
      webSocketInfo.get ! Json.parse(msg)
      true
    } else {
      if (WebSocketActor.activeSessions.contains(sessionId)) {
        // This is a headless session (active but without an open web socket)
        true
      } else {
        false
      }
    }
  }

    /**
   * Pushes given msg (in Json) to the given actor with given session
   */
  def push(sessionId:String, actorId:String, msg:String): Unit = {
    if (WebSocketActor.webSockets.contains(sessionId)) {
      // Send message to the client with given session ID
      WebSocketActor.webSockets(sessionId) ! Json.parse(msg)
    }
  }

  def props(out: ActorRef): Props = Props(new WebSocketActorHandler(out, this)).withDispatcher("blocking-processor-dispatcher")
}

class WebSocketActorHandler (out: ActorRef, webSocketActor: WebSocketActor) extends Actor {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[WebSocketActor])

  private final val REGISTER = "register"
  private final val NOTIFY   = "notify"
  private final val PING   = "ping"

  var sessionId:String = _

  def receive: Receive = {

    case msg: JsValue => //initially each browser client sends its session and actor information
      val jsCommand   = msg \ "command"
      var command:String = null

      if(!jsCommand.isInstanceOf[JsUndefined]){
        command = jsCommand.as[String]

        command match  {
          case REGISTER =>
            val jsSessionId = msg \ "sessionId"

            // This check is necessary since browser might send other stuff
            if (!jsSessionId.isInstanceOf[JsUndefined]){
              sessionId = jsSessionId.as[String]
              WebSocketActor.webSockets += (sessionId -> out)
              webSocketActor.registerActiveTestSession(sessionId)
            }
          case NOTIFY =>
            val message = msg.as[JsObject] - "command" //remove command field from msg
            val sessionId = (msg \ "sessionId").as[String]

            //send message to all actors
            webSocketActor.broadcast(sessionId, Json.obj("notify" -> message).toString())

          case PING =>
            // Do nothing. This is sent to keep alive the web socket connection.

          case _ =>
            logger.error("Unknown command")
        }

      } else {
        logger.error("Command not found")
      }
    case _ =>
      logger.error("Communication Failure")
  }

  /**
   * Called when WebSocket with the client has been closed. Do the clean up
   */
  override def postStop(): Unit = {
    WebSocketActor.webSockets.synchronized {
      // Remove the actor
      if (WebSocketActor.webSockets.contains(sessionId)) {
        WebSocketActor.webSockets -= sessionId
        // Ping the test engine - this is needed for cleanup in case a test session has not started yet.
        webSocketActor.pingTestEngineForClosedConnection(sessionId)
      }
    }
  }
}
