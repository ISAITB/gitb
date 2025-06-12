package controllers

import actors.WebSocketActor
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._

import javax.inject.Inject

/**
 * Handles the communication with browsers
 */
class WebSocketService @Inject() (cc: ControllerComponents,
                                  webSocketActor: WebSocketActor)
                                 (implicit system: ActorSystem) extends AbstractController(cc) {

  /**
   * Creates a WebSocket for the client
   */
  def socket: WebSocket = WebSocket.accept[JsValue, JsValue] { _ =>
    //create a handler actor for communication handling
    ActorFlow.actorRef(out => webSocketActor.props(out), 16000)
  }

}
