package managers

import actors.events.{ConformanceStatementCreatedEvent, ConformanceStatementUpdatedEvent, SystemUpdatedEvent}
import models.Enums.{TestResultStatus, UserRole}
import models._
import org.slf4j.LoggerFactory
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider
import utils.{CryptoUtil, RepositoryUtils}

import java.io.File
import java.sql.Timestamp
import java.util
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import utils.MimeUtil

@Singleton
class SystemManager @Inject() (repositoryUtils: RepositoryUtils, testResultManager: TestResultManager, triggerHelper: TriggerHelper, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("SystemManager")

  def checkSystemExists(sysId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.systems.filter(_.id === sysId).result.headOption)
    firstOption.isDefined
  }

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
            actions += defineConformanceStatement(toSystem, otherConformanceStatement.spec, otherConformanceStatement.actor, None)
          }
        }
        toDBIO(actions) andThen DBIO.successful(linkedActorIds.toList)
      }
      _ <- {
        if (copySystemParameters) {
          deleteSystemParameterValues(toSystem, onSuccessCalls) andThen
          (
            for {
              otherValues <- PersistenceSchema.systemParameterValues.filter(_.system === fromSystem).result.map(_.toList)
              _ <- {
                val copyActions = new ListBuffer[DBIO[_]]()
                otherValues.foreach(otherValue => {
                  onSuccessCalls += (() => repositoryUtils.setSystemPropertyFile(otherValue.parameter, toSystem, repositoryUtils.getSystemPropertyFile(otherValue.parameter, fromSystem), copy = true))
                  copyActions += (PersistenceSchema.systemParameterValues += SystemParameterValues(toSystem, otherValue.parameter, otherValue.value, otherValue.contentType))
                })
                DBIO.seq(copyActions.toList.map(a => a): _*)
              }
            } yield()
          )
        } else {
          DBIO.successful(())
        }
      }
      _ <- {
        if (copyStatementParameters) {
          deleteSystemStatementConfiguration(toSystem, onSuccessCalls) andThen
          (
            for {
              otherValues <- PersistenceSchema.configs.filter(_.system === fromSystem).result.map(_.toList)
              _ <- {
                val copyActions = new ListBuffer[DBIO[_]]()
                otherValues.foreach(otherValue => {
                  onSuccessCalls += (() => repositoryUtils.setStatementParameterFile(otherValue.parameter, toSystem, repositoryUtils.getStatementParameterFile(otherValue.parameter, fromSystem), copy = true))
                  copyActions += (PersistenceSchema.configs += Configs(toSystem, otherValue.parameter, otherValue.endpoint, otherValue.value, otherValue.contentType))
                })
                DBIO.seq(copyActions.toList.map(a => a): _*)
              }
            } yield()
          )
        } else {
          DBIO.successful(())
        }
      }
    } yield linkedActorIds
  }

  private def getCommunityIdForOrganisationId(organisationId: Long): DBIO[Long] = {
    PersistenceSchema.organizations.filter(_.id === organisationId).map(x => x.community).result.head
  }

  private def getCommunityIdForSystemId(systemId: Long): DBIO[Long] = {
    PersistenceSchema.organizations.join(PersistenceSchema.systems).on(_.id === _.owner).filter(_._2.id === systemId).map(x => x._1.community).result.head
  }

  def registerSystemWrapper(userId:Long, system: Systems, otherSystem: Option[Long], propertyValues: Option[List[SystemParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], copySystemParameters: Boolean, copyStatementParameters: Boolean) = {
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
      newSystemId <- registerSystem(system, Some(communityId), isAdmin, propertyValuesToUse, propertyFiles, onSuccessCalls)
      linkedActorIds <- {
        if (otherSystem.isDefined) {
          copyTestSetup(otherSystem.get, newSystemId, copySystemParameters, copyStatementParameters, onSuccessCalls)
        } else {
          DBIO.successful(List[Long]())
        }
      }
    } yield (newSystemId, linkedActorIds, communityId)
    val ids = exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
    triggerHelper.triggersFor(ids._3, new SystemCreationDbInfo(ids._1, Some(ids._2)))
    ids
  }

  def registerSystem(system: Systems, communityId: Option[Long], isAdmin: Option[Boolean], propertyValues: Option[List[SystemParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Long] = {
    for {
      newSystemId <- PersistenceSchema.insertSystem += system
      _ <- {
        if (propertyValues.isDefined) {
          saveSystemParameterValues(newSystemId, communityId.get, isAdmin.get, propertyValues.get, propertyFiles.get, onSuccessCalls)
        } else {
          DBIO.successful(())
        }
      }
      /*
      Set properties with default values.
       */
      // 1. Determine the properties that have default values.
      propertiesWithDefaults <-
        PersistenceSchema.systemParameters
          .filter(_.community === communityId.get)
          .filter(_.defaultValue.isDefined)
          .map(x => (x.id, x.defaultValue.get))
          .result
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
    // Load parameter definitions for the system's community
    val parameterDefinitions = exec(PersistenceSchema.systemParameters.filter(_.community === communityId).result).toList
    // Make updates
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
    } else
      DBIO.successful(())
  }

  def updateSystemProfile(userId: Long, systemId: Long, sname: String, fname: String, description: Option[String], version: String, otherSystem: Option[Long], propertyValues: Option[List[SystemParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], copySystemParameters: Boolean, copyStatementParameters: Boolean): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      communityId <- getCommunityIdForSystemId(systemId)
      linkedActorIds <- updateSystemProfileInternal(Some(userId), None, systemId, sname, fname, description, version, None, otherSystem, propertyValues, propertyFiles, copySystemParameters, copyStatementParameters, onSuccessCalls)
    } yield (communityId, linkedActorIds)
    val result = exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
    triggerHelper.publishTriggerEvent(new SystemUpdatedEvent(result._1, systemId))
    triggerHelper.triggersFor(result._1, systemId, Some(result._2))
  }

  def updateSystemProfileInternal(userId: Option[Long], communityId: Option[Long], systemId: Long, sname: String, fname: String, description: Option[String], version: String, apiKey: Option[Option[String]], otherSystem: Option[Long], propertyValues: Option[List[SystemParameterValues]], propertyFiles: Option[Map[Long, FileInfo]], copySystemParameters: Boolean, copyStatementParameters: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[List[Long]] = {
    for {
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (apiKey.isDefined) {
          // Update the API key conditionally.
          val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield (s.shortname, s.fullname, s.version, s.description, s.apiKey)
          actions += q.update(sname, fname, version, description, apiKey.get)
        } else {
          val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield (s.shortname, s.fullname, s.version, s.description)
          actions += q.update(sname, fname, version, description)
        }
        actions += testResultManager.updateForUpdatedSystem(systemId, sname)
        toDBIO(actions)
      }
      // Update test configuration
      linkedActorIds <- {
        if (otherSystem.isDefined) {
          deleteAllConformanceStatements(systemId, onSuccessCalls) andThen
            copyTestSetup(otherSystem.get, systemId, copySystemParameters, copyStatementParameters, onSuccessCalls)
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

  def getSystemProfile(systemId: Long): models.System = {
    //1) Get system info
    val s = exec(PersistenceSchema.systems.filter(_.id === systemId).result.head)

    //2) Get organization info
    val o = exec(PersistenceSchema.organizations.filter(_.id === s.owner).result.head)

    //3) Get admins
    val list = exec(PersistenceSchema.systemHasAdmins.filter(_.systemId === systemId).map(_.userId).result.map(_.toList))
    var admins: List[Users] = List()
    list.foreach { adminId =>
      val user = exec(PersistenceSchema.users.filter(_.id === adminId).result.head)
      admins ::= user
    }

    //4) Merge all info and return
    val system: models.System = new models.System(s, o, admins)
    system
  }

  def getSystemIdsForOrganization(orgId: Long): Set[Long] = {
    val systemIds = exec(PersistenceSchema.systems.filter(_.owner === orgId).map(s => {s.id}).result.map(_.toSet))
    systemIds
  }

  def checkIfSystemsHaveTests(systemIds: Set[Long]): Set[Long] = {
    exec(
      PersistenceSchema.testResults
        .filter(_.sutId inSet systemIds)
        .map(x => x.sutId)
        .result
    ).map(x => x.get).toSet
  }

  def getSystemsByOrganization(orgId: Long): List[Systems] = {
    val systems = exec(getSystemsByOrganizationInternal(orgId))
    systems
  }

  def getSystemsByOrganizationInternal(orgId: Long): DBIO[List[Systems]] = {
    PersistenceSchema.systems.filter(_.owner === orgId)
      .sortBy(_.shortname.asc)
      .result.map(_.toList)
  }

  def getVendorSystems(userId: Long): List[Systems] = {
    //1) Get organization id of the user, first
    val orgId = exec(PersistenceSchema.users.filter(_.id === userId).result.head).organization

    //2) Get systems of the organization
    val systems = exec(PersistenceSchema.systems.filter(_.owner === orgId)
      .sortBy(_.shortname.asc)
      .result.map(_.toList))
    systems
  }

  def defineConformanceStatementWrapper(system: Long, spec: Long, actor: Long, options: Option[List[Long]]):Unit = {
    val dbAction = for {
      communityId <- getCommunityIdForSystemId(system)
      _ <- defineConformanceStatement(system, spec, actor, options)
    } yield communityId
    val communityId = exec(dbAction.transactionally)
    triggerHelper.publishTriggerEvent(new ConformanceStatementCreatedEvent(communityId, system, actor))
  }

  def sutTestCasesExistForActor(actor: Long): Boolean = {
    val query = PersistenceSchema.testCaseHasActors
      .join(PersistenceSchema.testSuiteHasTestCases).on(_.testcase === _.testcase)
      .filter(_._1.actor === actor)
      .filter(_._1.sut === true)
    val result = exec(query.result.headOption)
    result.isDefined
  }

  def defineConformanceStatement(system: Long, spec: Long, actor: Long, options: Option[List[Long]]) = {
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
      existingResults <- PersistenceSchema.testResults
        .filter(_.sutId === system)
        .filter(_.actorId === actor)
        .filter(_.testCaseId.isDefined)
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
        PersistenceSchema.parameters
          .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
          .filter(_._2.actor === actor)
          .filter(_._1.defaultValue.isDefined)
          .map(x => (x._1.id, x._1.endpoint, x._1.defaultValue.get))
          .result
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

  def getConformanceStatementReferences(systemId: Long) = {
    val conformanceStatements = exec(getConformanceStatementReferencesInternal(systemId))
    conformanceStatements
  }

  private def getConformanceStatementReferencesInternal(systemId: Long) = {
    PersistenceSchema.conformanceResults.filter(_.sut === systemId).result.map(_.toList)
  }

  def getConformanceStatements(systemId: Long, spec: Option[Long], actor: Option[Long]): List[ConformanceStatement] = {
    var query = PersistenceSchema.conformanceResults
        .join(PersistenceSchema.specifications).on(_.spec === _.id)
        .join(PersistenceSchema.actors).on(_._1.actor === _.id)
        .join(PersistenceSchema.domains).on(_._2.domain === _.id)
    query = query.filter(_._1._1._1.sut === systemId)
    if (spec.isDefined && actor.isDefined) {
      query = query
        .filter(_._1._1._1.spec === spec.get)
        .filter(_._1._1._1.actor === actor.get)
    }
    query = query.sortBy(x => (x._2.fullname, x._1._1._2.fullname, x._1._2.name))

    val results = exec(query.result.map(_.toList))
    val resultBuilder = new ConformanceStatusBuilder[ConformanceStatement](recordDetails = false)
    results.foreach { result =>
      resultBuilder.addConformanceResult(
        new ConformanceStatement(
          result._2.id, result._2.shortname, result._2.fullname,
          result._1._2.id, result._1._2.actorId, result._1._2.name,
          result._1._1._2.id, result._1._1._2.shortname, result._1._1._2.fullname,
          result._1._1._1.sut, result._1._1._1.result, result._1._1._1.updateTime,
          0L, 0L, 0L)
      )
    }
    resultBuilder.getOverview(None)
  }


  def deleteAllConformanceStatements(systemId: Long, onSuccess: mutable.ListBuffer[() => _]) = {
    for {
      actors <- getImplementedActors(systemId)
      _ <- deleteConformanceStatements(systemId, actors.map(a => a.id), onSuccess)
    } yield ()
  }

  def deleteConformanceStatementsWrapper(systemId: Long, actorIds: List[Long]) = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteConformanceStatements(systemId, actorIds, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteConformanceStatements(systemId: Long, actorIds: List[Long], onSuccess: mutable.ListBuffer[() => _]) = {
    for {
      _ <- PersistenceSchema.systemImplementsActors.filter(_.systemId === systemId).filter(_.actorId inSet actorIds).delete andThen
              PersistenceSchema.conformanceResults.filter(_.sut === systemId).filter(_.actor inSet actorIds).delete
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

  def getImplementedActorsWrapper(system: Long): List[Actors] = {
    exec(getImplementedActors(system))
  }

  def getImplementedActors(system: Long): DBIO[List[Actors]] = {
    for {
      ids <- getActorsForSystem(system)
      actors <- PersistenceSchema.actors.filter(_.id inSet ids).sortBy(_.actorId.asc).result.map(_.toList)
    } yield actors
  }

  private def getActorsForSystem(system: Long): DBIO[List[Long]] = {
    PersistenceSchema.systemImplementsActors.filter(_.systemId === system).map(_.actorId).result.map(_.toList)
  }

  def getEndpointConfigurations(endpoint: Long, system: Long): List[Configs] = {
    exec(PersistenceSchema.configs.filter(_.endpoint === endpoint).filter(_.system === system).result.map(_.toList))
  }

  def deleteEndpointConfigurationInternal(systemId: Long, parameterId: Long, endpointId: Long, onSuccess: mutable.ListBuffer[() => _]) = {
    onSuccess += (() => repositoryUtils.deleteStatementParameterFile(parameterId, systemId))
    PersistenceSchema.configs
      .filter(_.system === systemId)
      .filter(_.parameter === parameterId)
      .filter(_.endpoint === endpointId)
      .delete
  }

  def deleteEndpointConfiguration(systemId: Long, parameterId: Long, endpointId: Long) = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      communityId <- getCommunityIdForSystemId(systemId)
      _ <- deleteEndpointConfigurationInternal(systemId, parameterId, endpointId, onSuccessCalls)
    } yield communityId
    val communityId = exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
    triggerHelper.publishTriggerEvent(new ConformanceStatementUpdatedEvent(communityId, systemId, getActorIdForEndpointId(endpointId)))
  }

  def saveEndpointConfigurationInternal(forceAdd: Boolean, forceUpdate: Boolean, config: Configs, fileValue: Option[File], onSuccess: mutable.ListBuffer[() => _]) = {
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

  def saveEndpointConfiguration(config: Configs, fileValue: Option[File]) = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      communityId <- getCommunityIdForSystemId(config.system)
      _ <- saveEndpointConfigurationInternal(forceAdd = false, forceUpdate = false, config, fileValue, onSuccessCalls)
    } yield communityId
    val communityId = exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
    triggerHelper.publishTriggerEvent(new ConformanceStatementUpdatedEvent(communityId, config.system, getActorIdForEndpointId(config.endpoint)))
  }

  private def getActorIdForEndpointId(endpointId: Long): Long = {
    exec(PersistenceSchema.endpoints.filter(_.id === endpointId).map(x => x.actor).result.head)
  }

  def getConfigurationsWithEndpointIds(system: Long, ids: List[Long]): List[Configs] = {
    exec(PersistenceSchema.configs.filter(_.system === system).filter(_.endpoint inSet ids).result.map(_.toList))
  }

  def deleteSystemWrapper(systemId: Long) = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteSystem(systemId, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
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

  def deleteSystem(systemId: Long, onSuccess: mutable.ListBuffer[() => _]) = {
    testResultManager.updateForDeletedSystem(systemId) andThen
    deleteSystemStatementConfiguration(systemId, onSuccess) andThen
    PersistenceSchema.systemHasAdmins.filter(_.systemId === systemId).delete andThen
    PersistenceSchema.systemImplementsActors.filter(_.systemId === systemId).delete andThen
    PersistenceSchema.systemImplementsOptions.filter(_.systemId === systemId).delete andThen
    PersistenceSchema.conformanceResults.filter(_.sut === systemId).delete andThen
    deleteSystemParameterValues(systemId, onSuccess) andThen
    PersistenceSchema.systems.filter(_.id === systemId).delete
  }

  def deleteSystemByOrganization(orgId: Long, onSuccess: mutable.ListBuffer[() => _]) = {
    for {
      systemIds <- PersistenceSchema.systems.filter(_.owner === orgId).map(_.id).result
      _ <- DBIO.seq(systemIds.map(systemId => deleteSystem(systemId, onSuccess)): _*)
    } yield()
	}

  def getSystemById(id: Long): Option[Systems] = {
    val system = exec(PersistenceSchema.systems.filter(_.id === id).result.headOption)
    system
  }

  def getSystems(ids: Option[List[Long]]): List[Systems] = {
    val q = ids match {
      case Some(idList) => {
        PersistenceSchema.systems
          .filter(_.id inSet idList)
      }
      case None => {
        PersistenceSchema.systems
      }
    }
    exec(q.sortBy(_.shortname.asc)
      .result.map(_.toList))
  }

  def getCommunityIdOfSystem(systemId: Long): Long = {
    exec(getCommunityIdOfSystemInternal(systemId))
  }

  private def getCommunityIdOfSystemInternal(systemId: Long): DBIO[Long] = {
    PersistenceSchema.systems
      .join(PersistenceSchema.organizations).on(_.owner === _.id)
      .filter(_._1.id === systemId)
      .map(x => x._2.community).result.head
  }

  def getSystemParameterValues(systemId: Long): List[SystemParametersWithValue] = {
    val communityId = getCommunityIdOfSystem(systemId)
    exec(PersistenceSchema.systemParameters
      .joinLeft(PersistenceSchema.systemParameterValues).on((p, v) => p.id === v.parameter && v.system === systemId)
      .filter(_._1.community === communityId)
      .sortBy(x => (x._1.displayOrder.asc, x._1.name.asc))
      .map(x => (x._1, x._2))
      .result
    ).toList.map(r => new SystemParametersWithValue(r._1, r._2))
  }

  def updateSystemApiKey(systemId: Long): String = {
    val newApiKey = CryptoUtil.generateApiKey()
    updateSystemApiKeyInternal(systemId, Some(newApiKey))
    newApiKey
  }

  def deleteSystemApiKey(systemId: Long): Unit = {
    updateSystemApiKeyInternal(systemId, None)
  }

  private def updateSystemApiKeyInternal(systemId: Long, apiKey: Option[String]): Unit = {
    exec(PersistenceSchema.systems.filter(_.id === systemId).map(_.apiKey).update(apiKey).transactionally)
  }

  def searchSystems(communityIds: Option[List[Long]], organisationIds: Option[List[Long]]): List[Systems] = {
    exec(
      PersistenceSchema.systems
        .join(PersistenceSchema.organizations).on(_.owner === _.id)
        .filterOpt(organisationIds)((q, ids) => q._1.owner inSet ids)
        .filterOpt(communityIds)((q, ids) => q._2.community inSet ids)
        .map(_._1)
        .result
        .map(_.toList)
    )
  }
}
