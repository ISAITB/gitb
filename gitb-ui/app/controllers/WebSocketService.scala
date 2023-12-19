package controllers

import actors.WebSocketActor
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import javax.inject.Inject
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.mvc._

/**
 * Handles the communication with browsers
 */
class WebSocketService @Inject() (implicit system: ActorSystem, cc: ControllerComponents, materializer: Materializer, webSocketActor: WebSocketActor) extends AbstractController(cc) {

  /**
   * Creates a WebSocket for the client
   */
  def socket = WebSocket.accept[JsValue, JsValue] { request =>
    //create a handler actor for communication handling
    ActorFlow.actorRef(out => webSocketActor.props(out))
  }

}
