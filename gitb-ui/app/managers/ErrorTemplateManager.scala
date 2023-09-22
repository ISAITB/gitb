package managers

import javax.inject.{Inject, Singleton}
import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ErrorTemplateManager @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("ErrorTemplateManager")

  /**
   * Gets all error templates for the specified community
   */
  def getErrorTemplatesByCommunity(communityId: Long): List[ErrorTemplates] = {
    val errorTemplates = exec(PersistenceSchema.errorTemplates.filter(_.community === communityId)
        .sortBy(_.name.asc)
      .result.map(_.toList))
    errorTemplates
  }

  /**
   * Checks if name exists
   */
  def checkUniqueName(name: String, communityId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.errorTemplates.filter(_.community === communityId).filter(_.name === name).result.headOption)
    firstOption.isEmpty
  }

  /**
    * Checks if a error template with given name exists for the given community
    */
  def checkUniqueName(templateId: Long, name: String, communityId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.errorTemplates.filter(_.community === communityId).filter(_.id =!= templateId).filter(_.name === name).result.headOption)
    firstOption.isEmpty
  }

  /**
   * Gets error template with specified id
   */
  def getErrorTemplateById(templateId: Long): ErrorTemplate = {
    val r = exec(getErrorTemplateByIdInternal(templateId))
    new ErrorTemplate(r.get)
  }

  def getErrorTemplateByIdInternal(templateId: Long): DBIO[Option[ErrorTemplates]] = {
    PersistenceSchema.errorTemplates.filter(_.id === templateId).result.headOption
  }

  def getCommunityId(templateId: Long): Long = {
    exec(PersistenceSchema.errorTemplates.filter(_.id === templateId).map(x => x.community).result.head)
  }

  /**
   * Creates new error template
   */
  def createErrorTemplate(errorTemplate: ErrorTemplates) = {
    exec(createErrorTemplateInternal(errorTemplate).transactionally)
  }

  def createErrorTemplateInternal(errorTemplate: ErrorTemplates) = {
    for {
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (errorTemplate.default) {
          val q = for {l <- PersistenceSchema.errorTemplates if l.default === true && l.community === errorTemplate.community} yield (l.default)
          actions += q.update(false)
        }
        toDBIO(actions)
      }
      newId <- PersistenceSchema.insertErrorTemplate += errorTemplate
    } yield newId
  }

  /**
   * Updates error template
   */
  def updateErrorTemplate(templateId: Long, name: String, description: Option[String], content: String, default: Boolean, communityId: Long) = {
    exec(updateErrorTemplateInternal(templateId, name, description, content, default, communityId).transactionally)
  }

  def updateErrorTemplateInternal(templateId: Long, name: String, description: Option[String], content: String, default: Boolean, communityId: Long) = {
    for {
      errorTemplateOption <- PersistenceSchema.errorTemplates.filter(_.id === templateId).result.headOption
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (errorTemplateOption.isDefined) {
          val errorTemplate = errorTemplateOption.get

          if (name.nonEmpty && errorTemplate.name != name) {
            val q = for {l <- PersistenceSchema.errorTemplates if l.id === templateId} yield (l.name)
            actions += q.update(name)
          }

          if (content != errorTemplate.content) {
            val q = for {l <- PersistenceSchema.errorTemplates if l.id === templateId} yield (l.content)
            actions += q.update(content)
          }

          if (!errorTemplate.default && default) {
            var q = for {l <- PersistenceSchema.errorTemplates if l.default === true && l.community === communityId} yield (l.default)
            actions += q.update(false)

            q = for {l <- PersistenceSchema.errorTemplates if l.id === templateId} yield (l.default)
            actions += q.update(default)
          }

          val q = for {l <- PersistenceSchema.errorTemplates if l.id === templateId} yield (l.description)
          actions += q.update(description)
        }
        toDBIO(actions)
      }
    } yield ()
  }

  /**
   * Deletes error template with specified id
   */
  def deleteErrorTemplate(templateId: Long) = {
    exec(deleteErrorTemplateInternal(templateId).transactionally)
  }

  def deleteErrorTemplateInternal(templateId: Long): DBIO[_] = {
    for {
      _ <- {
        (for {
          x <- PersistenceSchema.organizations if x.errorTemplate === templateId
        } yield x.errorTemplate).update(None)

      }
      _ <- PersistenceSchema.errorTemplates.filter(_.id === templateId).delete
    } yield ()
  }

  /**
   * Gets the default error template for given community
   */
  def getCommunityDefaultErrorTemplate(communityId: Long): Option[ErrorTemplate] = {
    val n = exec(PersistenceSchema.errorTemplates.filter(_.community === communityId).filter(_.default === true).result.headOption)
    val defaultErrorTemplate = n match {
      case Some(n) => Some(new ErrorTemplate(n))
      case None => None
    }
    defaultErrorTemplate
  }

  def deleteErrorTemplateByCommunity(communityId: Long) = {
    PersistenceSchema.errorTemplates.filter(_.community === communityId).delete
  }

}