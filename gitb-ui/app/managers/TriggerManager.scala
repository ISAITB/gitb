package managers

import com.gitb.core.{AnyContent, ValueEmbeddingEnumeration}
import com.gitb.ps._
import com.gitb.utils.{ClasspathResourceResolver, XMLUtils}
import jakarta.xml.bind.JAXBElement
import models.Enums.TriggerDataType.TriggerDataType
import models.Enums.TriggerEventType._
import models.Enums.TriggerServiceType.TriggerServiceType
import models.Enums.{TriggerDataType, TriggerServiceType}
import models._
import org.apache.commons.io.{FileUtils, IOUtils}
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.Environment
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json._
import play.api.libs.ws.WSClient
import utils.{JsonUtil, MimeUtil, RepositoryUtils}

import java.io.{ByteArrayOutputStream, StringReader}
import java.net.URI
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.{Files, Path}
import java.util.Base64
import javax.inject.{Inject, Singleton}
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success}

@Singleton
class TriggerManager @Inject()(env: Environment,
                               ws: WSClient,
                               repositoryUtils: RepositoryUtils,
                               triggerDataLoader: TriggerDataLoader,
                               reportManager: ReportManager,
                               dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  private val logger = LoggerFactory.getLogger(classOf[TriggerManager])

  def getCommunityId(triggerId: Long): Long = {
    exec(PersistenceSchema.triggers.filter(_.id === triggerId).map(_.community).result.head)
  }

  def checkUniqueName(name: String, communityId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.triggers.filter(_.community === communityId).filter(_.name === name).result.headOption)
    firstOption.isEmpty
  }

  def checkUniqueName(triggerIdToIgnore: Long, name: String, communityId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.triggers.filter(_.community === communityId).filter(_.name === name).filter(_.id =!= triggerIdToIgnore).result.headOption)
    firstOption.isEmpty
  }

  def getTriggersByCommunity(communityId: Long): List[Triggers] = {
    exec(PersistenceSchema.triggers
      .filter(_.community === communityId)
      .map(x => (x.id, x.name, x.description, x.url, x.eventType, x.serviceType, x.active, x.community, x.latestResultOk))
      .sortBy(_._2.asc)
      .result
    ).map(x => Triggers(x._1, x._2, x._3, x._4, x._5, x._6, None, x._7, x._9, None, x._8)).toList
  }

  def createTrigger(trigger: Trigger): Long = {
    exec(createTriggerInternal(trigger).transactionally)
  }

  private[managers] def createTriggerInternal(trigger: Trigger): DBIO[Long] = {
    for {
      triggerId <- PersistenceSchema.insertTriggers += trigger.trigger
      _ <- setTriggerDataInternal(triggerId, trigger.data, isNewTrigger = true)
    } yield triggerId
  }

  def getTriggerAndDataById(triggerId: Long): Trigger = {
    new Trigger(
      exec(PersistenceSchema.triggers.filter(_.id === triggerId).result.head),
      Some(exec(PersistenceSchema.triggerData.filter(_.trigger === triggerId).result).toList)
    )
  }

  def getTriggerAndDataByCommunityId(communityId: Long): List[Trigger] = {
    val triggerList = exec(PersistenceSchema.triggers.filter(_.community === communityId).result).toList
    val triggerDataList = exec(PersistenceSchema.triggerData.join(PersistenceSchema.triggers).on(_.trigger === _.id).filter(_._2.community === communityId).map(x => x._1).result).toList
    val triggerDataMap = mutable.Map[Long, ListBuffer[TriggerData]]()
    triggerDataList.foreach { dataItem =>
      var items = triggerDataMap.get(dataItem.trigger)
      if (items.isEmpty) {
        items = Some(ListBuffer[TriggerData]())
        triggerDataMap += (dataItem.trigger -> items.get)
      }
      items.get += dataItem
    }
    triggerList.map { trigger =>
      val items = triggerDataMap.get(trigger.id)
      if (items.isDefined) {
        new Trigger(trigger, Some(items.get.toList))
      } else {
        new Trigger(trigger, None)
      }
    }
  }

  def getTriggersAndDataByCommunityAndType(communityId: Long, eventType: TriggerEventType): Option[List[Trigger]] = {
    val result = exec(
      for {
        triggers <- PersistenceSchema.triggers
          .filter(_.community === communityId)
          .filter(_.eventType === eventType.id.toShort)
          .filter(_.active === true)
          .map(x => (x.id, x.url, x.operation, x.latestResultOk, x.serviceType))
          .result
        triggerData <- {
          if (triggers.nonEmpty) {
            PersistenceSchema.triggerData.filter(_.trigger inSet triggers.map(t => t._1)).result
          } else {
            DBIO.successful(List())
          }
        }
      } yield (triggers, triggerData)
    )
    val triggers = result._1
    val triggerData = result._2
    if (triggers.nonEmpty) {
      val itemMap = mutable.Map[Long, ListBuffer[TriggerData]]()
      triggerData.foreach { triggerItem =>
        var data = itemMap.get(triggerItem.trigger)
        if (data.isEmpty) {
          data = Some(new ListBuffer[TriggerData]())
          itemMap += (triggerItem.trigger -> data.get)
        }
        data.get += TriggerData(triggerItem.dataType, triggerItem.dataId, triggerItem.trigger)
      }
      Some(triggers.map { trigger =>
        var triggerData: Option[List[TriggerData]] = None
        if (itemMap.contains(trigger._1)) {
          triggerData = Some(itemMap(trigger._1).toList)
        }
        new Trigger(
          Triggers(trigger._1, "", None, trigger._2, eventType.id.toShort, trigger._5, trigger._3, active = true, trigger._4, None, communityId),
          triggerData
        )
      }.toList)
    } else {
      None
    }
  }

  def updateTrigger(trigger: Trigger): Unit = {
    exec(updateTriggerInternal(trigger).transactionally)
  }

  def clearStatus(triggerId: Long): Unit = {
    exec((for { t <- PersistenceSchema.triggers.filter(_.id === triggerId) } yield (t.latestResultOk, t.latestResultOutput)).update(None, None).transactionally)
  }

  private[managers] def updateTriggerInternal(trigger: Trigger): DBIO[_] = {
    val q = for { t <- PersistenceSchema.triggers if t.id === trigger.trigger.id } yield (t.name, t.description, t.url, t.eventType, t.serviceType, t.operation, t.active)
    q.update(trigger.trigger.name, trigger.trigger.description, trigger.trigger.url, trigger.trigger.eventType, trigger.trigger.serviceType, trigger.trigger.operation, trigger.trigger.active) andThen
      setTriggerDataInternal(trigger.trigger.id, trigger.data, isNewTrigger = false)
  }

  def deleteTrigger(triggerId: Long): Unit = {
    exec(deleteTriggerInternal(triggerId).transactionally)
  }

  private[managers] def deleteTriggersByCommunity(communityId: Long): DBIO[_] = {
    for {
      triggerIds <- PersistenceSchema.triggers.filter(_.community === communityId).map(x => x.id).result
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        triggerIds.foreach { triggerId =>
          dbActions += deleteTriggerInternal(triggerId)
        }
        toDBIO(dbActions)
      }
    } yield ()
  }

  private[managers] def deleteTriggerInternal(triggerId: Long): DBIO[_] = {
    deleteTriggerDataInternal(triggerId) andThen
      PersistenceSchema.triggers.filter(_.id === triggerId).delete
  }

  def deleteTriggerDataOfCommunityAndDomain(communityId: Long, domainId: Long): DBIO[_] = {
    for {
      domainParameterIds <- {
        PersistenceSchema.domainParameters.filter(_.domain === domainId).map(x => x.id).result
      }
      triggerDataToDelete <- {
        PersistenceSchema.triggerData
          .join(PersistenceSchema.triggers).on(_.trigger === _.id)
          .filter(_._2.community === communityId)
          .filter(_._1.dataType === TriggerDataType.DomainParameter.id.toShort)
          .filter(_._1.dataId inSet domainParameterIds)
          .map(x => x._1)
          .result
      }
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        triggerDataToDelete.foreach { data =>
          actions += PersistenceSchema.triggerData
            .filter(_.dataId === data.dataId)
            .filter(_.dataType === data.dataType)
            .filter(_.trigger === data.trigger)
            .delete
        }
        toDBIO(actions)
      }
    } yield ()
  }

  private def deleteTriggerDataInternal(triggerId: Long): DBIO[_] = {
    PersistenceSchema.triggerData.filter(_.trigger === triggerId).delete
  }

  private def setTriggerDataInternal(triggerId: Long, data: Option[List[TriggerData]], isNewTrigger: Boolean): DBIO[_] = {
    val dbActions = ListBuffer[DBIO[_]]()
    if (!isNewTrigger) {
      // Delete previous data (only if needed - new)
      dbActions += deleteTriggerDataInternal(triggerId)
    }
    if (data.isDefined) {
      data.get.foreach { dataItem =>
        dbActions += (PersistenceSchema.triggerData += TriggerData(dataItem.dataType, dataItem.dataId, triggerId))
      }
    }
    toDBIO(dbActions)
  }

  private[managers] def deleteTriggerDataByDataType(dataId: Long, dataType: TriggerDataType): DBIO[_] = {
    PersistenceSchema.triggerData.filter(_.dataId === dataId).filter(_.dataType === dataType.id.toShort).delete
  }

  private def hasTriggerDataType(data: List[TriggerData], dataType: TriggerDataType): Boolean = {
    data.foreach { dataItem =>
      if (TriggerDataType.apply(dataItem.dataType) == dataType) {
        return true
      }
    }
    false
  }

  private def toAnyContent(name: String, dataType: String, value: String, embeddingMethod: Option[ValueEmbeddingEnumeration]): AnyContent = {
    populateAnyContent(None, name, dataType, value, embeddingMethod)
  }

  private def populateAnyContent(target: Option[AnyContent], name: String, dataType: String, value: String, embeddingMethod: Option[ValueEmbeddingEnumeration]): AnyContent = {
    val result = if (target.isDefined) {
      target.get
    } else {
      new AnyContent()
    }
    result.setName(name)
    result.setType(dataType)
    result.setValue(value)
    if (embeddingMethod.isDefined) {
      result.setEmbeddingMethod(embeddingMethod.get)
    }
    result
  }

  private def getOrInitiatizeContentMap(name: String, map: Option[AnyContent]): Option[AnyContent] = {
    if (map.isDefined) {
      map
    } else {
      val newMap = new AnyContent()
      newMap.setName(name)
      newMap.setType("map")
      Some(newMap)
    }
  }

  private def loadOrganisationParameterData(parameterIds: List[Long], organisationId: Option[Long]): Map[Long, (String, String, Option[String], Option[Long], Option[String])] = {
    var data: List[(Long, String, String, Option[String], Option[Long], Option[String])] = null // id, key, kind, value, orgId, contentType
    if (organisationId.isDefined) {
      data = exec(PersistenceSchema.organisationParameters
        .join(PersistenceSchema.organisationParameterValues).on(_.id === _.parameter)
        .filter(_._1.id inSet parameterIds).filter(_._2.organisation === organisationId.get)
        .map(x => (x._1.id, x._1.testKey, x._1.kind, x._2.value, x._2.organisation, x._2.contentType))
        .result
      ).map(x => (x._1, x._2, x._3, Some(x._4), Some(x._5), x._6)).toList
    } else {
      data = exec(PersistenceSchema.organisationParameters.filter(_.id inSet parameterIds).map(x => (x.id, x.testKey, x.kind)).result).map(x => (x._1, x._2, x._3, None, None, None)).toList
    }
    val resultMap = mutable.Map[Long, (String, String, Option[String], Option[Long], Option[String])]()
    data.foreach { dataItem =>
      resultMap += (dataItem._1 -> (dataItem._2, dataItem._3, dataItem._4, dataItem._5, dataItem._6))
    }
    resultMap.iterator.toMap
  }

  private def loadSystemParameterData(parameterIds: List[Long], systemId: Option[Long]): Map[Long, (String, String, Option[String], Option[Long], Option[String])] = {
    var data: List[(Long, String, String, Option[String], Option[Long], Option[String])] = null // id, key, kind, value, sysId, contentType
    if (systemId.isDefined) {
      data = exec(PersistenceSchema.systemParameters
        .join(PersistenceSchema.systemParameterValues).on(_.id === _.parameter)
        .filter(_._1.id inSet parameterIds).filter(_._2.system === systemId.get)
        .map(x => (x._1.id, x._1.testKey, x._1.kind, x._2.value, x._2.system, x._2.contentType))
        .result
      ).map(x => (x._1, x._2, x._3, Some(x._4), Some(x._5), x._6)).toList
    } else {
      data = exec(PersistenceSchema.systemParameters.filter(_.id inSet parameterIds).map(x => (x.id, x.testKey, x.kind)).result).map(x => (x._1, x._2, x._3, None, None, None)).toList
    }
    val resultMap = mutable.Map[Long, (String, String, Option[String], Option[Long], Option[String])]()
    data.foreach { dataItem =>
      resultMap += (dataItem._1 -> (dataItem._2, dataItem._3, dataItem._4, dataItem._5, dataItem._6))
    }
    resultMap.iterator.toMap
  }

  private def loadDomainParameterData(parameterIds: List[Long]): Map[Long, (String, String, Option[String], Long, Option[String])] = {
    val data: List[(Long, String, String, Option[String], Long, Option[String])] = exec(
      PersistenceSchema.domainParameters.filter(_.id inSet parameterIds).map(x => (x.id, x.name, x.kind, x.value, x.domain, x.contentType)).result
    ).map(x => (x._1, x._2, x._3, x._4, x._5, x._6)).toList
    val resultMap = mutable.Map[Long, (String, String, Option[String], Long, Option[String])]()
    data.foreach { dataItem =>
      resultMap += (dataItem._1 -> (dataItem._2, dataItem._3, dataItem._4, dataItem._5, dataItem._6))
    }
    resultMap.iterator.toMap
  }

  private def loadStatementParameterKeys(parameterIds: List[Long]): Map[Long, String] = {
    exec(PersistenceSchema.parameters.filter(_.id inSet parameterIds).map(x => (x.id, x.testKey)).result).toMap
  }

  private def loadStatementParameterData(parameterKeys: Map[Long, String], actorId: Option[Long], systemId: Option[Long], communityId: Long): Map[String, (Long, String, Option[String], Option[Long], Option[String])] = {
    var data: List[(String, Long, String, Option[String], Option[Long], Option[String])] = null // key, ID, kind, value, system ID, contentType
    if (systemId.isDefined && actorId.isDefined) {
      data = exec(
        PersistenceSchema.parameters
          .join(PersistenceSchema.configs).on(_.id === _.parameter)
          .join(PersistenceSchema.endpoints).on(_._1.endpoint === _.id)
          .filter(_._2.actor === actorId.get)
          .filter(_._1._2.system === systemId.get)
          .filter(_._1._1.name inSet parameterKeys.values)
          .map(x => (x._1._1.testKey, x._1._1.id, x._1._1.kind, x._1._2.value, x._1._2.contentType))
          .result
      ).map(x => (x._1, x._2, x._3, Some(x._4), systemId, x._5)).toList
    } else {
      data = exec(
        for {
          domainId <- PersistenceSchema.communities.filter(_.id === communityId).map(_.domain).result.head
          parameterData <-
              PersistenceSchema.parameters
                .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
                .join(PersistenceSchema.actors).on(_._2.actor === _.id)
                .filterOpt(domainId)((table, domainIdValue) => table._2.domain === domainIdValue)
                .filter(_._1._1.testKey inSet parameterKeys.values)
                .map(x => (x._1._1.testKey, x._1._1.id, x._1._1.kind))
                .result
        } yield parameterData
      ).map(x => (x._1, x._2, x._3, None, None, None)).toList
    }
    val resultMap = mutable.Map[String, (Long, String, Option[String], Option[Long], Option[String])]()
    data.foreach { dataItem =>
      resultMap += (dataItem._1 -> ((dataItem._2, dataItem._3, dataItem._4, dataItem._5, dataItem._6)))
    }
    resultMap.iterator.toMap
  }

  private def fromCache[T](cache: mutable.Map[String, Any], cacheKey: String, fnLoad: () => T): T = {
    if (cache.contains(cacheKey)) {
      cache(cacheKey).asInstanceOf[T]
    } else {
      val data = fnLoad.apply()
      cache += (cacheKey -> data)
      data
    }
  }

  private def getSampleTestReport(): String = {
    IOUtils.resourceToString("other/sampleTestCaseReport.xml", StandardCharsets.UTF_8, env.classLoader)
  }

  private def prepareTriggerInput(trigger: Trigger, dataCache: mutable.Map[String, Any], callbacks: Option[TriggerCallbacks]): ProcessRequest = {
    var organisationParameterData: Map[Long, (String, String, Option[String], Option[Long], Option[String])] = null
    var systemParameterData: Map[Long, (String, String, Option[String], Option[Long], Option[String])] = null
    var domainParameterData: Map[Long, (String, String, Option[String], Long, Option[String])] = null
    var statementParameterData: Map[String, (Long, String, Option[String], Option[Long], Option[String])] = null // key => (kind, value, system ID, contentType)
    var statementParameterKeys: Map[Long, String] = null
    if (trigger.data.isDefined) {
      if (hasTriggerDataType(trigger.data.get, TriggerDataType.OrganisationParameter)) {
        organisationParameterData = fromCache(dataCache, "organisationParameterData", () => loadOrganisationParameterData(trigger.data.get.map(x => x.dataId), TriggerCallbacks.organisationId(callbacks)))
      }
      if (hasTriggerDataType(trigger.data.get, TriggerDataType.SystemParameter)) {
        systemParameterData = fromCache(dataCache, "systemParameterData", () => loadSystemParameterData(trigger.data.get.map(x => x.dataId), TriggerCallbacks.systemId(callbacks)))
      }
      if (hasTriggerDataType(trigger.data.get, TriggerDataType.DomainParameter)) {
        domainParameterData = fromCache(dataCache, "domainParameterData", () => loadDomainParameterData(trigger.data.get.map(x => x.dataId)))
      }
      if (hasTriggerDataType(trigger.data.get, TriggerDataType.StatementParameter)) {
        // Load the parameter keys (names) (the recorded IDs in the trigger definition are only indicative).
        statementParameterKeys = fromCache(dataCache, "statementParameterKeys", () => loadStatementParameterKeys(trigger.data.get.map(x => x.dataId)))
        statementParameterData = fromCache(dataCache, "statementParameterData", () => loadStatementParameterData(statementParameterKeys, TriggerCallbacks.actorId(callbacks), TriggerCallbacks.systemId(callbacks), trigger.trigger.community))
      }
    }
    val request = new ProcessRequest()
    if (trigger.trigger.operation.isDefined && !trigger.trigger.operation.get.isBlank) {
      request.setOperation(trigger.trigger.operation.get)
    }
    var communityData: Option[AnyContent] = None
    var organisationData: Option[AnyContent] = None
    var systemData: Option[AnyContent] = None
    var actorData: Option[AnyContent] = None
    var specificationData: Option[AnyContent] = None
    var testSessionData: Option[AnyContent] = None
    var testReportData: Option[AnyContent] = None
    var orgParamData: Option[AnyContent] = None
    var sysParamData: Option[AnyContent] = None
    var domainParamData: Option[AnyContent] = None
    var statementParamData: Option[AnyContent] = None
    if (trigger.data.isDefined) {
      trigger.data.get.foreach { dataItem =>
        TriggerDataType.apply(dataItem.dataType) match {
          case TriggerDataType.Community =>
            val data:(String, String) = fromCache(dataCache, "communityData", () => exec(PersistenceSchema.communities.filter(_.id === trigger.trigger.community).map(x => (x.shortname, x.fullname)).result.head))
            communityData = getOrInitiatizeContentMap("community", communityData)
            communityData.get.getItem.add(toAnyContent("id", "number", trigger.trigger.community.toString, None))
            communityData.get.getItem.add(toAnyContent("shortName", "string", data._1, None))
            communityData.get.getItem.add(toAnyContent("fullName", "string", data._2, None))
          case TriggerDataType.Organisation =>
            organisationData = getOrInitiatizeContentMap("organisation", organisationData)
            val organisationId:Option[Long] = fromCache(dataCache, "organisationId", () => TriggerCallbacks.organisationId(callbacks))
            if (organisationId.isEmpty) {
              organisationData.get.getItem.add(toAnyContent("id", "number", "123", None))
              organisationData.get.getItem.add(toAnyContent("shortName", "string", "Organisation short name", None))
              organisationData.get.getItem.add(toAnyContent("fullName", "string", "Organisation full name", None))
            } else {
              val organisation = fromCache(dataCache, "organisationData", () => exec(PersistenceSchema.organizations.filter(_.id === organisationId.get).map(x => (x.shortname, x.fullname)).result.headOption))
              if (organisation.isDefined) {
                organisationData.get.getItem.add(toAnyContent("id", "number", organisationId.get.toString, None))
                organisationData.get.getItem.add(toAnyContent("shortName", "string", organisation.get._1, None))
                organisationData.get.getItem.add(toAnyContent("fullName", "string", organisation.get._2, None))
              }
            }
          case TriggerDataType.System =>
            systemData = getOrInitiatizeContentMap("system", systemData)
            val systemId:Option[Long] = fromCache(dataCache, "systemId", () => TriggerCallbacks.systemId(callbacks))
            if (systemId.isEmpty) {
              systemData.get.getItem.add(toAnyContent("id", "number", "123", None))
              systemData.get.getItem.add(toAnyContent("shortName", "string", "System short name", None))
              systemData.get.getItem.add(toAnyContent("fullName", "string", "System full name", None))
            } else {
              val system = fromCache(dataCache, "systemData", () => exec(PersistenceSchema.systems.filter(_.id === systemId.get).map(x => (x.shortname, x.fullname)).result.headOption))
              if (system.isDefined) {
                systemData.get.getItem.add(toAnyContent("id", "number", systemId.get.toString, None))
                systemData.get.getItem.add(toAnyContent("shortName", "string", system.get._1, None))
                systemData.get.getItem.add(toAnyContent("fullName", "string", system.get._2, None))
              }
            }
          case TriggerDataType.Specification =>
            specificationData = getOrInitiatizeContentMap("specification", specificationData)
            val actorId:Option[Long] = fromCache(dataCache, "actorId", () => TriggerCallbacks.actorId(callbacks))
            var specificationId:Option[Long] = None
            if (actorId.isDefined) {
              specificationId = fromCache(dataCache, "specificationId", () => exec(PersistenceSchema.specificationHasActors.filter(_.actorId === actorId.get).map(x => x.specId).result.headOption))
            }
            if (actorId.isEmpty || specificationId.isEmpty) {
              specificationData.get.getItem.add(toAnyContent("id", "number", "123", None))
              specificationData.get.getItem.add(toAnyContent("shortName", "string", "Specification short name", None))
              specificationData.get.getItem.add(toAnyContent("fullName", "string", "Specification full name", None))
            } else {
              val specification = fromCache(dataCache, "specificationData", () => exec(PersistenceSchema.specifications.filter(_.id === specificationId.get).map(x => (x.shortname, x.fullname)).result.headOption))
              if (specification.isDefined) {
                specificationData.get.getItem.add(toAnyContent("id", "number", specificationId.get.toString, None))
                specificationData.get.getItem.add(toAnyContent("shortName", "string", specification.get._1, None))
                specificationData.get.getItem.add(toAnyContent("fullName", "string", specification.get._2, None))
              }
            }
          case TriggerDataType.Actor =>
            actorData = getOrInitiatizeContentMap("actor", actorData)
            val actorId:Option[Long] = fromCache(dataCache, "actorId", () => TriggerCallbacks.actorId(callbacks))
            if (actorId.isEmpty) {
              actorData.get.getItem.add(toAnyContent("id", "number", "123", None))
              actorData.get.getItem.add(toAnyContent("actorIdentifier", "string", "Actor identifier", None))
              actorData.get.getItem.add(toAnyContent("name", "string", "Actor name", None))
            } else {
              val actor = fromCache(dataCache, "actorData", () => exec(PersistenceSchema.actors.filter(_.id === actorId.get).map(x => (x.actorId, x.name)).result.headOption))
              if (actor.isDefined) {
                actorData.get.getItem.add(toAnyContent("id", "number", actorId.get.toString, None))
                actorData.get.getItem.add(toAnyContent("actorIdentifier", "string", actor.get._1, None))
                actorData.get.getItem.add(toAnyContent("name", "string", actor.get._2, None))
              }
            }
          case TriggerDataType.TestSession =>
            testSessionData = getOrInitiatizeContentMap("testSession", testSessionData)
            val testSessionId:Option[String] = fromCache(dataCache, "testSessionId", () => TriggerCallbacks.testSessionId(callbacks))
            if (testSessionId.isEmpty) {
              testSessionData.get.getItem.add(toAnyContent("testSuiteIdentifier", "string", "TestSuiteID", None))
              testSessionData.get.getItem.add(toAnyContent("testCaseIdentifier", "string", "TestCaseID", None))
              testSessionData.get.getItem.add(toAnyContent("testSessionIdentifier", "string", "TestSessionID", None))
            } else {
              val testSession: (Option[String], Option[String], String) = fromCache(dataCache, "testSessionData",
                () => {
                  exec(
                    for {
                      sessionIds <- PersistenceSchema.testResults.filter(_.testSessionId === testSessionId.get).map(x => (x.testSuiteId, x.testCaseId)).result.headOption
                      testSuiteIdentifier <- {
                        if (sessionIds.isDefined && sessionIds.get._1.isDefined) {
                          PersistenceSchema.testSuites.filter(_.id === sessionIds.get._1.get).map(_.identifier).result.headOption
                        } else {
                          DBIO.successful(None)
                        }
                      }
                      testCaseIdentifier <- {
                        if (sessionIds.isDefined && sessionIds.get._2.isDefined) {
                          PersistenceSchema.testCases.filter(_.id === sessionIds.get._2.get).map(_.identifier).result.headOption
                        } else {
                          DBIO.successful(None)
                        }
                      }
                    } yield (testSuiteIdentifier, testCaseIdentifier, testSessionId.get)
                  )
                }
              )
              if (testSession._1.isDefined) {
                testSessionData.get.getItem.add(toAnyContent("testSuiteIdentifier", "string", testSession._1.get, None))
              }
              if (testSession._2.isDefined) {
                testSessionData.get.getItem.add(toAnyContent("testCaseIdentifier", "string", testSession._2.get, None))
              }
              testSessionData.get.getItem.add(toAnyContent("testSessionIdentifier", "string", testSession._3, None))
            }
          case TriggerDataType.TestReport =>
            testReportData = getOrInitiatizeContentMap("testReport", testReportData)
            val testSessionId:Option[String] = fromCache(dataCache, "testSessionId", () => TriggerCallbacks.testSessionId(callbacks))
            if (testSessionId.isEmpty) {
              populateAnyContent(testReportData, "testReport", "string", getSampleTestReport(), None)
            } else {
              val testReportAsString: Option[String] = fromCache(dataCache, "testReportData", () => {
                var reportContent: Option[String] = None
                var reportPath: Option[Path] = None
                try {
                  reportPath = reportManager.generateTestCaseReport(repositoryUtils.getReportTempFile(".xml"), testSessionId.get, Constants.MimeTypeXML, Some(trigger.trigger.community), None)
                  reportContent = reportPath.filter(Files.exists(_)).map(Files.readString)
                } finally {
                  if (reportPath.exists(Files.exists(_))) {
                    FileUtils.deleteQuietly(reportPath.get.toFile)
                  }
                }
                reportContent
              })
              if (testReportAsString.isDefined) {
                populateAnyContent(testReportData, "testReport", "string", testReportAsString.get, None)
              }
            }
          case TriggerDataType.OrganisationParameter =>
            orgParamData = getOrInitiatizeContentMap("organisationProperties", orgParamData)
            val paramInfo = organisationParameterData.get(dataItem.dataId)
            if (paramInfo.isDefined) {
              if ("BINARY".equals(paramInfo.get._2)) {
                var value = "BASE64_CONTENT_OF_FILE"
                if (paramInfo.get._3.isDefined && paramInfo.get._4.isDefined) {
                  value = Base64.getEncoder.encodeToString(Files.readAllBytes(repositoryUtils.getOrganisationPropertyFile(dataItem.dataId, paramInfo.get._4.get).toPath))
                }
                orgParamData.get.getItem.add(toAnyContent(paramInfo.get._1, "binary", value, Some(ValueEmbeddingEnumeration.BASE_64)))
              } else {
                orgParamData.get.getItem.add(toAnyContent(paramInfo.get._1, "string", paramInfo.get._3.getOrElse("Sample data"), None))
              }
            }
          case TriggerDataType.SystemParameter =>
            sysParamData = getOrInitiatizeContentMap("systemProperties", sysParamData)
            val paramInfo = systemParameterData.get(dataItem.dataId)
            if (paramInfo.isDefined) {
              if ("BINARY".equals(paramInfo.get._2)) {
                var value = "BASE64_CONTENT_OF_FILE"
                if (paramInfo.get._3.isDefined && paramInfo.get._4.isDefined) {
                  value = Base64.getEncoder.encodeToString(Files.readAllBytes(repositoryUtils.getSystemPropertyFile(dataItem.dataId, paramInfo.get._4.get).toPath))
                }
                sysParamData.get.getItem.add(toAnyContent(paramInfo.get._1, "binary", value, Some(ValueEmbeddingEnumeration.BASE_64)))
              } else {
                sysParamData.get.getItem.add(toAnyContent(paramInfo.get._1, "string", paramInfo.get._3.getOrElse("Sample data"), None))
              }
            }
          case TriggerDataType.StatementParameter =>
            statementParamData = getOrInitiatizeContentMap("statementProperties", statementParamData)
            val paramKey = statementParameterKeys.get(dataItem.dataId)
            if (paramKey.isDefined) {
              val paramInfo = statementParameterData.get(paramKey.get)
              if (paramInfo.isDefined) {
                if ("BINARY".equals(paramInfo.get._2)) {
                  var value = "BASE64_CONTENT_OF_FILE"
                  if (paramInfo.get._3.isDefined) {
                    value = Base64.getEncoder.encodeToString(Files.readAllBytes(repositoryUtils.getStatementParameterFile(paramInfo.get._1, paramInfo.get._4.get).toPath))
                  }
                  statementParamData.get.getItem.add(toAnyContent(paramKey.get, "binary", value, Some(ValueEmbeddingEnumeration.BASE_64)))
                } else {
                  statementParamData.get.getItem.add(toAnyContent(paramKey.get, "string", paramInfo.get._3.getOrElse("Sample data"), None))
                }
              }
            }
          case TriggerDataType.DomainParameter =>
            domainParamData = getOrInitiatizeContentMap("domainParameters", domainParamData)
            val paramInfo = domainParameterData.get(dataItem.dataId)
            if (paramInfo.isDefined) {
              if ("BINARY".equals(paramInfo.get._2)) {
                var value = "BASE64_CONTENT_OF_FILE"
                if (paramInfo.get._3.isDefined) {
                  value = Base64.getEncoder.encodeToString(Files.readAllBytes(repositoryUtils.getDomainParameterFile(paramInfo.get._4, dataItem.dataId).toPath))
                }
                domainParamData.get.getItem.add(toAnyContent(paramInfo.get._1, "binary", value, Some(ValueEmbeddingEnumeration.BASE_64)))
              } else {
                domainParamData.get.getItem.add(toAnyContent(paramInfo.get._1, "string", paramInfo.get._3.getOrElse("Sample data"), None))
              }
            }
        }
      }
      if (communityData.isDefined) {
        request.getInput.add(communityData.get)
      }
      if (organisationData.isDefined) {
        request.getInput.add(organisationData.get)
      }
      if (systemData.isDefined) {
        request.getInput.add(systemData.get)
      }
      if (specificationData.isDefined) {
        request.getInput.add(specificationData.get)
      }
      if (actorData.isDefined) {
        request.getInput.add(actorData.get)
      }
      if (testSessionData.isDefined) {
        request.getInput.add(testSessionData.get)
      }
      if (domainParamData.isDefined) {
        request.getInput.add(domainParamData.get)
      }
      if (orgParamData.isDefined) {
        request.getInput.add(orgParamData.get)
      }
      if (sysParamData.isDefined) {
        request.getInput.add(sysParamData.get)
      }
      if (statementParamData.isDefined) {
        request.getInput.add(statementParamData.get)
      }
      if (testReportData.isDefined) {
        request.getInput.add(testReportData.get)
      }
    }
    request
  }

  def previewTriggerCall(communityId: Long, operation: Option[String], serviceType: TriggerServiceType, data: Option[List[TriggerData]]): String = {
    val request = prepareTriggerInput(new Trigger(Triggers(-1, "", None, "", -1, serviceType.id.toShort, operation, active = false, None, None, communityId), data), mutable.Map[String, Any](), None)
    if (serviceType == TriggerServiceType.GITB) {
      val requestWrapper = new JAXBElement[ProcessRequest](new QName("http://www.gitb.com/ps/v1/", "ProcessRequest"), classOf[ProcessRequest], request)
      val bos = new ByteArrayOutputStream()
      XMLUtils.marshalToStream(requestWrapper, bos)
      new String(bos.toByteArray, StandardCharsets.UTF_8)
    } else {
      JsonUtil.jsProcessRequest(request).toString()
    }
  }

  private def callHttpService(url: String, fnPayloadProvider: () => String): Future[(Boolean, List[String], String)] = {
    for {
      payload <- Future { fnPayloadProvider.apply() }
      response <- ws.url(url)
        .addHttpHeaders("Content-Type" -> "application/json")
        .post(payload)
        .map(response => (response.status == 200, List(response.body), response.headers.getOrElse("Content-Type", List("text/plain")).head))
    } yield response
  }

  private def callProcessingService(url: String, fnCallOperation: ProcessingService => JAXBElement[_]): Future[(Boolean, List[String], String)] = {
    Future {
      val service = new ProcessingServiceService(URI.create(url).toURL)
      val response = fnCallOperation.apply(service.getProcessingServicePort)
      val bos = new ByteArrayOutputStream()
      XMLUtils.marshalToStream(response, bos)
      (true, List(new String(bos.toByteArray, StandardCharsets.UTF_8)), Constants.MimeTypeXML)
    }
  }

  def testTriggerEndpoint(url: String, serviceType: TriggerServiceType): Future[(Boolean, List[String], String)] = {
    val promise = Promise[(Boolean, List[String], String)]()
    var future: Future[(Boolean, List[String], String)] = null
    if (serviceType == TriggerServiceType.GITB) {
      future = callProcessingService(url, service => {
        val response = service.getModuleDefinition(new com.gitb.ps.Void())
        new JAXBElement[GetModuleDefinitionResponse](new QName("http://www.gitb.com/ps/v1/", "GetModuleDefinitionResponse"), classOf[GetModuleDefinitionResponse], response)
      })
    } else {
      future = callHttpService(url, () => "{}")
    }
    future.onComplete {
      case Success(result) => promise.success(result)
      case Failure(exception) => promise.success(false, extractFailureDetails(exception), "text/plain")
    }
    promise.future
  }

  def testTriggerCall(url: String, serviceType: TriggerServiceType, payload: String): Future[(Boolean, List[String], String)] = {
    val promise = Promise[(Boolean, List[String], String)]()
    var future: Future[(Boolean, List[String], String)] = null
    if (serviceType == TriggerServiceType.GITB) {
      future = callProcessingService(url, service => {
        val request = XMLUtils.unmarshal(classOf[ProcessRequest],
          new StreamSource(new StringReader(payload)),
          new StreamSource(this.getClass.getResourceAsStream("/schema/gitb_ps.xsd")),
          new ClasspathResourceResolver())
        val response = service.process(request)
        new JAXBElement[ProcessResponse](new QName("http://www.gitb.com/ps/v1/", "ProcessResponse"), classOf[ProcessResponse], response)
      })
    } else {
      future = callHttpService(url, () => {
        val payloadAsJson = Json.parse(payload)
        payloadAsJson.validate(JsonUtil.validatorForProcessRequest())
        payloadAsJson.toString()
      })
    }
    future.onComplete {
      case Success(result) => promise.success(result)
      case Failure(exception) => promise.success(false, extractFailureDetails(exception), "text/plain")
    }
    promise.future
  }

  def fireTriggers(communityId: Long, eventType: TriggerEventType, triggerData: Any): Unit = {
    val triggers = getTriggersAndDataByCommunityAndType(communityId, eventType)
    if (triggers.isDefined) {
      // Use a cache to avoid the same DB operations.
      val dataCache = mutable.Map[String, Any]()
      val callbacks = TriggerCallbacks.newInstance(triggerData, triggerDataLoader)
      eventType match {
        // Organisation events: Data is a Long (organisation ID)
        case OrganisationCreated | OrganisationUpdated =>
          callbacks.fnOrganisation = Some(() => triggerData.asInstanceOf[Long])
        // System events: Data is a Long (system ID)
        case SystemCreated | SystemUpdated =>
          callbacks.fnSystem = Some(() => triggerData.asInstanceOf[Long])
        case ConformanceStatementCreated | ConformanceStatementUpdated | ConformanceStatementSucceeded =>
          // Conformance statement events: Data is a (Long, Long) (system ID, actor ID)
          callbacks.fnSystem = Some(() => triggerData.asInstanceOf[(Long, Long)]._1)
          callbacks.fnActor = Some(() => triggerData.asInstanceOf[(Long, Long)]._2)
        case TestSessionSucceeded | TestSessionFailed | TestSessionStarted =>
          // Test session events: Data is a String (test session ID)
          callbacks.fnTestSession = Some(() => triggerData.asInstanceOf[String])
      }
      triggers.get.foreach { trigger =>
        val triggerInput = prepareTriggerInput(trigger, dataCache, Some(callbacks))
        fireTrigger(trigger.trigger, callbacks, triggerInput)
      }
    }
  }

  def getParameterValue(item: AnyContent, parameterType: String, handleBinary: Array[Byte] => _): (String, Option[String]) = {
    var value: Option[String] = None
    var contentType: Option[String] = None
    var encoding = StandardCharsets.UTF_8
    if (item.getEncoding != null) {
      encoding = Charset.forName(item.getEncoding)
    }
    if ("BINARY".equals(parameterType)) {
      if (item.getEmbeddingMethod == ValueEmbeddingEnumeration.BASE_64) {
        if (MimeUtil.isDataURL(item.getValue)) {
          value = Some("")
          contentType = Some(MimeUtil.getMimeTypeFromDataURL(item.getValue))
          handleBinary.apply(Base64.getDecoder.decode(MimeUtil.getBase64FromDataURL(item.getValue)))
        } else {
          var mimeType = MimeUtil.getMimeTypeFromBase64(item.getValue)
          if (mimeType == null) {
            mimeType = "application/octet-stream"
          }
          value = Some("")
          contentType = Some(mimeType)
          handleBinary.apply(Base64.getDecoder.decode(item.getValue))
        }
      } else {
        value = Some("")
        contentType = Some("text/plain")
        handleBinary.apply(item.getValue.getBytes(encoding))
      }
    } else {
      if (item.getEmbeddingMethod == ValueEmbeddingEnumeration.BASE_64) {
        value = Some(new String(Base64.getDecoder.decode(item.getValue), encoding))
      } else {
        value = Some(item.getValue)
      }
    }
    (value.get, contentType)
  }

  private def saveAsOrganisationPropertyValues(community: Long, organisation: Long, items: Iterable[AnyContent], onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    val dbActions = ListBuffer[DBIO[_]]()
    val parameterMap = mutable.Map[String, (Long, String)]()
    exec(PersistenceSchema.organisationParameters.filter(_.community === community).map(x => (x.id, x.testKey, x.kind)).result).foreach { param =>
      parameterMap += (param._2 -> (param._1, param._3))
    }
    items.foreach { item =>
      if (item.getName != null && item.getValue != null) {
        val paramReference = parameterMap.get(item.getName)
        if (paramReference.isDefined) {
          onSuccess += (() => repositoryUtils.deleteOrganisationPropertyFile(paramReference.get._1, organisation))
          val paramData = getParameterValue(item, paramReference.get._2, bytes => {
            onSuccess += (() => repositoryUtils.setOrganisationPropertyFile(paramReference.get._1, organisation, Files.write(Files.createTempFile("itb", "trigger"), bytes).toFile))
          })
          dbActions += PersistenceSchema.organisationParameterValues.filter(_.organisation === organisation).filter(_.parameter === paramReference.get._1).delete
          dbActions += (PersistenceSchema.organisationParameterValues += OrganisationParameterValues(organisation, paramReference.get._1, paramData._1, paramData._2))
        }
      }
    }
    toDBIO(dbActions)
  }

  private def saveAsSystemPropertyValues(community: Long, system: Long, items: Iterable[AnyContent], onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    val dbActions = ListBuffer[DBIO[_]]()
    val parameterMap = mutable.Map[String, (Long, String)]()
    exec(PersistenceSchema.systemParameters.filter(_.community === community).map(x => (x.id, x.testKey, x.kind)).result).foreach { param =>
      parameterMap += (param._2 -> (param._1, param._3))
    }
    items.foreach { item =>
      if (item.getName != null && item.getValue != null) {
        val paramReference = parameterMap.get(item.getName)
        if (paramReference.isDefined) {
          onSuccess += (() => repositoryUtils.deleteSystemPropertyFile(paramReference.get._1, system))
          val paramData = getParameterValue(item, paramReference.get._2, bytes => {
            onSuccess += (() => repositoryUtils.setSystemPropertyFile(paramReference.get._1, system, Files.write(Files.createTempFile("itb", "trigger"), bytes).toFile))
          })
          dbActions += PersistenceSchema.systemParameterValues.filter(_.system === system).filter(_.parameter === paramReference.get._1).delete
          dbActions += (PersistenceSchema.systemParameterValues += SystemParameterValues(system, paramReference.get._1, paramData._1, paramData._2))
        }
      }
    }
    toDBIO(dbActions)
  }

  private def saveAsStatementPropertyValues(system: Long, actor: Long, items: Iterable[AnyContent], onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    val dbActions = ListBuffer[DBIO[_]]()
    val parameterMap = mutable.Map[String, (Long, String, Long)]()
    exec(PersistenceSchema.parameters
      .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
      .filter(_._2.actor === actor)
      .map(x => (x._1.id, x._1.testKey, x._1.kind, x._1.endpoint))
      .result
    ).foreach { param =>
      parameterMap += (param._2 -> (param._1, param._3, param._4))
    }
    items.foreach { item =>
      if (item.getName != null && item.getValue != null) {
        val paramReference = parameterMap.get(item.getName)
        if (paramReference.isDefined) {
          onSuccess += (() => repositoryUtils.deleteStatementParameterFile(paramReference.get._1, system))
          val paramData = getParameterValue(item, paramReference.get._2, bytes => {
            onSuccess += (() => repositoryUtils.setStatementParameterFile(paramReference.get._1, system, Files.write(Files.createTempFile("itb", "trigger"), bytes).toFile))
          })
          dbActions += PersistenceSchema.configs.filter(_.system === system).filter(_.parameter === paramReference.get._1).delete
          dbActions += (PersistenceSchema.configs += Configs(system, paramReference.get._1, paramReference.get._3, paramData._1, paramData._2))
        }
      }
    }
    toDBIO(dbActions)
  }

  private def callTriggerService(trigger: Triggers, request: ProcessRequest): Future[ProcessResponse] = {
    if (TriggerServiceType.apply(trigger.serviceType) == TriggerServiceType.GITB) {
      Future {
        val service = new ProcessingServiceService(URI.create(trigger.url).toURL)
        service.getProcessingServicePort.process(request)
      }
    } else {
      callHttpService(trigger.url, () => {
        JsonUtil.jsProcessRequest(request).toString()
      }).map { result =>
        if (result._1) {
          JsonUtil.parseJsProcessResponse(Json.parse(result._2.headOption.getOrElse("{}")))
        } else {
          throw new IllegalStateException(result._2.headOption.getOrElse("Unexpected error"))
        }
      }
    }
  }

  private def fireTrigger(trigger: Triggers, callbacks: TriggerCallbacks, request: ProcessRequest): Unit = {
    callTriggerService(trigger, request).onComplete {
      case Success(response) =>
        val onSuccessCalls = mutable.ListBuffer[() => _]()
        val dbActions = ListBuffer[DBIO[_]]()
        if (response != null) {
          response.getOutput.asScala.foreach { output =>
            if ("organisationProperties".equals(output.getName)) {
              val organisationId = callbacks.organisationId()
              if (organisationId.isDefined) {
                dbActions += saveAsOrganisationPropertyValues(trigger.community, organisationId.get, output.getItem.asScala, onSuccessCalls)
              }
            } else if ("systemProperties".equals(output.getName)) {
              val systemId = callbacks.systemId()
              if (systemId.isDefined) {
                dbActions += saveAsSystemPropertyValues(trigger.community, systemId.get, output.getItem.asScala, onSuccessCalls)
              }
            } else if ("statementProperties".equals(output.getName)) {
              val systemId = callbacks.systemId()
              val actorId = callbacks.actorId()
              if (systemId.isDefined && actorId.isDefined) {
                dbActions += saveAsStatementPropertyValues(systemId.get, actorId.get, output.getItem.asScala, onSuccessCalls)
              }
            }
          }
        }
        if (trigger.latestResultOk.isEmpty || !trigger.latestResultOk.get) {
          // Update latest result if this is none is recorded or if the previous one was a failure.
          dbActions += recordTriggerResultInternal(trigger.id, success = true, None)
        }
        exec(dbActionFinalisation(Some(onSuccessCalls), None, toDBIO(dbActions)).transactionally)
      case Failure(error) =>
        val details = extractFailureDetails(error)
        logger.warn("Trigger call resulted in error [community: "+trigger.community+"][type: "+trigger.eventType+"][url: "+trigger.url+"]: " + details.mkString(" | "))
        recordTriggerResult(trigger.id, success = false, Some(JsonUtil.jsTextArray(details).toString()))
    }
  }

  private def recordTriggerResult(trigger: Long, success: Boolean, message: Option[String]): Unit = {
    exec(recordTriggerResultInternal(trigger, success, message).transactionally)
  }

  private def recordTriggerResultInternal(triggerId: Long, success: Boolean, message: Option[String]): DBIO[_] = {
    val q = for { t <- PersistenceSchema.triggers.filter(_.id === triggerId) } yield (t.latestResultOk, t.latestResultOutput)
    q.update(Some(success), message)
  }

}