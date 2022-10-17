package managers

import models.Enums.TriggerDataType

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

  def checkParameterExistsForEndpoint(parameterKey: String, endpointId: Long, otherThanId: Option[Long]): Boolean = {
    var parameterQuery = PersistenceSchema.parameters
      .filter(_.testKey === parameterKey)
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
          existingParameterData <- PersistenceSchema.parameters
            .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
            .join(PersistenceSchema.actors).on(_._2.actor === _.id)
            .filter(_._1._1.id === parameterId)
            .map(x => (x._1._1.endpoint, x._1._1.testKey, x._1._1.kind, x._2.domain)) // Endpoint ID, Parameter key, Parameter kind, Domain ID
            .result.head
          _ <- {
            if ("SIMPLE".equals(existingParameterData._3)) {
              setParameterPrerequisitesForKey(existingParameterData._1, existingParameterData._2, None)
            } else {
              DBIO.successful(())
            }
          }
          triggerDataItemsReferringToParameter <- PersistenceSchema.triggerData
            .filter(_.dataId === parameterId)
            .filter(_.dataType === TriggerDataType.StatementParameter.id.toShort)
            .result
            .headOption
          replacementParameterId <- {
            // See if there is a parameter linked to the same domain with the same name.
            // If yes, set the parameter reference in the trigger data items to that.
            // If no, just delete the trigger data items.
            if (triggerDataItemsReferringToParameter.isDefined) {
              PersistenceSchema.parameters
                .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
                .join(PersistenceSchema.actors).on(_._2.actor === _.id)
                .filter(_._2.domain === existingParameterData._4)
                .filter(_._1._1.testKey === existingParameterData._2)
                .filter(_._1._1.id =!= parameterId)
                .map(_._1._1.id)
                .result.headOption
            } else {
              DBIO.successful(None)
            }
          }
          _ <- {
            val dbActions = ListBuffer[DBIO[_]]()
            if (replacementParameterId.isDefined && triggerDataItemsReferringToParameter.isDefined) {
              // Replace the parameter ID reference in the trigger data items.
              dbActions += PersistenceSchema.triggerData
                .filter(_.dataId === parameterId)
                .filter(_.dataType === TriggerDataType.StatementParameter.id.toShort)
                .map(_.dataId)
                .update(replacementParameterId.get)
            } else if (triggerDataItemsReferringToParameter.isDefined) {
              // Delete the trigger data items.
              dbActions += PersistenceSchema.triggerData
                .filter(_.dataId === parameterId)
                .filter(_.dataType === TriggerDataType.StatementParameter.id.toShort)
                .delete
            }
            toDBIO(dbActions)
          }
        } yield ()) andThen
      PersistenceSchema.parameters.filter(_.id === parameterId).delete
  }

  def updateParameterWrapper(parameterId: Long, name: String, testKey: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean, hidden: Boolean, allowedValues: Option[String], dependsOn: Option[String], dependsOnValue: Option[String], defaultValue: Option[String]): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = updateParameter(parameterId, name, testKey, description, use, kind, adminOnly, notForTests, hidden, allowedValues, dependsOn, dependsOnValue, defaultValue, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def getParameterById(parameterId: Long) = {
    exec(PersistenceSchema.parameters.filter(_.id === parameterId).result.headOption)
  }

  private def setParameterPrerequisitesForKey(endpointId: Long, testKey: String, newTestKey: Option[String]): DBIO[_] = {
    if (newTestKey.isDefined) {
      val q = for {p <- PersistenceSchema.parameters.filter(_.endpoint === endpointId).filter(_.dependsOn === testKey)} yield p.dependsOn
      q.update(newTestKey)
    } else {
      val q = for {p <- PersistenceSchema.parameters.filter(_.endpoint === endpointId).filter(_.dependsOn === testKey)} yield (p.dependsOn, p.dependsOnValue)
      q.update(None, None)
    }
  }

  def updateParameter(parameterId: Long, name: String, testKey: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean, hidden: Boolean, allowedValues: Option[String], dependsOn: Option[String], dependsOnValue: Option[String], defaultValue: Option[String], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      existingParameter <- PersistenceSchema.parameters.filter(_.id === parameterId).map(x => (x.endpoint, x.testKey, x.kind)).result.head
      _ <- {
        if (!existingParameter._2.equals(testKey)) {
          // Update the dependsOn of other properties.
          setParameterPrerequisitesForKey(existingParameter._1, existingParameter._2, Some(testKey))
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
        val q = for {p <- PersistenceSchema.parameters if p.id === parameterId} yield (p.desc, p.use, p.kind, p.name, p.testKey, p.adminOnly, p.notForTests, p.hidden, p.allowedValues, p.dependsOn, p.dependsOnValue, p.defaultValue)
        q.update(description, use, kind, name, testKey, adminOnly, notForTests, hidden, allowedValues, dependsOn, dependsOnValue, defaultValue)
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
