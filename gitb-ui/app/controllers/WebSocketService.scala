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
