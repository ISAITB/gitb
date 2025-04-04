package controllers

import config.Configurations
import controllers.util.ParameterExtractor.{optionalBodyParameter, requiredBodyParameter}
import controllers.util._
import exceptions.ErrorCodes
import managers.{AuthorizationManager, CommunityResourceManager}
import models.Constants
import org.apache.commons.io.FileUtils
import play.api.mvc._
import utils.JsonUtil

import java.io.File
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommunityResourceService @Inject() (authorizedAction: AuthorizedAction,
                                          cc: ControllerComponents,
                                          communityResourceManager: CommunityResourceManager,
                                          authorizationManager: AuthorizationManager)
                                         (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def createSystemResource(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      createResourceInternal(Constants.DefaultCommunityId, request)
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def createCommunityResource(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      createResourceInternal(communityId, request)
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  private def createResourceInternal(communityId: Long, request: RequestWithAttributes[AnyContent]): Future[Result] = {
    val files = ParameterExtractor.extractFiles(request)
    if (files.contains(Parameters.FILE)) {
      val fileToStore = files(Parameters.FILE).file
      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(List(fileToStore))) {
        Future.successful {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
        }
      } else {
        val paramMap = ParameterExtractor.paramMap(request)
        val resource = ParameterExtractor.extractCommunityResource(paramMap, communityId)
        communityResourceManager.createCommunityResource(resource, fileToStore).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    } else {
      Future.successful {
        ResponseConstructor.constructBadRequestResponse(500, "No file provided for the resource.")
      }
    }
  }

  def updateSystemResource(resourceId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      updateResourceInternal(resourceId, request)
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def updateCommunityResource(resourceId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunityResource(request, resourceId).flatMap { _ =>
      updateResourceInternal(resourceId, request)
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  private def updateResourceInternal(resourceId: Long, request: RequestWithAttributes[AnyContent]): Future[Result] = {
    for {
      fileToStore <- {
        val files = ParameterExtractor.extractFiles(request)
        if (files.contains(Parameters.FILE)) {
          val fileToStore = Some(files(Parameters.FILE).file)
          if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(List(fileToStore.get))) {
            Future.successful((None, Some(ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan."))))
          } else {
            Future.successful((fileToStore, None))
          }
        } else {
          Future.successful((None, None))
        }
      }
      result <- {
        if (fileToStore._2.isEmpty) {
          val paramMap = ParameterExtractor.paramMap(request)
          val name = requiredBodyParameter(paramMap, Parameters.NAME)
          val description = optionalBodyParameter(paramMap, Parameters.DESCRIPTION)
          communityResourceManager.updateCommunityResource(resourceId, name, description, fileToStore._1).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful(fileToStore._2.get)
        }
      }
    } yield result
  }

  def uploadSystemResourcesInBulk(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      uploadResourcesInBulkInternal(Constants.DefaultCommunityId, request)
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def uploadCommunityResourcesInBulk(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      uploadResourcesInBulkInternal(communityId, request)
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  private def uploadResourcesInBulkInternal(communityId: Long, request: RequestWithAttributes[AnyContent]): Future[Result] = {
    val files = ParameterExtractor.extractFiles(request)
    if (files.contains(Parameters.FILE)) {
      val fileToStore = files(Parameters.FILE).file
      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(List(fileToStore))) {
        Future.successful {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
        }
      } else {
        val paramMap = ParameterExtractor.paramMap(request)
        val updateMatchingResources = ParameterExtractor.optionalBooleanBodyParameter(paramMap, Parameters.UPDATE).getOrElse(true)
        communityResourceManager.saveCommunityResourcesInBulk(communityId, fileToStore, updateMatchingResources).map { counts =>
          ResponseConstructor.constructJsonResponse(JsonUtil.jsUpdateCounts(counts._1, counts._2).toString())
        }
      }
    } else {
      Future.successful {
        ResponseConstructor.constructBadRequestResponse(500, "No file provided for the resource.")
      }
    }
  }

  def deleteSystemResource(resourceId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      deleteResourceInternal(resourceId)
    }
  }

  def deleteCommunityResource(resourceId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunityResource(request, resourceId).flatMap { _ =>
      deleteResourceInternal(resourceId)
    }
  }

  private def deleteResourceInternal(resourceId: Long): Future[Result] = {
    communityResourceManager.deleteCommunityResource(resourceId).map { _ =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  def deleteSystemResources(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      deleteResourcesInternal(Constants.DefaultCommunityId, request)
    }
  }

  def deleteCommunityResources(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      deleteResourcesInternal(communityId, request)
    }
  }

  private def deleteResourcesInternal(communityId: Long, request: RequestWithAttributes[AnyContent]): Future[Result] = {
    val resourceIds = ParameterExtractor.extractIdsBodyParameter(request)
    communityResourceManager.deleteCommunityResources(communityId, resourceIds).map { _ =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  def downloadSystemResources(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      downloadResourcesInternal(Constants.DefaultCommunityId, request)
    }
  }

  def downloadCommunityResources(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      downloadResourcesInternal(communityId, request)
    }
  }

  private def downloadResourcesInternal(communityId: Long, request: RequestWithAttributes[AnyContent]): Future[Result] = {
    val filter = ParameterExtractor.optionalQueryParameter(request, Parameters.FILTER)
    communityResourceManager.createCommunityResourceArchive(communityId, filter).map { archive =>
      Ok.sendFile(
        content = archive.toFile,
        fileName = _ => Some("resources.zip"),
        onClose = () => {
          FileUtils.deleteQuietly(archive.toFile)
        }
      )
    }
  }

  def getSystemResources(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      getResourcesInternal(Constants.DefaultCommunityId)
    }
  }

  def getCommunityResources(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      getResourcesInternal(communityId)
    }
  }

  private def getResourcesInternal(communityId: Long): Future[Result] = {
    communityResourceManager.getCommunityResources(communityId).map { result =>
      ResponseConstructor.constructJsonResponse(JsonUtil.jsCommunityResources(result).toString)
    }
  }

  def searchSystemResources(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      searchResourcesInternal(Constants.DefaultCommunityId, request)
    }
  }

  def searchCommunityResources(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      searchResourcesInternal(communityId, request)
    }
  }

  private def searchResourcesInternal(communityId: Long, request: RequestWithAttributes[AnyContent]): Future[Result] = {
    val filter = ParameterExtractor.optionalQueryParameter(request, Parameters.FILTER)
    val page = ParameterExtractor.optionalQueryParameter(request, Parameters.PAGE) match {
      case Some(v) => v.toLong
      case None => 1L
    }
    val limit = ParameterExtractor.optionalQueryParameter(request, Parameters.LIMIT) match {
      case Some(v) => v.toLong
      case None => 10L
    }
    communityResourceManager.searchCommunityResources(communityId, page, limit, filter).map { result =>
      ResponseConstructor.constructJsonResponse(JsonUtil.jsCommunityResourceSearchResult(result._1, result._2).toString)
    }
  }

  def downloadSystemResourceByName(resourceName: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystemResource(request).flatMap { _ =>
      communityResourceManager.getSystemResourceFileByName(resourceName).map { resource =>
        downloadResourceByNameInternal(resourceName, resource)
      }
    }
  }

  def downloadCommunityResourceByName(resourceName: String): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = request.cookies.get("implicitCommunity").map(_.value.toLong)
    for {
      _ <- {
        if (communityId.isDefined) {
          authorizationManager.canViewCommunityBasic(request, communityId.get)
        } else {
          authorizationManager.canViewOwnCommunity(request)
        }
      }
      result <- {
        authorizationManager.getUserIdFromRequest(request).flatMap { userId =>
          communityResourceManager.getCommunityResourceFileByName(communityId, userId, resourceName).map { resource =>
            downloadResourceByNameInternal(resourceName, resource)
          }
        }
      }
    } yield result
  }

  private def downloadResourceByNameInternal(resourceName: String, resourceFile: Option[File]): Result = {
    if (resourceFile.isDefined && resourceFile.get.exists()) {
      Ok.sendFile(
        content = resourceFile.get,
        fileName = _ => Some(resourceName),
      )
    } else {
      NotFound
    }
  }

  def downloadSystemResourceById(resourceId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.checkTestBedAdmin(request).flatMap { _ =>
      downloadResourceByIdInternal(resourceId)
    }
  }

  def downloadCommunityResourceById(resourceId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunityResource(request, resourceId).flatMap { _ =>
      downloadResourceByIdInternal(resourceId)
    }
  }

  private def downloadResourceByIdInternal(resourceId: Long): Future[Result] = {
    communityResourceManager.getCommunityResourceFileById(resourceId).map { resource =>
      if (resource._2.exists()) {
        Ok.sendFile(
          content = resource._2,
          fileName = _ => Some(resource._1),
        )
      } else {
        NotFound
      }
    }
  }

}
