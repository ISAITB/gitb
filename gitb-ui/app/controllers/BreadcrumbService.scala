package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
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
      val domain = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
      val specification = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SPECIFICATION_ID)
      val specificationGroup = ParameterExtractor.optionalLongBodyParameter(request, Parameters.GROUP_ID)
      val actor = ParameterExtractor.optionalLongBodyParameter(request, Parameters.ACTOR_ID)
      val community = ParameterExtractor.optionalLongBodyParameter(request, Parameters.COMMUNITY_ID)
      val organisation = ParameterExtractor.optionalLongBodyParameter(request, Parameters.ORGANIZATION_ID)
      val system = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SYSTEM_ID)

      breadcrumbManager.getLabels(BreadcrumbLabelRequest(userId, domain, specification, specificationGroup, actor, community, organisation, system)).map { result =>
        val json: String = JsonUtil.jsBreadcrumbLabelResponse(result).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

}
