package managers

import scala.collection.mutable

object TriggerCallbacks {

  def newInstance(data: Any, triggerDataLoader: TriggerDataLoader): TriggerCallbacks = {
    new TriggerCallbacks(data, triggerDataLoader, None, None, None, None)
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

  def testSessionId(callbacks: Option[TriggerCallbacks]): Option[String] = {
    if (callbacks.isDefined) {
      callbacks.get.testSessionId()
    } else {
      None
    }
  }

}

class TriggerCallbacks(
    var data: Any,
    var triggerDataLoader: TriggerDataLoader,
    var fnOrganisation: Option[() => Long],
    var fnSystem: Option[() => Long],
    var fnActor: Option[() => Long],
    var fnTestSession: Option[() => String],
) {

  private val dataCache = mutable.Map[String, Any]()

  private def fromCache[T](cacheKey: String, fnLoad: () => T): T = {
    if (dataCache.contains(cacheKey)) {
      dataCache(cacheKey).asInstanceOf[T]
    } else {
      val data = fnLoad.apply()
      dataCache += (cacheKey -> data)
      data
    }
  }

  private def recordSessionLoadedIds(ids: (Option[Long], Option[Long], Option[Long], Option[Long])) = {
    if (!dataCache.contains("organisationId")) dataCache.put("organisationId", ids._1)
    if (!dataCache.contains("systemId")) dataCache.put("systemId", ids._2)
    if (!dataCache.contains("actorId")) dataCache.put("actorId", ids._3)
    if (!dataCache.contains("specificationId")) dataCache.put("specificationId", ids._4)
  }

  def organisationId(): Option[Long] = {
    fromCache("organisationId", () => {
      var value: Option[Long] = None
      if (fnOrganisation.isDefined) {
        value = Some(fnOrganisation.get.apply())
      } else if (fnSystem.isDefined) {
        value = triggerDataLoader.getOrganisationUsingSystem(systemId().get)
      } else if (fnTestSession.isDefined) {
        recordSessionLoadedIds(triggerDataLoader.getIdsUsingTestSession(testSessionId().get))
        value = organisationId()
      }
      value
    })
  }

  def systemId(): Option[Long] = {
    fromCache("systemId", () => {
      var value: Option[Long] = None
      if (fnSystem.isDefined) {
        value = Some(fnSystem.get.apply())
      } else if (fnTestSession.isDefined) {
        recordSessionLoadedIds(triggerDataLoader.getIdsUsingTestSession(testSessionId().get))
        value = systemId()
      }
      value
    })
  }

  def actorId(): Option[Long] = {
    fromCache("actorId", () => {
      var value: Option[Long] = None
      if (fnActor.isDefined) {
        value = Some(fnActor.get.apply())
      } else if (fnTestSession.isDefined) {
        recordSessionLoadedIds(triggerDataLoader.getIdsUsingTestSession(testSessionId().get))
        value = actorId()
      }
      value
    })
  }

  def testSessionId(): Option[String] = {
    fromCache("testSessionId", () => {
      var value: Option[String] = None
      if (fnTestSession.isDefined) {
        value = Some(fnTestSession.get.apply())
      }
      value
    })
  }

}
