package controllers

import play.api.Play.current

import org.slf4j.{LoggerFactory, Logger}
import actors.WebSocketActor
import play.api.mvc._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.JsValue

/**
 * Handles the communication with browsers
 */
class WebSocketService extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[WebSocketService])

  /**
   * Creates a WebSocket for the client
   */
  def socket = WebSocket.acceptWithActor[JsValue, String] { request => out =>
    //create a handler actor for communication handling
    WebSocketActor.props(out)
  }


}
