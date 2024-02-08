package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import managers.{ActorManager, AuthorizationManager, CommunityLabelManager}
import models.BadgeInfo
import models.Enums.{LabelType, TestResultStatus}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.RepositoryUtils

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ActorService @Inject() (implicit ec: ExecutionContext, authorizedAction: AuthorizedAction, cc: ControllerComponents, repositoryUtils: RepositoryUtils, actorManager: ActorManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager) extends AbstractController(cc) {

  def deleteActor(actorId: Long) = authorizedAction { request =>
    authorizationManager.canDeleteActor(request, actorId)
    actorManager.deleteActorWrapper(actorId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateActor(actorId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateActor(request, actorId)
    val paramMap = ParameterExtractor.paramMap(request)
    val actor = ParameterExtractor.extractActor(paramMap)
    val specificationId = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SPECIFICATION_ID).toLong
    if (actorManager.checkActorExistsInSpecification(actor.actorId, specificationId, Some(actorId))) {
      val labels = communityLabelManager.getLabelsByUserId(ParameterExtractor.extractUserId(request))
      ResponseConstructor.constructBadRequestResponse(500, communityLabelManager.getLabel(labels, LabelType.Actor) + " with this ID already exists in the " + communityLabelManager.getLabel(labels, LabelType.Specification, single = true, lowercase = true)+".")
    } else {
      val badgeInfo = ParameterExtractor.extractBadges(request, paramMap, forReport = false)
      if (badgeInfo._2.nonEmpty) {
        badgeInfo._2.get
      } else {
        val badgeInfoForReport = ParameterExtractor.extractBadges(request, paramMap, forReport = true)
        if (badgeInfoForReport._2.nonEmpty) {
          badgeInfoForReport._2.get
        } else {
          actorManager.updateActorWrapper(actorId, actor.actorId, actor.name, actor.description, actor.default, actor.hidden, actor.displayOrder, specificationId, BadgeInfo(badgeInfo._1.get, badgeInfoForReport._1.get))
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getBadgeForStatus(specId: Long, actorId: Long, status: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageSpecification(request, specId)
    val forReport = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.REPORT)
    val statusToLookup = TestResultStatus.withName(status).toString
    val badge = repositoryUtils.getConformanceBadge(specId, Some(actorId), None, statusToLookup, exactMatch = true, forReport.getOrElse(false))
    if (badge.isDefined && badge.get.exists()) {
      Ok.sendFile(content = badge.get)
    } else {
      NotFound
    }
  }

}
