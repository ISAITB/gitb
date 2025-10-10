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
import managers.breadcrumb.BreadcrumbLabelRequest
import managers.{AuthorizationManager, BreadcrumbManager}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class BreadcrumbService @Inject()(authorizationManager: AuthorizationManager,
                                  breadcrumbManager: BreadcrumbManager,
                                  authorizedAction: AuthorizedAction,
                                  cc: ControllerComponents)
                                 (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getBreadcrumbLabels(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewBreadcrumbLabels(request).flatMap { _ =>
      val userId = ParameterExtractor.extractUserId(request)
      val domain = ParameterExtractor.optionalLongBodyParameter(request, ParameterNames.DOMAIN_ID)
      val specification = ParameterExtractor.optionalLongBodyParameter(request, ParameterNames.SPECIFICATION_ID)
      val specificationGroup = ParameterExtractor.optionalLongBodyParameter(request, ParameterNames.GROUP_ID)
      val actor = ParameterExtractor.optionalLongBodyParameter(request, ParameterNames.ACTOR_ID)
      val community = ParameterExtractor.optionalLongBodyParameter(request, ParameterNames.COMMUNITY_ID)
      val organisation = ParameterExtractor.optionalLongBodyParameter(request, ParameterNames.ORGANIZATION_ID)
      val system = ParameterExtractor.optionalLongBodyParameter(request, ParameterNames.SYSTEM_ID)

      breadcrumbManager.getLabels(BreadcrumbLabelRequest(userId, domain, specification, specificationGroup, actor, community, organisation, system)).map { result =>
        val json: String = JsonUtil.jsBreadcrumbLabelResponse(result).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

}
