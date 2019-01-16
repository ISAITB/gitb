package controllers

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import javax.inject.Inject
import managers.ActorManager
import play.api.mvc.{Action, Controller}

class ActorService @Inject() (actorManager: ActorManager) extends Controller {

  def deleteActor(actorId: Long) = Action.apply { request =>
    actorManager.deleteActorWrapper(actorId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateActor(actorId: Long) = Action.apply { request =>
    val actor = ParameterExtractor.extractActor(request)
    val specificationId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPECIFICATION_ID).toLong

    if (actorManager.checkActorExistsInSpecification(actor.actorId, specificationId, Some(actorId))) {
      ResponseConstructor.constructBadRequestResponse(500, "An actor with this ID already exists in the specification")
    } else {
      actorManager.updateActorWrapper(actorId, actor.actorId, actor.name, actor.description, actor.default, actor.displayOrder, specificationId)
      ResponseConstructor.constructEmptyResponse
    }
  }
}
