package managers

import com.gitb.core.{AnyContent, ValueEmbeddingEnumeration}
import com.gitb.ps._
import com.gitb.utils.{ClasspathResourceResolver, XMLUtils}
import jakarta.xml.bind.JAXBElement
import models.Enums.TriggerDataType.TriggerDataType
import models.Enums.TriggerEventType._
import models.Enums.TriggerServiceType.TriggerServiceType
import models.Enums.{TriggerDataType, TriggerFireExpressionType, TriggerServiceType}
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
import java.util.regex.Pattern
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
                               triggerHelper: TriggerHelper,
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
      _ <- setTriggerFireExpressions(triggerId, trigger.fireExpressions, isNewTrigger = true)
    } yield triggerId
  }

  def getTriggerAndDataById(triggerId: Long): Trigger = {
    exec(
      for {
        trigger <- PersistenceSchema.triggers.filter(_.id === triggerId).result.head
        triggerData <- PersistenceSchema.triggerData.filter(_.trigger === triggerId).result
        triggerFireExpressions <- PersistenceSchema.triggerFireExpressions.filter(_.trigger === triggerId).result
      } yield new Trigger(trigger, Some(triggerData.toList), Some(triggerFireExpressions.toList))
    )
  }

  def getTriggerAndDataByCommunityId(communityId: Long): List[Trigger] = {
    val result = exec(
      for {
        triggerList <- PersistenceSchema.triggers.filter(_.community === communityId).result
        triggerIds <- DBIO.successful(triggerList.map(_.id))
        triggerDataList <- PersistenceSchema.triggerData
          .filter(_.trigger inSet triggerIds)
          .result
        triggerFireExpressionList <- PersistenceSchema.triggerFireExpressions
          .filter(_.trigger inSet triggerIds)
          .result
      } yield (triggerList.toList, triggerDataList.toList, triggerFireExpressionList.toList)
    )
    toTriggerList(result._1, result._2, result._3)
  }

  private def toTriggerList(triggerList: List[Triggers], triggerDataList: List[TriggerData], triggerFireExpressionList: List[TriggerFireExpression]): List[Trigger] = {
    val itemMap = mutable.Map[Long, (ListBuffer[TriggerData], ListBuffer[TriggerFireExpression])]()
    triggerDataList.foreach { triggerItem =>
      var data = itemMap.get(triggerItem.trigger)
      if (data.isEmpty) {
        data = Some(new ListBuffer[TriggerData](), new ListBuffer[TriggerFireExpression]())
        itemMap += (triggerItem.trigger -> data.get)
      }
      data.get._1 += TriggerData(triggerItem.dataType, triggerItem.dataId, triggerItem.trigger)
    }
    triggerFireExpressionList.foreach { expression =>
      var data = itemMap.get(expression.trigger)
      if (data.isEmpty) {
        data = Some(new ListBuffer[TriggerData](), new ListBuffer[TriggerFireExpression]())
        itemMap += (expression.trigger -> data.get)
      }
      data.get._2 += expression
    }
    triggerList.map { trigger =>
      val triggerData = if (itemMap.contains(trigger.id)) {
        val mapData = itemMap(trigger.id)
        Some((mapData._1.toList, mapData._2.toList))
      } else {
        None
      }
      new Trigger(trigger, triggerData.map(_._1), triggerData.map(_._2))
    }
  }

  private def getTriggersAndDataByCommunityAndType(communityId: Long, eventType: TriggerEventType): Option[List[Trigger]] = {
    val result = exec(
      for {
        triggers <- PersistenceSchema.triggers
          .filter(_.community === communityId)
          .filter(_.eventType === eventType.id.toShort)
          .filter(_.active === true)
          .map(x => (x.id, x.url, x.operation, x.latestResultOk, x.serviceType))
          .result
        triggerIds <- DBIO.successful(triggers.map(t => t._1))
        triggerData <- {
          if (triggers.nonEmpty) {
            PersistenceSchema.triggerData.filter(_.trigger inSet triggerIds).result
          } else {
            DBIO.successful(List())
          }
        }
        fireExpressions <- {
          if (triggers.nonEmpty) {
            PersistenceSchema.triggerFireExpressions.filter(_.trigger inSet triggerIds).result
          } else {
            DBIO.successful(List())
          }
        }
      } yield (triggers, triggerData, fireExpressions)
    )
    val triggers = result._1
    val triggerData = result._2
    val fireExpressions = result._3
    if (triggers.nonEmpty) {
      val itemMap = mutable.Map[Long, (ListBuffer[TriggerData], ListBuffer[TriggerFireExpression])]()
      triggerData.foreach { triggerItem =>
        var data = itemMap.get(triggerItem.trigger)
        if (data.isEmpty) {
          data = Some(new ListBuffer[TriggerData](), new ListBuffer[TriggerFireExpression]())
          itemMap += (triggerItem.trigger -> data.get)
        }
        data.get._1 += TriggerData(triggerItem.dataType, triggerItem.dataId, triggerItem.trigger)
      }
      fireExpressions.foreach { expression =>
        var data = itemMap.get(expression.trigger)
        if (data.isEmpty) {
          data = Some(new ListBuffer[TriggerData](), new ListBuffer[TriggerFireExpression]())
          itemMap += (expression.trigger -> data.get)
        }
        data.get._2 += expression
      }
      Some(triggers.map { trigger =>
        val triggerData = if (itemMap.contains(trigger._1)) {
          val mapData = itemMap(trigger._1)
          Some((mapData._1.toList, mapData._2.toList))
        } else {
          None
        }
        new Trigger(
          Triggers(trigger._1, "", None, trigger._2, eventType.id.toShort, trigger._5, trigger._3, active = true, trigger._4, None, communityId),
          triggerData.map(_._1), triggerData.map(_._2)
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
      setTriggerDataInternal(trigger.trigger.id, trigger.data, isNewTrigger = false) andThen
      setTriggerFireExpressions(trigger.trigger.id, trigger.fireExpressions, isNewTrigger = false)
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

  private def getOrInitializeContentMap(name: String, map: Option[AnyContent]): Option[AnyContent] = {
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
            communityData = getOrInitializeContentMap("community", communityData)
            communityData.get.getItem.add(toAnyContent("id", "number", trigger.trigger.community.toString, None))
            communityData.get.getItem.add(toAnyContent("shortName", "string", data._1, None))
            communityData.get.getItem.add(toAnyContent("fullName", "string", data._2, None))
          case TriggerDataType.Organisation =>
            organisationData = getOrInitializeContentMap("organisation", organisationData)
            val organisationId = getOrganisationId(dataCache, callbacks)
            if (organisationId.isEmpty) {
              organisationData.get.getItem.add(toAnyContent("id", "number", "123", None))
              organisationData.get.getItem.add(toAnyContent("shortName", "string", "Organisation short name", None))
              organisationData.get.getItem.add(toAnyContent("fullName", "string", "Organisation full name", None))
            } else {
              val organisation = getOrganisationData(dataCache, callbacks)
              if (organisation.isDefined) {
                organisationData.get.getItem.add(toAnyContent("id", "number", organisation.get.id.toString, None))
                organisationData.get.getItem.add(toAnyContent("shortName", "string", organisation.get.shortName, None))
                organisationData.get.getItem.add(toAnyContent("fullName", "string", organisation.get.fullName, None))
              }
            }
          case TriggerDataType.System =>
            systemData = getOrInitializeContentMap("system", systemData)
            val systemId = getSystemId(dataCache, callbacks)
            if (systemId.isEmpty) {
              systemData.get.getItem.add(toAnyContent("id", "number", "123", None))
              systemData.get.getItem.add(toAnyContent("shortName", "string", "System short name", None))
              systemData.get.getItem.add(toAnyContent("fullName", "string", "System full name", None))
            } else {
              val system = getSystemData(dataCache, callbacks)
              if (system.isDefined) {
                systemData.get.getItem.add(toAnyContent("id", "number", system.get.id.toString, None))
                systemData.get.getItem.add(toAnyContent("shortName", "string", system.get.shortName, None))
                systemData.get.getItem.add(toAnyContent("fullName", "string", system.get.fullName, None))
              }
            }
          case TriggerDataType.Specification =>
            specificationData = getOrInitializeContentMap("specification", specificationData)
            val specificationId = getSpecificationId(dataCache, callbacks)
            if (specificationId.isEmpty) {
              specificationData.get.getItem.add(toAnyContent("id", "number", "123", None))
              specificationData.get.getItem.add(toAnyContent("shortName", "string", "Specification short name", None))
              specificationData.get.getItem.add(toAnyContent("fullName", "string", "Specification full name", None))
            } else {
              val specification = getSpecificationData(dataCache, callbacks)
              if (specification.isDefined) {
                specificationData.get.getItem.add(toAnyContent("id", "number", specification.get.id.toString, None))
                specificationData.get.getItem.add(toAnyContent("shortName", "string", specification.get.shortName, None))
                specificationData.get.getItem.add(toAnyContent("fullName", "string", specification.get.fullName, None))
              }
            }
          case TriggerDataType.Actor =>
            actorData = getOrInitializeContentMap("actor", actorData)
            val actorId = getActorId(dataCache, callbacks)
            if (actorId.isEmpty) {
              actorData.get.getItem.add(toAnyContent("id", "number", "123", None))
              actorData.get.getItem.add(toAnyContent("actorIdentifier", "string", "Actor identifier", None))
              actorData.get.getItem.add(toAnyContent("name", "string", "Actor name", None))
            } else {
              val actor = getActorData(dataCache, callbacks)
              if (actor.isDefined) {
                actorData.get.getItem.add(toAnyContent("id", "number", actor.get.id.toString, None))
                actorData.get.getItem.add(toAnyContent("actorIdentifier", "string", actor.get.identifier, None))
                actorData.get.getItem.add(toAnyContent("name", "string", actor.get.name, None))
              }
            }
          case TriggerDataType.TestSession =>
            testSessionData = getOrInitializeContentMap("testSession", testSessionData)
            val testSessionId = getTestSessionId(dataCache, callbacks)
            if (testSessionId.isEmpty) {
              testSessionData.get.getItem.add(toAnyContent("testSuiteIdentifier", "string", "TestSuiteID", None))
              testSessionData.get.getItem.add(toAnyContent("testCaseIdentifier", "string", "TestCaseID", None))
              testSessionData.get.getItem.add(toAnyContent("testSessionIdentifier", "string", "TestSessionID", None))
            } else {
              val identifiers = getTestSessionData(dataCache, callbacks)
              if (identifiers.isDefined) {
                testSessionData.get.getItem.add(toAnyContent("testSuiteIdentifier", "string", identifiers.get.testSuiteIdentifier, None))
                testSessionData.get.getItem.add(toAnyContent("testCaseIdentifier", "string", identifiers.get.testCaseIdentifier, None))
                testSessionData.get.getItem.add(toAnyContent("testSessionIdentifier", "string", identifiers.get.testSessionIdentifier, None))
              }
            }
          case TriggerDataType.TestReport =>
            testReportData = getOrInitializeContentMap("testReport", testReportData)
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
            orgParamData = getOrInitializeContentMap("organisationProperties", orgParamData)
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
            sysParamData = getOrInitializeContentMap("systemProperties", sysParamData)
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
            statementParamData = getOrInitializeContentMap("statementProperties", statementParamData)
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
            domainParamData = getOrInitializeContentMap("domainParameters", domainParamData)
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

  private def getOrganisationId(dataCache: mutable.Map[String, Any], callbacks: Option[TriggerCallbacks]): Option[Long] = {
    fromCache(dataCache, "organisationId", () => TriggerCallbacks.organisationId(callbacks))
  }

  private def getSystemId(dataCache: mutable.Map[String, Any], callbacks: Option[TriggerCallbacks]): Option[Long] = {
    fromCache(dataCache, "systemId", () => TriggerCallbacks.systemId(callbacks))
  }

  private def getActorId(dataCache: mutable.Map[String, Any], callbacks: Option[TriggerCallbacks]): Option[Long] = {
    fromCache(dataCache, "actorId", () => TriggerCallbacks.actorId(callbacks))
  }

  private def getTestSessionId(dataCache: mutable.Map[String, Any], callbacks: Option[TriggerCallbacks]): Option[String] = {
    fromCache(dataCache, "testSessionId", () => TriggerCallbacks.testSessionId(callbacks))
  }

  private def getSpecificationId(dataCache: mutable.Map[String, Any], callbacks: Option[TriggerCallbacks]): Option[Long] = {
    val actorId = getActorId(dataCache, callbacks)
    var specificationId:Option[Long] = None
    if (actorId.isDefined) {
      specificationId = fromCache(dataCache, "specificationId", () => exec(PersistenceSchema.specificationHasActors.filter(_.actorId === actorId.get).map(x => x.specId).result.headOption))
    }
    specificationId
  }

  private def getOrganisationData(dataCache: mutable.Map[String, Any], callbacks: Option[TriggerCallbacks]): Option[OrganisationData] = {
    val organisationId = getOrganisationId(dataCache, callbacks)
    if (organisationId.isDefined) {
      fromCache(dataCache, "organisationData", () => exec(
        PersistenceSchema.organizations
          .filter(_.id === organisationId.get)
          .map(x => (x.shortname, x.fullname))
          .result.headOption
      ).map(x => OrganisationData(organisationId.get, x._1, x._2)))
    } else {
      None
    }
  }

  private def getSystemData(dataCache: mutable.Map[String, Any], callbacks: Option[TriggerCallbacks]): Option[SystemData] = {
    val systemId = getSystemId(dataCache, callbacks)
    if (systemId.isDefined) {
      fromCache(dataCache, "systemData", () => exec(
        PersistenceSchema.systems
          .filter(_.id === systemId.get)
          .map(x => (x.shortname, x.fullname))
          .result.headOption
      ).map(x => SystemData(systemId.get, x._1, x._2)))
    } else {
      None
    }
  }

  private def getSpecificationData(dataCache: mutable.Map[String, Any], callbacks: Option[TriggerCallbacks]): Option[SpecificationData] = {
    val specificationId = getSpecificationId(dataCache, callbacks)
    if (specificationId.isDefined) {
      fromCache(dataCache, "specificationData", () => exec(
        PersistenceSchema.specifications
          .filter(_.id === specificationId.get)
          .map(x => (x.shortname, x.fullname))
          .result.headOption
      ).map(x => SpecificationData(specificationId.get, x._1, x._2)))
    } else {
      None
    }
  }

  private def getActorData(dataCache: mutable.Map[String, Any], callbacks: Option[TriggerCallbacks]): Option[ActorData] = {
    val actorId = getActorId(dataCache, callbacks)
    if (actorId.isDefined) {
      fromCache(dataCache, "actorData", () => exec(
        PersistenceSchema.actors
          .filter(_.id === actorId.get)
          .map(x => (x.actorId, x.name))
          .result.headOption
      ).map(x => ActorData(actorId.get, x._1, x._2)))
    } else {
      None
    }
  }

  private def getTestSessionData(dataCache: mutable.Map[String, Any], callbacks: Option[TriggerCallbacks]): Option[TestSessionData] = {
    val testSessionId = getTestSessionId(dataCache, callbacks)
    if (testSessionId.isDefined) {
      fromCache(dataCache, "testSessionData",
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
            } yield testCaseIdentifier.map(TestSessionData(testSuiteIdentifier.get, _, testSessionId.get))
          )
        }
      )
    } else {
      None
    }
  }

  def previewTriggerCall(communityId: Long, operation: Option[String], serviceType: TriggerServiceType, data: Option[List[TriggerData]]): String = {
    val request = prepareTriggerInput(new Trigger(Triggers(-1, "", None, "", -1, serviceType.id.toShort, operation, active = false, None, None, communityId), data, None), mutable.Map[String, Any](), None)
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
      triggers.get.filter(checkTriggerFireCondition(_, dataCache, Some(callbacks))).foreach { trigger =>
        val triggerInput = prepareTriggerInput(trigger, dataCache, Some(callbacks))
        fireTrigger(trigger.trigger, callbacks, triggerInput)
      }
    }
  }

  private def checkTriggerFireCondition(trigger: Trigger, dataCache: mutable.Map[String, Any], callbacks: Option[TriggerCallbacks]): Boolean = {
    if (trigger.fireExpressions.exists(_.nonEmpty)) {
      trigger.fireExpressions.get.foreach { expression =>
        TriggerFireExpressionType.apply(expression.expressionType) match {
          case TriggerFireExpressionType.TestSuiteIdentifier =>
            if (!checkFireExpression(expression, getTestSessionData(dataCache, callbacks).map(_.testSuiteIdentifier))) {
              return false
            }
          case TriggerFireExpressionType.TestCaseIdentifier =>
            if (!checkFireExpression(expression, getTestSessionData(dataCache, callbacks).map(_.testCaseIdentifier))) {
              return false
            }
          case TriggerFireExpressionType.ActorIdentifier =>
            if (!checkFireExpression(expression, getActorData(dataCache, callbacks).map(_.identifier))) {
              return false
            }
          case TriggerFireExpressionType.SpecificationName =>
            if (!checkFireExpression(expression, getSpecificationData(dataCache, callbacks).map(_.fullName))) {
              return false
            }
          case TriggerFireExpressionType.SystemName =>
            if (!checkFireExpression(expression, getSystemData(dataCache, callbacks).map(_.fullName))) {
              return false
            }
          case TriggerFireExpressionType.OrganisationName =>
            if (!checkFireExpression(expression, getOrganisationData(dataCache, callbacks).map(_.fullName))) {
              return false
            }
          case _ => // No action
        }
      }
    }
    true
  }

  private def checkFireExpression(expression: TriggerFireExpression, valueToCheck: Option[String]): Boolean = {
    if (valueToCheck.isDefined) {
      // We use "matcher.find()" as we want to accept also partial matches.
      val matched = Pattern.compile(expression.expression).matcher(valueToCheck.get).find()
      matched && !expression.notMatch || !matched && expression.notMatch
    } else {
      // If the value cannot be determined we don't block the trigger's firing.
      true
    }
  }

  private def getParameterValue(item: AnyContent, parameterType: String, handleBinary: Array[Byte] => _): (String, Option[String]) = {
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

  private def setTriggerDataInternal(triggerId: Long, data: Option[List[TriggerData]], isNewTrigger: Boolean): DBIO[_] = {
    val dbActions = ListBuffer[DBIO[_]]()
    if (!isNewTrigger) {
      // Delete previous data (only if needed - new)
      dbActions += triggerHelper.deleteTriggerDataInternal(triggerId)
    }
    if (data.isDefined) {
      data.get.foreach { dataItem =>
        dbActions += (PersistenceSchema.triggerData += dataItem.withTrigger(triggerId))
      }
    }
    toDBIO(dbActions)
  }

  private def setTriggerFireExpressions(triggerId: Long, expressions: Option[List[TriggerFireExpression]], isNewTrigger: Boolean): DBIO[_] = {
    val dbActions = ListBuffer[DBIO[_]]()
    if (!isNewTrigger) {
      // Delete previous expressions (only if needed - new)
      dbActions += triggerHelper.deleteTriggerFireExpressionsInternal(triggerId)
    }
    if (expressions.isDefined) {
      expressions.get.foreach { expression =>
        dbActions += (PersistenceSchema.triggerFireExpressions += expression.withTrigger(triggerId))
      }
    }
    toDBIO(dbActions)
  }

  private case class TestSessionData(testSuiteIdentifier: String, testCaseIdentifier: String, testSessionIdentifier: String)
  private case class ActorData(id: Long, identifier: String, name: String)
  private case class SpecificationData(id: Long, shortName: String, fullName: String)
  private case class SystemData(id: Long, shortName: String, fullName: String)
  private case class OrganisationData(id: Long, shortName: String, fullName: String)

}