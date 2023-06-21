package controllers

import controllers.util._
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.{AuthorizationManager, TriggerManager}
import models.Enums.TriggerServiceType
import play.api.libs.json.{JsBoolean, JsString, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import scala.concurrent.ExecutionContext.Implicits.global

class TriggerService @Inject()(authorizedAction: AuthorizedAction, cc: ControllerComponents, triggerManager: TriggerManager, authorizationManager: AuthorizationManager) extends AbstractController(cc) {

  def getTriggersByCommunity(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageTriggers(request, communityId)
    val list = triggerManager.getTriggersByCommunity(communityId)
    val json: String = JsonUtil.jsTriggers(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def createTrigger(): Action[AnyContent] = authorizedAction { request =>
    val trigger = ParameterExtractor.extractTriggerInfo(request, None)
    authorizationManager.canManageTriggers(request, trigger.trigger.community)
    val nameUnique = triggerManager.checkUniqueName(trigger.trigger.name, trigger.trigger.community)
    if (nameUnique) {
      triggerManager.createTrigger(trigger)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Trigger with name '" + trigger.trigger.name + "' already exists.")
    }
  }

  def getTriggerById(triggerId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageTrigger(request, triggerId)
    val trigger = triggerManager.getTriggerAndDataById(triggerId)
    val json: String = JsonUtil.jsTriggerInfo(trigger).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def updateTrigger(triggerId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageTrigger(request, triggerId)
    val triggerInfo = ParameterExtractor.extractTriggerInfo(request, Some(triggerId))
    val uniqueName = triggerManager.checkUniqueName(triggerId, triggerInfo.trigger.name, triggerInfo.trigger.community)
    if (uniqueName) {
      triggerManager.updateTrigger(triggerInfo)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Trigger with name '" + triggerInfo.trigger.name + "' already exists.")
    }
  }

  def deleteTrigger(triggerId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageTrigger(request, triggerId)
    triggerManager.deleteTrigger(triggerId)
    ResponseConstructor.constructEmptyResponse
  }

  def testTriggerEndpoint(): Action[AnyContent] = Action.async { request =>
    authorizedAction.wrap(request, (request: RequestWithAttributes[AnyContent]) => {
      val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
      authorizationManager.canManageCommunity(request, communityId)
      val url = ParameterExtractor.requiredBodyParameter(request, Parameters.URL)
      val serviceType = TriggerServiceType.apply(ParameterExtractor.requiredBodyParameter(request, Parameters.TYPE).toInt)
      triggerManager.testTriggerEndpoint(url, serviceType)
        .map { result =>
          var json = JsonUtil.jsTextArray(result._2)
          json = json+("success", JsBoolean(result._1))
          json = json+("contentType", JsString(result._3))
          ResponseConstructor.constructJsonResponse(json.toString())
        }
    })
  }

  def previewTriggerCall(): Action[AnyContent] = authorizedAction { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canManageCommunity(request, communityId)
    val operation = ParameterExtractor.optionalBodyParameter(request, Parameters.OPERATION)
    val serviceType = TriggerServiceType.apply(ParameterExtractor.requiredBodyParameter(request, Parameters.TYPE).toInt)
    val data = ParameterExtractor.extractTriggerDataItems(request, Parameters.DATA, None)
    val message = triggerManager.previewTriggerCall(communityId, operation, serviceType, data)
    val json = Json.obj(
      "message"    -> message
    )
    ResponseConstructor.constructJsonResponse(json.toString())
  }

  def testTriggerCall(): Action[AnyContent] = Action.async { request =>
    authorizedAction.wrap(request, (request: RequestWithAttributes[AnyContent]) => {
      val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
      authorizationManager.canManageCommunity(request, communityId)
      val url = ParameterExtractor.requiredBodyParameter(request, Parameters.URL)
      val serviceType = TriggerServiceType.apply(ParameterExtractor.requiredBodyParameter(request, Parameters.TYPE).toInt)
      val payloadString = ParameterExtractor.requiredBodyParameter(request, Parameters.PAYLOAD)
      triggerManager.testTriggerCall(url, serviceType, payloadString)
        .map { result =>
          var json = JsonUtil.jsTextArray(result._2)
          json = json + ("success", JsBoolean(result._1))
          json = json+("contentType", JsString(result._3))
          ResponseConstructor.constructJsonResponse(json.toString())
        }
    })
  }

  def clearStatus(triggerId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageTrigger(request, triggerId)
    triggerManager.clearStatus(triggerId)
    ResponseConstructor.constructEmptyResponse
  }

}