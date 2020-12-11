package actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.gitb.tbs.BasicCommand

import javax.inject.{Inject, Singleton}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object WebSocketActor {

  //references to all the connection handling actors
  //[sessionId -> [actorId -> Actor]]
  var webSockets: Map[String, Map[String, ActorRef]] = Map[String, Map[String, ActorRef]]()
  var activeSessions: Set[String] = Set[String]()

}

@Singleton
class WebSocketActor @Inject() (actorSystem: ActorSystem, testbedClient: managers.TestbedBackendClient) {

  private final val logger = LoggerFactory.getLogger("WebSocketActor")

  def pingTestEngineForClosedConnection(sessionId: String): Unit = {
    val command = new BasicCommand()
    command.setTcInstanceId("CONNECTION_CLOSED|"+sessionId)
    testbedClient.service().stop(command)
  }

  private def broadcastAttempt(sessionId:String, msg:String, attempt: Int): Unit = {
    if (attempt <= 10) {
      if (!broadcastMessage(sessionId, msg)) {
        akka.pattern.after(duration = 1.seconds, using = actorSystem.scheduler) {
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

  def testSessionStarted(sessionId:String): Unit = {
    WebSocketActor.activeSessions.synchronized {
      if (!WebSocketActor.activeSessions.contains(sessionId)) {
        WebSocketActor.activeSessions += sessionId
      }
    }
  }

  def testSessionEnded(sessionId:String):Unit = {
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
    broadcast(sessionId, msg, true)
  }

  def broadcastMessage(sessionId:String, msg:String):Boolean = {
    val webSocketInfo = WebSocketActor.webSockets.get(sessionId)
    if (webSocketInfo.isDefined) {
      webSocketInfo.get.foreach {
        case (actorId, out) =>
          //send message to each ActorRef of the same session ID
          out ! Json.parse(msg)
      }
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
  def push(sessionId:String, actorId:String, msg:String) = {
    if(WebSocketActor.webSockets.contains(sessionId)) {
      val actors = WebSocketActor.webSockets(sessionId)
      if (actors.contains(actorId)) {
        val out = actors(actorId)
        //send message to the client with given session and actor IDs
        out ! Json.parse(msg)
      }
    }
  }

  def props(out: ActorRef) = Props(new WebSocketActorHandler(out, this)).withDispatcher("blocking-processor-dispatcher")
}

class WebSocketActorHandler (out: ActorRef, webSocketActor: WebSocketActor) extends Actor{

  private final val logger: Logger = LoggerFactory.getLogger(classOf[WebSocketActor])

  private final val REGISTER = "register"
  private final val NOTIFY   = "notify"
  private final val PING   = "ping"

  var sessionId:String = null
  var actorId:String = null

  def receive = {

    case msg: JsValue => //initially each browser client sends its session and actor information
      val jsCommand   = (msg \ "command")
      var command:String = null

      if(!jsCommand.isInstanceOf[JsUndefined]){
        command = jsCommand.as[String]

        command match  {
          case REGISTER =>
            val jsSessionId = msg \ "sessionId"
            val jsActorId   = msg \ "actorId"
            val jsConfigurations = msg \ "configurations"

            //this check is necessary since browser might send other stuff
            if(!jsSessionId.isInstanceOf[JsUndefined] && !jsActorId.isInstanceOf[JsUndefined]){
              sessionId = jsSessionId.as[String]
              actorId   = jsActorId.as[String]

              var actors = WebSocketActor.webSockets.getOrElse(sessionId, {
                //create a new map if there is no actor with the given sessionId
                Map[String, ActorRef]()
              })
              actors += (actorId -> out)

              WebSocketActor.webSockets = WebSocketActor.webSockets.updated(sessionId, actors)
              webSocketActor.testSessionStarted(sessionId)
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
  override def postStop() = {
    WebSocketActor.webSockets.synchronized {
      //remove the actor
      if (WebSocketActor.webSockets.contains(sessionId)) {
        var actors = WebSocketActor.webSockets(sessionId)
        actors -= actorId
        //if all actors are gone within a session, remove the session as well
        if(actors.isEmpty) {
          WebSocketActor.webSockets -= sessionId
          // Ping the test engine - this is needed for cleanup in case a test session has not started yet.
          webSocketActor.pingTestEngineForClosedConnection(sessionId)
        }
        //otherwise, update the webSockets map
        else {
          WebSocketActor.webSockets = WebSocketActor.webSockets.updated(sessionId, actors)
        }
      }
    }
  }
}
