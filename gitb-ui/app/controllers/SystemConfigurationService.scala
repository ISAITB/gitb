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

import controllers.util._
import exceptions.ErrorCodes
import managers.{AuthorizationManager, SystemConfigurationManager}
import models.Constants
import org.apache.commons.io.FileUtils
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc._
import utils.{HtmlUtil, JsonUtil, RepositoryUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SystemConfigurationService @Inject()(authorizedAction: AuthorizedAction,
                                           cc: ControllerComponents,
                                           repositoryUtils: RepositoryUtils,
                                           systemConfigurationManager: SystemConfigurationManager,
                                           environment: play.api.Environment,
                                           authorizationManager: AuthorizationManager)
                                          (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def testEmailSettings(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      val toAddress = ParameterExtractor.requiredBodyParameter(request, ParameterNames.TO)
      val settingsJson = ParameterExtractor.requiredBodyParameter(request, ParameterNames.SETTINGS)
      val emailSettings = JsonUtil.parseJsEmailSettings(settingsJson)
      systemConfigurationManager.testEmailSettings(emailSettings, toAddress).map { errors =>
        var json = Json.obj("success" -> JsBoolean(errors.isEmpty))
        if (errors.isDefined) {
          json = json + ("messages" -> JsonUtil.jsStringArray(errors.get))
        }
        ResponseConstructor.constructJsonResponse(json.toString())
      }
    }
  }

  def getConfigurationValues(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystemConfigurationValues(request).flatMap { _ =>
      systemConfigurationManager.getEditableSystemConfigurationValues().map { configs =>
        val json: String = JsonUtil.jsSystemConfigurations(configs).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def updateConfigurationValue(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateSystemConfigurationValues(request).flatMap { _ =>
      val name = ParameterExtractor.requiredBodyParameter(request, ParameterNames.NAME)
      var value = ParameterExtractor.optionalBodyParameter(request, ParameterNames.PARAMETER)
      if (systemConfigurationManager.isEditableSystemParameter(name)) {
        if (name == Constants.WelcomeMessage && value.isDefined) {
          value = Some(HtmlUtil.sanitizeEditorContent(value.get))
          if (value.get.isBlank) {
            value = None
          }
        }
        systemConfigurationManager.updateSystemParameter(name, value).map { resultToReport =>
          if (resultToReport.isDefined) {
            ResponseConstructor.constructJsonResponse(JsonUtil.jsSystemConfiguration(resultToReport.get).toString)
          } else {
            ResponseConstructor.constructEmptyResponse
          }
        }
      } else {
        Future.successful {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getCssForTheme: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canAccessThemeData(request).flatMap { _ =>
      systemConfigurationManager.getCssForActiveTheme().map { css =>
        ResponseConstructor.constructCssResponse(css)
      }
    }
  }

  def getThemeResource(themeId: Long, resourceName: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canAccessThemeData(request).flatMap { _ =>
      systemConfigurationManager.getThemeResource(themeId, resourceName).map { resourceFile =>
        if (resourceFile.isDefined) {
          Ok.sendFile(content = resourceFile.get)
        } else {
          NotFound
        }
      }
    }
  }

  private def streamThemeResource(resourcePath: String, themeId: Option[Long]): Result = {
    if (systemConfigurationManager.isBuiltInThemeResource(resourcePath)) {
      // Built-in resource shared under /assets/. Transform to /public/ for the classpath lookup.
      val resourceClassloaderPath = systemConfigurationManager.adaptBuiltInThemeResourcePathForClasspathLookup(resourcePath)
      Ok.sendResource(resourceClassloaderPath, environment.classLoader, inline = true)
    } else {
      // Custom resource.
      if (themeId.isDefined) {
        val themeResource = repositoryUtils.getThemeResource(themeId.get, resourcePath)
        if (themeResource.isDefined) {
          Ok.sendFile(content = themeResource.get)
        } else {
          NotFound
        }
      } else {
        NotFound
      }
    }
  }

  def previewThemeResource: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageThemes(request).map { _ =>
      val themeId = ParameterExtractor.optionalLongQueryParameter(request, ParameterNames.ID)
      val resourcePath = ParameterExtractor.requiredQueryParameter(request, ParameterNames.NAME)
      streamThemeResource(resourcePath, themeId)
    }
  }

  def getFaviconForTheme: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canAccessThemeData(request).flatMap { _ =>
      systemConfigurationManager.getActiveThemeId().flatMap { activeThemeId =>
        systemConfigurationManager.getFaviconPath().map { faviconPath =>
          streamThemeResource(faviconPath, Some(activeThemeId))
        }
      }
    }
  }

  def getThemes: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageThemes(request).flatMap { _ =>
      systemConfigurationManager.getThemes().map { themes =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsThemes(themes).toString)
      }
    }
  }

  def getTheme(themeId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageThemes(request).flatMap { _ =>
      systemConfigurationManager.getTheme(themeId).map { theme =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsTheme(theme).toString)
      }
    }
  }

  def createTheme: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageThemes(request).flatMap { _ =>
      val paramMap = ParameterExtractor.paramMap(request)
      val themeData = ParameterExtractor.extractTheme(request, paramMap)
      if (themeData._3.isDefined) {
        Future.successful {
          themeData._3.get
        }
      } else if (themeData._1.isDefined && themeData._2.isDefined) {
        systemConfigurationManager.themeExists(themeData._1.get.key, None).flatMap { exists =>
          if (exists) {
            Future.successful {
              ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A theme with this key already exists.", Some("key"))
            }
          } else {
            val referenceThemeId = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.REFERENCE).toLong
            systemConfigurationManager.createTheme(referenceThemeId, themeData._1.get, themeData._2.get).map { _ =>
              ResponseConstructor.constructEmptyResponse
            }
          }
        }
      } else {
        Future.successful {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Missing expected information.")
        }
      }
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def activateTheme(themeId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageThemes(request).flatMap { _ =>
      systemConfigurationManager.activateTheme(themeId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def updateTheme(themeId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageThemes(request).flatMap { _ =>
      val paramMap = ParameterExtractor.paramMap(request)
      val themeData = ParameterExtractor.extractTheme(request, paramMap, Some(themeId))
      if (themeData._3.isDefined) {
        Future.successful {
          themeData._3.get
        }
      } else if (themeData._1.isDefined && themeData._2.isDefined) {
        systemConfigurationManager.themeExists(themeData._1.get.key, Some(themeId)).flatMap { exists =>
          if (exists) {
            Future.successful {
              ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A theme with this key already exists.", Some("key"))
            }
          } else {
            systemConfigurationManager.updateTheme(themeData._1.get, themeData._2.get).map { _ =>
              ResponseConstructor.constructEmptyResponse
            }
          }
        }
      } else {
        Future.successful {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Missing expected information.")
        }
      }
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def deleteTheme(themeId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageThemes(request).flatMap { _ =>
      systemConfigurationManager.deleteTheme(themeId).map { deleted =>
        if (deleted) {
          ResponseConstructor.constructEmptyResponse
        } else {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "The selected theme cannot be deleted.")
        }
      }
    }
  }

}
