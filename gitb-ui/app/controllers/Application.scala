package controllers

import config.Configurations
import config.Configurations.versionInfo
import controllers.util.ResponseConstructor
import filters.CorsFilter
import managers.LegalNoticeManager
import models.{Constants, PublicConfig}
import org.apache.commons.lang3.StringUtils
import play.api.Environment
import play.api.mvc._
import play.api.routing._
import utils.RepositoryUtils

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

class Application @Inject() (implicit ec: ExecutionContext, cc: ControllerComponents, defaultAction: DefaultActionBuilder, legalNoticeManager: LegalNoticeManager, environment: Environment, repositoryUtils: RepositoryUtils) extends AbstractController(cc) {

  def index(): Action[AnyContent] = defaultAction {
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
      versionInfo(),
      resourceVersionToUse(),
      hasDefaultLegalNotice,
      legalNoticeContent,
      Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD,
      Configurations.DEMOS_ENABLED,
      Configurations.USERGUIDE_OU,
      Configurations.REGISTRATION_ENABLED,
      Configurations.GUIDES_EULOGIN_USE,
      Configurations.GUIDES_EULOGIN_MIGRATION,
      Configurations.PUBLIC_CONTEXT_ROOT,
      enableWebInit,
      Configurations.TESTBED_MODE == Constants.DevelopmentMode,
      Configurations.PUBLIC_CONTEXT_ROOT_WITH_SLASH,
      Configurations.MORE_INFO_ENABLED,
      Configurations.MORE_INFO_ADDRESS,
      Configurations.RELEASE_INFO_ENABLED,
      Configurations.RELEASE_INFO_ADDRESS,
      Configurations.WELCOME_MESSAGE,
      Configurations.WEB_CONTEXT_ROOT_WITH_SLASH
    )))
  }

  private def resourceVersionToUse(): String = {
    Configurations.versionInfo().replace(' ', '_')+Constants.VersionNumberPostfixForResources
  }

  def app(): Action[AnyContent] = defaultAction {
    handleAppLoad()
  }

  def appWithRequestedPath(path: String): Action[AnyContent] = defaultAction { request =>
    if (StringUtils.isNoneBlank(path)) {
      val cookie = ResponseConstructor.createRequestedUrlCookie(request.uri)
      if (cookie.isDefined) {
        handleAppLoad().withCookies(cookie.get)
      } else {
        handleAppLoad()
      }
    } else {
      handleAppLoad()
    }
  }

  private def handleAppLoad(): Result = {
    Ok(views.html.ngApp(new PublicConfig(resourceVersionToUse(), Configurations.PUBLIC_CONTEXT_ROOT, Configurations.PUBLIC_CONTEXT_ROOT_WITH_SLASH, Configurations.WEB_CONTEXT_ROOT_WITH_SLASH), environment.mode))
  }

  def preFlight(all: String): Action[AnyContent] = defaultAction {
    Ok("").withHeaders(
      ALLOW -> CorsFilter.origin,
      ACCESS_CONTROL_ALLOW_ORIGIN -> CorsFilter.origin,
      ACCESS_CONTROL_ALLOW_METHODS -> CorsFilter.methods,
      ACCESS_CONTROL_ALLOW_HEADERS -> CorsFilter.headers
    )
  }

  def healthCheck: Action[AnyContent] = defaultAction {
    Ok
  }

  def restApiInfo: Action[AnyContent] = defaultAction {
    if (Configurations.AUTOMATION_API_ENABLED) {
      Ok.sendFile(
        content = repositoryUtils.getRestApiDocsDocumentation(),
        fileName = _ => Some("openapi.json")
      ).as("application/json")
    } else {
      NotFound
    }
  }

  def javascriptRoutes: Action[AnyContent] = defaultAction { implicit request =>
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
    // Replace absolute address in the javascript routes' file to match the public address
    val host = if (Configurations.TESTBED_HOME_LINK != "/") {
      val linkLength = Configurations.TESTBED_HOME_LINK.length
      if (StringUtils.startsWithIgnoreCase(Configurations.TESTBED_HOME_LINK, "http://") && linkLength > 7) {
        Configurations.TESTBED_HOME_LINK.substring(7)
      } else if (StringUtils.startsWithIgnoreCase(Configurations.TESTBED_HOME_LINK, "https://") && linkLength > 8) {
        Configurations.TESTBED_HOME_LINK.substring(8)
      } else {
        "/"
      }
    } else {
      request.host
    }
    Ok(JavaScriptReverseRouter("jsRoutes", Some("jQuery.ajax"), host, routeActions.toList:_*)).as("text/javascript")
  }

}
