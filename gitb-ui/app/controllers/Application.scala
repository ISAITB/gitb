package controllers

import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import filters.CorsFilter

import scala.collection.mutable.ListBuffer
import play.api.routing._

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
    val jsRoutesClass = classOf[routes.javascript]
    val controllers = jsRoutesClass.getFields.map(_.get(null))
    val routeActions = new ListBuffer[play.api.routing.JavaScriptReverseRoute]()
    controllers.foreach{ controller =>
      controller.getClass.getDeclaredMethods.foreach{ action =>
        if (action.getReturnType == classOf[play.api.routing.JavaScriptReverseRoute]) {
          routeActions += action.invoke(controller).asInstanceOf[play.api.routing.JavaScriptReverseRoute]
        }
      }
    }
    Ok(JavaScriptReverseRouter("jsRoutes")(routeActions.toList:_*)).as("text/javascript")
  }

}
