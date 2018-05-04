package controllers

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import managers.ActorManager
import play.api.mvc.{Action, Controller}

class ActorService extends Controller {

  def deleteActor(actorId: Long) = Action.apply { request =>
    ActorManager.deleteActorWrapper(actorId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateActor(actorId: Long) = Action.apply { request =>
    val actor = ParameterExtractor.extractActor(request)
    val specificationId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPECIFICATION_ID).toLong

    if (ActorManager.checkActorExistsInSpecification(actor.actorId, specificationId, Some(actorId))) {
      ResponseConstructor.constructBadRequestResponse(500, "An actor with this ID already exists in the specification")
    } else {
      ActorManager.updateActorWrapper(actorId, actor.actorId, actor.name, actor.description)
      ResponseConstructor.constructEmptyResponse
    }
  }
}
