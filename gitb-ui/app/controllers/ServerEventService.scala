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

package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, ParameterNames, ResponseConstructor}
import exceptions.UnauthorizedAccessException
import managers.{AuthorizationManager, ServerEventManager}
import models.Constants
import play.api.http.HttpEntity
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

/**
 * Handles the server-sent event (SSE) channels between the server and users' browsers.
 */
@Singleton
class ServerEventService @Inject() (authorizedAction: AuthorizedAction,
                                    cc: ControllerComponents,
                                    authorizationManager: AuthorizationManager,
                                    serverEventManager: ServerEventManager)
                                   (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private final val LAST_EVENT_ID_HEADER = "Last-Event-ID"

  /**
   * Opens (or resumes) the user's event channel. The browser's EventSource cannot set an authorization header, so this
   * relies on the access token recorded in the user's session cookie (see AuthenticationFilter).
   */
  def connect(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canConnectToServerEvents(request).flatMap { _ =>
      val userId = ParameterExtractor.extractUserId(request)
      val accessToken = request.session.get(Constants.AccessTokenKey)
      if (accessToken.isEmpty) {
        throw UnauthorizedAccessException("Missing session access token")
      }
      serverEventManager.openChannel(userId, accessToken.get, request.headers.get(LAST_EVENT_ID_HEADER)).map { source =>
        Ok.sendEntity(HttpEntity.Streamed(source, None, Some("text/event-stream")))
          // Make sure proxies do not cache or buffer the stream.
          .withHeaders(CACHE_CONTROL -> "no-cache", "X-Accel-Buffering" -> "no")
      }
    }
  }

  /**
   * Have the user's event channel follow the updates of a test session.
   */
  def subscribeToTestSession(sessionId: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canExecuteTestSession(request, sessionId).map { _ =>
      val channelId = ParameterExtractor.requiredQueryParameter(request, ParameterNames.CHANNEL)
      val userId = ParameterExtractor.extractUserId(request)
      if (!serverEventManager.channelBelongsToUser(channelId, userId)) {
        throw UnauthorizedAccessException("Unknown event channel")
      }
      if (!serverEventManager.subscribeToTestSession(channelId, sessionId)) {
        throw UnauthorizedAccessException("Test session cannot be followed")
      }
      ResponseConstructor.constructEmptyResponse
    }
  }

  /**
   * Stop following the updates of a test session through the user's event channel.
   */
  def unsubscribeFromTestSession(sessionId: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canExecuteTestSession(request, sessionId).map { _ =>
      val channelId = ParameterExtractor.requiredQueryParameter(request, ParameterNames.CHANNEL)
      val userId = ParameterExtractor.extractUserId(request)
      if (serverEventManager.channelBelongsToUser(channelId, userId)) {
        serverEventManager.unsubscribeFromTestSession(channelId, sessionId)
      }
      ResponseConstructor.constructEmptyResponse
    }
  }

}
