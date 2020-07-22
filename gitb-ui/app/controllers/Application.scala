package controllers

import config.Configurations
import filters.CorsFilter
import javax.inject.Inject
import managers.{LegalNoticeManager, SystemConfigurationManager}
import models.{Constants, PublicConfig}
import org.apache.commons.lang3.StringUtils
import org.webjars.play.{WebJarAssets, WebJarsUtil}
import play.api.mvc._
import play.api.routing._
import utils.RepositoryUtils

import scala.collection.mutable.ListBuffer

class Application @Inject() (cc: ControllerComponents, defaultAction: DefaultActionBuilder, webjarAssets: WebJarAssets, webJarsUtil: WebJarsUtil, systemConfigurationManager: SystemConfigurationManager, legalNoticeManager: LegalNoticeManager) extends AbstractController(cc) {

  def index() = defaultAction {
    val legalNotice = legalNoticeManager.getCommunityDefaultLegalNotice(Constants.DefaultCommunityId)
    var hasDefaultLegalNotice = false
    var legalNoticeContent = ""
    if (legalNotice.isDefined && !StringUtils.isBlank(legalNotice.get.content)) {
      hasDefaultLegalNotice = true
      legalNoticeContent = legalNotice.get.content
    }
    val enableWebInit = Configurations.DATA_WEB_INIT_ENABLED && !RepositoryUtils.getDataLockFile().exists()
    Ok(views.html.index(webJarsUtil,
      new PublicConfig(
      Configurations.AUTHENTICATION_SSO_ENABLED,
      systemConfigurationManager.getLogoPath(),
      systemConfigurationManager.getFooterLogoPath(),
      Constants.VersionNumber,
      hasDefaultLegalNotice,
      legalNoticeContent,
      Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD,
      Configurations.DEMOS_ENABLED,
      Configurations.USERGUIDE_OU,
      Configurations.REGISTRATION_ENABLED,
      Configurations.GUIDES_EULOGIN_USE,
      Configurations.GUIDES_EULOGIN_MIGRATION,
      Configurations.AUTHENTICATION_COOKIE_PATH,
      enableWebInit
    )))
  }

  def app() = defaultAction {
    Ok(views.html.app(webJarsUtil, new PublicConfig(Constants.VersionNumber)))
  }

  def preFlight(all: String) = defaultAction {
    Ok("").withHeaders(
      ALLOW -> CorsFilter.origin,
      ACCESS_CONTROL_ALLOW_ORIGIN -> CorsFilter.origin,
      ACCESS_CONTROL_ALLOW_METHODS -> CorsFilter.methods,
      ACCESS_CONTROL_ALLOW_HEADERS -> CorsFilter.headers
    )
  }

  def javascriptRoutes = defaultAction { implicit request =>
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
