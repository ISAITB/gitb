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

import config.Configurations
import play.api.http._
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router
import play.core.WebCommands
import play.core.j.JavaHandlerComponents
import javax.inject.{Inject, Provider}

/**
 * The purpose if this custom HTTP request handler is only to ensure that a request for the context root with and
 * without trailing slash will work the same. Without this, requesting the web context with trailing slash is not
 * matched in the routes file (and there is no way of adding such an additional route there).
 *
 * In terms of implementation, this custom handler simply delegates to the default one used by Play.
 */
class CustomHttpRequestHandler @Inject()(webCommands: WebCommands,
                                         router: Provider[Router],
                                         errorHandler: HttpErrorHandler,
                                         configuration: HttpConfiguration,
                                         enabledFilters: Filters,
                                         handlerComponents: JavaHandlerComponents
                                         ) extends JavaCompatibleHttpRequestHandler(
  webCommands,
  None,
  router,
  errorHandler,
  configuration,
  enabledFilters.filters,
  handlerComponents
) {

  override def routeRequest(request: RequestHeader): Option[Handler] = {
    var handler = super.routeRequest(request)
    if (handler.isEmpty && request.target.path == Configurations.WEB_CONTEXT_ROOT_WITH_SLASH) {
      handler = super.routeRequest(request.withTarget(request.target.withPath(Configurations.WEB_CONTEXT_ROOT)))
    }
    handler
  }
}