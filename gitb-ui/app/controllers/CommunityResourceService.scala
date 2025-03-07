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
import java.nio.file.Path
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CommunityResourceService @Inject() (implicit ec: ExecutionContext,
                                          authorizedAction: AuthorizedAction,
                                          cc: ControllerComponents,
                                          communityResourceManager: CommunityResourceManager,
                                          authorizationManager: AuthorizationManager
                                         ) extends AbstractController(cc) {

  def createSystemResource(): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.checkTestBedAdmin(request)
      createResourceInternal(Constants.DefaultCommunityId, request)
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def createCommunityResource(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canManageCommunity(request, communityId)
      createResourceInternal(communityId, request)
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  private def createResourceInternal(communityId: Long, request: RequestWithAttributes[AnyContent]): Result = {
    var response: Result = null
    val files = ParameterExtractor.extractFiles(request)
    if (files.contains(Parameters.FILE)) {
      val fileToStore = files(Parameters.FILE).file
      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(List(fileToStore))) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
      } else {
        val paramMap = ParameterExtractor.paramMap(request)
        val resource = ParameterExtractor.extractCommunityResource(paramMap, communityId)
        communityResourceManager.createCommunityResource(resource, fileToStore)
        response = ResponseConstructor.constructEmptyResponse
      }
    } else {
      response = ResponseConstructor.constructBadRequestResponse(500, "No file provided for the resource.")
    }
    response
  }

  def updateSystemResource(resourceId: Long): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.checkTestBedAdmin(request)
      updateResourceInternal(resourceId, request)
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def updateCommunityResource(resourceId: Long): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canManageCommunityResource(request, resourceId)
      updateResourceInternal(resourceId, request)
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  private def updateResourceInternal(resourceId: Long, request: RequestWithAttributes[AnyContent]): Result = {
    var response: Result = null
    val files = ParameterExtractor.extractFiles(request)
    var fileToStore: Option[File] = None
    if (files.contains(Parameters.FILE)) {
      fileToStore = Some(files(Parameters.FILE).file)
      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(List(fileToStore.get))) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
      }
    }
    if (response == null) {
      val paramMap = ParameterExtractor.paramMap(request)
      val name = requiredBodyParameter(paramMap, Parameters.NAME)
      val description = optionalBodyParameter(paramMap, Parameters.DESCRIPTION)
      communityResourceManager.updateCommunityResource(resourceId, name, description, fileToStore)
      response = ResponseConstructor.constructEmptyResponse
    }
    response
  }

  def uploadSystemResourcesInBulk(): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.checkTestBedAdmin(request)
      uploadResourcesInBulkInternal(Constants.DefaultCommunityId, request)
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def uploadCommunityResourcesInBulk(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canManageCommunity(request, communityId)
      uploadResourcesInBulkInternal(communityId, request)
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  private def uploadResourcesInBulkInternal(communityId: Long, request: RequestWithAttributes[AnyContent]): Result = {
    var response: Result = null
    val files = ParameterExtractor.extractFiles(request)
    if (files.contains(Parameters.FILE)) {
      val fileToStore = files(Parameters.FILE).file
      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(List(fileToStore))) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
      } else {
        val paramMap = ParameterExtractor.paramMap(request)
        val updateMatchingResources = ParameterExtractor.optionalBooleanBodyParameter(paramMap, Parameters.UPDATE).getOrElse(true)
        val counts = communityResourceManager.saveCommunityResourcesInBulk(communityId, fileToStore, updateMatchingResources)
        response = ResponseConstructor.constructJsonResponse(JsonUtil.jsUpdateCounts(counts._1, counts._2).toString())
      }
    } else {
      response = ResponseConstructor.constructBadRequestResponse(500, "No file provided for the resource.")
    }
    response
  }

  def deleteSystemResource(resourceId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.checkTestBedAdmin(request)
    deleteResourceInternal(resourceId)
  }

  def deleteCommunityResource(resourceId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunityResource(request, resourceId)
    deleteResourceInternal(resourceId)
  }

  private def deleteResourceInternal(resourceId: Long): Result = {
    communityResourceManager.deleteCommunityResource(resourceId)
    ResponseConstructor.constructEmptyResponse
  }

  def deleteSystemResources(): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.checkTestBedAdmin(request)
    deleteResourcesInternal(Constants.DefaultCommunityId, request)
  }

  def deleteCommunityResources(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    deleteResourcesInternal(communityId, request)
  }

  private def deleteResourcesInternal(communityId: Long, request: RequestWithAttributes[AnyContent]): Result = {
    val resourceIds = ParameterExtractor.extractIdsBodyParameter(request)
    communityResourceManager.deleteCommunityResources(communityId, resourceIds)
    ResponseConstructor.constructEmptyResponse
  }

  def downloadSystemResources(): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.checkTestBedAdmin(request)
    downloadResourcesInternal(Constants.DefaultCommunityId, request)
  }

  def downloadCommunityResources(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    downloadResourcesInternal(communityId, request)
  }

  private def downloadResourcesInternal(communityId: Long, request: RequestWithAttributes[AnyContent]): Result = {
    val filter = ParameterExtractor.optionalQueryParameter(request, Parameters.FILTER)
    var archive: Option[Path] = None
    try {
      archive = Some(communityResourceManager.createCommunityResourceArchive(communityId, filter))
      Ok.sendFile(
        content = archive.get.toFile,
        fileName = _ => Some("resources.zip"),
        onClose = () => {
          if (archive.isDefined) {
            FileUtils.deleteQuietly(archive.get.toFile)
          }
        }
      )
    } catch {
      case e: Exception =>
        if (archive.isDefined) {
          FileUtils.deleteQuietly(archive.get.toFile)
        }
        throw e
    }
  }

  def getSystemResources(): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.checkTestBedAdmin(request)
    getResourcesInternal(Constants.DefaultCommunityId)
  }

  def getCommunityResources(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    getResourcesInternal(communityId)
  }

  private def getResourcesInternal(communityId: Long): Result = {
    val result = communityResourceManager.getCommunityResources(communityId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsCommunityResources(result).toString)
  }

  def searchSystemResources(): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.checkTestBedAdmin(request)
    searchResourcesInternal(Constants.DefaultCommunityId, request)
  }

  def searchCommunityResources(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    searchResourcesInternal(communityId, request)
  }

  private def searchResourcesInternal(communityId: Long, request: RequestWithAttributes[AnyContent]): Result = {
    val filter = ParameterExtractor.optionalQueryParameter(request, Parameters.FILTER)
    val page = ParameterExtractor.optionalQueryParameter(request, Parameters.PAGE) match {
      case Some(v) => v.toLong
      case None => 1L
    }
    val limit = ParameterExtractor.optionalQueryParameter(request, Parameters.LIMIT) match {
      case Some(v) => v.toLong
      case None => 10L
    }
    val result = communityResourceManager.searchCommunityResources(communityId, page, limit, filter)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsCommunityResourceSearchResult(result._1, result._2).toString)
  }

  def downloadSystemResourceByName(resourceName: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canViewSystemResource(request)
    val resource = communityResourceManager.getSystemResourceFileByName(resourceName)
    downloadResourceByNameInternal(resourceName, resource)
  }

  def downloadCommunityResourceByName(resourceName: String): Action[AnyContent] = authorizedAction { request =>
    val communityId = request.cookies.get("implicitCommunity").map(_.value.toLong)
    if (communityId.isDefined) {
      authorizationManager.canViewCommunityBasic(request, communityId.get)
    } else {
      authorizationManager.canViewOwnCommunity(request)
    }
    val userId = authorizationManager.getRequestUserId(request)
    val resource = communityResourceManager.getCommunityResourceFileByName(communityId, userId, resourceName)
    downloadResourceByNameInternal(resourceName, resource)
  }

  private def downloadResourceByNameInternal(resourceName: String, resourceFile: Option[File]) = {
    if (resourceFile.isDefined && resourceFile.get.exists()) {
      Ok.sendFile(
        content = resourceFile.get,
        fileName = _ => Some(resourceName),
      )
    } else {
      NotFound
    }
  }

  def downloadSystemResourceById(resourceId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.checkTestBedAdmin(request)
    downloadResourceByIdInternal(resourceId)
  }

  def downloadCommunityResourceById(resourceId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunityResource(request, resourceId)
    downloadResourceByIdInternal(resourceId)
  }

  private def downloadResourceByIdInternal(resourceId: Long): Result = {
    val resource = communityResourceManager.getCommunityResourceFileById(resourceId)
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
