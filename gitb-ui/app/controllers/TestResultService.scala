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

import controllers.util.{AuthorizedAction, ParameterExtractor, ParameterNames, ResponseConstructor}

import javax.inject.Inject
import managers.AuthorizationManager
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils.{JsonUtil, MimeUtil}

import scala.concurrent.ExecutionContext

class TestResultService @Inject() (authorizedAction: AuthorizedAction,
                                   cc: ControllerComponents,
                                   authorizationManager: AuthorizationManager)
                                  (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getBinaryMetadata: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canGetBinaryFileMetadata(request).map { _ =>
      val data:String= ParameterExtractor.requiredBodyParameter(request, ParameterNames.DATA)
      val isBase64:Boolean = java.lang.Boolean.valueOf(ParameterExtractor.requiredBodyParameter(request, ParameterNames.IS_BASE64))
      val mimeType = MimeUtil.getMimeType(data, !isBase64)
      val extension = MimeUtil.getExtensionFromMimeType(mimeType)
      val json = JsonUtil.jsBinaryMetadata(mimeType, extension).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }

}
