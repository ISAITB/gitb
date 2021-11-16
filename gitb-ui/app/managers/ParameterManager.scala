package managers

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.RepositoryUtils

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ParameterManager @Inject() (repositoryUtils: RepositoryUtils, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {
  def logger = LoggerFactory.getLogger("ParameterManager")

  import dbConfig.profile.api._

  def checkParameterExistsForEndpoint(parameterName: String, endpointId: Long, otherThanId: Option[Long]): Boolean = {
    var parameterQuery = PersistenceSchema.parameters
      .filter(_.name === parameterName)
      .filter(_.endpoint === endpointId)
    if (otherThanId.isDefined) {
      parameterQuery = parameterQuery.filter(_.id =!= otherThanId.get)
    }
    exec(parameterQuery.result.headOption).isDefined
  }

  def createParameterWrapper(parameter: models.Parameters): Long = {
    exec(createParameter(parameter).transactionally)
  }

  def createParameter(parameter: models.Parameters): DBIO[Long] = {
    PersistenceSchema.parameters.returning(PersistenceSchema.parameters.map(_.id)) += parameter
  }

  def deleteParameterByEndPoint(endPointId: Long, onSuccessCalls: mutable.ListBuffer[() => _]) = {
    val action = for {
      ids <- PersistenceSchema.parameters.filter(_.endpoint === endPointId).map(_.id).result
      _ <- DBIO.seq(ids.map(id => delete(id, onSuccessCalls)): _*)
    } yield()
    action
  }

  def deleteParameter(parameterId: Long) = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = delete(parameterId, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def delete(parameterId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    onSuccessCalls += (() => repositoryUtils.deleteStatementParametersFolder(parameterId))
    PersistenceSchema.configs.filter(_.parameter === parameterId).delete andThen
        (for {
          existingParameter <- PersistenceSchema.parameters.filter(_.id === parameterId).map(x => (x.endpoint, x.name, x.kind)).result.head
          _ <- {
            if (existingParameter._3.equals("SIMPLE")) {
              setParameterPrerequisitesForKey(existingParameter._1, existingParameter._2, None)
            } else {
              DBIO.successful(())
            }
          }
        } yield ()) andThen
      PersistenceSchema.parameters.filter(_.id === parameterId).delete
  }

  def updateParameterWrapper(parameterId: Long, name: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean, hidden: Boolean, allowedValues: Option[String], dependsOn: Option[String], dependsOnValue: Option[String]): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = updateParameter(parameterId, name, description, use, kind, adminOnly, notForTests, hidden, allowedValues, dependsOn, dependsOnValue, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def getParameterById(parameterId: Long) = {
    exec(PersistenceSchema.parameters.filter(_.id === parameterId).result.headOption)
  }

  private def setParameterPrerequisitesForKey(endpointId: Long, name: String, newName: Option[String]): DBIO[_] = {
    if (newName.isDefined) {
      val q = for {p <- PersistenceSchema.parameters.filter(_.endpoint === endpointId).filter(_.dependsOn === name)} yield p.dependsOn
      q.update(newName)
    } else {
      val q = for {p <- PersistenceSchema.parameters.filter(_.endpoint === endpointId).filter(_.dependsOn === name)} yield (p.dependsOn, p.dependsOnValue)
      q.update(None, None)
    }
  }

  def updateParameter(parameterId: Long, name: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean, hidden: Boolean, allowedValues: Option[String], dependsOn: Option[String], dependsOnValue: Option[String], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      existingParameter <- PersistenceSchema.parameters.filter(_.id === parameterId).map(x => (x.endpoint, x.name, x.kind)).result.head
      _ <- {
        if (!existingParameter._2.equals(name)) {
          // Update the dependsOn of other properties.
          setParameterPrerequisitesForKey(existingParameter._1, existingParameter._2, Some(name))
        } else if (existingParameter._3.equals("SIMPLE") && !existingParameter._3.equals(kind)) {
          // Remove the dependsOn of other properties.
          setParameterPrerequisitesForKey(existingParameter._1, existingParameter._2, None)
        } else {
          DBIO.successful(())
        }
      }
      _ <- {
        if (existingParameter._3 != kind) {
          // Remove previous values.
          onSuccessCalls += (() => repositoryUtils.deleteStatementParametersFolder(parameterId))
          PersistenceSchema.configs.filter(_.parameter === parameterId).delete
        } else {
          DBIO.successful(())
        }
      }
      _ <- {
        // Don't update display order here.
        val q = for {p <- PersistenceSchema.parameters if p.id === parameterId} yield (p.desc, p.use, p.kind, p.name, p.adminOnly, p.notForTests, p.hidden, p.allowedValues, p.dependsOn, p.dependsOnValue)
        q.update(description, use, kind, name, adminOnly, notForTests, hidden, allowedValues, dependsOn, dependsOnValue)
      }
    } yield ()
  }

  def orderParameters(endpointId: Long, orderedIds: List[Long]): Unit = {
    val dbActions = ListBuffer[DBIO[_]]()
    var counter = 0
    orderedIds.foreach { id =>
      counter += 1
      val q = for { p <- PersistenceSchema.parameters.filter(_.endpoint === endpointId).filter(_.id === id) } yield p.displayOrder
      dbActions += q.update(counter.toShort)
    }
    exec(toDBIO(dbActions).transactionally)
  }

}
