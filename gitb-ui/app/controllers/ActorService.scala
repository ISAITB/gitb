package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import javax.inject.Inject
import managers.{ActorManager, AuthorizationManager, CommunityLabelManager}
import models.Enums.LabelType
import play.api.mvc.{AbstractController, ControllerComponents}

class ActorService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, actorManager: ActorManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager) extends AbstractController(cc) {

  def deleteActor(actorId: Long) = authorizedAction { request =>
    authorizationManager.canDeleteActor(request, actorId)
    actorManager.deleteActorWrapper(actorId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateActor(actorId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateActor(request, actorId)
    val actor = ParameterExtractor.extractActor(request)
    val specificationId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPECIFICATION_ID).toLong

    if (actorManager.checkActorExistsInSpecification(actor.actorId, specificationId, Some(actorId))) {
      val labels = communityLabelManager.getLabels(request)
      ResponseConstructor.constructBadRequestResponse(500, communityLabelManager.getLabel(labels, LabelType.Actor) + " with this ID already exists in the " + communityLabelManager.getLabel(labels, LabelType.Specification, true, true)+".")
    } else {
      actorManager.updateActorWrapper(actorId, actor.actorId, actor.name, actor.description, actor.default, actor.hidden, actor.displayOrder, specificationId)
      ResponseConstructor.constructEmptyResponse
    }
  }
}
