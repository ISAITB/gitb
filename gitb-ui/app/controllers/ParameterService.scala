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

import controllers.util.{AuthorizedAction, ParameterExtractor, ResponseConstructor}
import managers.{AuthorizationManager, CommunityLabelManager, ParameterManager}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ParameterService @Inject() (authorizedAction: AuthorizedAction,
                                  cc: ControllerComponents,
                                  parameterManager: ParameterManager,
                                  authorizationManager: AuthorizationManager,
                                  communityLabelManager: CommunityLabelManager)
                                 (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def deleteParameter(parameterId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteParameter(request, parameterId).flatMap { _ =>
      parameterManager.deleteParameter(parameterId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def updateParameter(parameterId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateParameter(request, parameterId).flatMap { _ =>
      val parameter = ParameterExtractor.extractParameter(request)
      parameterManager.checkParameterExistsForEndpoint(parameter.testKey, parameter.endpoint, Some(parameterId)).flatMap { parameterExists =>
        if (parameterExists) {
          communityLabelManager.getLabel(request, models.Enums.LabelType.Endpoint, single = true, lowercase = true).map { endpointLabel =>
            ResponseConstructor.constructBadRequestResponse(500, "A parameter with this key already exists for the " + endpointLabel +".")
          }
        } else{
          parameterManager.updateParameterWrapper(parameterId, parameter.name, parameter.testKey, parameter.desc, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue, parameter.defaultValue).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        }
      }
    }
  }

  def orderParameters(endpointId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateEndpoint(request, endpointId).flatMap { _ =>
      val orderedIds = ParameterExtractor.extractLongIdsBodyParameter(request)
      parameterManager.orderParameters(endpointId, orderedIds.getOrElse(List[Long]())).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

}
