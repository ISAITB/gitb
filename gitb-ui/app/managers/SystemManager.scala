package managers

import java.util

import actors.events.{ConformanceStatementCreatedEvent, ConformanceStatementUpdatedEvent, SystemCreatedEvent, SystemUpdatedEvent}
import javax.inject.{Inject, Singleton}
import models.Enums.{TestResultStatus, UserRole}
import models.{ConformanceResult, ConformanceStatement, _}
import org.slf4j.LoggerFactory
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SystemManager @Inject() (testResultManager: TestResultManager, triggerHelper: TriggerHelper, triggerManager: TriggerManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("SystemManager")

  def checkSystemExists(sysId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.systems.filter(_.id === sysId).result.headOption)
    firstOption.isDefined
  }

  def copyTestSetup(fromSystem: Long, toSystem: Long, copySystemParameters: Boolean, copyStatementParameters: Boolean): DBIO[List[Long]] = {
    val actions = new ListBuffer[DBIO[_]]()
    val linkedActorIds = ListBuffer[Long]()
    val conformanceStatements = getConformanceStatementReferences(fromSystem)
    var addedStatements = scala.collection.mutable.Set[String]()
    conformanceStatements.foreach { otherConformanceStatement =>
      val key = otherConformanceStatement.spec+"-"+otherConformanceStatement.actor
      if (!addedStatements.contains(key)) {
        addedStatements += key
        linkedActorIds += otherConformanceStatement.actor
        actions += defineConformanceStatement(toSystem, otherConformanceStatement.spec, otherConformanceStatement.actor, None)
      }
    }
    if (copySystemParameters) {
      actions += PersistenceSchema.systemParameterValues.filter(_.system === toSystem).delete
      actions += (
        for {
          otherValues <- PersistenceSchema.systemParameterValues.filter(_.system === fromSystem).result.map(_.toList)
          _ <- {
            val copyActions = new ListBuffer[DBIO[_]]()
            otherValues.map(otherValue => {
              copyActions += (PersistenceSchema.systemParameterValues += SystemParameterValues(toSystem, otherValue.parameter, otherValue.value))
            })
            DBIO.seq(copyActions.map(a => a): _*)
          }
        } yield()
      )
    }
    if (copyStatementParameters) {
      actions += PersistenceSchema.configs.filter(_.system === toSystem).delete
      actions += (
        for {
          otherValues <- PersistenceSchema.configs.filter(_.system === fromSystem).result.map(_.toList)
          _ <- {
            val copyActions = new ListBuffer[DBIO[_]]()
            otherValues.map(otherValue => {
              copyActions += (PersistenceSchema.configs += Configs(toSystem, otherValue.parameter, otherValue.endpoint, otherValue.value))
            })
            DBIO.seq(copyActions.map(a => a): _*)
          }
        } yield()
        )
    }
    DBIO.seq(actions.map(a => a): _*) andThen DBIO.successful(linkedActorIds.toList)
  }

  private def getCommunityIdForOrganisationId(organisationId: Long): Long = {
    exec(PersistenceSchema.organizations.filter(_.id === organisationId).map(x => x.community).result.head)
  }

  private def getCommunityIdForSystemId(systemId: Long): Long = {
    exec(PersistenceSchema.organizations.join(PersistenceSchema.systems).on(_.id === _.owner).filter(_._2.id === systemId).map(x => x._1.community).result.head)
  }

  def registerSystemWrapper(userId:Long, system: Systems, otherSystem: Option[Long], propertyValues: Option[List[SystemParameterValues]], copySystemParameters: Boolean, copyStatementParameters: Boolean) = {
    var isAdmin: Option[Boolean] = None
    val communityId = getCommunityIdForOrganisationId(system.owner)

    var propertyValuesToUse = propertyValues
    if (propertyValues.isDefined && (otherSystem.isEmpty || !copySystemParameters)) {
      val user = exec(PersistenceSchema.users.filter(_.id === userId).result.head)
      isAdmin = Some(user.role == UserRole.SystemAdmin.id.toShort || user.role == UserRole.CommunityAdmin.id.toShort)
    } else {
      propertyValuesToUse = None
    }
    val ids = exec(
      (
        for {
          newSystemId <- registerSystem(system, Some(communityId), isAdmin, propertyValuesToUse)
          linkedActorIds <- {
            if (otherSystem.isDefined) {
              copyTestSetup(otherSystem.get, newSystemId, copySystemParameters, copyStatementParameters)
            } else {
              DBIO.successful(List[Long]())
            }
          }
        } yield (newSystemId, linkedActorIds)
      ).transactionally
    )
    triggerHelper.triggersFor(communityId, new SystemCreationDbInfo(ids._1, Some(ids._2)))
    ids
  }

  def registerSystem(system: Systems, communityId: Option[Long], isAdmin: Option[Boolean], propertyValues: Option[List[SystemParameterValues]]): DBIO[Long] = {
    for {
      newSystemId <- PersistenceSchema.insertSystem += system
      _ <- {
        if (propertyValues.isDefined) {
          saveSystemParameterValues(newSystemId, communityId.get, isAdmin.get, propertyValues.get)
        } else {
          DBIO.successful(())
        }
      }
    } yield newSystemId
  }

  def saveSystemParameterValues(systemId: Long, communityId: Long, isAdmin: Boolean, values: List[SystemParameterValues]): DBIO[_] = {
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
            actions += PersistenceSchema.systemParameterValues.filter(_.parameter === parameterDefinition.id).filter(_.system === systemId).delete
            actions += (PersistenceSchema.systemParameterValues += matchedProvidedParameter.get.withSystemId(systemId))
          }
        } else {
          // Delete existing (if present)
          actions += PersistenceSchema.systemParameterValues.filter(_.parameter === parameterDefinition.id).filter(_.system === systemId).delete
        }
      }
    }
    if (actions.nonEmpty) {
      DBIO.seq(actions.map(a => a): _*)
    } else
      DBIO.successful(())
  }

  def updateSystemProfile(userId: Long, systemId: Long, sname: Option[String], fname: Option[String], description: Option[String], version: Option[String], otherSystem: Option[Long], propertyValues: Option[List[SystemParameterValues]], copySystemParameters: Boolean, copyStatementParameters: Boolean) = {
    val communityId = getCommunityIdForSystemId(systemId)
    val linkedActorIds = exec(updateSystemProfileInternal(Some(userId), None, systemId, sname, fname, description, version, otherSystem, propertyValues, copySystemParameters, copyStatementParameters).transactionally)
    triggerHelper.publishTriggerEvent(new SystemUpdatedEvent(communityId, systemId))
    triggerHelper.triggersFor(communityId, systemId, Some(linkedActorIds))
  }

  def updateSystemProfileInternal(userId: Option[Long], communityId: Option[Long], systemId: Long, sname: Option[String], fname: Option[String], description: Option[String], version: Option[String], otherSystem: Option[Long], propertyValues: Option[List[SystemParameterValues]], copySystemParameters: Boolean, copyStatementParameters: Boolean): DBIO[List[Long]] = {
    for {
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        // Update short name of the system
        if (sname.isDefined) {
          val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield s.shortname
          actions += q.update(sname.get)
          actions += testResultManager.updateForUpdatedSystem(systemId, sname.get)
        }
        // Update full name of the system
        if (fname.isDefined) {
          val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield s.fullname
          actions += q.update(fname.get)
        }
        // Update description of the system
        if (description.isDefined) {
          val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield s.description
          actions += q.update(description)
        }
        toDBIO(actions)
      }
      // Update test configuration
      linkedActorIds <- {
        if (otherSystem.isDefined) {
          deleteAllConformanceStatements(systemId) andThen
            copyTestSetup(otherSystem.get, systemId, copySystemParameters, copyStatementParameters)
        } else {
          DBIO.successful(List[Long]())
        }
      }
      _ <- {
        if (propertyValues.isDefined && (otherSystem.isEmpty || !copySystemParameters)) {
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
            _ <- saveSystemParameterValues(systemId, communityIdToUse, isAdmin, propertyValues.get)
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
    val systems = exec(PersistenceSchema.systems.filter(_.owner === orgId)
        .sortBy(_.shortname.asc)
      .result.map(_.toList))
    systems
  }

  def getSystemsByCommunity(communityId: Long): List[Systems] = {
    val systems = exec(
      PersistenceSchema.systems
        .join(PersistenceSchema.organizations).on(_.owner === _.id)
        .filter(_._2.community === communityId)
        .map(r => r._1)
      .sortBy(_.shortname.asc)
      .result.map(_.toList))
    systems
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
    exec(defineConformanceStatement(system, spec, actor, options).transactionally)
    val communityId = getCommunityIdForSystemId(system)
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
      existingResults <- PersistenceSchema.testResults.filter(_.sutId === system).filter(_.actorId === actor).filter(_.testCaseId.isDefined).result
      existingResultsMap <- {
        val existingResultsMap = new util.HashMap[Long, (String, String)]
        existingResults.foreach { existingResult =>
          existingResultsMap.put(existingResult.testCaseId.get, (existingResult.sessionId, existingResult.result))
        }
        DBIO.successful(existingResultsMap)
      }
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        conformanceInfo.foreach { conformanceInfoEntry =>
          val testCase = conformanceInfoEntry._1
          val testSuite = conformanceInfoEntry._2
          var result = TestResultStatus.UNDEFINED
          var sessionId: Option[String] = None
          if (existingResultsMap.containsKey(testCase)) {
            val existingData = existingResultsMap.get(testCase)
            sessionId = Some(existingData._1)
            result = TestResultStatus.withName(existingData._2)
          }
          actions += (PersistenceSchema.conformanceResults += ConformanceResult(0L, system, spec, actor, testSuite, testCase, result.toString, sessionId))
        }
        toDBIO(actions)
      }
    } yield ()
  }

  def getConformanceStatementReferences(systemId: Long) = {
    //1) Get organization id of the user, first
    val conformanceStatements = exec(PersistenceSchema.conformanceResults.filter(_.sut === systemId).result.map(_.toList))
    conformanceStatements
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
    val conformanceMap = new util.LinkedHashMap[Long, ConformanceStatement]
    results.foreach { result =>
      var conformanceStatement = conformanceMap.get(result._1._1._1.actor)
      if (conformanceStatement == null) {
        conformanceStatement = ConformanceStatement(
          result._2.id, result._2.shortname, result._2.fullname,
          result._1._2.id, result._1._2.actorId, result._1._2.name,
          result._1._1._2.id, result._1._1._2.shortname, result._1._1._2.fullname,
          0L, 0L, 0L)
        conformanceMap.put(result._1._1._1.actor, conformanceStatement)
      }
      if (TestResultStatus.withName(result._1._1._1.result) == TestResultStatus.SUCCESS) {
        conformanceStatement.completedTests += 1
      } else if (TestResultStatus.withName(result._1._1._1.result) == TestResultStatus.FAILURE) {
        conformanceStatement.failedTests += 1
      } else {
        conformanceStatement.undefinedTests += 1
      }
    }
    var statements = new ListBuffer[ConformanceStatement]
    import scala.collection.JavaConverters._
    for (conformanceEntry <- mapAsScalaMap(conformanceMap)) {
      statements += conformanceEntry._2
    }
    statements.toList
  }


  def deleteAllConformanceStatements(systemId: Long) = {
    val actors = getImplementedActors(systemId)
    var actorIds = scala.collection.mutable.ListBuffer[Long]()
    actors.foreach{ actor =>
      actorIds += actor.id
    }
    deleteConformanceStatments(systemId, actorIds.toList)
  }

  def deleteConformanceStatmentsWrapper(systemId: Long, actorIds: List[Long]) = {
    exec(deleteConformanceStatments(systemId, actorIds).transactionally)
  }

  def deleteConformanceStatments(systemId: Long, actorIds: List[Long]) = {
    val actions = new ListBuffer[DBIO[_]]()

    actorIds foreach { actorId =>
      actions += PersistenceSchema.systemImplementsActors
        .filter(_.systemId === systemId)
        .filter(_.actorId === actorId)
        .delete

      actions += PersistenceSchema.conformanceResults
        .filter(_.actor === actorId)
        .filter(_.sut === systemId)
        .delete

      // Configs
      exec(PersistenceSchema.configs
        .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
        .filter(_._2.actor === actorId)
        .map(_._1).result.map(_.toList))
        .foreach { config =>
          actions += PersistenceSchema.configs
            .filter(_.system === config.system)
            .filter(_.endpoint === config.endpoint)
            .filter(_.parameter === config.parameter)
            .delete
        }

      val optionIds = exec(PersistenceSchema.options
                      .filter(_.actor === actorId)
                      .map(_.id).result.map(_.toList))

      optionIds foreach { optionId =>
        actions += PersistenceSchema.systemImplementsOptions
          .filter(_.systemId === systemId)
          .filter(_.optionId === optionId)
          .delete
      }
    }
    if (actions.isEmpty) {
      DBIO.successful(())
    } else {
      DBIO.seq(actions.map(a => a): _*)
    }
	}

  def getImplementedActorsWrapper(system: Long): List[Actors] = {
    getImplementedActors(system)
  }

  def getImplementedActors(system: Long): List[Actors] = {
    val ids = getActorsForSystem(system)
    exec(PersistenceSchema.actors.filter(_.id inSet ids)
        .sortBy(_.actorId.asc)
      .result.map(_.toList))
  }

  private def getActorsForSystem(system: Long): List[Long] = {
    //1) Get actors that the system implements
    val actors = exec(PersistenceSchema.systemImplementsActors.filter(_.systemId === system).map(_.actorId).result.map(_.toList))
    actors
  }

  def getEndpointConfigurations(endpoint: Long, system: Long): List[Configs] = {
    exec(PersistenceSchema.configs.filter(_.endpoint === endpoint).filter(_.system === system).result.map(_.toList))
  }

  def deleteEndpointConfigurationInternal(systemId: Long, parameterId: Long, endpointId: Long) = {
    PersistenceSchema.configs
      .filter(_.system === systemId)
      .filter(_.parameter === parameterId)
      .filter(_.endpoint === endpointId)
      .delete
  }

  def deleteEndpointConfiguration(systemId: Long, parameterId: Long, endpointId: Long) = {
    exec(deleteEndpointConfigurationInternal(systemId, parameterId, endpointId).transactionally)
    triggerHelper.publishTriggerEvent(new ConformanceStatementUpdatedEvent(getCommunityIdForSystemId(systemId), systemId, getActorIdForEndpointId(endpointId)))
  }

  def saveEndpointConfigurationInternal(forceAdd: Boolean, forceUpdate: Boolean, config: Configs) = {
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

  def saveEndpointConfiguration(config: Configs) = {
    exec(saveEndpointConfigurationInternal(false, false, config).transactionally)
    triggerHelper.publishTriggerEvent(new ConformanceStatementUpdatedEvent(getCommunityIdForSystemId(config.system), config.system, getActorIdForEndpointId(config.endpoint)))
  }

  private def getActorIdForEndpointId(endpointId: Long): Long = {
    exec(PersistenceSchema.endpoints.filter(_.id === endpointId).map(x => x.actor).result.head)
  }

  def getConfigurationsWithEndpointIds(system: Long, ids: List[Long]): List[Configs] = {
    exec(PersistenceSchema.configs.filter(_.system === system).filter(_.endpoint inSet ids).result.map(_.toList))
  }

  def deleteSystemWrapper(systemId: Long) = {
    exec(deleteSystem(systemId).transactionally)
  }

  def deleteSystem(systemId: Long) = {
    testResultManager.updateForDeletedSystem(systemId) andThen
    PersistenceSchema.configs.filter(_.system === systemId).delete andThen
    PersistenceSchema.systemHasAdmins.filter(_.systemId === systemId).delete andThen
    PersistenceSchema.systemImplementsActors.filter(_.systemId === systemId).delete andThen
    PersistenceSchema.systemImplementsOptions.filter(_.systemId === systemId).delete andThen
    PersistenceSchema.conformanceResults.filter(_.sut === systemId).delete andThen
    PersistenceSchema.systemParameterValues.filter(_.system === systemId).delete andThen
    PersistenceSchema.systems.filter(_.id === systemId).delete
  }

  def deleteSystemByOrganization(orgId: Long) = {
    for {
      systemIds <- PersistenceSchema.systems.filter(_.owner === orgId).map(_.id).result
      _ <- DBIO.seq(systemIds.map(systemId => deleteSystem(systemId)): _*)
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

}
