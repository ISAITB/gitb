package managers

import javax.inject.{Inject, Singleton}
import models.{Constants, Specifications}
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.CryptoUtil

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SpecificationManager @Inject() (actorManager: ActorManager, testResultManager: TestResultManager, testSuiteManager: TestSuiteManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {
  def logger = LoggerFactory.getLogger("SpecificationManager")

  import dbConfig.profile.api._

  /**
   * Checks if domain exists
   */
  def checkSpecifiationExists(specId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.specifications.filter(_.id === specId).result.headOption)
    firstOption.isDefined
  }

  def getSpecificationById(specId: Long): Specifications = {
    val spec = exec(PersistenceSchema.specifications.filter(_.id === specId).result.head)
    spec
  }

  def getSpecificationsById(specIds: List[Long]): List[Specifications] = {
    exec(PersistenceSchema.specifications.filter(_.id inSet specIds).result.map(_.toList))
  }

  def getSpecificationByApiKeys(apiKey: String, communityApiKey: String): Option[Specifications] = {
    exec(for {
      communityIds <- {
        PersistenceSchema.communities.filter(_.apiKey === communityApiKey).map(x => (x.id, x.domain)).result.headOption
      }
      relevantDomainId <- {
        if (communityIds.isDefined) {
          var domainId: Option[Long] = None
          if (communityIds.get._1 != Constants.DefaultCommunityId) {
            domainId = communityIds.get._2
          }
          DBIO.successful(domainId)
        } else {
          throw new IllegalStateException("Community not found for provided API key")
        }
      }
      specification <- PersistenceSchema.specifications.filter(_.apiKey === apiKey).filterOpt(relevantDomainId)((q, id) => q.domain === id).result.headOption
    } yield specification)
  }

  def updateSpecificationInternal(specId: Long, sname: String, fname: String, descr: Option[String], hidden:Boolean, apiKey: Option[String], checkApiKeyUniqueness: Boolean): DBIO[_] = {
    for {
      _ <- {
        val q = for {s <- PersistenceSchema.specifications if s.id === specId} yield (s.shortname, s.fullname, s.description, s.hidden)
        q.update(sname, fname, descr, hidden) andThen
          testResultManager.updateForUpdatedSpecification(specId, sname)
      }
      replaceApiKey <- {
        if (apiKey.isDefined && checkApiKeyUniqueness) {
          PersistenceSchema.specifications.filter(_.apiKey === apiKey.get).filter(_.id =!= specId).exists.result
        } else {
          DBIO.successful(false)
        }
      }
      _ <- {
        if (apiKey.isDefined) {
          val apiKeyToUse = if (replaceApiKey) CryptoUtil.generateApiKey() else apiKey.get
          PersistenceSchema.specifications.filter(_.id === specId).map(_.apiKey).update(apiKeyToUse)
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
  }

  def updateSpecification(specId: Long, sname: String, fname: String, descr: Option[String], hidden:Boolean) = {
    exec(updateSpecificationInternal(specId, sname, fname, descr, hidden, None, checkApiKeyUniqueness = false).transactionally)
  }

  def getSpecificationIdOfActor(actorId: Long) = {
    exec(PersistenceSchema.specificationHasActors.filter(_.actorId === actorId).result.head)._1
  }

  def getSpecificationOfActor(actorId: Long) = {
    val query = PersistenceSchema.specifications
        .join(PersistenceSchema.specificationHasActors).on(_.id === _.specId)
    exec(query.filter(_._2.actorId === actorId).result.head)._1
  }

}
