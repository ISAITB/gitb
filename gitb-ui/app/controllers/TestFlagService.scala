/*
 * Copyright (C) 2026 European Union
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
import managers.{AuthorizationManager, ServerEventManager, TestFlagManager}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import org.slf4j.{Logger, LoggerFactory}
import utils.JsonUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestFlagService @Inject()(authorizedAction: AuthorizedAction,
                                cc: ControllerComponents,
                                testFlagManager: TestFlagManager,
                                authorizationManager: AuthorizationManager,
                                serverEventManager: ServerEventManager)
                               (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[TestFlagService])

  def getTestFlagsByCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestFlags(request, communityId).flatMap { _ =>
      val page = ParameterExtractor.extractPageNumber(request)
      val limit = ParameterExtractor.extractPageLimit(request)
      testFlagManager.getTestFlagsByCommunity(communityId, page, limit).map { result =>
        val json: String = JsonUtil.jsSearchResult(result, JsonUtil.jsTestFlags).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getAllTestFlagsByCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestFlags(request, communityId).flatMap { _ =>
      testFlagManager.getAllTestFlagsByCommunity(communityId).map { result =>
        val json: String = JsonUtil.jsTestFlags(result).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def createTestFlag(): Action[AnyContent] = authorizedAction.async { request =>
    val testFlag = ParameterExtractor.extractTestFlagInfo(request, None)
    authorizationManager.canManageTestFlags(request, testFlag.community).flatMap { _ =>
      testFlagManager.checkUniqueName(testFlag.name, testFlag.community).flatMap { nameUnique =>
        if (nameUnique) {
          testFlagManager.createTestFlag(testFlag).map { _ =>
            publishTestFlags(testFlag.community)
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A test flag with this name already exists.", Some("name"))
          }
        }
      }
    }
  }

  def updateTestFlag(testFlagId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestFlag(request, testFlagId).flatMap { _ =>
      val testFlag = ParameterExtractor.extractTestFlagInfo(request, Some(testFlagId))
      testFlagManager.checkUniqueName(testFlagId, testFlag.name, testFlag.community).flatMap { uniqueName =>
        if (uniqueName) {
          testFlagManager.updateTestFlag(testFlag).map { _ =>
            publishTestFlags(testFlag.community)
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A test flag with this name already exists.", Some("name"))
          }
        }
      }
    }
  }

  def deleteTestFlag(testFlagId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestFlag(request, testFlagId).flatMap { _ =>
      testFlagManager.deleteTestFlag(testFlagId).map { communityId =>
        publishTestFlags(communityId)
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def orderTestFlags(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestFlags(request, communityId).flatMap { _ =>
      val orderedIds = ParameterExtractor.extractLongIdsBodyParameter(request)
      testFlagManager.orderTestFlags(communityId, orderedIds.getOrElse(List[Long]())).map { _ =>
        publishTestFlags(communityId)
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def resetTestFlagOrder(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestFlags(request, communityId).flatMap { _ =>
      testFlagManager.resetTestFlagOrder(communityId).map { _ =>
        publishTestFlags(communityId)
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
   * Push the community's current test flags to its connected users. The flags are reloaded as their order is
   * determined by the database. This is done in the background (failures are only logged).
   */
  private def publishTestFlags(communityId: Long): Unit = {
    testFlagManager.getAllTestFlagsByCommunity(communityId).map { flags =>
      serverEventManager.publishCommunityTestFlags(communityId, flags)
    }.recover {
      case e: Exception => logger.warn(s"Unable to publish updated test flags for community [$communityId] to server event channels", e)
    }
  }

}
