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