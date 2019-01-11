package actors

import akka.actor.{Props, Actor, ActorRef}
import com.gitb.core.TestCaseType
import controllers.TestService
import play.api.libs.json._
import org.slf4j.{LoggerFactory, Logger}

object WebSocketActor {

  private final val logger = LoggerFactory.getLogger("WebSocketActor")

  //references to all the connection handling actors
  //[sessionId -> [actorId -> Actor]]
  private var webSockets = Map[String, Map[String, ActorRef]]()
  private var testTypes  = Map[String, Short]()

  def broadcast(sessionId:String, msg:String, retry: Boolean):Unit = {
    var attempts = 0
    while (attempts < 10 && !broadcastMessage(sessionId, msg) && retry) {
      attempts += 1
      logger.warn("Unable to send message ["+msg+"] for session ["+sessionId+"] - attempt " + attempts)
      Thread.sleep(1000)
    }
  }

  /**
   * Broadcasts given msg (in Json) to all clients with the given session
   */
  def broadcast(sessionId:String, msg:String):Unit = {
    broadcast(sessionId, msg, true)
  }

  def broadcastMessage(sessionId:String, msg:String):Boolean = {
    if (webSockets.get(sessionId).isDefined) {
      webSockets.get(sessionId).get.foreach {
        case (actorId, out) =>
          //send message to each ActorRef of the same session ID
          out ! msg
      }
      true
    } else {
      false
    }
  }

    /**
   * Pushes given msg (in Json) to the given actor with given session
   */
  def push(sessionId:String, actorId:String, msg:String) = {
    if(webSockets.get(sessionId).isDefined) {
      val actors = webSockets.get(sessionId).get
      if (actors.get(actorId).isDefined) {
        val out = actors.get(actorId).get
        //send message to the client with given session and actor IDs
        out ! msg
      }
    }
  }

  /**
   * Returns all sessions in JSON format
   */
  def getSessions() = {
    //convert websockets into map of ("session" -> list("actorId") by filtering by type first
    val interopabilitySessions = WebSocketActor.testTypes.filter(_._2 == TestCaseType.INTEROPERABILITY.ordinal().toShort).map(_._1).toList
    val list = WebSocketActor.webSockets
      .filter(interopabilitySessions contains _._1)
      .map(entry => Json.obj("session" -> entry._1, "actors" -> entry._2.map(_._1)))
    Json.stringify(Json.toJson(list))
  }

  def props(out: ActorRef) = Props(new WebSocketActor(out)).withDispatcher("blocking-processor-dispatcher")
}

class WebSocketActor(out: ActorRef) extends Actor{

  private final val logger: Logger = LoggerFactory.getLogger(classOf[WebSocketActor])

  private final val REGISTER = "register"
  private final val NOTIFY   = "notify"
  private final val PING   = "ping"

  var sessionId:String = null
  var actorId:String = null
  var testCaseType:Short = -1;

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
            val jsType      = msg \ "type"
            val jsConfigurations = msg \ "configurations"

            //this check is necessary since browser might send other stuff
            if(!jsSessionId.isInstanceOf[JsUndefined] && !jsActorId.isInstanceOf[JsUndefined] && !jsType.isInstanceOf[JsUndefined]){
              sessionId = jsSessionId.as[String]
              actorId   = jsActorId.as[String]
              testCaseType = jsType.as[Short]

              var actors = WebSocketActor.webSockets.get(sessionId) getOrElse  {
                //create a new map if there is no actor with the given sessionId
                Map[String, ActorRef]()
              }
              actors += (actorId -> out)

              WebSocketActor.webSockets = WebSocketActor.webSockets.updated(sessionId, actors)
              WebSocketActor.testTypes  = WebSocketActor.testTypes.updated(sessionId, testCaseType)

              //if any actor sends its configurations, broadcast it.
              if(!jsConfigurations.isInstanceOf[JsUndefined] && jsConfigurations.toOption.isDefined) {
                WebSocketActor.broadcast(sessionId, Json.obj("configuration" -> jsConfigurations.get).toString())
              }
            }
          case NOTIFY =>
            val message = msg.as[JsObject] - "command" //remove command field from msg
            val sessionId = (msg \ "sessionId").as[String]

            //send message to all actors
            WebSocketActor.broadcast(sessionId, Json.obj("notify" -> message).toString())

          case PING =>
            // Do nothing. This is sent to keep alive the web socket connection.

          case _ =>
            logger.error("Unknown command")
        }

      } else {
        logger.error("Command not found")
      }



    case _ =>
      //TODO throw exception here
      logger.error("Communication Failure")
  }

  /**
   * Called when WebSocket with the client has been closed. Do the clean up
   */
  override def postStop() = {
    WebSocketActor.webSockets.synchronized {
      //remove the actor
      if(WebSocketActor.webSockets.get(sessionId).isDefined){
        var actors = WebSocketActor.webSockets.get(sessionId).get
        actors -= actorId

        //if all actors are gone within a session, remove the session as well
        if(actors.size == 0) {
          WebSocketActor.webSockets -= sessionId

          //also tell engine to end the session
          TestService.endSession(sessionId)
        }
        //otherwise, update the webSockets map
        else {
          WebSocketActor.webSockets = WebSocketActor.webSockets.updated(sessionId, actors)
        }
      }
    }
  }
}
