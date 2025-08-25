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

import com.gitb.core.{AnyContent, ValueEmbeddingEnumeration}
import com.gitb.ps._
import com.gitb.utils.{ClasspathResourceResolver, XMLUtils}
import jakarta.xml.bind.JAXBElement
import managers.TriggerManager.parseResponse
import managers.triggers.CacheCata.{fromCache, fromCacheWithCache}
import managers.triggers.{CacheData, TriggerCallbacks, TriggerDataLoader, TriggerHelper}
import models.Enums.TriggerDataType.TriggerDataType
import models.Enums.TriggerEventType._
import models.Enums.TriggerServiceType.TriggerServiceType
import models.Enums.{TriggerDataType, TriggerFireExpressionType, TriggerServiceType}
import models._
import org.apache.commons.io.{FileUtils, IOUtils}
import org.apache.commons.lang3.StringUtils
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
import java.nio.file.Files
import java.util.Base64
import java.util.regex.Pattern
import javax.inject.{Inject, Singleton}
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource
import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success}

object TriggerManager {

  def parseResponse(outputs: Iterable[AnyContent]): TriggerResponse = {
    val organisationBuffer = ListBuffer[AnyContent]()
    val systemBuffer = ListBuffer[AnyContent]()
    val statementBuffer = ListBuffer[AnyContent]()
    outputs.foreach { output =>
      if ("organisationProperties".equals(output.getName)) {
        organisationBuffer += output
      } else if ("systemProperties".equals(output.getName)) {
        systemBuffer += output
      } else if ("statementProperties".equals(output.getName)) {
        statementBuffer += output
      }
    }
    TriggerResponse(organisationBuffer.toList, systemBuffer.toList, statementBuffer.toList)
  }

  case class TriggerResponse(organisationProperties: List[AnyContent], systemProperties: List[AnyContent], statementProperties: List[AnyContent])

}

@Singleton
class TriggerManager @Inject()(env: Environment,
                               ws: WSClient,
                               repositoryUtils: RepositoryUtils,
                               triggerDataLoader: TriggerDataLoader,
                               reportManager: ReportManager,
                               triggerHelper: TriggerHelper,
                               dbConfigProvider: DatabaseConfigProvider)
                              (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  private val logger = LoggerFactory.getLogger(classOf[TriggerManager])

  def getCommunityId(triggerId: Long): Future[Long] = {
    DB.run(PersistenceSchema.triggers.filter(_.id === triggerId).map(_.community).result.head)
  }

  def checkUniqueName(name: String, communityId: Long): Future[Boolean] = {
    DB.run(
      PersistenceSchema.triggers
        .filter(_.community === communityId)
        .filter(_.name === name)
        .exists
        .result
    ).map(!_)
  }

  def checkUniqueName(triggerIdToIgnore: Long, name: String, communityId: Long): Future[Boolean] = {
    DB.run(
      PersistenceSchema.triggers
        .filter(_.community === communityId)
        .filter(_.name === name)
        .filter(_.id =!= triggerIdToIgnore)
        .exists
        .result
    ).map(!_)
  }

  def getTriggersByCommunity(communityId: Long): Future[List[Triggers]] = {
    DB.run(PersistenceSchema.triggers
      .filter(_.community === communityId)
      .map(x => (x.id, x.name, x.description, x.url, x.eventType, x.serviceType, x.active, x.community, x.latestResultOk))
      .sortBy(_._2.asc)
      .result
    ).map { result =>
      result.map(x => Triggers(x._1, x._2, x._3, x._4, x._5, x._6, None, x._7, x._9, None, x._8)).toList
    }
  }

  def createTrigger(trigger: Trigger): Future[Long] = {
    DB.run(createTriggerInternal(trigger).transactionally)
  }

  private[managers] def createTriggerInternal(trigger: Trigger): DBIO[Long] = {
    for {
      triggerId <- PersistenceSchema.insertTriggers += trigger.trigger
      _ <- setTriggerDataInternal(triggerId, trigger.data, isNewTrigger = true)
      _ <- setTriggerFireExpressions(triggerId, trigger.fireExpressions, isNewTrigger = true)
    } yield triggerId
  }

  def getTriggerAndDataById(triggerId: Long): Future[Trigger] = {
    DB.run(
      for {
        trigger <- PersistenceSchema.triggers.filter(_.id === triggerId).result.head
        triggerData <- PersistenceSchema.triggerData.filter(_.trigger === triggerId).result
        triggerFireExpressions <- PersistenceSchema.triggerFireExpressions.filter(_.trigger === triggerId).result
      } yield new Trigger(trigger, Some(triggerData.toList), Some(triggerFireExpressions.toList))
    )
  }

  def getTriggerAndDataByCommunityId(communityId: Long): Future[List[Trigger]] = {
    DB.run(
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
    ).map { result =>
      toTriggerList(result._1, result._2, result._3)
    }
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

  private def getTriggersAndDataByCommunityAndType(communityId: Long, eventType: TriggerEventType): Future[Option[List[Trigger]]] = {
    DB.run(
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
    ).map { result =>
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
  }

  def updateTrigger(trigger: Trigger): Future[Unit] = {
    DB.run(updateTriggerInternal(trigger).transactionally)
  }

  def clearStatus(triggerId: Long): Future[Int] = {
    DB.run((for { t <- PersistenceSchema.triggers.filter(_.id === triggerId) } yield (t.latestResultOk, t.latestResultOutput)).update(None, None).transactionally)
  }

  private[managers] def updateTriggerInternal(trigger: Trigger): DBIO[Unit] = {
    for {
      _ <- {
        val q = for { t <- PersistenceSchema.triggers if t.id === trigger.trigger.id } yield (t.name, t.description, t.url, t.eventType, t.serviceType, t.operation, t.active)
        q.update(trigger.trigger.name, trigger.trigger.description, trigger.trigger.url, trigger.trigger.eventType, trigger.trigger.serviceType, trigger.trigger.operation, trigger.trigger.active)
      }
      _ <- setTriggerDataInternal(trigger.trigger.id, trigger.data, isNewTrigger = false)
      _ <- setTriggerFireExpressions(trigger.trigger.id, trigger.fireExpressions, isNewTrigger = false)
    } yield ()
  }

  private def hasTriggerDataType(data: Option[List[TriggerData]], dataType: TriggerDataType): Boolean = {
    if (data.isDefined) {
      data.get.foreach { dataItem =>
        if (TriggerDataType.apply(dataItem.dataType) == dataType) {
          return true
        }
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

  private def initializeContentMap(name: String): AnyContent = {
    val newMap = new AnyContent()
    newMap.setName(name)
    newMap.setType("map")
    newMap
  }

  private def loadOrganisationParameterData(parameterIds: List[Long], organisationId: Option[Long]): Future[Map[Long, (String, String, Option[String], Option[Long], Option[String])]] = {
    for {
      // id, key, kind, value, orgId, contentType
      data <- {
        if (organisationId.isDefined) {
          DB.run(PersistenceSchema.organisationParameters
            .join(PersistenceSchema.organisationParameterValues).on(_.id === _.parameter)
            .filter(_._1.id inSet parameterIds).filter(_._2.organisation === organisationId.get)
            .map(x => (x._1.id, x._1.testKey, x._1.kind, x._2.value, x._2.organisation, x._2.contentType))
            .result
          ).map(_.map(x => (x._1, x._2, x._3, Some(x._4), Some(x._5), x._6)).toList)
        } else {
          DB.run(PersistenceSchema.organisationParameters
            .filter(_.id inSet parameterIds)
            .map(x => (x.id, x.testKey, x.kind))
            .result
          ).map(_.map(x => (x._1, x._2, x._3, None, None, None)).toList)
        }
      }
      result <- {
        val resultMap = mutable.LinkedHashMap[Long, (String, String, Option[String], Option[Long], Option[String])]()
        data.foreach { dataItem =>
          resultMap += (dataItem._1 -> (dataItem._2, dataItem._3, dataItem._4, dataItem._5, dataItem._6))
        }
        Future.successful {
          ListMap.from(resultMap)
        }
      }
    } yield result
  }

  private def loadSystemParameterData(parameterIds: List[Long], systemId: Option[Long]): Future[Map[Long, (String, String, Option[String], Option[Long], Option[String])]] = {
    for {
      // id, key, kind, value, sysId, contentType
      data <- {
        if (systemId.isDefined) {
          DB.run(PersistenceSchema.systemParameters
            .join(PersistenceSchema.systemParameterValues).on(_.id === _.parameter)
            .filter(_._1.id inSet parameterIds).filter(_._2.system === systemId.get)
            .map(x => (x._1.id, x._1.testKey, x._1.kind, x._2.value, x._2.system, x._2.contentType))
            .result
          ).map(_.map(x => (x._1, x._2, x._3, Some(x._4), Some(x._5), x._6)).toList)
        } else {
          DB.run(PersistenceSchema.systemParameters
            .filter(_.id inSet parameterIds)
            .map(x => (x.id, x.testKey, x.kind))
            .result
          ).map(_.map(x => (x._1, x._2, x._3, None, None, None)).toList)
        }
      }
      result <- {
        val resultMap = mutable.LinkedHashMap[Long, (String, String, Option[String], Option[Long], Option[String])]()
        data.foreach { dataItem =>
          resultMap += (dataItem._1 -> (dataItem._2, dataItem._3, dataItem._4, dataItem._5, dataItem._6))
        }
        Future.successful {
          ListMap.from(resultMap)
        }
      }
    } yield result
  }

  private def loadDomainParameterData(parameterIds: List[Long]): Future[Map[Long, (String, String, Option[String], Long, Option[String])]] = {
    DB.run(
      PersistenceSchema.domainParameters.filter(_.id inSet parameterIds).map(x => (x.id, x.name, x.kind, x.value, x.domain, x.contentType)).result
    ).map(_.map(x => (x._1, x._2, x._3, x._4, x._5, x._6)).toList).map { data =>
      val resultMap = mutable.LinkedHashMap[Long, (String, String, Option[String], Long, Option[String])]()
      data.foreach { dataItem =>
        resultMap += (dataItem._1 -> (dataItem._2, dataItem._3, dataItem._4, dataItem._5, dataItem._6))
      }
      ListMap.from(resultMap)
    }
  }

  private def loadStatementParameterData(parameterIds: List[Long], actorId: Option[Long], systemId: Option[Long], communityId: Long): Future[Map[Long, (String, String, Option[String], Option[Long], Option[String])]] = {
    for {
      // key, ID, kind, value, system ID, contentType
      data <- {
        if (systemId.isDefined && actorId.isDefined) {
          DB.run(
            PersistenceSchema.parameters
              .join(PersistenceSchema.configs).on(_.id === _.parameter)
              .join(PersistenceSchema.endpoints).on(_._1.endpoint === _.id)
              .filter(_._2.actor === actorId.get)
              .filter(_._1._2.system === systemId.get)
              .filter(_._1._1.id inSet parameterIds)
              .map(x => (x._1._1.id, x._1._1.testKey, x._1._1.kind, x._1._2.value, x._1._2.contentType))
              .result
          ).map(_.map(x => (x._1, x._2, x._3, Some(x._4), systemId, x._5)).toList)
        } else {
          DB.run(
            for {
              domainId <- PersistenceSchema.communities.filter(_.id === communityId).map(_.domain).result.head
              parameterData <-
                PersistenceSchema.parameters
                  .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
                  .join(PersistenceSchema.actors).on(_._2.actor === _.id)
                  .filterOpt(domainId)((table, domainIdValue) => table._2.domain === domainIdValue)
                  .filter(_._1._1.id inSet parameterIds)
                  .map(x => (x._1._1.id, x._1._1.testKey, x._1._1.kind))
                  .result
            } yield parameterData
          ).map(_.map(x => (x._1, x._2, x._3, None, None, None)).toList)
        }
      }
      result <- {
        val resultMap = mutable.LinkedHashMap[Long, (String, String, Option[String], Option[Long], Option[String])]()
        data.foreach { dataItem =>
          resultMap += (dataItem._1 -> ((dataItem._2, dataItem._3, dataItem._4, dataItem._5, dataItem._6)))
        }
        Future.successful {
          ListMap.from(resultMap)
        }
      }
    } yield result
  }

  private def getSampleTestReport(): String = {
    IOUtils.resourceToString("other/sampleTestCaseReport.xml", StandardCharsets.UTF_8, env.classLoader)
  }

  /**
   * Load (or initialize) data from the cache when the operation to load the data does not change the cache itself.
   */
  private def loadTriggerData[T](cache: Map[String, Any], trigger: Trigger, dataType: TriggerDataType, cacheKey: String, extraCondition: Option[Boolean], loader: () => Future[T]): Future[Option[CacheData[T]]] = {
    if (hasTriggerDataType(trigger.data, dataType) && (extraCondition.isEmpty || extraCondition.get)) {
      fromCache(cache, cacheKey, loader).map(Some(_))
    } else {
      Future.successful(None)
    }
  }

  /**
   * Load (or initialize) data from the cache when the operation to load the data is also expected to change the cache itself (e.g. multistep lookups).
   */
  private def loadTriggerDataWithCache[T](cache: Map[String, Any], trigger: Trigger, dataType: TriggerDataType, cacheKey: String, loader: Map[String, Any] => Future[CacheData[T]]): Future[Option[CacheData[T]]] = {
    if (hasTriggerDataType(trigger.data, dataType)) {
      fromCacheWithCache(cache, cacheKey, () => loader.apply(cache)).map(Some(_))
    } else {
      Future.successful(None)
    }
  }

  private def prepareTriggerInput(trigger: Trigger, dataCache: Map[String, Any], callbacks: Option[TriggerCallbacks]): Future[CacheData[Option[ProcessRequest]]] = {
    for {
      /*
       * Load data to include in request
       */
      // Organisation parameter data
      dataLookup <- {
        loadTriggerDataWithCache(dataCache, trigger, TriggerDataType.OrganisationParameter, "organisationParameterData",
          dataCache => {
            for {
              organisationIdLookup <- TriggerCallbacks.organisationId(dataCache, callbacks)
              dataCache <- Future.successful(organisationIdLookup.cache)
              organisationId <- Future.successful(organisationIdLookup.data)
              params <- loadOrganisationParameterData(trigger.data.get.filter(_.dataType == TriggerDataType.OrganisationParameter.id).map(x => x.dataId), organisationId).map { data =>
                if (data.nonEmpty) {
                  val orgParamData = initializeContentMap("organisationProperties")
                  data.foreach { param =>
                    if ("BINARY".equals(param._2._2)) {
                      var value = "BASE64_CONTENT_OF_FILE"
                      if (param._2._3.isDefined && param._2._4.isDefined) {
                        value = Base64.getEncoder.encodeToString(Files.readAllBytes(repositoryUtils.getOrganisationPropertyFile(param._1, param._2._4.get).toPath))
                      }
                      orgParamData.getItem.add(toAnyContent(param._2._1, "binary", value, Some(ValueEmbeddingEnumeration.BASE_64)))
                    } else if ("HIDDEN".equals(param._2._2) || "SECRET".equals(param._2._2)) {
                      orgParamData.getItem.add(toAnyContent(param._2._1, "string", param._2._3.map(MimeUtil.decryptString).getOrElse("Sample data"), None))
                    } else {
                      orgParamData.getItem.add(toAnyContent(param._2._1, "string", param._2._3.getOrElse("Sample data"), None))
                    }
                  }
                  Some(orgParamData)
                } else {
                  None
                }
              }
            } yield CacheData(dataCache, params)
          }
        )
      }
      organisationParameterData <- Future.successful(dataLookup.flatMap(_.data))
      dataCache <- Future.successful(dataLookup.map(_.cache).getOrElse(dataCache))
      // System parameter data
      dataLookup <- {
        loadTriggerDataWithCache(dataCache, trigger, TriggerDataType.SystemParameter, "systemParameterData",
          dataCache => {
            for {
              systemIdLookup <- TriggerCallbacks.systemId(dataCache, callbacks)
              dataCache <- Future.successful(systemIdLookup.cache)
              systemId <- Future.successful(systemIdLookup.data)
              params <- loadSystemParameterData(trigger.data.get.filter(_.dataType == TriggerDataType.SystemParameter.id).map(x => x.dataId), systemId).map { data =>
                if (data.nonEmpty) {
                  val sysParamData = initializeContentMap("systemProperties")
                  data.foreach { param =>
                    if ("BINARY".equals(param._2._2)) {
                      var value = "BASE64_CONTENT_OF_FILE"
                      if (param._2._3.isDefined && param._2._4.isDefined) {
                        value = Base64.getEncoder.encodeToString(Files.readAllBytes(repositoryUtils.getSystemPropertyFile(param._1, param._2._4.get).toPath))
                      }
                      sysParamData.getItem.add(toAnyContent(param._2._1, "binary", value, Some(ValueEmbeddingEnumeration.BASE_64)))
                    } else if ("HIDDEN".equals(param._2._2) || "SECRET".equals(param._2._2)) {
                      sysParamData.getItem.add(toAnyContent(param._2._1, "string", param._2._3.map(MimeUtil.decryptString).getOrElse("Sample data"), None))
                    } else {
                      sysParamData.getItem.add(toAnyContent(param._2._1, "string", param._2._3.getOrElse("Sample data"), None))
                    }
                  }
                  Some(sysParamData)
                } else {
                  None
                }
              }
            } yield CacheData(dataCache, params)
          }
        )
      }
      systemParameterData <- Future.successful(dataLookup.flatMap(_.data))
      dataCache <- Future.successful(dataLookup.map(_.cache).getOrElse(dataCache))
      // Domain parameter data
      dataLookup <- {
        loadTriggerData(dataCache, trigger, TriggerDataType.DomainParameter, "domainParameterData", None,
          () => {
            loadDomainParameterData(trigger.data.get.filter(_.dataType == TriggerDataType.DomainParameter.id).map(x => x.dataId)).map { data =>
              if (data.nonEmpty) {
                val domainParamData = initializeContentMap("domainParameters")
                data.foreach { param =>
                  if ("BINARY".equals(param._2._2)) {
                    var value = "BASE64_CONTENT_OF_FILE"
                    if (param._2._3.isDefined) {
                      value = Base64.getEncoder.encodeToString(Files.readAllBytes(repositoryUtils.getDomainParameterFile(param._2._4, param._1).toPath))
                    }
                    domainParamData.getItem.add(toAnyContent(param._2._1, "binary", value, Some(ValueEmbeddingEnumeration.BASE_64)))
                  } else if ("HIDDEN".equals(param._2._2) || "SECRET".equals(param._2._2)) {
                    domainParamData.getItem.add(toAnyContent(param._2._1, "string", param._2._3.map(MimeUtil.decryptString).getOrElse("Sample data"), None))
                  } else {
                    domainParamData.getItem.add(toAnyContent(param._2._1, "string", param._2._3.getOrElse("Sample data"), None))
                  }
                }
                Some(domainParamData)
              } else {
                None
              }
            }
          }
        )
      }
      domainParameterData <- Future.successful(dataLookup.flatMap(_.data))
      dataCache <- Future.successful(dataLookup.map(_.cache).getOrElse(dataCache))
      // Statement parameter data
      dataLookup <- {
        loadTriggerDataWithCache(dataCache, trigger, TriggerDataType.StatementParameter, "statementParameterData",
          dataCache => {
            for {
              actorIdLookup <- TriggerCallbacks.actorId(dataCache, callbacks)
              dataCache <- Future.successful(actorIdLookup.cache)
              actorId <- Future.successful(actorIdLookup.data)
              systemIdLookup <- TriggerCallbacks.systemId(dataCache, callbacks)
              dataCache <- Future.successful(systemIdLookup.cache)
              systemId <- Future.successful(systemIdLookup.data)
              params <- loadStatementParameterData(trigger.data.get.filter(_.dataType == TriggerDataType.StatementParameter.id).map(x => x.dataId), actorId, systemId, trigger.trigger.community).map { data =>
                if (data.nonEmpty) {
                  val statementParamData = initializeContentMap("statementProperties")
                  data.foreach { param =>
                    if ("BINARY".equals(param._2._2)) {
                      var value = "BASE64_CONTENT_OF_FILE"
                      if (param._2._3.isDefined) {
                        value = Base64.getEncoder.encodeToString(Files.readAllBytes(repositoryUtils.getStatementParameterFile(param._1, param._2._4.get).toPath))
                      }
                      statementParamData.getItem.add(toAnyContent(param._2._1, "binary", value, Some(ValueEmbeddingEnumeration.BASE_64)))
                    } else if ("HIDDEN".equals(param._2._2) || "SECRET".equals(param._2._2)) {
                      statementParamData.getItem.add(toAnyContent(param._2._1, "string", param._2._3.map(MimeUtil.decryptString).getOrElse("Sample data"), None))
                    } else {
                      statementParamData.getItem.add(toAnyContent(param._2._1, "string", param._2._3.getOrElse("Sample data"), None))
                    }
                  }
                  Some(statementParamData)
                } else {
                  None
                }
              }
            } yield CacheData(dataCache, params)
          }
        )
      }
      statementParameterData <- Future.successful(dataLookup.flatMap(_.data))
      dataCache <- Future.successful(dataLookup.map(_.cache).getOrElse(dataCache))
      // Community data
      dataLookup <- {
        loadTriggerData(dataCache, trigger, TriggerDataType.Community, "communityData", None,
          () => DB.run(PersistenceSchema.communities
            .filter(_.id === trigger.trigger.community)
            .map(x => (x.shortname, x.fullname))
            .result
            .head
          ).map { result =>
            val communityData = initializeContentMap("community")
            communityData.getItem.add(toAnyContent("id", "number", trigger.trigger.community.toString, None))
            communityData.getItem.add(toAnyContent("shortName", "string", result._1, None))
            communityData.getItem.add(toAnyContent("fullName", "string", result._2, None))
            communityData
          }
        )
      }
      communityData <- Future.successful(dataLookup.map(_.data))
      dataCache <- Future.successful(dataLookup.map(_.cache).getOrElse(dataCache))
      // Organisation
      dataLookup <- {
        loadTriggerDataWithCache(dataCache, trigger, TriggerDataType.Organisation, "organisation",
          cache => {
            for {
              organisationIdLookup <- TriggerCallbacks.organisationId(cache, callbacks)
              organisationDataLookup <- {
                val organisationId = organisationIdLookup.data
                if (organisationId.isEmpty) {
                  val organisationData = initializeContentMap("organisation")
                  organisationData.getItem.add(toAnyContent("id", "number", "123", None))
                  organisationData.getItem.add(toAnyContent("shortName", "string", "Organisation short name", None))
                  organisationData.getItem.add(toAnyContent("fullName", "string", "Organisation full name", None))
                  Future.successful {
                    CacheData(organisationIdLookup.cache, Option(organisationData))
                  }
                } else {
                  getOrganisationData(organisationIdLookup.cache, callbacks).map { organisationLookup =>
                    if (organisationLookup.data.isDefined) {
                      val organisationData = initializeContentMap("organisation")
                      organisationData.getItem.add(toAnyContent("id", "number", organisationLookup.data.get.id.toString, None))
                      organisationData.getItem.add(toAnyContent("shortName", "string", organisationLookup.data.get.shortName, None))
                      organisationData.getItem.add(toAnyContent("fullName", "string", organisationLookup.data.get.fullName, None))
                      CacheData(organisationLookup.cache, Option(organisationData))
                    } else {
                      CacheData(organisationLookup.cache, Option.empty[AnyContent])
                    }
                  }
                }
              }
            } yield organisationDataLookup
          }
        )
      }
      organisationData <- Future.successful(dataLookup.flatMap(_.data))
      dataCache <- Future.successful(dataLookup.map(_.cache).getOrElse(dataCache))
      // System
      dataLookup <- {
        loadTriggerDataWithCache(dataCache, trigger, TriggerDataType.System, "system",
          cache => {
            for {
              systemIdLookup <- TriggerCallbacks.systemId(cache, callbacks)
              systemDataLookup <- {
                val systemId = systemIdLookup.data
                if (systemId.isEmpty) {
                  val systemData = initializeContentMap("system")
                  systemData.getItem.add(toAnyContent("id", "number", "123", None))
                  systemData.getItem.add(toAnyContent("shortName", "string", "System short name", None))
                  systemData.getItem.add(toAnyContent("fullName", "string", "System full name", None))
                  Future.successful {
                    CacheData(systemIdLookup.cache, Option(systemData))
                  }
                } else {
                  getSystemData(systemIdLookup.cache, callbacks).map { systemLookup =>
                    if (systemLookup.data.isDefined) {
                      val systemData = initializeContentMap("system")
                      systemData.getItem.add(toAnyContent("id", "number", systemLookup.data.get.id.toString, None))
                      systemData.getItem.add(toAnyContent("shortName", "string", systemLookup.data.get.shortName, None))
                      systemData.getItem.add(toAnyContent("fullName", "string", systemLookup.data.get.fullName, None))
                      CacheData(systemLookup.cache, Option(systemData))
                    } else {
                      CacheData(systemLookup.cache, Option.empty[AnyContent])
                    }
                  }
                }
              }
            } yield systemDataLookup
          }
        )
      }
      systemData <- Future.successful(dataLookup.flatMap(_.data))
      dataCache <- Future.successful(dataLookup.map(_.cache).getOrElse(dataCache))
      // Specification
      dataLookup <- {
        loadTriggerDataWithCache(dataCache, trigger, TriggerDataType.Specification, "specification",
          cache => {
            for {
              specificationIdLookup <- getSpecificationId(cache, callbacks)
              specificationDataLookup <- {
                val specificationId = specificationIdLookup.data
                if (specificationId.isEmpty) {
                  val specificationData = initializeContentMap("specification")
                  specificationData.getItem.add(toAnyContent("id", "number", "123", None))
                  specificationData.getItem.add(toAnyContent("shortName", "string", "Specification short name", None))
                  specificationData.getItem.add(toAnyContent("fullName", "string", "Specification full name", None))
                  Future.successful {
                    CacheData(specificationIdLookup.cache, Option(specificationData))
                  }
                } else {
                  getSpecificationData(specificationIdLookup.cache, callbacks).map { specificationLookup =>
                    if (specificationLookup.data.isDefined) {
                      val specificationData = initializeContentMap("specification")
                      specificationData.getItem.add(toAnyContent("id", "number", specificationLookup.data.get.id.toString, None))
                      specificationData.getItem.add(toAnyContent("shortName", "string", specificationLookup.data.get.shortName, None))
                      specificationData.getItem.add(toAnyContent("fullName", "string", specificationLookup.data.get.fullName, None))
                      CacheData(specificationLookup.cache, Option(specificationData))
                    } else {
                      CacheData(specificationLookup.cache, Option.empty[AnyContent])
                    }
                  }
                }
              }
            } yield specificationDataLookup
          }
        )
      }
      specificationData <- Future.successful(dataLookup.flatMap(_.data))
      dataCache <- Future.successful(dataLookup.map(_.cache).getOrElse(dataCache))
      // Actor
      dataLookup <- {
        loadTriggerDataWithCache(dataCache, trigger, TriggerDataType.Actor, "actor",
          cache => {
            for {
              actorIdLookup <- TriggerCallbacks.actorId(cache, callbacks)
              actorDataLookup <- {
                val actorId = actorIdLookup.data
                if (actorId.isEmpty) {
                  val actorData = initializeContentMap("actor")
                  actorData.getItem.add(toAnyContent("id", "number", "123", None))
                  actorData.getItem.add(toAnyContent("actorIdentifier", "string", "Actor identifier", None))
                  actorData.getItem.add(toAnyContent("name", "string", "Actor name", None))
                  Future.successful {
                    CacheData(actorIdLookup.cache, Option(actorData))
                  }
                } else {
                  getActorData(actorIdLookup.cache, callbacks).map { actorLookup =>
                    if (actorLookup.data.isDefined) {
                      val actorData = initializeContentMap("actor")
                      actorData.getItem.add(toAnyContent("id", "number", actorLookup.data.get.id.toString, None))
                      actorData.getItem.add(toAnyContent("actorIdentifier", "string", actorLookup.data.get.identifier, None))
                      actorData.getItem.add(toAnyContent("name", "string", actorLookup.data.get.name, None))
                      CacheData(actorLookup.cache, Option(actorData))
                    } else {
                      CacheData(actorLookup.cache, Option.empty[AnyContent])
                    }
                  }
                }
              }
            } yield actorDataLookup
          }
        )
      }
      actorData <- Future.successful(dataLookup.flatMap(_.data))
      dataCache <- Future.successful(dataLookup.map(_.cache).getOrElse(dataCache))
      // Test session
      dataLookup <- {
        loadTriggerDataWithCache(dataCache, trigger, TriggerDataType.TestSession, "testSession",
          cache => {
            for {
              testSessionIdLookup <- TriggerCallbacks.testSessionId(cache, callbacks)
              testSessionDataLookup <- {
                val sessionId = testSessionIdLookup.data
                if (sessionId.isEmpty) {
                  val testSessionData = initializeContentMap("testSession")
                  testSessionData.getItem.add(toAnyContent("testSuiteIdentifier", "string", "TestSuiteID", None))
                  testSessionData.getItem.add(toAnyContent("testCaseIdentifier", "string", "TestCaseID", None))
                  testSessionData.getItem.add(toAnyContent("testSessionIdentifier", "string", "TestSessionID", None))
                  Future.successful {
                    CacheData(testSessionIdLookup.cache, Option(testSessionData))
                  }
                } else {
                  getTestSessionData(testSessionIdLookup.cache, callbacks).map { sessionLookup =>
                    if (sessionLookup.data.isDefined) {
                      val testSessionData = initializeContentMap("testSession")
                      testSessionData.getItem.add(toAnyContent("testSuiteIdentifier", "string", sessionLookup.data.get.testSuiteIdentifier, None))
                      testSessionData.getItem.add(toAnyContent("testCaseIdentifier", "string", sessionLookup.data.get.testCaseIdentifier, None))
                      testSessionData.getItem.add(toAnyContent("testSessionIdentifier", "string", sessionLookup.data.get.testSessionIdentifier, None))
                      CacheData(sessionLookup.cache, Option(testSessionData))
                    } else {
                      CacheData(sessionLookup.cache, Option.empty[AnyContent])
                    }
                  }
                }
              }
            } yield testSessionDataLookup
          }
        )
      }
      sessionData <- Future.successful(dataLookup.flatMap(_.data))
      dataCache <- Future.successful(dataLookup.map(_.cache).getOrElse(dataCache))
      // Test report
      dataLookup <- {
        loadTriggerDataWithCache(dataCache, trigger, TriggerDataType.TestReport, "testReport",
          cache => {
            for {
              testSessionIdLookup <- TriggerCallbacks.testSessionId(cache, callbacks)
              testSessionDataLookup <- {
                val sessionId = testSessionIdLookup.data
                if (sessionId.isEmpty) {
                  val testReportData = initializeContentMap("testReport")
                  populateAnyContent(Some(testReportData), "testReport", "string", getSampleTestReport(), None)
                  Future.successful {
                    CacheData(testSessionIdLookup.cache, Option(testReportData))
                  }
                } else {
                  val reportPath = repositoryUtils.getReportTempFile(".xml")
                  reportManager.generateTestCaseReport(reportPath, sessionId.get, Constants.MimeTypeXML, Some(trigger.trigger.community), None).map { _ =>
                    if (Files.exists(reportPath)) {
                      val reportContent = Files.readString(reportPath)
                      val testReportData = initializeContentMap("testReport")
                      populateAnyContent(Some(testReportData), "testReport", "string", reportContent, None)
                      CacheData(testSessionIdLookup.cache, Option(testReportData))
                    } else {
                      CacheData(testSessionIdLookup.cache, Option.empty[AnyContent])
                    }
                  }.andThen { _ =>
                    if (Files.exists(reportPath)) FileUtils.deleteQuietly(reportPath.toFile)
                  }
                }
              }
            } yield testSessionDataLookup
          }
        )
      }
      testReportData <- Future.successful(dataLookup.flatMap(_.data))
      dataCache <- Future.successful(dataLookup.map(_.cache).getOrElse(dataCache))
      /*
       * Prepare request
       */
      request <- {
        val request = new ProcessRequest()
        if (trigger.trigger.operation.isDefined && !trigger.trigger.operation.get.isBlank) {
          request.setOperation(trigger.trigger.operation.get)
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
        if (sessionData.isDefined) {
          request.getInput.add(sessionData.get)
        }
        if (domainParameterData.isDefined) {
          request.getInput.add(domainParameterData.get)
        }
        if (organisationParameterData.isDefined) {
          request.getInput.add(organisationParameterData.get)
        }
        if (systemParameterData.isDefined) {
          request.getInput.add(systemParameterData.get)
        }
        if (statementParameterData.isDefined) {
          request.getInput.add(statementParameterData.get)
        }
        if (testReportData.isDefined) {
          request.getInput.add(testReportData.get)
        }
        Future.successful(CacheData(dataCache, Option(request)))
      }
    } yield request
  }

  private def getSpecificationId(dataCache: Map[String, Any], callbacks: Option[TriggerCallbacks]): Future[CacheData[Option[Long]]] = {
    for {
      actorLookup <- TriggerCallbacks.actorId(dataCache, callbacks)
      specificationLookup <- {
        fromCache(actorLookup.cache, "specificationId", () => {
          if (actorLookup.data.isDefined) {
            DB.run(
              PersistenceSchema.specificationHasActors
                .filter(_.actorId === actorLookup.data.get)
                .map(x => x.specId)
                .result
                .headOption
            )
          } else {
            Future.successful(None)
          }
        })
      }
    } yield specificationLookup
  }

  private def getOrganisationData(dataCache: Map[String, Any], callbacks: Option[TriggerCallbacks]): Future[CacheData[Option[OrganisationData]]] = {
    for {
      organisationIdLookup <- TriggerCallbacks.organisationId(dataCache, callbacks)
      organisationLookup <- {
        fromCache(organisationIdLookup.cache, "organisationData", () => {
          if (organisationIdLookup.data.isDefined) {
            DB.run(
              PersistenceSchema.organizations
                .filter(_.id === organisationIdLookup.data.get)
                .map(x => (x.shortname, x.fullname))
                .result
                .headOption
                .map(_.map(x => OrganisationData(organisationIdLookup.data.get, x._1, x._2)))
            )
          } else {
            Future.successful(None)
          }
        })
      }
    } yield organisationLookup
  }

  private def getSystemData(dataCache: Map[String, Any], callbacks: Option[TriggerCallbacks]): Future[CacheData[Option[SystemData]]] = {
    for {
      systemIdLookup <- TriggerCallbacks.systemId(dataCache, callbacks)
      systemLookup <- {
        fromCache(systemIdLookup.cache, "systemData", () => {
          if (systemIdLookup.data.isDefined) {
            DB.run(
              PersistenceSchema.systems
                .filter(_.id === systemIdLookup.data.get)
                .map(x => (x.shortname, x.fullname))
                .result
                .headOption
                .map(_.map(x => SystemData(systemIdLookup.data.get, x._1, x._2)))
            )
          } else {
            Future.successful(None)
          }
        })
      }
    } yield systemLookup
  }

  private def getSpecificationData(dataCache: Map[String, Any], callbacks: Option[TriggerCallbacks]): Future[CacheData[Option[SpecificationData]]] = {
    for {
      specificationIdLookup <- getSpecificationId(dataCache, callbacks)
      specificationLookup <- {
        fromCache(specificationIdLookup.cache, "specificationData", () => {
          if (specificationIdLookup.data.isDefined) {
            DB.run(
              PersistenceSchema.specifications
                .filter(_.id === specificationIdLookup.data.get)
                .map(x => (x.shortname, x.fullname))
                .result
                .headOption
                .map(_.map(x => SpecificationData(specificationIdLookup.data.get, x._1, x._2)))
            )
          } else {
            Future.successful(None)
          }
        })
      }
    } yield specificationLookup
  }

  private def getActorData(dataCache: Map[String, Any], callbacks: Option[TriggerCallbacks]): Future[CacheData[Option[ActorData]]] = {
    for {
      actorIdLookup <- TriggerCallbacks.actorId(dataCache, callbacks)
      actorLookup <- {
        fromCache(actorIdLookup.cache, "actorData", () => {
          if (actorIdLookup.data.isDefined) {
            DB.run(
              PersistenceSchema.actors
                .filter(_.id === actorIdLookup.data.get)
                .map(x => (x.actorId, x.name))
                .result
                .headOption
                .map(_.map(x => ActorData(actorIdLookup.data.get, x._1, x._2)))
            )
          } else {
            Future.successful(None)
          }
        })
      }
    } yield actorLookup
  }

  private def getTestSessionData(dataCache: Map[String, Any], callbacks: Option[TriggerCallbacks]): Future[CacheData[Option[TestSessionData]]] = {
    for {
      sessionIdLookup <- TriggerCallbacks.testSessionId(dataCache, callbacks)
      sessionLookup <- {
        fromCache(sessionIdLookup.cache, "testSessionData", () => {
          if (sessionIdLookup.data.isDefined) {
            DB.run(
              for {
                sessionIds <- PersistenceSchema.testResults.filter(_.testSessionId === sessionIdLookup.data.get).map(x => (x.testSuiteId, x.testCaseId)).result.headOption
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
              } yield testCaseIdentifier.map(TestSessionData(testSuiteIdentifier.get, _, sessionIdLookup.data.get))
            )
          } else {
            Future.successful(None)
          }
        })
      }
    } yield sessionLookup
  }

  def previewTriggerCall(communityId: Long, operation: Option[String], serviceType: TriggerServiceType, data: Option[List[TriggerData]]): Future[String] = {
    prepareTriggerInput(new Trigger(Triggers(-1, "", None, "", -1, serviceType.id.toShort, operation, active = false, None, None, communityId), data, None), Map[String, Any](), None).map { request =>
      if (serviceType == TriggerServiceType.GITB) {
        val requestWrapper = new JAXBElement[ProcessRequest](new QName("http://www.gitb.com/ps/v1/", "ProcessRequest"), classOf[ProcessRequest], request.data.get)
        val bos = new ByteArrayOutputStream()
        XMLUtils.marshalToStream(requestWrapper, bos)
        new String(bos.toByteArray, StandardCharsets.UTF_8)
      } else {
        JsonUtil.jsProcessRequest(request.data.get).toString()
      }
    }
  }

  private def callHttpService(url: String, fnPayloadProvider: () => String): Future[ServiceTestResult] = {
    for {
      payload <- Future.successful { fnPayloadProvider.apply() }
      response <- ws.url(url)
        .addHttpHeaders("Content-Type" -> "application/json")
        .withFollowRedirects(true)
        .post(payload)
        .map { response => ServiceTestResult(response.status < 400, Some(List(response.body)), response.headers.getOrElse("Content-Type", List("text/plain")).head)
        }
    } yield response
  }

  private def callProcessingService(url: String, fnCallOperation: ProcessingService => JAXBElement[_]): Future[ServiceTestResult] = {
    Future {
      val service = new ProcessingServiceService(URI.create(url).toURL)
      val response = fnCallOperation.apply(service.getProcessingServicePort)
      val bos = new ByteArrayOutputStream()
      XMLUtils.marshalToStream(response, bos)
      ServiceTestResult(success = true, Some(List(new String(bos.toByteArray, StandardCharsets.UTF_8))), Constants.MimeTypeXML)
    }
  }

  def testTriggerEndpoint(url: String, serviceType: TriggerServiceType): Future[ServiceTestResult] = {
    val promise = Promise[ServiceTestResult]()
    val future: Future[ServiceTestResult] = if (serviceType == TriggerServiceType.GITB) {
      callProcessingService(url, service => {
        val response = service.getModuleDefinition(new com.gitb.ps.Void())
        new JAXBElement[GetModuleDefinitionResponse](new QName("http://www.gitb.com/ps/v1/", "GetModuleDefinitionResponse"), classOf[GetModuleDefinitionResponse], response)
      })
    } else {
      callHttpService(url, () => "{}")
    }
    future.onComplete {
      case Success(result) => promise.success(result)
      case Failure(exception) => promise.success(ServiceTestResult(success = false, Some(extractFailureDetails(exception)), Constants.MimeTypeTextPlain))
    }
    promise.future
  }

  def testTriggerCall(url: String, serviceType: TriggerServiceType, payload: String): Future[ServiceTestResult] = {
    val promise = Promise[ServiceTestResult]()
    val future: Future[ServiceTestResult] = if (serviceType == TriggerServiceType.GITB) {
      callProcessingService(url, service => {
        val request = XMLUtils.unmarshal(classOf[ProcessRequest],
          new StreamSource(new StringReader(payload)),
          new StreamSource(this.getClass.getResourceAsStream("/schema/gitb_ps.xsd")),
          new ClasspathResourceResolver())
        val response = service.process(request)
        new JAXBElement[ProcessResponse](new QName("http://www.gitb.com/ps/v1/", "ProcessResponse"), classOf[ProcessResponse], response)
      })
    } else {
      callHttpService(url, () => {
        val payloadAsJson = Json.parse(payload)
        payloadAsJson.validate(JsonUtil.validatorForProcessRequest())
        payloadAsJson.toString()
      })
    }
    future.onComplete {
      case Success(result) => promise.success(result)
      case Failure(exception) => promise.success(ServiceTestResult(success = false, Some(extractFailureDetails(exception)), Constants.MimeTypeTextPlain))
    }
    promise.future
  }

  def fireTriggers(communityId: Long, eventType: TriggerEventType, triggerData: Any): Future[Unit] = {
    getTriggersAndDataByCommunityAndType(communityId, eventType).flatMap { triggers =>
      if (triggers.isDefined) {
        // Use a cache to avoid the same DB operations.
        val dataCache = Map[String, Any]()
        val callbacks = TriggerCallbacks.newInstance(triggerDataLoader)
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
        // Check to see which triggers satisfy our execution conditions
        Future.sequence {
          triggers.get.map { trigger =>
            checkTriggerFireCondition(trigger, dataCache, Some(callbacks)).map { shouldFire =>
              if (shouldFire.data) {
                (shouldFire, Some(trigger))
              } else {
                (shouldFire, None)
              }
            }
          }
        }.flatMap { checkedTriggers =>
          val latestCacheState = checkedTriggers.last._1.cache
          // For the triggers whose firing conditions are satisfied, proceed to execute them
          val triggersToFire = checkedTriggers.filter(_._2.isDefined).map(_._2.get)
          /*
           * Execute the futures via left folding to make sure that each trigger execution follows the previous one. We
           * want to propagate the aggregated state from prior executions to make sure we use the cache as it gets updated
           * over the executions.
           */
          val initialState: Future[CacheData[Option[ProcessRequest]]] = Future.successful(CacheData(latestCacheState, None))
          triggersToFire.foldLeft(initialState) { (tasksUpToNow, trigger) =>
            tasksUpToNow.flatMap { previousState =>
              prepareTriggerInput(trigger, previousState.cache, Some(callbacks)).flatMap { inputResult =>
                fireTrigger(trigger.trigger, inputResult.cache, callbacks, inputResult.data.get).map { _ =>
                  inputResult
                }
              }
            }
          }
        }.map(_ => ())
      } else {
        Future.successful(())
      }
    }.recover {
      case e: Exception =>
        logger.error("Trigger execution raised an unexpected error", e)
    }
  }

  private def checkTriggerFireCondition(trigger: Trigger, dataCache: Map[String, Any], callbacks: Option[TriggerCallbacks]): Future[CacheData[Boolean]] = {
    if (trigger.fireExpressions.exists(_.nonEmpty)) {
      // Execute expressions in sequence and return as soon as one returns 'false'
      trigger.fireExpressions.get.foldLeft(Future.successful(CacheData(dataCache, true))) { (checksUpToNow, expression) =>
        checksUpToNow.flatMap { proceedToFire =>
          if (proceedToFire.data) {
            TriggerFireExpressionType.apply(expression.expressionType) match {
              case TriggerFireExpressionType.TestSuiteIdentifier =>
                getTestSessionData(dataCache, callbacks).map { lookup =>
                  CacheData(lookup.cache, checkFireExpression(expression, lookup.data.map(_.testSuiteIdentifier)))
                }
              case TriggerFireExpressionType.TestCaseIdentifier =>
                getTestSessionData(dataCache, callbacks).map { lookup =>
                  CacheData(lookup.cache, checkFireExpression(expression, lookup.data.map(_.testCaseIdentifier)))
                }
              case TriggerFireExpressionType.ActorIdentifier =>
                getActorData(dataCache, callbacks).map { lookup =>
                  CacheData(lookup.cache, checkFireExpression(expression, lookup.data.map(_.identifier)))
                }
              case TriggerFireExpressionType.SpecificationName =>
                getSpecificationData(dataCache, callbacks).map { lookup =>
                  CacheData(lookup.cache, checkFireExpression(expression, lookup.data.map(_.fullName)))
                }
              case TriggerFireExpressionType.SystemName =>
                getSystemData(dataCache, callbacks).map { lookup =>
                  CacheData(lookup.cache, checkFireExpression(expression, lookup.data.map(_.fullName)))
                }
              case TriggerFireExpressionType.OrganisationName =>
                getOrganisationData(dataCache, callbacks).map { lookup =>
                  CacheData(lookup.cache, checkFireExpression(expression, lookup.data.map(_.fullName)))
                }
              case _ =>
                // No action
                Future.successful(CacheData(proceedToFire.cache, true))
            }
          } else {
            Future.successful(CacheData(proceedToFire.cache, false))
          }
        }
      }
    } else {
      Future.successful(CacheData(dataCache, true))
    }
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
    for {
      parameterMap <- PersistenceSchema.organisationParameters
        .filter(_.community === community)
        .map(x => (x.id, x.testKey, x.kind))
        .result
        .map { params =>
          val parameterMap = mutable.Map[String, (Long, String)]()
          params.foreach { param =>
            parameterMap += (param._2 -> (param._1, param._3))
          }
          parameterMap.toMap
        }
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
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
    } yield ()
  }

  private def saveAsSystemPropertyValues(community: Long, system: Long, items: Iterable[AnyContent], onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      parameterMap <- PersistenceSchema.systemParameters
        .filter(_.community === community)
        .map(x => (x.id, x.testKey, x.kind))
        .result
        .map { params =>
          val parameterMap = mutable.Map[String, (Long, String)]()
          params.foreach { param =>
            parameterMap += (param._2 -> (param._1, param._3))
          }
          parameterMap.toMap
        }
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
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
    } yield ()
  }

  private def saveAsStatementPropertyValues(system: Long, actor: Long, items: Iterable[AnyContent], onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      parameterMap <- PersistenceSchema.parameters
        .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
        .filter(_._2.actor === actor)
        .map(x => (x._1.id, x._1.testKey, x._1.kind, x._1.endpoint))
        .result
        .map { params =>
          val parameterMap = mutable.Map[String, (Long, String, Long)]()
          params.foreach { param =>
            parameterMap += (param._2 -> (param._1, param._3, param._4))
          }
          parameterMap.toMap
        }
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
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
    } yield ()
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
        if (result.success) {
          JsonUtil.parseJsProcessResponse(Json.parse(result.errorMessages.flatMap(_.headOption).filter(StringUtils.isNotEmpty(_)).getOrElse("{}")))
        } else {
          throw new IllegalStateException(result.errorMessages.flatMap(_.headOption).getOrElse("Unexpected error"))
        }
      }
    }
  }

  private def fireTrigger(trigger: Triggers, dataCache: Map[String, Any], callbacks: TriggerCallbacks, request: ProcessRequest): Future[Unit] = {
    callTriggerService(trigger, request).flatMap { response =>
      val parsedResponse = parseResponse(response.getOutput.asScala)
      for {
        // Organisation ID
        lookup <- {
          if (parsedResponse.organisationProperties.nonEmpty) {
            TriggerCallbacks.organisationId(dataCache, Some(callbacks))
          } else {
            Future.successful(CacheData(dataCache, Option.empty[Long]))
          }
        }
        dataCache <- Future.successful(lookup.cache)
        organisationId <- Future.successful(lookup.data)
        // System ID
        lookup <- {
          if (parsedResponse.systemProperties.nonEmpty || parsedResponse.statementProperties.nonEmpty) {
            TriggerCallbacks.systemId(dataCache, Some(callbacks))
          } else {
            Future.successful(CacheData(dataCache, Option.empty[Long]))
          }
        }
        dataCache <- Future.successful(lookup.cache)
        systemId <- Future.successful(lookup.data)
        // Actor ID
        lookup <- {
          if (parsedResponse.statementProperties.nonEmpty) {
            TriggerCallbacks.actorId(dataCache, Some(callbacks))
          } else {
            Future.successful(CacheData(dataCache, Option.empty[Long]))
          }
        }
        actorId <- Future.successful(lookup.data)
        // Proceed.
        _ <- {
          val onSuccessCalls = mutable.ListBuffer[() => _]()
          val dbActions = ListBuffer[DBIO[_]]()
          if (organisationId.isDefined) {
            parsedResponse.organisationProperties.foreach { output =>
              dbActions += saveAsOrganisationPropertyValues(trigger.community, organisationId.get, output.getItem.asScala, onSuccessCalls)
            }
          }
          if (systemId.isDefined) {
            parsedResponse.systemProperties.foreach { output =>
              dbActions += saveAsSystemPropertyValues(trigger.community, systemId.get, output.getItem.asScala, onSuccessCalls)
            }
            if (actorId.isDefined) {
              parsedResponse.statementProperties.foreach { output =>
                dbActions += saveAsStatementPropertyValues(systemId.get, actorId.get, output.getItem.asScala, onSuccessCalls)
              }
            }
          }
          if (trigger.latestResultOk.isEmpty || !trigger.latestResultOk.get) {
            // Update latest result if this is none is recorded or if the previous one was a failure.
            dbActions += recordTriggerResultInternal(trigger.id, success = true, None)
          }
          DB.run(dbActionFinalisation(Some(onSuccessCalls), None, toDBIO(dbActions)).transactionally)
        }
      } yield ()
    }.recoverWith {
      case error: Exception =>
        val details = extractFailureDetails(error)
        logger.warn("Trigger call resulted in error [community: "+trigger.community+"][type: "+trigger.eventType+"][url: "+trigger.url+"]: " + details.mkString(" | "))
        recordTriggerFailure(trigger.id, Some(JsonUtil.jsTextArray(details).toString()))
    }
  }

  private def recordTriggerFailure(trigger: Long, message: Option[String]): Future[Unit] = {
    DB.run(recordTriggerResultInternal(trigger, success = false, message).transactionally).map(_ => ())
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