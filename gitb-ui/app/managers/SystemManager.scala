package managers

import java.util

import models.Enums.TestResultStatus
import models.{ConformanceStatementSet, _}
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

  def registerSystem(system: Systems) = {
    DB.withTransaction { implicit session =>
      PersistenceSchema.insertSystem += system
    }
  }

  def updateSystemProfile(systemId: Long, sname: Option[String], fname: Option[String], description: Option[String], version: Option[String]) = {
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
      val systems = PersistenceSchema.systems.filter(_.owner === orgId).list
      systems
    }
  }

  def getVendorSystems(userId: Long): List[Systems] = {
    DB.withSession { implicit session =>
      //1) Get organization id of the user, first
      val orgId = PersistenceSchema.users.filter(_.id === userId).firstOption.get.organization

      //2) Get systems of the organization
      val systems = PersistenceSchema.systems.filter(_.owner === orgId).list
      systems
    }
  }

  def defineConformanceStatement(system: Long, spec: Long, actor: Long, options: Option[List[Long]]) = {
    DB.withTransaction { implicit session =>
      PersistenceSchema.systemImplementsActors.insert(system, spec, actor)
      options match {
        case Some(optionIds) => optionIds foreach ((optionId) => PersistenceSchema.systemImplementsOptions.insert((system, optionId)))
        case _ =>
      }
    }
  }

  def getConformanceStatements(systemId: Long, spec: Option[String], actor: Option[String]): List[ConformanceStatement] = {
    DB.withSession { implicit session =>
      var query = for {
        systemImplementsActors <- PersistenceSchema.systemImplementsActors
        specificationHasActors <- PersistenceSchema.specificationHasActors if specificationHasActors.actorId === systemImplementsActors.actorId
        specifications <- PersistenceSchema.specifications if specifications.id === specificationHasActors.specId
        testCaseHasActors <- PersistenceSchema.testCaseHasActors if specificationHasActors.actorId === testCaseHasActors.actor
        actors <- PersistenceSchema.actors if actors.id === specificationHasActors.actorId
        domains <- PersistenceSchema.domains if domains.id === actors.domain
        testCases <- PersistenceSchema.testCases if testCases.id === testCaseHasActors.testcase
      } yield (systemImplementsActors, specificationHasActors, testCaseHasActors, actors, domains, specifications, testCases)
      query = query.filter(_._1.systemId === systemId)
      if (spec.isDefined && actor.isDefined) {
        query = query
          .filter(_._2.specId === spec.get.toLong)
          .filter(_._2.actorId === actor.get.toLong)
      }
      val results = query.list

      // data: [0] Domain ID, [1] Domain name, [2] Specification ID, [3] Specification name, [4] Actor name, [5] Test case IDs
      val statementMap: util.Map[Long, ConformanceStatementSet] = new util.TreeMap[Long, ConformanceStatementSet]
      results.foreach { result =>
        var data = statementMap.get(result._4.id)
        if (data == null) {
          data = new ConformanceStatementSet(
            result._5.id, result._5.shortname, result._5.fullname,
            result._4.id, result._4.actorId, result._4.name,
            result._6.id, result._6.shortname, result._6.fullname,
            new util.HashSet[Long]())
          statementMap.put(result._4.id, data)
        }
        data.testCaseIds.add(result._7.id)
      }

      val conformanceStatements = new ListBuffer[ConformanceStatement]

      import scala.collection.JavaConversions._
      for (entry <- statementMap.entrySet) {
        var completed = 0
        for (testCaseId <- entry.getValue.testCaseIds) {
          val lastResult = {
            val result = PersistenceSchema.testResults
              .filter(_.testCaseId === testCaseId)
              .filter(_.sutId === systemId)
              .filter(_.endTime isDefined)
              .sortBy(_.endTime.desc)
              .map(_.result)
              .firstOption
            result match {
              case None => TestResultStatus.UNDEFINED
              case Some(resultStr) => TestResultStatus.withName(resultStr)
            }
          }
          if (lastResult == TestResultStatus.SUCCESS) {
            completed += 1
          }
        }
        conformanceStatements.add(ConformanceStatement(
          entry.getValue.domainId, entry.getValue.domainName, entry.getValue.domainNameFull,
          entry.getValue.actorId, entry.getValue.actorName, entry.getValue.actorFull,
          entry.getValue.specificationId, entry.getValue.specificationName, entry.getValue.specificationNameFull,
          completed, entry.getValue.testCaseIds.size()))
      }
      conformanceStatements.toList
    }
  }

  def deleteConformanceStatments(systemId: Long, actorIds: List[Long]) = {
    DB.withSession { implicit session =>
      actorIds foreach { actorId =>
        PersistenceSchema.systemImplementsActors
          .filter(_.systemId === systemId)
          .filter(_.actorId === actorId)
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
	}

  def getImplementedActors(system: Long): List[Actors] = {
    val ids = getActorsForSystem(system)
    DB.withSession { implicit session =>
      PersistenceSchema.actors.filter(_.id inSet ids).list
    }
  }

  private def getActorsForSystem(system: Long): List[Long] = {
    DB.withSession { implicit session =>
      //1) Get actors that the system implements
      val actors = PersistenceSchema.systemImplementsActors.filter(_.systemId === system).map(_.actorId).list

      actors
    }
  }

  def getEndpointConfigurations(endpoint: Long, system: Long): List[Config] = {
    DB.withSession { implicit session =>
      PersistenceSchema.configs.filter(_.endpoint === endpoint).filter(_.system === system).list
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

  def deleteSystemByOrganization(orgId: Long) = {
    DB.withTransaction { implicit session =>
      val systemIds = PersistenceSchema.systems.filter(_.owner === orgId).map(_.id).list
      systemIds foreach { systemId =>
        TestResultManager.updateForDeletedSystem(systemId)
        PersistenceSchema.configs.filter(_.system === systemId).delete
        PersistenceSchema.systemHasAdmins.filter(_.systemId === systemId).delete
        PersistenceSchema.systemHasAdmins.filter(_.systemId === systemId).delete
        PersistenceSchema.systemImplementsActors.filter(_.systemId === systemId).delete
        PersistenceSchema.systemImplementsOptions.filter(_.systemId === systemId).delete
      }
      PersistenceSchema.systems.filter(_.owner === orgId).delete
    }
	}

  def getSystemById(id: Long): Option[Systems] = {
    DB.withSession { implicit session =>
      PersistenceSchema.systems.filter(_.id === id).firstOption
    }
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
      q.list
    }
  }

}
