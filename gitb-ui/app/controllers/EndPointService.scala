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

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import managers.{AuthorizationManager, CommunityLabelManager, EndPointManager}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EndPointService @Inject() (authorizedAction: AuthorizedAction,
                                 cc: ControllerComponents,
                                 endPointManager: EndPointManager,
                                 authorizationManager: AuthorizationManager,
                                 communityLabelManager: CommunityLabelManager)
                                (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def deleteEndPoint(endPointId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteEndpoint(request, endPointId).flatMap { _ =>
      endPointManager.deleteEndPoint(endPointId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def updateEndPoint(endPointId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateEndpoint(request, endPointId).flatMap { _ =>
      val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
      val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
      val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
      endPointManager.checkEndPointExistsForActor(name, actorId, Some(endPointId)).flatMap { endpointExists =>
        if (endpointExists) {
          communityLabelManager.getLabelsByUserId(ParameterExtractor.extractUserId(request)).map { labels =>
            ResponseConstructor.constructBadRequestResponse(500, communityLabelManager.getLabel(labels, models.Enums.LabelType.Endpoint) + " with this name already exists for the " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor, true, true)+".")
          }
        } else{
          endPointManager.updateEndPointWrapper(endPointId, name, description).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        }
      }
    }
  }

}
