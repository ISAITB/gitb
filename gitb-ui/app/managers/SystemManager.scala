package managers

import java.util

import javax.inject.{Inject, Singleton}
import models.Enums.TestResultStatus
import models.{ConformanceResult, ConformanceStatement, _}
import org.slf4j.LoggerFactory
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SystemManager @Inject() (testResultManager: TestResultManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("SystemManager")

  def checkSystemExists(sysId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.systems.filter(_.id === sysId).result.headOption)
    firstOption.isDefined
  }

  def registerSystem(adminId: Long, system: Systems) = {
    //1) Get organization id of the admin
    val orgId = exec(PersistenceSchema.users.filter(_.id === adminId).result.head).organization

    //2) Persist new System
    exec((PersistenceSchema.insertSystem += system.withOrganizationId(orgId)).transactionally)
  }

  def copyTestSetup(fromSystem: Long, toSystem: Long) = {
    val actions = new ListBuffer[DBIO[_]]()
    val conformanceStatements = getConformanceStatementReferences(fromSystem)
    var addedStatements = scala.collection.mutable.Set[String]()
    conformanceStatements.foreach { otherConformanceStatement =>
      val key = otherConformanceStatement.spec+"-"+otherConformanceStatement.actor
      if (!addedStatements.contains(key)) {
        addedStatements += key
        actions += defineConformanceStatement(toSystem, otherConformanceStatement.spec, otherConformanceStatement.actor, None)
      }
    }
    DBIO.seq(actions.map(a => a): _*)
  }

  def registerSystemWrapper(system: Systems, otherSystem: Option[Long]) = {
    val id: Long = exec(
      (
        for {
          newSystemId <- registerSystem(system)
          _ <- {
            if (otherSystem.isDefined) {
              copyTestSetup(otherSystem.get, newSystemId)
            } else {
              DBIO.successful(())
            }
          }
        } yield newSystemId
      ).transactionally
    )
    id
  }

  def registerSystem(system: Systems): DBIO[Long] = {
    val action = (PersistenceSchema.insertSystem += system)
    action
  }

  def updateSystemProfile(systemId: Long, sname: Option[String], fname: Option[String], description: Option[String], version: Option[String], otherSystem: Option[Long]) = {
    val actions = new ListBuffer[DBIO[_]]()

    //update short name of the system
    if (sname.isDefined) {
      val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield (s.shortname)
      actions += q.update(sname.get)
      actions += testResultManager.updateForUpdatedSystem(systemId, sname.get)
    }
    //update full name of the system
    if (fname.isDefined) {
      val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield (s.fullname)
      actions += q.update(fname.get)
    }
    //update description of the system
    if (description.isDefined) {
      val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield (s.description)
      actions += q.update(description)
    }
    //update version of the system
    if (version.isDefined) {
      val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield (s.version)
      actions += q.update(version.get)
    }
    // Update test configuration
    if (otherSystem.isDefined) {
      actions += deleteAllConformanceStatements(systemId)
      actions += copyTestSetup(otherSystem.get, systemId)
    }

    if (actions.nonEmpty) {
      exec(DBIO.seq(actions.map(a => a): _*).transactionally)
    }
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

  def defineConformanceStatementWrapper(system: Long, spec: Long, actor: Long, options: Option[List[Long]]) = {
    exec(defineConformanceStatement(system, spec, actor, options).transactionally)
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
    val actions = new ListBuffer[DBIO[_]]()
    val query = PersistenceSchema.testCaseHasActors
        .join(PersistenceSchema.testSuiteHasTestCases).on(_.testcase === _.testcase)
        .filter(_._1.actor === actor)
        .filter(_._1.sut === true)
        .map(r => (r._1.testcase, r._2.testsuite))
    val conformanceInfo = exec(query.result.map(_.toList))
    if (conformanceInfo.isEmpty) {
      actions += DBIO.successful(())
    } else {
      // Add the system to actor mapping. This may be missing due to test suite updates that changed actor roles.
      val qCheck = PersistenceSchema.systemImplementsActors
        .filter(_.systemId === system)
        .filter(_.actorId === actor)
        .filter(_.specId === spec)
      val existingSystemMapping = exec(qCheck.result.headOption)
      if (existingSystemMapping.isDefined) {
        actions += DBIO.successful(())
      } else {
        actions += (PersistenceSchema.systemImplementsActors += (system, spec, actor))
      }
      // Load any existing test results for the system and actor.
      val existingResults = exec(PersistenceSchema.testResults.filter(_.sutId === system).filter(_.actorId === actor).filter(_.testCaseId.isDefined).result.map(_.toList))
      val existingResultsMap = new util.HashMap[Long, (String, String)]
      existingResults.foreach { existingResult =>
        existingResultsMap.put(existingResult.testCaseId.get, (existingResult.sessionId, existingResult.result))
      }
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
    }
    DBIO.seq(actions.map(a => a): _*)
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
    import scala.collection.JavaConversions._
    for (conformanceEntry <- conformanceMap) {
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

  def deleteEndpointConfiguration(systemId: Long, parameterId: Long, endpointId: Long) = {
    exec(PersistenceSchema.configs
      .filter(_.system === systemId)
      .filter(_.parameter === parameterId)
      .filter(_.endpoint === endpointId)
      .delete.transactionally)
  }

  def saveEndpointConfiguration(config: Configs) = {
//    DB.withTransaction { implicit session =>
    val size = exec(PersistenceSchema.configs
      .filter(_.system === config.system)
      .filter(_.parameter === config.parameter)
      .filter(_.endpoint === config.endpoint)
      .size.result)
    var action: DBIO[_] = null
    if (size == 0) {
      action = (PersistenceSchema.configs += config)
    } else {
      action = PersistenceSchema.configs
        .filter(_.system === config.system)
        .filter(_.parameter === config.parameter)
        .filter(_.endpoint === config.endpoint)
        .update(config)
    }
    exec(action.transactionally)
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

}
