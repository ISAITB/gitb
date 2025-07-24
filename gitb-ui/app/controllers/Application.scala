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

package controllers

import config.Configurations
import config.Configurations.versionInfo
import controllers.util.ResponseConstructor
import filters.CorsFilter
import managers.LegalNoticeManager
import models.{Constants, PublicConfig}
import org.apache.commons.lang3.{StringUtils, Strings}
import play.api.Environment
import play.api.mvc._
import play.api.routing._
import utils.RepositoryUtils

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class Application @Inject() (cc: ControllerComponents,
                             defaultAction: DefaultActionBuilder,
                             legalNoticeManager: LegalNoticeManager,
                             environment: Environment,
                             repositoryUtils: RepositoryUtils)
                            (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def index(): Action[AnyContent] = defaultAction.async {
    legalNoticeManager.getCommunityDefaultLegalNotice(Constants.DefaultCommunityId).map { legalNotice =>
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
          Configurations.WEB_CONTEXT_ROOT_WITH_SLASH,
          Configurations.restApiSwaggerLink(),
          Configurations.AUTHENTICATION_SSO_TYPE
        )))
    }
  }

  private def resourceVersionToUse(): String = {
    Configurations.versionInfo().replace(' ', '_')+Constants.VersionNumberPostfixForResources
  }

  def app(): Action[AnyContent] = defaultAction.async {
    handleAppLoad()
  }

  def swagger(): Action[AnyContent] = defaultAction.async {
    Future.successful {
      if (Configurations.AUTOMATION_API_ENABLED) {
        Ok(
          views.html.swagger(
            Configurations.PUBLIC_CONTEXT_ROOT_WITH_SLASH,
            versionInfo(),
            Configurations.restApiJsonLink().get
          )
        )
      } else {
        NotFound
      }
    }
  }

  def appWithRequestedPath(path: String): Action[AnyContent] = defaultAction.async { request =>
    if (StringUtils.isNoneBlank(path)) {
      val cookie = ResponseConstructor.createRequestedUrlCookie(request.uri)
      if (cookie.isDefined) {
        handleAppLoad().map { result =>
          result.withCookies(cookie.get)
        }
      } else {
        handleAppLoad()
      }
    } else {
      handleAppLoad()
    }
  }

  private def handleAppLoad(): Future[Result] = {
    Future.successful {
      Ok(views.html.ngApp(new PublicConfig(resourceVersionToUse(), Configurations.PUBLIC_CONTEXT_ROOT, Configurations.PUBLIC_CONTEXT_ROOT_WITH_SLASH, Configurations.WEB_CONTEXT_ROOT_WITH_SLASH), environment.mode))
    }
  }

  def preFlight(all: String): Action[AnyContent] = defaultAction.async {
    Future.successful {
      Ok("").withHeaders(
        ALLOW -> CorsFilter.origin,
        ACCESS_CONTROL_ALLOW_ORIGIN -> CorsFilter.origin,
        ACCESS_CONTROL_ALLOW_METHODS -> CorsFilter.methods,
        ACCESS_CONTROL_ALLOW_HEADERS -> CorsFilter.headers
      )
    }
  }

  def healthCheck: Action[AnyContent] = defaultAction.async {
    Future.successful(Ok)
  }

  def restApiInfo: Action[AnyContent] = defaultAction.async {
    Future.successful {
      if (Configurations.AUTOMATION_API_ENABLED) {
        Ok.sendFile(
          content = repositoryUtils.getRestApiDocsDocumentation(),
          fileName = _ => Some("openapi.json")
        ).as("application/json")
      } else {
        NotFound
      }
    }
  }

  def javascriptRoutes: Action[AnyContent] = defaultAction.async { implicit request =>
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
      if (Strings.CI.startsWith(Configurations.TESTBED_HOME_LINK, "http://") && linkLength > 7) {
        removePublicRootPath(Configurations.TESTBED_HOME_LINK.substring(7))
      } else if (Strings.CI.startsWith(Configurations.TESTBED_HOME_LINK, "https://") && linkLength > 8) {
        removePublicRootPath(Configurations.TESTBED_HOME_LINK.substring(8))
      } else {
        request.host
      }
    } else {
      request.host
    }
    Future.successful {
      Ok(JavaScriptReverseRouter("jsRoutes", Some("jQuery.ajax"), host, routeActions.toList:_*)).as("text/javascript")
    }
  }

  private def removePublicRootPath(pathWithoutProtocol: String): String = {
    val pathWithoutTrailingSlash = Strings.CS.removeEnd(pathWithoutProtocol, "/")
    if (Configurations.PUBLIC_CONTEXT_ROOT == "/") {
      pathWithoutTrailingSlash
    } else {
      Strings.CS.removeEnd(pathWithoutTrailingSlash, Configurations.PUBLIC_CONTEXT_ROOT)
    }
  }

}
