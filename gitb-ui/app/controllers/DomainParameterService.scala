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

import config.Configurations
import controllers.util.{AuthorizedAction, ParameterExtractor, ParameterNames, ResponseConstructor}
import exceptions.ErrorCodes
import managers.{AuthorizationManager, DomainParameterManager}
import models.TestServiceWithParameter
import org.apache.commons.io.FileUtils
import play.api.mvc._
import utils.{JsonUtil, RepositoryUtils}

import java.io.File
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DomainParameterService @Inject() (authorizedAction: AuthorizedAction,
                                        cc: ControllerComponents,
                                        authorizationManager: AuthorizationManager,
                                        domainParameterManager: DomainParameterManager,
                                        repositoryUtils: RepositoryUtils)
                                       (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getDomainParameters(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomainParameters(request, domainId).flatMap { _ =>
      // Optionally skip loading values (if we only want to show the list of parameters)
      val loadValues = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.VALUES).getOrElse(false)
      val onlySimple = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.SIMPLE).getOrElse(false)
      domainParameterManager.getDomainParameters(domainId, loadValues, None, onlySimple).map { result =>
        val json = JsonUtil.jsDomainParameters(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getDomainParametersOfCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewDomainParametersForCommunity(request, communityId).flatMap { _ =>
      val loadValues = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.VALUES).getOrElse(false)
      val onlySimple = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.SIMPLE).getOrElse(false)
      domainParameterManager.getDomainParametersByCommunityId(communityId, onlySimple, loadValues).map { result =>
        val json = JsonUtil.jsDomainParameters(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getDomainParameter(domainId: Long, domainParameterId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomainParameters(request, domainId).flatMap { _ =>
      domainParameterManager.getDomainParameter(domainParameterId).map { result =>
        val json = JsonUtil.jsDomainParameter(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def createDomainParameter(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomainParameters(request, domainId).flatMap { _ =>
      val paramMap = ParameterExtractor.paramMap(request)
      val files = ParameterExtractor.extractFiles(request)
      val jsDomainParameter = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.CONFIG)
      var fileToStore: Option[File] = None
      if (files.contains(ParameterNames.FILE)) {
        fileToStore = Some(files(ParameterNames.FILE).file)
      }
      val domainParameter = JsonUtil.parseJsDomainParameter(jsDomainParameter, None, domainId, isTestService = false)
      domainParameterManager.getDomainParameterByDomainAndName(domainId, domainParameter.name).flatMap { parameter =>
        var response: Result = null
        if (parameter.isDefined) {
          response = ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A parameter with this name already exists.", Some("name"))
        } else {
          if (domainParameter.kind == "BINARY") {
            if (fileToStore.isDefined) {
              if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(List(fileToStore.get))) {
                response = ResponseConstructor.constructErrorResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.", Some("file"))
              }
            } else {
              response = ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_REQUEST, "No file provided for binary parameter.", Some("file"))
            }
          }
        }
        if (response == null) {
          domainParameterManager.createDomainParameter(domainParameter, fileToStore).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful(response)
        }
      }
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def deleteDomainParameter(domainId: Long, domainParameterId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomainParameters(request, domainId).flatMap { _ =>
      domainParameterManager.deleteDomainParameterWrapper(domainId, domainParameterId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def updateDomainParameter(domainId: Long, domainParameterId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomainParameters(request, domainId).flatMap { _ =>
      val paramMap = ParameterExtractor.paramMap(request)
      val files = ParameterExtractor.extractFiles(request)
      val jsDomainParameter = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.CONFIG)
      var fileToStore: Option[File] = None
      if (files.contains(ParameterNames.FILE)) {
        fileToStore = Some(files(ParameterNames.FILE).file)
      }
      val domainParameter = JsonUtil.parseJsDomainParameter(jsDomainParameter, Some(domainParameterId), domainId, isTestService = false)
      domainParameterManager.getDomainParameterByDomainAndName(domainId, domainParameter.name).flatMap { existingDomainParameter =>
        var result: Result = null
        if (existingDomainParameter.isDefined && (existingDomainParameter.get.id != domainParameterId)) {
          result = ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A parameter with this name already exists.", Some("name"))
        } else if (domainParameter.kind == "BINARY") {
          if (fileToStore.isDefined) {
            if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(List(fileToStore.get))) {
              result = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
            }
          }
        }
        if (result == null) {
          domainParameterManager.updateDomainParameter(domainId, domainParameterId, domainParameter.name, domainParameter.desc, domainParameter.kind, domainParameter.value, domainParameter.inTests, domainParameter.contentType, fileToStore).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful(result)
        }
      }
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def downloadDomainParameterFile(domainId: Long, domainParameterId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomainParameters(request, domainId).map { _ =>
      val file = repositoryUtils.getDomainParameterFile(domainId, domainParameterId)
      if (file.exists()) {
        Ok.sendFile(
          content = file,
          inline = false
        )
      } else {
        ResponseConstructor.constructNotFoundResponse(ErrorCodes.INVALID_PARAM, "Domain parameter was not found")
      }
    }
  }

  def getTestServices(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestServices(request, domainId).flatMap { _ =>
      domainParameterManager.getTestServicesWithParameters(domainId).map { result =>
        val json = JsonUtil.jsTestServicesWithParameters(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getTestService(domainId: Long, serviceId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestServices(request, domainId).flatMap { _ =>
      domainParameterManager.getTestServiceWithParameter(serviceId).map { result =>
        val json = JsonUtil.jsTestServiceWithParameter(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  private def checkForExistingTestService(request: Request[AnyContent], testService: TestServiceWithParameter): Future[Option[Result]] = {
    for {
      existingParameter <- {
        val parameterIdToSkip = if (testService.service.parameter == 0L) {
          None
        } else {
          Some(testService.service.parameter)
        }
        domainParameterManager.getDomainParameterByDomainAndName(testService.parameter.domain, testService.parameter.name, parameterIdToSkip)
      }
      resultToReturn <- {
        if (existingParameter.isDefined) {
          if (existingParameter.get.isTestService) {
            // The name of the service already exists and is another test service.
            Future.successful {
              Some(ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A test service with this identifier already exists.", Some("name")))
            }
          } else {
            val updateParameter = ParameterExtractor.optionalBodyParameter(request, ParameterNames.UPDATE).exists(_.toBoolean)
            if (updateParameter) {
              // The name of the service matches a domain parameter and we have confirmation to update it.
              Future.successful(None)
            } else {
              // The name of the service matches a domain parameter. Return the ID of the other parameter to see if we migrate it.
              Future.successful {
                Some(ResponseConstructor.constructJsonResponse(JsonUtil.jsId(existingParameter.get.id).toString()))
              }
            }
          }
        } else {
          Future.successful(None)
        }
      }
    } yield resultToReturn
  }

  def createTestService(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestServices(request, domainId).flatMap { _ =>
      val testService = ParameterExtractor.extractTestServiceWithParameter(request, domainId, None)
      for {
        // Check to see if the test service or its linked parameter already exist.
        resultToReturn <- checkForExistingTestService(request, testService)
        result <- resultToReturn.map(Future.successful).getOrElse {
          // Proceed with creation.
          domainParameterManager.createTestServiceWithParameter(testService).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        }
      } yield result
    }
  }

  def updateTestService(domainId: Long, serviceId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestServices(request, domainId).flatMap { _ =>
      val testService = ParameterExtractor.extractTestServiceWithParameter(request, domainId, Some(serviceId))
      for {
        // Check to see if the test service or its linked parameter already exist.
        resultToReturn <- checkForExistingTestService(request, testService)
        result <- resultToReturn.map(Future.successful).getOrElse {
          // Proceed with update.
          domainParameterManager.updateTestServiceWithParameter(testService).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        }
      } yield result
    }
  }

  def deleteTestService(domainId: Long, serviceId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestServices(request, domainId).flatMap { _ =>
      domainParameterManager.deleteTestServiceWithParameter(serviceId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def getAvailableDomainParametersForTestServiceConversion(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestServices(request, domainId).flatMap { _ =>
      domainParameterManager.getAvailableDomainParametersForTestServiceConversion(domainId).map { result =>
        val json = JsonUtil.jsDomainParameters(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def testTestService(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageTestServices(request, domainId).flatMap { _ =>
      val serviceId = ParameterExtractor.optionalLongBodyParameter(request, ParameterNames.ID)
      val testService = ParameterExtractor.extractTestServiceWithParameter(request, domainId, serviceId)
      domainParameterManager.testTestService(testService).map { result =>
        val json = JsonUtil.jsServiceTestResult(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

}
