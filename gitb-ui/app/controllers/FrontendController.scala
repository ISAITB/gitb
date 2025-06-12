package controllers

import config.Configurations
import play.api.http.HttpErrorHandler
import play.api.mvc._

import javax.inject._

/**
 * Frontend controller managing all static resource associate routes.
 *
 * @param assets Assets controller reference.
 * @param cc Controller components reference.
 */
@Singleton
class FrontendController @Inject()(assets: Assets, errorHandler: HttpErrorHandler, cc: ControllerComponents) extends AbstractController(cc) {

  def index: Action[AnyContent] = assets.at("index.html")

  def assetOrDefault(resource: String): Action[AnyContent] = {
    if (resource.startsWith(Configurations.API_PREFIX)) {
      Action.async(r => errorHandler.onClientError(r, NOT_FOUND, "Not found"))
    } else {
      if (resource.contains(".")) assets.at(resource) else index
    }
  }
}