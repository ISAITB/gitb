/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package managers

import exceptions.{AutomationApiException, ErrorCodes}
import models.Endpoints
import models.Enums.TriggerDataType
import models.automation.CustomPropertyInfo
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{JsonUtil, RepositoryUtils}

import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ParameterManager @Inject() (repositoryUtils: RepositoryUtils,
                                  automationApiHelper: AutomationApiHelper,
                                  dbConfigProvider: DatabaseConfigProvider)
                                 (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def checkParameterExistsForEndpoint(parameterKey: String, endpointId: Long, otherThanId: Option[Long]): Future[Boolean] = {
    var parameterQuery = PersistenceSchema.parameters
      .filter(_.testKey === parameterKey)
      .filter(_.endpoint === endpointId)
    if (otherThanId.isDefined) {
      parameterQuery = parameterQuery.filter(_.id =!= otherThanId.get)
    }
    DB.run(parameterQuery.exists.result)
  }

  def createParameterAndEndpoint(parameter: models.Parameters, actorId: Long): Future[(Long, Long)] = {
    val dbAction = for {
      endpointId <- PersistenceSchema.endpoints.returning(PersistenceSchema.endpoints.map(_.id)) += Endpoints(0L, "config", None, actorId)
      parameterId <- createParameter(parameter.withEndpoint(endpointId, None))
    } yield (endpointId, parameterId)
    DB.run(dbAction.transactionally)
  }

  def createParameterWrapper(parameter: models.Parameters): Future[Long] = {
    DB.run(createParameter(parameter).transactionally)
  }

  private def checkParameterExistence(endpointId: Option[Long], parameterKey: String, expectedToExist: Boolean, parameterIdToIgnore: Option[Long]): DBIO[Option[models.Parameters]] = {
    if (endpointId.isEmpty) {
      if (expectedToExist) {
        throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "No property with name [%s] exists in the target actor".formatted(parameterKey))
      } else {
        DBIO.successful(None)
      }
    } else {
      for {
        parameter <- PersistenceSchema.parameters
          .filter(_.endpoint === endpointId.get)
          .filter(_.testKey === parameterKey)
          .filterOpt(parameterIdToIgnore)((q, id) => q.id =!= id)
          .result
          .headOption
        _ <- {
          if (parameter.isDefined && !expectedToExist) {
            throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "A property with name [%s] already exists in the target actor".formatted(parameterKey))
          } else if (parameter.isEmpty && expectedToExist) {
            throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "No property with name [%s] exists in the target actor".formatted(parameterKey))
          } else {
            DBIO.successful(())
          }
        }
      } yield parameter
    }
  }

  private def checkDependedParameterExistence(endpointId: Option[Long], dependsOn: Option[String], parameterIdToIgnore: Option[Long]): DBIO[Option[models.Parameters]] = {
    if (dependsOn.isEmpty) {
      DBIO.successful(None)
    } else {
      for {
        parameter <- checkParameterExistence(endpointId, dependsOn.get, expectedToExist = true, parameterIdToIgnore)
        _ <- {
          if (parameter.get.kind != "SIMPLE") {
            throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "Property [%s] upon which this property depends on must be of simple type".formatted(dependsOn.get))
          } else {
            DBIO.successful(())
          }
        }
      } yield parameter
    }
  }

  def createParameterDefinitionThroughAutomationApi(communityApiKey: String, actorApiKey: String, input: CustomPropertyInfo): Future[Unit] = {
    val dbAction = for {
      // Load community IDs.
      communityIds <- automationApiHelper.getCommunityIdsByCommunityApiKey(communityApiKey)
      // Load actor and endpoint IDs.
      actorIds <- automationApiHelper.getActorIdsByDomainId(communityIds._2, actorApiKey, endpointRequired = false)
      // Check for existing property with provided name.
      _ <- checkParameterExistence(actorIds._2, input.key, expectedToExist = false, None)
      // If this depends on another property check that it exists.
      dependency <- checkDependedParameterExistence(actorIds._2, input.dependsOn.flatten, None)
      // Determine endpoint ID.
      endpointIdToUse <- {
        if (actorIds._2.isEmpty) {
          // Create an endpoint on the fly.
          PersistenceSchema.endpoints.returning(PersistenceSchema.endpoints.map(_.id)) += Endpoints(0L, "config", None, actorIds._1)
        } else {
          // Use the existing one.
          DBIO.successful(actorIds._2.get)
        }
      }
      // Create property.
      _ <- {
        val dependsOnStatus = automationApiHelper.propertyDependsOnStatus(input, dependency.flatMap(_.allowedValues))
        createParameter(models.Parameters(0L,
          input.name.getOrElse(input.key),
          input.key,
          input.description.flatten,
          automationApiHelper.propertyUseText(input.required),
          "SIMPLE",
          !input.editableByUsers.getOrElse(true),
          !input.inTests.getOrElse(false),
          input.hidden.getOrElse(false),
          automationApiHelper.propertyAllowedValuesText(input.allowedValues.flatten),
          input.displayOrder.getOrElse(0),
          dependsOnStatus._1.flatten,
          dependsOnStatus._2.flatten,
          automationApiHelper.propertyDefaultValue(input.defaultValue.flatten, input.allowedValues.flatten),
          endpointIdToUse
        ))
      }
    } yield ()
    DB.run(dbAction.transactionally)
  }

  def updateParameterDefinitionThroughAutomationApi(communityApiKey: String, actorApiKey: String, input: CustomPropertyInfo): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      // Load community IDs.
      communityIds <- automationApiHelper.getCommunityIdsByCommunityApiKey(communityApiKey)
      // Load actor and endpoint IDs.
      actorIds <- automationApiHelper.getActorIdsByDomainId(communityIds._2, actorApiKey, endpointRequired = true)
      // Check for existing property with provided name.
      parameter <- checkParameterExistence(actorIds._2, input.key, expectedToExist = true, None)
      // If this depends on another property check that it exists.
      dependency <- checkDependedParameterExistence(actorIds._2, input.dependsOn.flatten, parameter.map(_.id))
      // Proceed with update.
      _ <- {
        val dependsOnStatus = automationApiHelper.propertyDependsOnStatus(input, dependency.flatMap(_.allowedValues))
        updateParameter(
          parameter.get.id,
          input.name.getOrElse(parameter.get.name),
          input.key,
          input.description.getOrElse(parameter.get.desc),
          automationApiHelper.propertyUseText(input.required, parameter.get.use),
          "SIMPLE",
          !input.editableByUsers.getOrElse(!parameter.get.adminOnly),
          !input.inTests.getOrElse(!parameter.get.notForTests),
          input.hidden.getOrElse(parameter.get.hidden),
          input.allowedValues.map(x => automationApiHelper.propertyAllowedValuesText(x)).getOrElse(parameter.get.allowedValues),
          dependsOnStatus._1.getOrElse(parameter.get.dependsOn),
          dependsOnStatus._2.getOrElse(parameter.get.dependsOnValue),
          automationApiHelper.propertyDefaultValue(
            input.defaultValue.getOrElse(parameter.get.defaultValue),
            input.allowedValues.getOrElse(parameter.get.allowedValues.map(x => JsonUtil.parseJsAllowedPropertyValues(x)))
          ),
          input.displayOrder.orElse(Some(parameter.get.displayOrder)),
          onSuccessCalls
        )
      }
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction.transactionally))
  }

  def deleteParameterDefinitionThroughAutomationApi(communityApiKey: String, actorApiKey: String, parameterApiKey: String): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      // Load community IDs.
      communityIds <- automationApiHelper.getCommunityIdsByCommunityApiKey(communityApiKey)
      // Load actor and endpoint IDs.
      actorIds <- automationApiHelper.getActorIdsByDomainId(communityIds._2, actorApiKey, endpointRequired = true)
      // Check for existing property with provided name.
      parameter <- checkParameterExistence(actorIds._2, parameterApiKey, expectedToExist = true, None)
      // Delete property.
      _ <- {
        delete(parameter.get.id, onSuccessCalls)
      }
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def createParameter(parameter: models.Parameters): DBIO[Long] = {
    PersistenceSchema.parameters.returning(PersistenceSchema.parameters.map(_.id)) += parameter
  }

  def deleteParameterByEndPoint(endPointId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    val action = for {
      ids <- PersistenceSchema.parameters.filter(_.endpoint === endPointId).map(_.id).result
      _ <- DBIO.seq(ids.map(id => delete(id, onSuccessCalls)): _*)
    } yield()
    action
  }

  def deleteParameter(parameterId: Long): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = delete(parameterId, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
  }

  def delete(parameterId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      _ <- PersistenceSchema.configs.filter(_.parameter === parameterId).delete
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
      _ <- PersistenceSchema.parameters.filter(_.id === parameterId).delete
      _ <- {
        onSuccessCalls += (() => repositoryUtils.deleteStatementParametersFolder(parameterId))
        DBIO.successful(())
      }
    } yield ()
  }

  def updateParameterWrapper(parameterId: Long, name: String, testKey: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean, hidden: Boolean, allowedValues: Option[String], dependsOn: Option[String], dependsOnValue: Option[String], defaultValue: Option[String]): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = updateParameter(parameterId, name, testKey, description, use, kind, adminOnly, notForTests, hidden, allowedValues, dependsOn, dependsOnValue, defaultValue, None, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
  }

  def getParameterById(parameterId: Long): Future[Option[models.Parameters]] = {
    DB.run(PersistenceSchema.parameters.filter(_.id === parameterId).result.headOption)
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

  def updateParameter(parameterId: Long, name: String, testKey: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean, hidden: Boolean, allowedValues: Option[String], dependsOn: Option[String], dependsOnValue: Option[String], defaultValue: Option[String], displayOrder: Option[Short], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
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
      _ <- {
        if (displayOrder.isDefined) {
          PersistenceSchema.parameters.filter(_.id === parameterId).map(_.displayOrder).update(displayOrder.get)
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
  }

  def orderParameters(endpointId: Long, orderedIds: List[Long]): Future[Unit] = {
    val dbActions = ListBuffer[DBIO[_]]()
    var counter = 0
    orderedIds.foreach { id =>
      counter += 1
      val q = for { p <- PersistenceSchema.parameters.filter(_.endpoint === endpointId).filter(_.id === id) } yield p.displayOrder
      dbActions += q.update(counter.toShort)
    }
    DB.run(toDBIO(dbActions).transactionally).map(_ => ())
  }

}
