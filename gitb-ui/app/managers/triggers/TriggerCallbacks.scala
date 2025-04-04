package managers.triggers

import managers.triggers.CacheCata.{fromCache, fromCacheWithCache}

import scala.concurrent.{ExecutionContext, Future}

object TriggerCallbacks {

  def newInstance(triggerDataLoader: TriggerDataLoader)(implicit ec: ExecutionContext): TriggerCallbacks = {
    new TriggerCallbacks(triggerDataLoader, None, None, None, None)
  }

  def organisationId(cache: Map[String, Any], callbacks: Option[TriggerCallbacks]): Future[CacheData[Option[Long]]] = {
    if (callbacks.isDefined) {
      callbacks.get.organisationId(cache)
    } else {
      Future.successful(CacheData(cache, None))
    }
  }

  def systemId(cache: Map[String, Any], callbacks: Option[TriggerCallbacks]): Future[CacheData[Option[Long]]] = {
    if (callbacks.isDefined) {
      callbacks.get.systemId(cache)
    } else {
      Future.successful(CacheData(cache, None))
    }
  }

  def actorId(cache: Map[String, Any], callbacks: Option[TriggerCallbacks]): Future[CacheData[Option[Long]]] = {
    if (callbacks.isDefined) {
      callbacks.get.actorId(cache)
    } else {
      Future.successful(CacheData(cache, None))
    }
  }

  def testSessionId(cache: Map[String, Any], callbacks: Option[TriggerCallbacks]): Future[CacheData[Option[String]]] = {
    if (callbacks.isDefined) {
      callbacks.get.testSessionId(cache)
    } else {
      Future.successful(CacheData(cache, None))
    }
  }

}

class TriggerCallbacks(
    var triggerDataLoader: TriggerDataLoader,
    var fnOrganisation: Option[() => Long],
    var fnSystem: Option[() => Long],
    var fnActor: Option[() => Long],
    var fnTestSession: Option[() => String]
)(implicit ec: ExecutionContext) {

  private def recordSessionLoadedIds(cache: Map[String, Any], ids: (Option[Long], Option[Long], Option[Long], Option[Long])): Future[Map[String, Any]] = {
    var updatedCache = cache
    if (!updatedCache.contains("organisationId")) updatedCache = updatedCache + ("organisationId" -> ids._1)
    if (!updatedCache.contains("systemId")) updatedCache = updatedCache + ("systemId" -> ids._2)
    if (!updatedCache.contains("actorId")) updatedCache = updatedCache + ("actorId" -> ids._3)
    if (!updatedCache.contains("specificationId")) updatedCache = updatedCache + ("specificationId" -> ids._4)
    Future.successful(updatedCache)
  }

  def organisationId(cache: Map[String, Any]): Future[CacheData[Option[Long]]] = {
    fromCacheWithCache(cache, "organisationId", () => {
      if (fnOrganisation.isDefined) {
        Future.successful(CacheData(cache, Some(fnOrganisation.get.apply())))
      } else if (fnSystem.isDefined) {
        for {
          systemIdLookup <- systemId(cache)
          cache <- Future.successful(systemIdLookup.cache)
          systemId <- Future.successful(systemIdLookup.data)
          organisationId <- {
            if (systemId.isDefined) {
              triggerDataLoader.getOrganisationUsingSystem(systemId.get)
            } else {
              Future.successful(None)
            }
          }
        } yield CacheData(cache, organisationId)
      } else if (fnTestSession.isDefined) {
        for {
          sessionLookup <- testSessionId(cache)
          cache <- Future.successful(sessionLookup.cache)
          session <- Future.successful(sessionLookup.data)
          organisationIdEntry <- {
            if (session.isDefined) {
              triggerDataLoader.getIdsUsingTestSession(session.get).flatMap { ids =>
                for {
                  cache <- recordSessionLoadedIds(cache, ids)
                  organisationId <- Future.successful(ids._1)
                } yield CacheData(cache, organisationId)
              }
            } else {
              Future.successful(CacheData(cache, Option.empty[Long]))
            }
          }
        } yield organisationIdEntry
      } else {
        Future.successful(CacheData(cache, None))
      }
    })
  }

  def systemId(cache: Map[String, Any]): Future[CacheData[Option[Long]]] = {
    fromCacheWithCache(cache, "systemId", () => {
      if (fnSystem.isDefined) {
        Future.successful(CacheData(cache, Some(fnSystem.get.apply())))
      } else if (fnTestSession.isDefined) {
        for {
          sessionLookup <- testSessionId(cache)
          cache <- Future.successful(sessionLookup.cache)
          session <- Future.successful(sessionLookup.data)
          systemIdEntry <- {
            if (session.isDefined) {
              triggerDataLoader.getIdsUsingTestSession(session.get).flatMap { ids =>
                for {
                  cache <- recordSessionLoadedIds(cache, ids)
                  systemId <- Future.successful(ids._2)
                } yield CacheData(cache, systemId)
              }
            } else {
              Future.successful(CacheData(cache, Option.empty[Long]))
            }
          }
        } yield systemIdEntry
      } else {
        Future.successful(CacheData(cache, None))
      }
    })
  }

  def actorId(cache: Map[String, Any]): Future[CacheData[Option[Long]]] = {
    fromCacheWithCache(cache, "actorId", () => {
      if (fnActor.isDefined) {
        Future.successful(CacheData(cache, Some(fnActor.get.apply())))
      } else if (fnTestSession.isDefined) {
        for {
          sessionLookup <- testSessionId(cache)
          cache <- Future.successful(sessionLookup.cache)
          session <- Future.successful(sessionLookup.data)
          actorIdEntry <- {
            if (session.isDefined) {
              triggerDataLoader.getIdsUsingTestSession(session.get).flatMap { ids =>
                for {
                  cache <- recordSessionLoadedIds(cache, ids)
                  actorId <- Future.successful(ids._3)
                } yield CacheData(cache, actorId)
              }
            } else {
              Future.successful(CacheData(cache, Option.empty[Long]))
            }
          }
        } yield actorIdEntry
      } else {
        Future.successful(CacheData(cache, None))
      }
    })
  }

  def testSessionId(cache: Map[String, Any]): Future[CacheData[Option[String]]] = {
    fromCache(cache, "testSessionId", () => {
      var value: Option[String] = None
      if (fnTestSession.isDefined) {
        value = Some(fnTestSession.get.apply())
      }
      Future.successful(value)
    })
  }

}
