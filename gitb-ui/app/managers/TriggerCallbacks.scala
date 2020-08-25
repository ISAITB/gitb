package managers

object TriggerCallbacks {

  def newInstance(data: Any): TriggerCallbacks = {
    new TriggerCallbacks(data, None, None, None)
  }

  def organisationId(callbacks: Option[TriggerCallbacks]): Option[Long] = {
    if (callbacks.isDefined) {
      callbacks.get.organisationId()
    } else {
      None
    }
  }

  def systemId(callbacks: Option[TriggerCallbacks]): Option[Long] = {
    if (callbacks.isDefined) {
      callbacks.get.systemId()
    } else {
      None
    }
  }

  def actorId(callbacks: Option[TriggerCallbacks]): Option[Long] = {
    if (callbacks.isDefined) {
      callbacks.get.actorId()
    } else {
      None
    }
  }

}

class TriggerCallbacks(
    var data: Any,
    var fnOrganisation: Option[() => Long],
    var fnSystem: Option[() => Long],
    var fnActor: Option[() => Long],
) {

  def organisationId(): Option[Long] = {
    if (fnOrganisation.isDefined) {
      Some(fnOrganisation.get.apply())
    } else {
      None
    }
  }

  def systemId(): Option[Long] = {
    if (fnSystem.isDefined) {
      Some(fnSystem.get.apply())
    } else {
      None
    }
  }

  def actorId(): Option[Long] = {
    if (fnActor.isDefined) {
      Some(fnActor.get.apply())
    } else {
      None
    }
  }

}
