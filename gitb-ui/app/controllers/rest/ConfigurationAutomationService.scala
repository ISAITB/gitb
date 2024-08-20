package controllers.rest

import controllers.util.{AuthorizedAction, ResponseConstructor}
import managers.{AuthorizationManager, CommunityManager}
import models.Constants
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import javax.inject.{Inject, Singleton}

@Singleton
class ConfigurationAutomationService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, authorizationManager: AuthorizationManager, communityManager: CommunityManager) extends BaseAutomationService(cc) {

  def configure: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsConfigurationRequest(body)
      val warnings = communityManager.applyConfigurationViaAutomationApi(communityKey, input)
      var json = Json.obj()
      if (warnings.nonEmpty) {
        json = json + ("messages" -> JsonUtil.jsStringArray(warnings))
      }
      ResponseConstructor.constructJsonResponse(json.toString())
    })
  }

}
