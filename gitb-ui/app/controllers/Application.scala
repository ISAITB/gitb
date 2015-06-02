package controllers

import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._
import filters.CorsFilter
import play.api.Routes

class Application extends Controller {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[Application])

  def index = Action {
    logger.info("Serving index page...")
    Ok(views.html.index())
  }

  def preFlight(all: String) = Action {
    Ok("").withHeaders(
      ALLOW -> CorsFilter.origin,
      ACCESS_CONTROL_ALLOW_ORIGIN -> CorsFilter.origin,
      ACCESS_CONTROL_ALLOW_METHODS -> CorsFilter.methods,
      ACCESS_CONTROL_ALLOW_HEADERS -> CorsFilter.headers
    )
  }

  def javascriptRoutes = Action { implicit request =>
    //cache for all the methods defined in the controllers in app.controllers package
    val routeCache = {
      val jsRoutesClass = classOf[routes.javascript]
      val controllers = jsRoutesClass.getFields().map(_.get(null))
      controllers.flatMap { controller =>
        controller.getClass().getDeclaredMethods().map { action =>
          action.invoke(controller).asInstanceOf[play.core.Router.JavascriptReverseRoute]
        }
      }
    }

    Ok(Routes.javascriptRouter("jsRoutes")(routeCache:_*)).as("text/javascript")
  }


}
