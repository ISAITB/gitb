package controllers

import config.Configurations
import filters.CorsFilter
import javax.inject.Inject
import managers.LegalNoticeManager
import models.{Constants, PublicConfig}
import org.apache.commons.lang3.StringUtils
import play.api.mvc._
import play.api.routing._

import scala.collection.mutable.ListBuffer

class Application @Inject() (webJarAssets: WebJarAssets, systemConfigurationService: SystemConfigurationService, legalNoticeManager: LegalNoticeManager) extends Controller {

  def index() = Action {
    val legalNotice = legalNoticeManager.getCommunityDefaultLegalNotice(Constants.DefaultCommunityId)
    var hasDefaultLegalNotice = false
    var legalNoticeContent = ""
    if (legalNotice.isDefined && !StringUtils.isBlank(legalNotice.get.content)) {
      hasDefaultLegalNotice = true
      legalNoticeContent = legalNotice.get.content
    }
    Ok(views.html.index(webJarAssets,
      new PublicConfig(
      Configurations.AUTHENTICATION_SSO_ENABLED,
      systemConfigurationService.getLogoPath(),
      systemConfigurationService.getFooterLogoPath(),
      Constants.VersionNumber,
      hasDefaultLegalNotice,
      legalNoticeContent,
      Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD,
      Configurations.DEMOS_ENABLED,
      Configurations.USERGUIDE_OU,
      Configurations.REGISTRATION_ENABLED,
      Configurations.GUIDES_EULOGIN_USE,
      Configurations.GUIDES_EULOGIN_MIGRATION,
      Configurations.AUTHENTICATION_COOKIE_PATH
    )))
  }

  def app() = Action {
    Ok(views.html.app(webJarAssets, new PublicConfig(Constants.VersionNumber)))
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
