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
import managers.triggers.TriggerHelper
import managers.{AuthorizationManager, TriggerManager}
import models.Enums.TriggerServiceType
import play.api.libs.json.{JsBoolean, JsString, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TriggerService @Inject()(authorizedAction: AuthorizedAction,
                               cc: ControllerComponents,
                               triggerManager: TriggerManager,
                               triggerHelper: TriggerHelper,
                               authorizationManager: AuthorizationManager)
                              (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getTriggersByCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTriggers(request, communityId).flatMap { _ =>
      triggerManager.getTriggersByCommunity(communityId).map { list =>
        val json: String = JsonUtil.jsTriggers(list).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def createTrigger(): Action[AnyContent] = authorizedAction.async { request =>
    val trigger = ParameterExtractor.extractTriggerInfo(request, None)
    authorizationManager.canManageTriggers(request, trigger.trigger.community).flatMap { _ =>
      triggerManager.checkUniqueName(trigger.trigger.name, trigger.trigger.community).flatMap { nameUnique =>
        if (nameUnique) {
          triggerManager.createTrigger(trigger).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A trigger with this name already exists.", Some("name"))
          }
        }
      }
    }
  }

  def getTriggerById(triggerId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTrigger(request, triggerId).flatMap { _ =>
      triggerManager.getTriggerAndDataById(triggerId).map { trigger =>
        val json: String = JsonUtil.jsTriggerInfo(trigger).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def updateTrigger(triggerId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTrigger(request, triggerId).flatMap { _ =>
      val triggerInfo = ParameterExtractor.extractTriggerInfo(request, Some(triggerId))
      triggerManager.checkUniqueName(triggerId, triggerInfo.trigger.name, triggerInfo.trigger.community).flatMap { uniqueName =>
        if (uniqueName) {
          triggerManager.updateTrigger(triggerInfo).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A trigger with this name already exists.", Some("name"))
          }
        }
      }
    }
  }

  def deleteTrigger(triggerId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTrigger(request, triggerId).flatMap { _ =>
      triggerHelper.deleteTrigger(triggerId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def testTriggerEndpoint(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      val url = ParameterExtractor.requiredBodyParameter(request, Parameters.URL)
      val serviceType = TriggerServiceType.apply(ParameterExtractor.requiredBodyParameter(request, Parameters.TYPE).toInt)
      triggerManager.testTriggerEndpoint(url, serviceType).map { result =>
        val json = JsonUtil.jsServiceTestResult(result)
        ResponseConstructor.constructJsonResponse(json.toString())
      }
    }
  }

  def previewTriggerCall(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      val operation = ParameterExtractor.optionalBodyParameter(request, Parameters.OPERATION)
      val serviceType = TriggerServiceType.apply(ParameterExtractor.requiredBodyParameter(request, Parameters.TYPE).toInt)
      val data = ParameterExtractor.extractTriggerDataItems(request, Parameters.DATA, None)
      triggerManager.previewTriggerCall(communityId, operation, serviceType, data).map { message =>
        val json = Json.obj(
          "message"    -> message
        )
        ResponseConstructor.constructJsonResponse(json.toString())
      }
    }
  }

  def testTriggerCall(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      val url = ParameterExtractor.requiredBodyParameter(request, Parameters.URL)
      val serviceType = TriggerServiceType.apply(ParameterExtractor.requiredBodyParameter(request, Parameters.TYPE).toInt)
      val payloadString = ParameterExtractor.requiredBodyParameter(request, Parameters.PAYLOAD)
      triggerManager.testTriggerCall(url, serviceType, payloadString).map { result =>
        val json = JsonUtil.jsServiceTestResult(result)
        ResponseConstructor.constructJsonResponse(json.toString())
      }
    }
  }

  def clearStatus(triggerId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTrigger(request, triggerId).flatMap { _ =>
      triggerManager.clearStatus(triggerId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

}