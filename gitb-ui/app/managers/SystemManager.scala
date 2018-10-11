package managers

import java.util

import models.Enums.TestResultStatus
import models.{ConformanceResult, ConformanceStatement, _}
import org.slf4j.LoggerFactory
import persistence.db._

import scala.collection.mutable.ListBuffer
import scala.slick.driver.MySQLDriver.simple._

object SystemManager extends BaseManager {
  def logger = LoggerFactory.getLogger("SystemManager")

  def checkSystemExists(sysId: Long): Boolean = {
    DB.withSession { implicit session =>
      val firstOption = PersistenceSchema.systems.filter(_.id === sysId).firstOption
      firstOption.isDefined
    }
  }

  def registerSystem(adminId: Long, system: Systems) = {
    DB.withSession { implicit session =>
      //1) Get organization id of the admin
      val orgId = PersistenceSchema.users.filter(_.id === adminId).firstOption.get.organization

      //2) Persist new System
      PersistenceSchema.insertSystem += system.withOrganizationId(orgId)
    }
  }

  def copyTestSetup(fromSystem: Long, toSystem: Long)(implicit session:Session) = {
    val conformanceStatements = SystemManager.getConformanceStatementReferences(fromSystem)
    var addedStatements = scala.collection.mutable.Set[String]()
    conformanceStatements.foreach { otherConformanceStatement =>
      val key = otherConformanceStatement.spec+"-"+otherConformanceStatement.actor
      if (!addedStatements.contains(key)) {
        addedStatements += key
        SystemManager.defineConformanceStatement(toSystem, otherConformanceStatement.spec, otherConformanceStatement.actor, None)
      }
    }
  }

  def registerSystemWrapper(system: Systems, otherSystem: Option[Long]) = {
    DB.withTransaction { implicit session =>
      val newSystemId = registerSystem(system)
      if (otherSystem.isDefined) {
        copyTestSetup(otherSystem.get, newSystemId)
      }
      newSystemId
    }
  }

  def registerSystem(system: Systems)(implicit session: Session) = {
    PersistenceSchema.insertSystem += system
  }

  def updateSystemProfile(systemId: Long, sname: Option[String], fname: Option[String], description: Option[String], version: Option[String], otherSystem: Option[Long]) = {
    DB.withTransaction { implicit session =>
      //update short name of the system
      if (sname.isDefined) {
        val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield (s.shortname)
        q.update(sname.get)

        TestResultManager.updateForUpdatedSystem(systemId, sname.get)
      }
      //update full name of the system
      if (fname.isDefined) {
        val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield (s.fullname)
        q.update(fname.get)
      }
      //update description of the system
      if (description.isDefined) {
        val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield (s.description)
        q.update(description)
      }
      //update version of the system
      if (version.isDefined) {
        val q = for {s <- PersistenceSchema.systems if s.id === systemId} yield (s.version)
        q.update(version.get)
      }
      // Update test configuration
      if (otherSystem.isDefined) {
        deleteAllConformanceStatements(systemId)
        copyTestSetup(otherSystem.get, systemId)
      }
    }
  }

  def getSystemProfile(systemId: Long): models.System = {
    DB.withSession { implicit session =>
      //1) Get system info
      val s = PersistenceSchema.systems.filter(_.id === systemId).firstOption.get

      //2) Get organization info
      val o = PersistenceSchema.organizations.filter(_.id === s.owner).firstOption.get

      //3) Get admins
      val list = PersistenceSchema.systemHasAdmins.filter(_.systemId === systemId).map(_.userId).list
      var admins: List[Users] = List()
      list.foreach { adminId =>
        val user = PersistenceSchema.users.filter(_.id === adminId).firstOption.get
        admins ::= user
      }

      //4) Merge all info and return
      val system: models.System = new models.System(s, o, admins)
      system
    }
  }

  def getSystemsByOrganization(orgId: Long): List[Systems] = {
    DB.withSession { implicit session =>
      val systems = PersistenceSchema.systems.filter(_.owner === orgId)
          .sortBy(_.shortname.asc)
        .list
      systems
    }
  }

  def getVendorSystems(userId: Long): List[Systems] = {
    DB.withSession { implicit session =>
      //1) Get organization id of the user, first
      val orgId = PersistenceSchema.users.filter(_.id === userId).firstOption.get.organization

      //2) Get systems of the organization
      val systems = PersistenceSchema.systems.filter(_.owner === orgId)
          .sortBy(_.shortname.asc)
        .list
      systems
    }
  }

  def defineConformanceStatementWrapper(system: Long, spec: Long, actor: Long, options: Option[List[Long]]) = {
    DB.withTransaction { implicit session =>
      defineConformanceStatement(system, spec, actor, options)
    }
  }

  def defineConformanceStatement(system: Long, spec: Long, actor: Long, options: Option[List[Long]])(implicit session:Session) = {
    PersistenceSchema.systemImplementsActors.insert(system, spec, actor)
    options match {
      case Some(optionIds) => optionIds foreach ((optionId) => PersistenceSchema.systemImplementsOptions.insert((system, optionId)))
      case _ =>
    }
    // Load any existing test results for the system and actor.
    val existingResults = PersistenceSchema.testResults.filter(_.sutId === system).filter(_.actorId === actor).filter(_.testCaseId.isDefined).list
    val existingResultsMap = new util.HashMap[Long, (String, String)]
    existingResults.foreach { existingResult =>
      existingResultsMap.put(existingResult.testCaseId.get, (existingResult.sessionId, existingResult.result))
    }
    var query = for {
      testCaseHasActors <- PersistenceSchema.testCaseHasActors
      testSuiteHasTestCases <- PersistenceSchema.testSuiteHasTestCases if testSuiteHasTestCases.testcase === testCaseHasActors.testcase
    } yield (testCaseHasActors, testSuiteHasTestCases)
    val conformanceInfo = query.filter(_._1.actor === actor).list
    conformanceInfo.foreach { conformanceInfoEntry =>
      val testCase = conformanceInfoEntry._1._1
      val testSuite = conformanceInfoEntry._2._1
      var result = TestResultStatus.UNDEFINED
      var sessionId: Option[String] = None
      if (existingResultsMap.containsKey(testCase)) {
        val existingData = existingResultsMap.get(testCase)
        sessionId = Some(existingData._1)
        result = TestResultStatus.withName(existingData._2)
      }
      PersistenceSchema.conformanceResults.insert(ConformanceResult(0L, system, spec, actor, testSuite, testCase, result.toString, sessionId))
    }
  }

  def getConformanceStatementReferences(systemId: Long)(implicit session: Session) = {
    DB.withSession { implicit session =>
      //1) Get organization id of the user, first
      val conformanceStatements = PersistenceSchema.conformanceResults.filter(_.sut === systemId).list
      conformanceStatements
    }
  }

  def getConformanceStatements(systemId: Long, spec: Option[String], actor: Option[String]): List[ConformanceStatement] = {
    DB.withSession { implicit session =>
      var query = for {
        conformanceResults <- PersistenceSchema.conformanceResults
        specifications <- PersistenceSchema.specifications if specifications.id === conformanceResults.spec
        actors <- PersistenceSchema.actors if actors.id === conformanceResults.actor
        domains <- PersistenceSchema.domains if domains.id === actors.domain
      } yield (conformanceResults, specifications, actors, domains)
      query = query.filter(_._1.sut === systemId)
      if (spec.isDefined && actor.isDefined) {
        query = query
          .filter(_._1.spec === spec.get.toLong)
          .filter(_._1.actor === actor.get.toLong)
      }
      query = query.sortBy(x => (x._4.fullname, x._2.fullname, x._3.name))

      val results = query.list
      val conformanceMap = new util.LinkedHashMap[Long, ConformanceStatement]
      results.foreach { result =>
        var conformanceStatement = conformanceMap.get(result._1.actor)
        if (conformanceStatement == null) {
          conformanceStatement = ConformanceStatement(
            result._4.id, result._4.shortname, result._4.fullname,
            result._3.id, result._3.actorId, result._3.name,
            result._2.id, result._2.shortname, result._2.fullname,
            0L, 0L)
          conformanceMap.put(result._1.actor, conformanceStatement)
        }
        conformanceStatement.totalTests += 1
        if (TestResultStatus.withName(result._1.result) == TestResultStatus.SUCCESS) {
          conformanceStatement.completedTests += 1
        }
      }
      var statements = new ListBuffer[ConformanceStatement]
      import scala.collection.JavaConversions._
      for (conformanceEntry <- conformanceMap) {
        statements += conformanceEntry._2
      }
      statements.toList
    }
  }


  def deleteAllConformanceStatements(systemId: Long)(implicit session:Session) = {
    val actors = getImplementedActors(systemId)
    var actorIds = scala.collection.mutable.ListBuffer[Long]()
    actors.foreach{ actor =>
      actorIds += actor.id
    }
    deleteConformanceStatments(systemId, actorIds.toList)
  }

  def deleteConformanceStatmentsWrapper(systemId: Long, actorIds: List[Long]) = {
    DB.withTransaction { implicit session =>
      deleteConformanceStatments(systemId, actorIds)
    }
  }

  def deleteConformanceStatments(systemId: Long, actorIds: List[Long])(implicit session:Session) = {
    actorIds foreach { actorId =>
      PersistenceSchema.systemImplementsActors
        .filter(_.systemId === systemId)
        .filter(_.actorId === actorId)
        .delete

      PersistenceSchema.conformanceResults
        .filter(_.actor === actorId)
        .filter(_.sut === systemId)
        .delete

      val optionIds = PersistenceSchema.options
                      .filter(_.actor === actorId)
                      .map(_.id).list

      optionIds foreach { optionId =>
        PersistenceSchema.systemImplementsOptions
          .filter(_.systemId === systemId)
          .filter(_.optionId === optionId)
          .delete
      }
    }
	}

  def getImplementedActorsWrapper(system: Long): List[Actors] = {
    DB.withSession { implicit session =>
      getImplementedActors(system)
    }
  }

  def getImplementedActors(system: Long)(implicit session:Session): List[Actors] = {
    val ids = getActorsForSystem(system)
    PersistenceSchema.actors.filter(_.id inSet ids)
        .sortBy(_.actorId.asc)
      .list
  }

  private def getActorsForSystem(system: Long)(implicit session: Session): List[Long] = {
    //1) Get actors that the system implements
    val actors = PersistenceSchema.systemImplementsActors.filter(_.systemId === system).map(_.actorId).list
    actors
  }

  def getEndpointConfigurations(endpoint: Long, system: Long): List[Config] = {
    DB.withSession { implicit session =>
      PersistenceSchema.configs.filter(_.endpoint === endpoint).filter(_.system === system).list
    }
  }

  def deleteEndpointConfiguration(systemId: Long, parameterId: Long, endpointId: Long) = {
    DB.withTransaction { implicit session =>
      PersistenceSchema.configs
        .filter(_.system === systemId)
        .filter(_.parameter === parameterId)
        .filter(_.endpoint === endpointId)
        .delete
    }
  }

  def saveEndpointConfiguration(config: Config) = {
    DB.withTransaction { implicit session =>
      val size = PersistenceSchema.configs
        .filter(_.system === config.system)
        .filter(_.parameter === config.parameter)
        .filter(_.endpoint === config.endpoint)
        .size.run
      if (size == 0) {
        PersistenceSchema.configs
          .insert(config)
      } else {
        PersistenceSchema.configs
          .filter(_.system === config.system)
          .filter(_.parameter === config.parameter)
          .filter(_.endpoint === config.endpoint)
          .update(config)
      }
    }
  }

  def getConfigurationsWithEndpointIds(system: Long, ids: List[Long]): List[Config] = {
    DB.withSession { implicit session =>
      PersistenceSchema.configs.filter(_.system === system).filter(_.endpoint inSet ids).list
    }
  }

  def deleteSystemWrapper(systemId: Long) = {
    DB.withTransaction { implicit session =>
      deleteSystem(systemId)
    }
  }

  def deleteSystem(systemId: Long)(implicit session: Session) = {
    TestResultManager.updateForDeletedSystem(systemId)
    PersistenceSchema.configs.filter(_.system === systemId).delete
    PersistenceSchema.systemHasAdmins.filter(_.systemId === systemId).delete
    PersistenceSchema.systemImplementsActors.filter(_.systemId === systemId).delete
    PersistenceSchema.systemImplementsOptions.filter(_.systemId === systemId).delete
    PersistenceSchema.conformanceResults.filter(_.sut === systemId).delete
    PersistenceSchema.systems.filter(_.id === systemId).delete
  }

  def deleteSystemByOrganization(orgId: Long)(implicit session: Session) = {
    val systemIds = PersistenceSchema.systems.filter(_.owner === orgId).map(_.id).list
    systemIds foreach { systemId =>
      deleteSystem(systemId)
    }
	}

  def getSystemByIdWrapper(id: Long): Option[Systems] = {
    DB.withSession { implicit session =>
      PersistenceSchema.systems.filter(_.id === id).firstOption
    }
  }

  def getSystemById(id: Long)(implicit session: Session): Option[Systems] = {
    PersistenceSchema.systems.filter(_.id === id).firstOption
  }

  def getSystems(ids: Option[List[Long]]): List[Systems] = {
    DB.withSession { implicit session =>
      val q = ids match {
        case Some(idList) => {
          PersistenceSchema.systems
            .filter(_.id inSet idList)
        }
        case None => {
          PersistenceSchema.systems
        }
      }
      q.sortBy(_.shortname.asc)
        .list
    }
  }

}
