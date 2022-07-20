package controllers

import config.Configurations
import filters.CorsFilter
import managers.{LegalNoticeManager, SystemConfigurationManager}
import models.{Constants, PublicConfig}
import org.apache.commons.lang3.StringUtils
import play.api.Environment
import play.api.mvc._
import play.api.routing._
import utils.RepositoryUtils

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

class Application @Inject() (implicit ec: ExecutionContext, cc: ControllerComponents, defaultAction: DefaultActionBuilder, systemConfigurationManager: SystemConfigurationManager, legalNoticeManager: LegalNoticeManager, environment: Environment, repositoryUtils: RepositoryUtils) extends AbstractController(cc) {

  def index() = defaultAction {
    val legalNotice = legalNoticeManager.getCommunityDefaultLegalNotice(Constants.DefaultCommunityId)
    var hasDefaultLegalNotice = false
    var legalNoticeContent = ""
    if (legalNotice.isDefined && !StringUtils.isBlank(legalNotice.get.content)) {
      hasDefaultLegalNotice = true
      legalNoticeContent = legalNotice.get.content
    }
    val enableWebInit = Configurations.DATA_WEB_INIT_ENABLED && !repositoryUtils.getDataLockFile().exists()
    Ok(views.html.index(
      new PublicConfig(
      Configurations.AUTHENTICATION_SSO_ENABLED,
      systemConfigurationManager.getLogoPath(),
      systemConfigurationManager.getFooterLogoPath(),
      Constants.VersionNumber,
      Constants.ResourceVersionNumber,
      hasDefaultLegalNotice,
      legalNoticeContent,
      Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD,
      Configurations.DEMOS_ENABLED,
      Configurations.USERGUIDE_OU,
      Configurations.REGISTRATION_ENABLED,
      Configurations.GUIDES_EULOGIN_USE,
      Configurations.GUIDES_EULOGIN_MIGRATION,
      Configurations.AUTHENTICATION_COOKIE_PATH,
      enableWebInit,
      Configurations.TESTBED_MODE == Constants.DevelopmentMode,
      contextPath(),
      Configurations.MORE_INFO_ENABLED,
      Configurations.MORE_INFO_ADDRESS,
      Configurations.RELEASE_INFO_ENABLED,
      Configurations.RELEASE_INFO_ADDRESS
    )))
  }

  def app() = defaultAction {
    Ok(views.html.ngApp(new PublicConfig(Constants.VersionNumber, Constants.ResourceVersionNumber, Configurations.AUTHENTICATION_COOKIE_PATH, contextPath()), environment.mode))
  }

  private def contextPath(): String = {
    var contextPath = Configurations.AUTHENTICATION_COOKIE_PATH
    if (!contextPath.endsWith("/")) {
      contextPath += "/"
    }
    contextPath += "app"
    contextPath
  }

  def preFlight(all: String) = defaultAction {
    Ok("").withHeaders(
      ALLOW -> CorsFilter.origin,
      ACCESS_CONTROL_ALLOW_ORIGIN -> CorsFilter.origin,
      ACCESS_CONTROL_ALLOW_METHODS -> CorsFilter.methods,
      ACCESS_CONTROL_ALLOW_HEADERS -> CorsFilter.headers
    )
  }

  def restApiInfo = defaultAction {
    if (Configurations.AUTOMATION_API_ENABLED) {
      Ok.sendFile(
        content = repositoryUtils.getRestApiDocsDocumentation(),
        fileName = _ => Some("openapi.json")
      ).as("application/json")
    } else {
      NotFound
    }
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
