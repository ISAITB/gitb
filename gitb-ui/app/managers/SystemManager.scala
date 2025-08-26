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

import actors.events.{ConformanceStatementCreatedEvent, SystemUpdatedEvent}
import exceptions.{AutomationApiException, ErrorCodes}
import managers.triggers.TriggerHelper
import models.Enums.{TestResultStatus, UserRole}
import models._
import models.automation.{CreateSystemRequest, UpdateSystemRequest}
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider
import utils.{CryptoUtil, MimeUtil, RepositoryUtils}

import java.io.File
import java.sql.Timestamp
import java.util
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SystemManager @Inject() (repositoryUtils: RepositoryUtils,
                               apiHelper: AutomationApiHelper,
                               testResultManager: TestResultManager,
                               triggerHelper: TriggerHelper,
                               dbConfigProvider: DatabaseConfigProvider)
                              (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def copyTestSetup(fromSystem: Long, toSystem: Long, copySystemParameters: Boolean, copyStatementParameters: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[List[Long]] = {
    for {
      conformanceStatements <- getConformanceStatementReferencesInternal(fromSystem)
      linkedActorIds <- {
        val actions = new ListBuffer[DBIO[_]]()
        val linkedActorIds = ListBuffer[Long]()
        val addedStatements = scala.collection.mutable.Set[String]()
        conformanceStatements.foreach { otherConformanceStatement =>
          val key = s"${otherConformanceStatement.spec}-${otherConformanceStatement.actor}"
          if (!addedStatements.contains(key)) {
            addedStatements += key
            linkedActorIds += otherConformanceStatement.actor
            // We create default parameter values only if we are not copying the other system's parameter values.
            actions += defineConformanceStatement(toSystem, otherConformanceStatement.spec, otherConformanceStatement.actor, setDefaultParameterValues = !copyStatementParameters)
          }
        }
        toDBIO(actions).map(_ => linkedActorIds.toList)
      }
      _ <- {
        if (copySystemParameters) {
          for {
            _ <- deleteSystemParameterValues(toSystem, onSuccessCalls)
            otherValues <- PersistenceSchema.systemParameterValues.filter(_.system === fromSystem).result.map(_.toList)
            _ <- {
              val copyActions = new ListBuffer[DBIO[_]]()
              otherValues.foreach(otherValue => {
                onSuccessCalls += (() => repositoryUtils.setSystemPropertyFile(otherValue.parameter, toSystem, repositoryUtils.getSystemPropertyFile(otherValue.parameter, fromSystem), copy = true))
                copyActions += (PersistenceSchema.systemParameterValues += SystemParameterValues(toSystem, otherValue.parameter, otherValue.value, otherValue.contentType))
              })
              DBIO.seq(copyActions.toList.map(a => a): _*)
            }
          } yield ()
        } else {
          DBIO.successful(())
        }
      }
      _ <- {
        if (copyStatementParameters) {
          for {
            _ <- deleteSystemStatementConfiguration(toSystem, onSuccessCalls)
            otherValues <- PersistenceSchema.configs.filter(_.system === fromSystem).result.map(_.toList)
            _ <- {
              val copyActions = new ListBuffer[DBIO[_]]()
              otherValues.foreach(otherValue => {
                onSuccessCalls += (() => repositoryUtils.setStatementParameterFile(otherValue.parameter, toSystem, repositoryUtils.getStatementParameterFile(otherValue.parameter, fromSystem), copy = true))
                copyActions += (PersistenceSchema.configs += Configs(toSystem, otherValue.parameter, otherValue.endpoint, otherValue.value, otherValue.contentType))
              })
              DBIO.seq(copyActions.toList.map(a => a): _*)
            }
          } yield ()
        } else {
          DBIO.successful(())
        }
      }
    } yield linkedActorIds
  }

  private def getCommunityIdForOrganisationId(organisationId: Long): DBIO[Long] = {
    PersistenceSchema.organizations.filter(_.id === organisationId).map(x => x.community).result.head
  }

  def getSystemsThroughAutomationApi(communityApiKey: String, organisationApiKey: String): Future[Seq[Systems]] = {
    DB.run {
      for {
        communityId <- apiHelper.getCommunityByCommunityApiKey(communityApiKey)
        organisationId <- apiHelper.getOrganisationByOrganisationApiKey(communityId, organisationApiKey)
        systems <- PersistenceSchema.systems
          .filter(_.owner === organisationId)
          .result
      } yield systems
    }
  }

  def getSystemThroughAutomationApi(communityApiKey: String, systemApiKey: String): Future[Systems] = {
    DB.run {
      for {
        communityId <- apiHelper.getCommunityByCommunityApiKey(communityApiKey)
        system <- PersistenceSchema.systems
          .join(PersistenceSchema.organizations).on(_.owner === _.id)
          .filter(_._2.community === communityId)
          .filter(_._1.apiKey === systemApiKey)
          .map(_._1)
          .result
          .headOption
          .map { result =>
            result.getOrElse(throw AutomationApiException(ErrorCodes.API_SYSTEM_NOT_FOUND, "No system found for the provided API key"))
          }
      } yield system
    }
  }

  def createSystemThroughAutomationApi(input: CreateSystemRequest): Future[String] = {
    val action = for {
      communityId <- apiHelper.getCommunityByCommunityApiKey(input.communityApiKey)
      organisationId <- {
        for {
          organisationId <- PersistenceSchema.organizations
            .filter(_.community === communityId)
            .filter(_.apiKey === input.organisationApiKey)
            .map(_.id)
            .result
            .headOption
          _ <- {
            if (organisationId.isEmpty) {
              throw AutomationApiException(ErrorCodes.API_ORGANISATION_NOT_FOUND, "No organisation found for the provided API keys")
            } else {
              DBIO.successful(())
            }
          }
        } yield organisationId.get
      }
      apiKeyToUse <- {
        for {
          generateApiKey <- if (input.apiKey.isEmpty) {
            DBIO.successful(true)
          } else {
            PersistenceSchema.systems.filter(_.apiKey === input.apiKey.get).exists.result
          }
          apiKeyToUse <- if (generateApiKey) {
            DBIO.successful(CryptoUtil.generateApiKey())
          } else {
            DBIO.successful(input.apiKey.get)
          }
        } yield apiKeyToUse
      }
      newSystemId <- {
        registerSystemInternal(Systems(0L, input.shortName, input.fullName, input.description, input.version,
          apiKeyToUse, CryptoUtil.generateApiKey(), organisationId
        ), checkApiKeyUniqueness = false)
      }
    } yield (communityId, apiKeyToUse, newSystemId)
    DB.run(action.transactionally).map { result =>
      // Call triggers in separate transactions.
      triggerHelper.triggersFor(result._1, new SystemCreationDbInfo(result._3, Some(List[Long]())))
      // Return assigned API key.
      result._2
    }
  }

  def registerSystemWrapper(userId:Long, system: Systems, otherSystem: Option[Long], propertyValues: Option[List[SystemParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], copySystemParameters: Boolean, copyStatementParameters: Boolean): Future[(Long, List[Long], Long)] = {
    var propertyValuesToUse = propertyValues
    if (otherSystem.isDefined && copySystemParameters) {
      propertyValuesToUse = None
    }
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      communityId <- getCommunityIdForOrganisationId(system.owner)
      isAdmin <- {
        if (propertyValues.isDefined && (otherSystem.isEmpty || !copySystemParameters)) {
          for {
            user <- PersistenceSchema.users.filter(_.id === userId).result.head
            isAdmin <- DBIO.successful(Some(user.role == UserRole.SystemAdmin.id.toShort || user.role == UserRole.CommunityAdmin.id.toShort))
          } yield isAdmin
        } else {
          DBIO.successful(None)
        }
      }
      newSystemId <- registerSystem(system, communityId, isAdmin, propertyValuesToUse, propertyFiles, setPropertiesWithDefaultValues = true, onSuccessCalls)
      linkedActorIds <- {
        if (otherSystem.isDefined) {
          copyTestSetup(otherSystem.get, newSystemId, copySystemParameters, copyStatementParameters, onSuccessCalls)
        } else {
          DBIO.successful(List[Long]())
        }
      }
    } yield (newSystemId, linkedActorIds, communityId)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map { ids =>
      triggerHelper.triggersFor(ids._3, new SystemCreationDbInfo(ids._1, Some(ids._2)))
      ids
    }
  }

  def registerSystemInternal(system: Systems, checkApiKeyUniqueness: Boolean): DBIO[Long] = {
    for {
      replaceApiKey <- if (checkApiKeyUniqueness) {
        PersistenceSchema.systems.filter(_.apiKey === system.apiKey).exists.result
      } else {
        DBIO.successful(false)
      }
      replaceBadgeKey <- {
        if (system.badgeKey.isEmpty) {
          DBIO.successful(true)
        } else {
          if (checkApiKeyUniqueness) {
            PersistenceSchema.systems.filter(_.badgeKey === system.badgeKey).exists.result
          } else {
            DBIO.successful(false)
          }
        }
      }
      newSysId <- {
        var sysToUse = system
        sysToUse = if (replaceApiKey) sysToUse.withApiKey(CryptoUtil.generateApiKey()) else sysToUse
        sysToUse = if (replaceBadgeKey) sysToUse.withBadgeKey(CryptoUtil.generateApiKey()) else sysToUse
        PersistenceSchema.insertSystem += sysToUse
      }
    } yield newSysId
  }

  def registerSystem(system: Systems, communityId: Long, isAdmin: Option[Boolean], propertyValues: Option[List[SystemParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], setPropertiesWithDefaultValues: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Long] = {
    for {
      newSystemId <- registerSystemInternal(system, checkApiKeyUniqueness = false)
      _ <- {
        if (propertyValues.isDefined) {
          saveSystemParameterValues(newSystemId, communityId, isAdmin.get, propertyValues.get, propertyFiles.get, onSuccessCalls)
        } else {
          DBIO.successful(())
        }
      }
      /*
      Set properties with default values.
       */
      // 1. Determine the properties that have default values.
      propertiesWithDefaults <-
        if (setPropertiesWithDefaultValues) {
          PersistenceSchema.systemParameters
            .filter(_.community === communityId)
            .filter(_.defaultValue.isDefined)
            .map(x => (x.id, x.defaultValue.get))
            .result
        } else {
          DBIO.successful(Seq.empty)
        }
      // 2. See which of these properties have values.
      propertiesWithDefaultsThatAreSet <-
        if (propertiesWithDefaults.isEmpty) {
          DBIO.successful(List.empty)
        } else {
          PersistenceSchema.systemParameterValues
            .filter(_.system === newSystemId)
            .filter(_.parameter inSet propertiesWithDefaults.map(x => x._1))
            .map(x => x.parameter)
            .result
        }
      // 3. Apply the default values for any properties that are not set.
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (propertiesWithDefaults.nonEmpty) {
          propertiesWithDefaults.foreach { defaultPropertyInfo =>
            if (!propertiesWithDefaultsThatAreSet.contains(defaultPropertyInfo._1)) {
              actions += (PersistenceSchema.systemParameterValues += SystemParameterValues(newSystemId, defaultPropertyInfo._1, defaultPropertyInfo._2, None))
            }
          }
        }
        toDBIO(actions)
      }
    } yield newSystemId
  }

  def saveSystemParameterValues(systemId: Long, communityId: Long, isAdmin: Boolean, values: List[SystemParameterValues], files: Map[Long, FileInfo], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    var providedParameters:Map[Long, SystemParameterValues] = Map()
    values.foreach{ v =>
      providedParameters += (v.parameter -> v)
    }
    val dbAction = for {
      // Load parameter definitions for the system's community
      parameterDefinitions <- PersistenceSchema.systemParameters.filter(_.community === communityId).result
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        parameterDefinitions.foreach{ parameterDefinition =>
          if ((!parameterDefinition.adminOnly && !parameterDefinition.hidden) || isAdmin) {
            val matchedProvidedParameter = providedParameters.get(parameterDefinition.id)
            if (matchedProvidedParameter.isDefined) {
              // Create or update
              if (parameterDefinition.kind != "SECRET" || (parameterDefinition.kind == "SECRET" && matchedProvidedParameter.get.value != "")) {
                // Special case: No update for secret parameters that are defined but not updated.
                var valueToSet = matchedProvidedParameter.get.value
                var existingBinaryNotUpdated = false
                var contentTypeToSet: Option[String] = None
                if (parameterDefinition.kind == "SECRET") {
                  // Encrypt secret value at rest.
                  valueToSet = MimeUtil.encryptString(valueToSet)
                } else if (parameterDefinition.kind == "BINARY") {
                  // Store file.
                  if (files.contains(parameterDefinition.id)) {
                    contentTypeToSet = files(parameterDefinition.id).contentType
                    onSuccessCalls += (() => repositoryUtils.setSystemPropertyFile(parameterDefinition.id, systemId, files(parameterDefinition.id).file))
                  } else {
                    existingBinaryNotUpdated = true
                  }
                }
                if (!existingBinaryNotUpdated) {
                  actions += PersistenceSchema.systemParameterValues.filter(_.parameter === parameterDefinition.id).filter(_.system === systemId).delete
                  actions += (PersistenceSchema.systemParameterValues += SystemParameterValues(systemId, matchedProvidedParameter.get.parameter, valueToSet, contentTypeToSet))
                }
              }
            } else {
              // Delete existing (if present)
              onSuccessCalls += (() => repositoryUtils.deleteSystemPropertyFile(parameterDefinition.id, systemId))
              actions += PersistenceSchema.systemParameterValues.filter(_.parameter === parameterDefinition.id).filter(_.system === systemId).delete
            }
          }
        }
        if (actions.nonEmpty) {
          DBIO.seq(actions.toList.map(a => a): _*)
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
    dbAction
  }

  def updateSystemThroughAutomationApi(updateRequest: UpdateSystemRequest): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      communityId <- apiHelper.getCommunityByCommunityApiKey(updateRequest.communityApiKey)
      system <- {
        for {
          system <- PersistenceSchema.systems
            .join(PersistenceSchema.organizations).on(_.owner === _.id)
            .filter(_._1.apiKey === updateRequest.systemApiKey)
            .filter(_._2.community === communityId)
            .map(_._1)
            .result
            .headOption
          _ <- {
            if (system.isEmpty) {
              throw AutomationApiException(ErrorCodes.API_SYSTEM_NOT_FOUND, "No system found for the provided API keys")
            } else {
              DBIO.successful(())
            }
          }
        } yield system.get
      }
      linkedActorIds <- {
        updateSystemProfileInternal(None, Some(communityId), system.id,
          updateRequest.shortName.getOrElse(system.shortname),
          updateRequest.fullName.getOrElse(system.fullname),
          updateRequest.description.getOrElse(system.description),
          updateRequest.version.getOrElse(system.version),
          None, None, None, None, None,
          copySystemParameters = false, copyStatementParameters = false, checkApiKeyUniqueness = false, onSuccessCalls
        )
      }
    } yield (communityId, system.id, linkedActorIds)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally).map { result =>
      // Call triggers in separate transactions.
      triggerHelper.publishTriggerEvent(new SystemUpdatedEvent(result._1, result._2))
      triggerHelper.triggersFor(result._1, result._2, Some(result._3))
    }
  }

  def updateSystemProfile(userId: Long, systemId: Long, shortName: String, fullName: String, description: Option[String], version: Option[String], otherSystem: Option[Long], propertyValues: Option[List[SystemParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], copySystemParameters: Boolean, copyStatementParameters: Boolean): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      communityId <- getCommunityIdOfSystemInternal(systemId)
      linkedActorIds <- updateSystemProfileInternal(Some(userId), None, systemId, shortName, fullName, description, version, None, None, otherSystem, propertyValues, propertyFiles, copySystemParameters, copyStatementParameters, checkApiKeyUniqueness = false, onSuccessCalls)
    } yield (communityId, linkedActorIds)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map { result =>
      triggerHelper.publishTriggerEvent(new SystemUpdatedEvent(result._1, systemId))
      triggerHelper.triggersFor(result._1, systemId, Some(result._2))
    }
  }

  def updateSystemProfileInternal(userId: Option[Long], communityId: Option[Long], systemId: Long, shortName: String, fullName: String, description: Option[String], version: Option[String], apiKey: Option[String], badgeKey: Option[String], otherSystem: Option[Long], propertyValues: Option[List[SystemParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], copySystemParameters: Boolean, copyStatementParameters: Boolean, checkApiKeyUniqueness: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[List[Long]] = {
    for {
      _ <- PersistenceSchema.systems.filter(_.id === systemId).map(s => (s.shortname, s.fullname, s.version, s.description)).update(shortName, fullName, version, description)
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (apiKey.isDefined) {
          // Update the API key conditionally.
          actions += (for {
            replaceApiKey <- {
              if (checkApiKeyUniqueness) {
                PersistenceSchema.systems.filter(_.apiKey === apiKey.get).filter(_.id =!= systemId).exists.result
              } else {
                DBIO.successful(false)
              }
            }
            _ <- {
              val apiKeyToUse = if (replaceApiKey) CryptoUtil.generateApiKey() else apiKey.get
              PersistenceSchema.systems.filter(_.id === systemId).map(_.apiKey).update(apiKeyToUse)
            }
          } yield ())
        }
        if (badgeKey.isDefined) {
          // Update the badge key conditionally.
          actions += (for {
            replaceBadgeKey <- {
              if (checkApiKeyUniqueness) {
                PersistenceSchema.systems.filter(_.badgeKey === badgeKey.get).filter(_.id =!= systemId).exists.result
              } else {
                DBIO.successful(false)
              }
            }
            _ <- {
              val badgeKeyToUse = if (replaceBadgeKey) CryptoUtil.generateApiKey() else badgeKey.get
              PersistenceSchema.systems.filter(_.id === systemId).map(_.badgeKey).update(badgeKeyToUse)
            }
          } yield ())
        }
        actions += testResultManager.updateForUpdatedSystem(systemId, shortName)
        toDBIO(actions)
      }
      // Update test configuration
      linkedActorIds <- {
        if (otherSystem.isDefined) {
          for {
            _ <- deleteAllConformanceStatements(systemId, onSuccessCalls)
            result <- copyTestSetup(otherSystem.get, systemId, copySystemParameters, copyStatementParameters, onSuccessCalls)
          } yield result
        } else {
          DBIO.successful(List[Long]())
        }
      }
      _ <- {
        if (propertyValues.isDefined && propertyFiles.isDefined && (otherSystem.isEmpty || !copySystemParameters)) {
          for {
            isAdmin <- {
              if (userId.isEmpty) {
                DBIO.successful(true)
              } else {
                for {
                  user <- PersistenceSchema.users.filter(_.id === userId.get).result.head
                  isAdmin <- DBIO.successful(user.role == UserRole.SystemAdmin.id.toShort || user.role == UserRole.CommunityAdmin.id.toShort)
                } yield isAdmin
              }
            }
            communityIdToUse <- {
              if (communityId.isDefined) {
                DBIO.successful(communityId.get)
              } else {
                getCommunityIdOfSystemInternal(systemId)
              }
            }
            _ <- saveSystemParameterValues(systemId, communityIdToUse, isAdmin, propertyValues.get, propertyFiles.get, onSuccessCalls)
          } yield ()
        } else {
          DBIO.successful(())
        }
      }
    } yield linkedActorIds
  }

  def getSystemProfile(systemId: Long): Future[models.System] = {
    DB.run(
      PersistenceSchema.systems
        .join(PersistenceSchema.organizations).on(_.owner === _.id)
        .filter(_._1.id === systemId)
        .result
        .head
    ).map { result =>
      new models.System(result._1, result._2)
    }
  }

  def getCommunityIdsOfSystems(systemIds: Iterable[Long]): Future[Set[Long]] = {
    DB.run(
      PersistenceSchema.systems
        .join(PersistenceSchema.organizations).on(_.owner === _.id)
        .filter(_._1.id inSet systemIds)
        .map(_._2.community)
        .distinct
        .result
    ).map(_.toSet)
  }

  def getSystemIdsForOrganization(orgId: Long): Future[Set[Long]] = {
    DB.run(PersistenceSchema.systems.filter(_.owner === orgId).map(s => {s.id}).result.map(_.toSet))
  }

  def checkIfSystemsHaveTests(systemIds: Set[Long]): Future[Set[Long]] = {
    DB.run(
      PersistenceSchema.testResults
        .filter(_.sutId inSet systemIds)
        .map(x => x.sutId)
        .result
    ).map { results =>
      results.map(x => x.get).toSet
    }
  }

  def getSystemsByOrganization(orgId: Long, snapshotId: Option[Long]): Future[List[Systems]] = {
    if (snapshotId.isEmpty) {
      DB.run(getSystemsByOrganizationInternal(orgId))
    } else {
      DB.run(
        PersistenceSchema.conformanceSnapshotResults
          .join(PersistenceSchema.conformanceSnapshotSystems).on((res, sys) => res.snapshotId === sys.snapshotId && res.systemId === sys.id)
          .filter(_._1.snapshotId === snapshotId.get)
          .filter(_._1.organisationId === orgId)
          .map(_._2)
          .distinct
          .sortBy(_.shortname.asc)
          .result
      ).map { results =>
        results
          .map(x => Systems(id = x.id, shortname = x.shortname, fullname = x.fullname, description = x.description, version = None, apiKey = x.apiKey, badgeKey = x.badgeKey, owner = orgId))
          .toList
      }
    }
  }

  def getSystemsByOrganizationInternal(orgId: Long): DBIO[List[Systems]] = {
    PersistenceSchema.systems.filter(_.owner === orgId)
      .sortBy(_.shortname.asc)
      .result.map(_.toList)
  }

  def defineConformanceStatements(systemId: Long, actorIds: Seq[Long]): Future[Unit] = {
    DB.run(
      (for {
        communityId <- getCommunityIdOfSystemInternal(systemId)
        domainId <- {
          if (communityId == Constants.DefaultCommunityId) {
            // This is to allow the test bed admin to create any statement.
            DBIO.successful(None)
          } else {
            PersistenceSchema.communities.filter(_.id === communityId).map(_.domain).result.head
          }
        }
        existingStatementActorIds <- PersistenceSchema.systemImplementsActors
          .filter(_.systemId === systemId)
          .map(_.actorId)
          .result
        actorsToProcess <- PersistenceSchema.specificationHasActors
          .join(PersistenceSchema.actors).on(_.actorId === _.id)
          .filterOpt(domainId)((q, id) => q._2.domain === id) // Ensure these are valid actors within the domain.
          .filter(_._1.actorId inSet actorIds)
          .filterNot(_._1.actorId inSet existingStatementActorIds)
          .map(_._1)
          .result
        _ <- {
          val actions = new ListBuffer[DBIO[_]]
          actorsToProcess.foreach { actor =>
            actions += defineConformanceStatement(systemId, actor._1, actor._2, setDefaultParameterValues = true)
          }
          toDBIO(actions)
        }
      } yield (communityId, actorsToProcess.map(_._2))).transactionally
    ).map { result =>
      result._2.foreach { actorId =>
        triggerHelper.publishTriggerEvent(new ConformanceStatementCreatedEvent(result._1, systemId, actorId))
      }
    }
  }

  def defineConformanceStatementViaApi(organisationKey: String, systemKey: String, actorKey: String): Future[Unit] = {
    DB.run((
      for {
        statementIds <- apiHelper.getStatementIdsForApiKeys(organisationKey, Some(systemKey), Some(actorKey), None, None, None)
        // Check to see if statement already exists
        statementExists <- PersistenceSchema.systemImplementsActors
          .filter(_.systemId === statementIds.systemId)
          .filter(_.actorId === statementIds.actorId)
          .exists
          .result
        // Lookup specification ID
        specificationId <- {
          if (statementExists) {
            throw AutomationApiException(ErrorCodes.API_STATEMENT_EXISTS, "A conformance statement already exists for the provided system and actor")
          } else {
            PersistenceSchema.specificationHasActors
              .filter(_.actorId === statementIds.actorId)
              .map(_.specId)
              .result
              .head
          }
        }
        // Create statement
        _ <- defineConformanceStatement(statementIds.systemId, specificationId, statementIds.actorId, setDefaultParameterValues = true)
      } yield ()
    ).transactionally)
  }

  def defineConformanceStatement(system: Long, spec: Long, actor: Long, setDefaultParameterValues: Boolean): DBIO[_] = {
    for {
      conformanceInfo <- PersistenceSchema.testCaseHasActors
        .join(PersistenceSchema.testSuiteHasTestCases).on(_.testcase === _.testcase)
        .filter(_._1.actor === actor)
        .filter(_._1.sut === true)
        .map(r => (r._1.testcase, r._2.testsuite))
        .result
      // Add the system to actor mapping. This may be missing due to test suite updates that changed actor roles.
      existingSystemMapping <- {
        if (conformanceInfo.isEmpty) {
          DBIO.successful(None)
        } else {
          PersistenceSchema.systemImplementsActors
            .filter(_.systemId === system)
            .filter(_.actorId === actor)
            .filter(_.specId === spec)
            .result
            .headOption
        }
      }
      _ <- {
        if (existingSystemMapping.isDefined) {
          DBIO.successful(())
        } else {
          PersistenceSchema.systemImplementsActors += (system, spec, actor)
        }
      }
      // Load any existing test results for the system and actor.
      // We look up based on test case ID (and not actor) because we may have shared test suites.
      existingResults <- PersistenceSchema.testResults
        .filter(_.sutId === system)
        .filter(_.testCaseId inSet conformanceInfo.map(_._1)) // In expected set of test case IDs
        .sortBy(_.endTime.desc)
        .result
      existingResultsMap <- {
        val existingResultsMap = new util.HashMap[Long, (String, String, Option[String], Option[Timestamp])]
        existingResults.foreach { existingResult =>
          if (!existingResultsMap.containsKey(existingResult.testCaseId.get)) {
            existingResultsMap.put(existingResult.testCaseId.get, (existingResult.sessionId, existingResult.result, existingResult.outputMessage, existingResult.endTime))
          }
        }
        DBIO.successful(existingResultsMap)
      }
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        conformanceInfo.foreach { conformanceInfoEntry =>
          val testCase = conformanceInfoEntry._1
          val testSuite = conformanceInfoEntry._2
          var result = TestResultStatus.UNDEFINED
          var outputMessage: Option[String] = None
          var sessionId: Option[String] = None
          var updateTime: Option[Timestamp] = None
          if (existingResultsMap.containsKey(testCase)) {
            val existingData = existingResultsMap.get(testCase)
            sessionId = Some(existingData._1)
            result = TestResultStatus.withName(existingData._2)
            outputMessage = existingData._3
            updateTime = existingData._4
          }
          actions += (PersistenceSchema.conformanceResults += ConformanceResult(0L, system, spec, actor, testSuite, testCase, result.toString, outputMessage, sessionId, updateTime))
        }
        toDBIO(actions)
      }
      /*
      Set properties with default values.
       */
      // 1. Determine the properties that have default values.
      propertiesWithDefaults <-
        if (setDefaultParameterValues) {
          PersistenceSchema.parameters
            .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
            .filter(_._2.actor === actor)
            .filter(_._1.defaultValue.isDefined)
            .map(x => (x._1.id, x._1.endpoint, x._1.defaultValue.get))
            .result
        } else {
          DBIO.successful(Seq.empty)
        }
      // 2. See which of these properties have values.
      propertiesWithDefaultsThatAreSet <-
        if (propertiesWithDefaults.isEmpty) {
          DBIO.successful(List.empty)
        } else {
          PersistenceSchema.configs
            .filter(_.system === system)
            .filter(_.parameter inSet propertiesWithDefaults.map(x => x._1))
            .map(x => x.parameter)
            .result
        }
      // 3. Apply the default values for any properties that are not set.
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (propertiesWithDefaults.nonEmpty) {
          propertiesWithDefaults.foreach { defaultPropertyInfo =>
            if (!propertiesWithDefaultsThatAreSet.contains(defaultPropertyInfo._1)) {
              actions += (PersistenceSchema.configs += Configs(system, defaultPropertyInfo._1, defaultPropertyInfo._2, defaultPropertyInfo._3, None))
            }
          }
        }
        toDBIO(actions)
      }
    } yield ()
  }

  private def getConformanceStatementReferencesInternal(systemId: Long): DBIO[List[ConformanceResult]] = {
    PersistenceSchema.conformanceResults.filter(_.sut === systemId).result.map(_.toList)
  }

  private def deleteAllConformanceStatements(systemId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      actors <- getImplementedActors(systemId)
      _ <- deleteConformanceStatements(systemId, actors.map(a => a.id), onSuccess)
    } yield ()
  }

  def deleteConformanceStatementsWrapper(systemId: Long, actorIds: List[Long]): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteConformanceStatements(systemId, actorIds, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
  }

  def deleteConformanceStatementViaApi(organisationKey: String, systemKey: String, actorKey: String): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      statementIds <- apiHelper.getStatementIdsForApiKeys(organisationKey, Some(systemKey), Some(actorKey), None, None, None)
      // Check to see if statement already exists
      statementExists <- PersistenceSchema.systemImplementsActors
        .filter(_.systemId === statementIds.systemId)
        .filter(_.actorId === statementIds.actorId)
        .exists
        .result
      // Delete statement
      _ <- {
        if (statementExists) {
          deleteConformanceStatements(statementIds.systemId, List(statementIds.actorId), onSuccessCalls)
        } else {
          throw AutomationApiException(ErrorCodes.API_STATEMENT_DOES_NOT_EXIST, "Unable to find conformance statement based on provided API keys")
        }
      }
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteConformanceStatements(systemId: Long, actorIds: List[Long], onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      _ <- PersistenceSchema.systemImplementsActors.filter(_.systemId === systemId).filter(_.actorId inSet actorIds).delete
      _ <- PersistenceSchema.conformanceResults.filter(_.sut === systemId).filter(_.actor inSet actorIds).delete
      actorParameters <- PersistenceSchema.parameters
                          .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
                          .filter(_._2.actor inSet actorIds)
                          .map(x => x._1.id)
                          .result
      _ <- {
        actorParameters.foreach { paramId =>
          onSuccess += (() => repositoryUtils.deleteStatementParameterFile(paramId, systemId))
        }
        PersistenceSchema.configs.filter(_.system === systemId).filter(_.parameter inSet actorParameters).delete
      }
      optionIds <- PersistenceSchema.options.filter(_.actor inSet actorIds).map(x => x.id).result
      _ <- PersistenceSchema.systemImplementsOptions.filter(_.systemId === systemId).filter(_.optionId inSet optionIds).delete
    } yield ()
	}

  private def getImplementedActors(system: Long): DBIO[List[Actors]] = {
    for {
      ids <- getActorsForSystem(system)
      actors <- PersistenceSchema.actors.filter(_.id inSet ids).sortBy(_.actorId.asc).result.map(_.toList)
    } yield actors
  }

  private def getActorsForSystem(system: Long): DBIO[List[Long]] = {
    PersistenceSchema.systemImplementsActors.filter(_.systemId === system).map(_.actorId).result.map(_.toList)
  }

  def deleteEndpointConfigurationInternal(systemId: Long, parameterId: Long, endpointId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    onSuccess += (() => repositoryUtils.deleteStatementParameterFile(parameterId, systemId))
    PersistenceSchema.configs
      .filter(_.system === systemId)
      .filter(_.parameter === parameterId)
      .filter(_.endpoint === endpointId)
      .delete
  }

  def saveEndpointConfigurationInternal(forceAdd: Boolean, forceUpdate: Boolean, config: Configs, fileValue: Option[File], onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    require((!forceAdd && !forceUpdate) || (forceAdd != forceUpdate), "When forcing an action this must be either an addition or an update but not both")
    for {
      existingConfig <- {
        if (!forceAdd && !forceUpdate) {
          PersistenceSchema.configs
            .filter(_.system === config.system)
            .filter(_.parameter === config.parameter)
            .filter(_.endpoint === config.endpoint)
            .size.result
        } else {
          DBIO.successful(1)
        }
      }
      _ <- {
        if (fileValue.isDefined) {
          onSuccess += (() => repositoryUtils.setStatementParameterFile(config.parameter, config.system, fileValue.get))
        }
        if (forceAdd || existingConfig == 0) {
          PersistenceSchema.configs += config
        } else {
          PersistenceSchema.configs
            .filter(_.system === config.system)
            .filter(_.parameter === config.parameter)
            .filter(_.endpoint === config.endpoint)
            .update(config)
        }
      }
    } yield ()
  }

  def deleteSystemThroughAutomationApi(systemApiKey: String, communityApiKey: String): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      communityId <- apiHelper.getCommunityByCommunityApiKey(communityApiKey)
      systemId <- PersistenceSchema.systems
        .join(PersistenceSchema.organizations).on(_.owner === _.id)
        .filter(_._1.apiKey === systemApiKey)
        .filter(_._2.community === communityId)
        .map(_._1.id)
        .result
        .headOption
        _ <- {
          if (systemId.isEmpty) {
            throw AutomationApiException(ErrorCodes.API_SYSTEM_NOT_FOUND, "No system found for the provided API keys")
          } else {
            deleteSystem(systemId.get, onSuccessCalls)
          }
        }
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  def deleteSystemWrapper(systemId: Long): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteSystem(systemId, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
  }

  private def deleteSystemParameterValues(systemId: Long, onSuccess: mutable.ListBuffer[() => _]) = {
    for {
      parameterValueIds <- PersistenceSchema.systemParameterValues.filter(_.system === systemId).map(x => x.parameter).result
      _ <- {
        parameterValueIds.foreach { paramId =>
          onSuccess += (() => repositoryUtils.deleteSystemPropertyFile(paramId, systemId))
        }
        PersistenceSchema.systemParameterValues.filter(_.system === systemId).delete
      }
    } yield ()
  }

  private def deleteSystemStatementConfiguration(systemId: Long, onSuccess: mutable.ListBuffer[() => _]) = {
    for {
      parameterIds <- PersistenceSchema.configs.filter(_.system === systemId).map(x => x.parameter).result
      _ <- {
        parameterIds.foreach { paramId =>
          onSuccess += (() => repositoryUtils.deleteStatementParameterFile(paramId, systemId))
        }
        PersistenceSchema.configs.filter(_.system === systemId).delete
      }
    } yield ()
  }

  def deleteSystem(systemId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      _ <- testResultManager.updateForDeletedSystem(systemId)
      _ <- deleteSystemStatementConfiguration(systemId, onSuccess)
      _ <- PersistenceSchema.systemHasAdmins.filter(_.systemId === systemId).delete
      _ <- PersistenceSchema.systemImplementsActors.filter(_.systemId === systemId).delete
      _ <- PersistenceSchema.systemImplementsOptions.filter(_.systemId === systemId).delete
      _ <- PersistenceSchema.conformanceResults.filter(_.sut === systemId).delete
      _ <- PersistenceSchema.conformanceSnapshotResults.filter(_.systemId === systemId).map(_.systemId).update(systemId * -1)
      _ <- PersistenceSchema.conformanceSnapshotSystems.filter(_.id === systemId).map(_.id).update(systemId * -1)
      _ <- PersistenceSchema.conformanceSnapshotSystemProperties.filter(_.systemId === systemId).map(_.systemId).update(systemId * -1)
      _ <- deleteSystemParameterValues(systemId, onSuccess)
      _ <- PersistenceSchema.systems.filter(_.id === systemId).delete
    } yield ()
  }

  def deleteSystemByOrganization(orgId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      systemIds <- PersistenceSchema.systems.filter(_.owner === orgId).map(_.id).result
      _ <- DBIO.seq(systemIds.map(systemId => deleteSystem(systemId, onSuccess)): _*)
    } yield()
	}

  def getSystemById(id: Long): Future[Option[Systems]] = {
    DB.run(PersistenceSchema.systems.filter(_.id === id).result.headOption)
  }

  def getSystems(ids: Option[List[Long]]): Future[List[Systems]] = {
    val q = ids match {
      case Some(idList) =>
        PersistenceSchema.systems.filter(_.id inSet idList)
      case None =>
        PersistenceSchema.systems
    }
    DB.run(q.sortBy(_.shortname.asc).result.map(_.toList))
  }

  def getCommunityIdOfSystem(systemId: Long): Future[Long] = {
    DB.run(getCommunityIdOfSystemInternal(systemId))
  }

  def getCommunityIdOfSystemInternal(systemId: Long): DBIO[Long] = {
    PersistenceSchema.systems
      .join(PersistenceSchema.organizations).on(_.owner === _.id)
      .filter(_._1.id === systemId)
      .map(x => x._2.community).result.head
  }

  def getSystemParameterValues(systemId: Long, onlySimple: Option[Boolean] = None, forExports: Option[Boolean] = None): Future[List[SystemParametersWithValue]] = {
    var typeToCheck: Option[String] = None
    if (onlySimple.isDefined && onlySimple.get) {
      typeToCheck = Some("SIMPLE")
    }
    val action = for {
      communityId <- getCommunityIdOfSystemInternal(systemId)
      values <- PersistenceSchema.systemParameters
        .joinLeft(PersistenceSchema.systemParameterValues).on((p, v) => p.id === v.parameter && v.system === systemId)
        .filter(_._1.community === communityId)
        .filterOpt(forExports)((q, flag) => q._1.inExports === flag)
        .filterOpt(typeToCheck)((table, propertyType)=> table._1.kind === propertyType)
        .sortBy(x => (x._1.displayOrder.asc, x._1.name.asc))
        .map(x => (x._1, x._2))
        .result
    } yield values
    DB.run(action).map { values =>
      values.toList.map(r => new SystemParametersWithValue(r._1, r._2))
    }
  }

  def updateSystemApiKey(systemId: Long): Future[String] = {
    val newApiKey = CryptoUtil.generateApiKey()
    updateSystemApiKeyInternal(systemId, newApiKey).map { _ =>
      newApiKey
    }
  }

  private def updateSystemApiKeyInternal(systemId: Long, apiKey: String): Future[Unit] = {
    DB.run(PersistenceSchema.systems.filter(_.id === systemId).map(_.apiKey).update(apiKey).transactionally).map(_ => ())
  }

  def searchSystems(communityIds: Option[List[Long]], organisationIds: Option[List[Long]], snapshotId: Option[Long] = None): Future[List[Systems]] = {
    val query = if (snapshotId.isDefined) {
      PersistenceSchema.conformanceSnapshotResults
        .join(PersistenceSchema.conformanceSnapshotSystems).on((q, s) => q.snapshotId === s.snapshotId && q.systemId === s.id)
        .join(PersistenceSchema.conformanceSnapshots).on(_._1.snapshotId === _.id)
        .filter(_._2.id === snapshotId.get)
        .filterOpt(communityIds)((q, ids) => q._2.community inSet ids)
        .filterOpt(organisationIds)((q, ids) => q._1._1.organisationId inSet ids)
        .map(_._1._2)
        .result
        .map { results =>
          results.map { x =>
            Systems(x.id, x.shortname, x.fullname, x.description, x.version, x.apiKey, x.badgeKey, -1)
          }.toList
        }
    } else {
      PersistenceSchema.systems
        .join(PersistenceSchema.organizations).on(_.owner === _.id)
        .filterOpt(organisationIds)((q, ids) => q._1.owner inSet ids)
        .filterOpt(communityIds)((q, ids) => q._2.community inSet ids)
        .map(_._1)
        .sortBy(_.shortname.asc)
        .result
        .map(_.toList)
    }
    DB.run(query)
  }
}
