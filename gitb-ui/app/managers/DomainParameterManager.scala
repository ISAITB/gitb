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

import com.gitb.PropertyConstants
import com.gitb.remote.messaging.RemoteMessagingModuleClient
import com.gitb.remote.processing.RemoteProcessingModuleClient
import com.gitb.remote.validation.RemoteValidationModuleClient
import com.gitb.utils.XMLUtils
import exceptions.{AutomationApiException, ErrorCodes}
import jakarta.xml.bind.JAXBElement
import managers.triggers.TriggerHelper
import models.Enums.{TestServiceAuthTokenPasswordType, TestServiceType, TriggerDataType}
import models.automation.DomainParameterInfo
import models._
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{MimeUtil, RepositoryUtils}

import java.io.{ByteArrayOutputStream, File}
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Properties
import javax.inject.{Inject, Singleton}
import javax.xml.namespace.QName
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DomainParameterManager @Inject()(repositoryUtils: RepositoryUtils,
                                       triggerHelper: TriggerHelper,
                                       automationApiHelper: AutomationApiHelper,
                                       dbConfigProvider: DatabaseConfigProvider)
                                      (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def createDomainParameterInternal(parameter: DomainParameter, fileToStore: Option[File], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Long] = {
    for {
      id <- PersistenceSchema.domainParameters.returning(PersistenceSchema.domainParameters.map(_.id)) += parameter
      _ <- {
        if (fileToStore.isDefined) {
          onSuccessCalls += (() => repositoryUtils.setDomainParameterFile(parameter.domain, id, fileToStore.get))
        }
        DBIO.successful(())
      }
    } yield id
  }

  def createDomainParameter(parameter: DomainParameter, fileToStore: Option[File]): Future[Long] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = createDomainParameterInternal(parameter, fileToStore, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def updateDomainParameter(domainId: Long, parameterId: Long, name: String, description: Option[String], kind: String, value: Option[String], inTests: Boolean, contentType: Option[String], fileToStore: Option[File]): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = updateDomainParameterInternal(domainId, parameterId, name, description, kind, value, inTests, isTestService = false, contentType, fileToStore, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
  }

  private def checkIfDomainParameterIsTestService(parameterId: Long): DBIO[Boolean] = {
    PersistenceSchema.domainParameters
      .filter(_.id === parameterId)
      .map(_.isTestService)
      .result
      .headOption
      .map { result =>
        result.getOrElse(false)
      }
  }

  def updateDomainParameterInternal(domainId: Long, parameterId: Long, name: String, description: Option[String], kind: String, value: Option[String], inTests: Boolean, isTestService: Boolean, contentType: Option[String], fileToStore: Option[File], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      // Check to see if this parameter is linked to a test service.
      existingParameterIsTestService <- checkIfDomainParameterIsTestService(parameterId)
      // In case the existing parameter was a test service and is not anymore we need to delete the linked test service.
      _ <- {
        if (existingParameterIsTestService && !isTestService) {
          deleteTestServiceByParameterIdInternal(parameterId)
        } else {
          DBIO.successful(())
        }
      }
      // Update the parameter.
      _ <- {
        if (kind == "BINARY") {
          if (fileToStore.isDefined) {
            onSuccessCalls += (() => repositoryUtils.setDomainParameterFile(domainId, parameterId, fileToStore.get))
            PersistenceSchema.domainParameters.filter(_.id === parameterId)
              .map(x => (x.name, x.desc, x.kind, x.inTests, x.value, x.contentType))
              .update((name, description, kind, inTests, value, contentType))
          } else {
            PersistenceSchema.domainParameters.filter(_.id === parameterId)
              .map(x => (x.name, x.desc, x.kind, x.inTests, x.value))
              .update((name, description, kind, inTests, value))
          }
        } else {
          onSuccessCalls += (() => repositoryUtils.deleteDomainParameterFile(domainId, parameterId))
          if (kind == "SIMPLE" || (kind == "HIDDEN" && value.isDefined)) {
            PersistenceSchema.domainParameters.filter(_.id === parameterId)
              .map(x => (x.name, x.desc, x.kind, x.inTests, x.value, x.contentType, x.isTestService))
              .update((name, description, kind, inTests, value, None, isTestService))
          } else { // HIDDEN no value
            PersistenceSchema.domainParameters.filter(_.id === parameterId)
              .map(x => (x.name, x.desc, x.kind, x.inTests, x.contentType))
              .update((name, description, kind, inTests, None))
          }
        }
      }
    } yield ()
  }

  def deleteDomainParameterWrapper(domainId: Long, domainParameter: Long): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteDomainParameter(domainId, domainParameter, checkToDeleteLinkedTestService = true, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
  }

  def deleteDomainParameter(domainId: Long, domainParameter: Long, checkToDeleteLinkedTestService: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    onSuccessCalls += (() => repositoryUtils.deleteDomainParameterFile(domainId, domainParameter))
    for {
      _ <- {
        if (checkToDeleteLinkedTestService) {
          for {
            existingParameterIsTestService <- checkIfDomainParameterIsTestService(domainParameter)
            _ <- {
              if (existingParameterIsTestService) {
                deleteTestServiceByParameterIdInternal(domainParameter)
              } else {
                DBIO.successful(())
              }
            }
          } yield ()
        } else {
          DBIO.successful(())
        }
      }
      _ <- triggerHelper.deleteTriggerDataByDataType(domainParameter, TriggerDataType.DomainParameter)
      _ <- PersistenceSchema.domainParameters.filter(_.id === domainParameter).delete
    } yield ()
  }

  def getDomainParameter(domainParameterId: Long): Future[DomainParameter] = {
    DB.run(PersistenceSchema.domainParameters.filter(_.id === domainParameterId).result.head)
  }

  def getDomainParametersByCommunityIdInternal(communityId: Long, onlySimple: Boolean, loadValues: Boolean): DBIO[Seq[DomainParameter]] = {
    for {
      domainId <- PersistenceSchema.communities.filter(_.id === communityId).map(x => x.domain).result.head
      domainParameters <- {
        if (domainId.isDefined) {
          val query = PersistenceSchema.domainParameters
            .filter(_.domain === domainId.get)
            .filterIf(onlySimple)(_.kind === "SIMPLE")
          if (loadValues) {
            query.map(x => (x.id, x.name, x.kind, x.desc, x.value))
              .sortBy(_._2.asc)
              .result
              .map(_.toList.map(x => DomainParameter(x._1, x._2, x._4, x._3, x._5, inTests = false, None, isTestService = false, domainId.get)))
          } else {
            query.map(x => (x.id, x.name, x.kind, x.desc))
              .sortBy(_._2.asc)
              .result
              .map(_.toList.map(x => DomainParameter(x._1, x._2, x._4, x._3, None, inTests = false, None, isTestService = false, domainId.get)))

          }
        } else {
          DBIO.successful(List[DomainParameter]())
        }
      }
    } yield domainParameters
  }

  def getDomainParametersByCommunityId(communityId: Long, onlySimple: Boolean, loadValues: Boolean): Future[List[DomainParameter]] = {
    DB.run(getDomainParametersByCommunityIdInternal(communityId, onlySimple, loadValues)).map(_.toList)
  }

  def getDomainParameters(domainId: Long, loadValues: Boolean, onlyForTests: Option[Boolean], onlySimple: Boolean): Future[List[DomainParameter]] = {
    val query = PersistenceSchema.domainParameters.filter(_.domain === domainId)
      .filterOpt(onlyForTests)((table, filterValue) => table.inTests === filterValue)
      .filterIf(onlySimple)(_.kind === "SIMPLE")
      .sortBy(_.name.asc)
    if (loadValues) {
      DB.run(
        query
          .result
          .map(_.toList)
      )
    } else {
      DB.run(
        query
          .map(x => (x.id, x.name, x.desc, x.kind, x.inTests, x.contentType, x.isTestService))
          .result

      ).map { results =>
        results.map(x => DomainParameter(x._1, x._2, x._3, x._4, None, x._5, x._6, x._7, domainId)).toList
      }
    }
  }

  def getDomainParameters(domainId: Long): Future[List[DomainParameter]] = {
    getDomainParameters(domainId, loadValues = true, None, onlySimple = false)
  }

  def getDomainParameterByDomainAndName(domainId: Long, name: String, parameterIdToSkip: Option[Long] = None): Future[Option[DomainParameter]] = {
    DB.run(
      PersistenceSchema.domainParameters
        .filter(_.domain === domainId)
        .filter(_.name === name)
        .filterOpt(parameterIdToSkip)((q, id) => q.id =!= id)
        .result
        .headOption
    )
  }

  def deleteDomainParameters(domainId: Long, onSuccessCalls: ListBuffer[() => _]): DBIO[_] = {
    for {
      ids <- PersistenceSchema.domainParameters.filter(_.domain === domainId).map(_.id).result
      _ <- DBIO.seq(ids.map(id => deleteDomainParameter(domainId, id, checkToDeleteLinkedTestService = false, onSuccessCalls)): _*)
    } yield ()
  }

  def deleteDomainParameterThroughAutomationApi(communityApiKey: String, parameter: DomainParameterInfo): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      // Load domain ID.
      domainId <- automationApiHelper.getDomainIdByCommunityOrDomainApiKey(communityApiKey, parameter.domainApiKey)
      // Check to see if the property exists.
      domainParameter <- checkDomainParameterExistence(domainId, parameter.parameterInfo.key, expectedToExist = true)
      // Delete property.
      _ <- {
        deleteDomainParameter(domainId, domainParameter.get.id, checkToDeleteLinkedTestService = true, onSuccessCalls)
      }
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  private def checkDomainParameterExistence(domainId: Long, parameterName: String, expectedToExist: Boolean): DBIO[Option[DomainParameter]] = {
    for {
      parameter <- PersistenceSchema.domainParameters
        .filter(_.domain === domainId)
        .filter(_.name === parameterName)
        .result
        .headOption
      _ <- {
        if (parameter.isDefined && !expectedToExist) {
          throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "A property with the provided name already exists in the target domain")
        } else if (parameter.isEmpty && expectedToExist) {
          throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "No property with the provided name exists in the target domain")
        } else {
          DBIO.successful(())
        }
      }
    } yield parameter
  }

  def createDomainParameterThroughAutomationApi(communityApiKey: String, parameter: DomainParameterInfo): Future[Unit] = {
    if (parameter.parameterInfo.value.isEmpty) {
      throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "No value provided for property")
    }
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      // Load domain ID.
      domainId <- automationApiHelper.getDomainIdByCommunityOrDomainApiKey(communityApiKey, parameter.domainApiKey)
      // Check to see if the domain already defines a property with the same name.
      _ <- checkDomainParameterExistence(domainId, parameter.parameterInfo.key, expectedToExist = false)
      // Create property.
      _ <- {
        createDomainParameterInternal(DomainParameter(0L, parameter.parameterInfo.key,
          parameter.description.flatten, "SIMPLE",
          parameter.parameterInfo.value,
          parameter.inTests.getOrElse(true), None, isTestService = false,
          domainId
        ), None, onSuccessCalls)
      }
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def updateDomainParameterThroughAutomationApi(communityApiKey: String, update: DomainParameterInfo): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      // Load domain ID.
      domainId <- automationApiHelper.getDomainIdByCommunityOrDomainApiKey(communityApiKey, update.domainApiKey)
      // Check to see if the domain defines the property and load it.
      domainParameter <- checkDomainParameterExistence(domainId, update.parameterInfo.key, expectedToExist = true)
      // Update property.
      _ <- {
        if (domainParameter.get.kind != "SIMPLE") {
          throw AutomationApiException(ErrorCodes.API_INVALID_CONFIGURATION_PROPERTY_DEFINITION, "Only simple properties can be updated through the REST API")
        } else {
          val inTestsValue = domainParameter.get.isTestService || update.inTests.getOrElse(domainParameter.get.inTests)
          updateDomainParameterInternal(
            domainId,
            domainParameter.get.id,
            domainParameter.get.name,
            update.description.getOrElse(domainParameter.get.desc),
            domainParameter.get.kind,
            update.parameterInfo.value.orElse(domainParameter.get.value),
            inTestsValue,
            domainParameter.get.isTestService,
            None, None, onSuccessCalls
          )
        }
      }
    } yield ()
    DB.run(dbAction.transactionally)
  }

  def updateDomainParametersViaApiInternal(domainId: Option[Long], updates: List[DomainParameterInfo], warnings: ListBuffer[String]): DBIO[_] = {
    for {
      existingDomainProperties <- {
        if (updates.nonEmpty) {
          PersistenceSchema.domainParameters
            .join(PersistenceSchema.domains).on(_.domain === _.id)
            .filterOpt(domainId)((q, id) => q._2.id === id)
            .map(x => (x._1.name, x._1.id, x._1.kind, x._2.apiKey, x._2.id))
            .result
            .map { properties =>
              val keyMap = new mutable.HashMap[String, ListBuffer[(Long, String, String, Long)]]() // Key to (ID, type, domainApiKey, domainID)
              properties.foreach { property =>
                var propertyList = keyMap.get(property._1)
                if (propertyList.isEmpty) {
                  propertyList = Some(new ListBuffer[(Long, String, String, Long)])
                  keyMap.put(property._1, propertyList.get)
                }
                propertyList.get.append((property._2, property._3, property._4, property._5))
              }
              keyMap.map(x => (x._1, x._2.toList)).toMap
            }
        } else {
          DBIO.successful(Map.empty[String, List[(Long, String, String, Long)]])
        }
      }
      // Update domain properties
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        updates.foreach { propertyData =>
          if (existingDomainProperties.contains(propertyData.parameterInfo.key)) {
            val matchingProperties = existingDomainProperties(propertyData.parameterInfo.key)
              .filter(prop => propertyData.domainApiKey.isEmpty || propertyData.domainApiKey.get.equals(prop._3))
            if (matchingProperties.size == 1) {
              val matchingPropertyInfo = matchingProperties.head
              if (matchingPropertyInfo._2 == "SIMPLE") {
                // Update.
                if (propertyData.parameterInfo.value.isDefined) {
                  actions += PersistenceSchema.domainParameters.filter(_.id === matchingPropertyInfo._1)
                    .map(_.value)
                    .update(propertyData.parameterInfo.value)
                } else {
                  // No value.
                  warnings += "Ignoring update for domain property [%s] as no value was provided.".formatted(propertyData.parameterInfo.key)
                }
              } else {
                warnings += "Ignoring update for domain property [%s]. Only simple properties can be updated via the automation API.".formatted(propertyData.parameterInfo.key)
              }
            } else if (matchingProperties.size > 1) {
              warnings += "Ignoring update for domain property [%s]. Multiple properties were found matching the provided key from different domains. Please specify the domain API key to identify the specific property to update.".formatted(propertyData.parameterInfo.key)
            } else {
              // This case normally never occurs. A property that is not found is never recorded with an empty list.
              warnings += "Ignoring update for domain property [%s]. Property was not found.".formatted(propertyData.parameterInfo.key)
            }
          } else {
            warnings += "Ignoring update for domain property [%s]. Property was not found.".formatted(propertyData.parameterInfo.key)
          }
        }
        toDBIO(actions)
      }
    } yield ()
  }

  private def getDomainParameterIdForName(domainId: Long, name: String): DBIO[Option[Long]] = {
    PersistenceSchema.domainParameters
      .filter(_.domain === domainId)
      .filter(_.name === name)
      .map(_.id)
      .result
      .headOption
  }

  def createTestServiceWithParameter(serviceData: TestServiceWithParameter): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      // Check to see if the parameter name of the test service matches an existing parameter.
      matchingParameterId <- getDomainParameterIdForName(serviceData.parameter.domain, serviceData.parameter.name)
      parameterIdToUse <- {
        if (matchingParameterId.isEmpty) {
          // This is a new domain parameter.
          createDomainParameterInternal(serviceData.parameter.copy(kind = "SIMPLE", inTests = true), None, onSuccessCalls)
        } else {
          // The test service points to an existing domain parameter.
          updateDomainParameterAsTestService(serviceData.parameter.copy(id = matchingParameterId.get), onSuccessCalls).map(_ => matchingParameterId.get)
        }
      }
      _ <- createTestServiceInternal(serviceData.service.copy(parameter = parameterIdToUse))
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def createTestServiceInternal(service: TestService): DBIO[Long] = {
    val serviceToSave = if (service.authBasicPassword.isDefined || service.authTokenPassword.isDefined) {
      service.copy(
        authBasicPassword = service.authBasicPassword.map(MimeUtil.encryptString),
        authTokenPassword = service.authTokenPassword.map(MimeUtil.encryptString)
      )
    } else {
      service
    }
    PersistenceSchema.testServices.returning(PersistenceSchema.testServices.map(_.id)) += serviceToSave
  }

  private def updateDomainParameterAsTestService(parameter: DomainParameter, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Unit] = {
    updateDomainParameterInternal(parameter.domain, parameter.id, parameter.name, parameter.desc,
      "SIMPLE", parameter.value, inTests = true, isTestService = true, None, None, onSuccessCalls).map(_ => ())
  }

  def updateTestServiceWithParameter(serviceData: TestServiceWithParameter): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      // Determine the parameter ID to update. This may differ from the provided one in case the parameter name changed and matched another existing parameter.
      parameterIdToUpdate <- getDomainParameterIdForName(serviceData.parameter.domain, serviceData.parameter.name)
        .map(matchedId => matchedId.getOrElse(serviceData.service.parameter))
      // Update the domain parameter data.
      _ <- updateDomainParameterAsTestService(serviceData.parameter.copy(id = parameterIdToUpdate), onSuccessCalls)
      // If another parameter was matched by name, delete the previously linked one.
      _ <- {
        if (parameterIdToUpdate != serviceData.service.parameter) {
          deleteDomainParameter(serviceData.parameter.domain, serviceData.service.parameter, checkToDeleteLinkedTestService = false, onSuccessCalls)
        } else {
          DBIO.successful(())
        }
      }
      // Update the test service data
      _ <- updateTestServiceInternal(serviceData.service.copy(parameter = parameterIdToUpdate))
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteTestServiceWithParameter(serviceId: Long): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = for {
      // Determine the linked domain parameter to delete.
      parameterToDeleteIds <- PersistenceSchema.testServices
        .join(PersistenceSchema.domainParameters).on(_.parameter === _.id)
        .filter(_._1.id === serviceId)
        .map(x => (x._2.domain, x._2.id))
        .result
        .head
      // Delete the test service.
      _ <- deleteTestServiceInternal(serviceId)
      // Delete the linked domain parameter.
      _ <- deleteDomainParameter(parameterToDeleteIds._1, parameterToDeleteIds._2, checkToDeleteLinkedTestService = false, onSuccessCalls)
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteTestServiceInternal(serviceId: Long): DBIO[Unit] = {
    PersistenceSchema.testServices.filter(_.id === serviceId).delete.map(_ => ())
  }

  def deleteTestServiceByParameterIdInternal(parameterId: Long): DBIO[Unit] = {
    PersistenceSchema.testServices.filter(_.parameter === parameterId).delete.map(_ => ())
  }

  def deleteTestServices(domainId: Long): DBIO[_] = {
    for {
      // Locate the service IDs to delete.
      serviceIds <- PersistenceSchema.testServices
        .join(PersistenceSchema.domainParameters).on(_.parameter === _.id)
        .filter(_._2.domain === domainId)
        .map(_._1.id)
        .result
      // Delete the services.
      _ <- PersistenceSchema.testServices.filter(_.id inSet serviceIds).delete
    } yield ()
  }

  def updateTestServiceInternal(service: TestService): DBIO[Unit] = {
    for {
      // Update non-password information
      _ <- PersistenceSchema.testServices
        .filter(_.id === service.id)
        .map(x => (x.serviceType, x.apiType, x.identifier, x.version, x.authBasicUsername, x.authTokenUsername, x.authTokenPasswordType, x.parameter))
        .update((service.serviceType, service.apiType, service.identifier, service.version, service.authBasicUsername, service.authTokenUsername, service.authTokenPasswordType, service.parameter))
      // Handle basic auth password
      _ <- {
        if (service.authBasicUsername.isDefined) {
          if (service.authBasicPassword.isDefined) {
            // We are updating the password
            PersistenceSchema.testServices
              .filter(_.id === service.id)
              .map(_.authBasicPassword)
              .update(service.authBasicPassword.map(MimeUtil.encryptString))
          } else {
            // No password update
            DBIO.successful(())
          }
        } else {
          // No username - ensure the password is not set
          PersistenceSchema.testServices
            .filter(_.id === service.id)
            .map(_.authBasicPassword)
            .update(None)
        }
      }
      // Handle token auth password
      _ <- {
        if (service.authTokenUsername.isDefined) {
          if (service.authTokenPassword.isDefined) {
            // We are updating the password
            PersistenceSchema.testServices
              .filter(_.id === service.id)
              .map(_.authTokenPassword)
              .update(service.authTokenPassword.map(MimeUtil.encryptString))
          } else {
            // No password update
            DBIO.successful(())
          }
        } else {
          // No username - ensure the password is not set
          PersistenceSchema.testServices
            .filter(_.id === service.id)
            .map(_.authTokenPassword)
            .update(None)
        }
      }
    } yield ()
  }

  def getTestServices(domainId: Long): Future[List[TestService]] = {
    DB.run(getTestServicesInternal(domainId))
  }

  def getTestServicesInternal(domainId: Long): DBIO[List[TestService]] = {
    PersistenceSchema.testServices
      .join(PersistenceSchema.domainParameters).on(_.parameter === _.id)
      .filter(_._2.domain === domainId)
      .map(_._1)
      .result
      .map(_.toList)
  }

  def getTestServicesWithParameters(domainId: Long): Future[Seq[TestServiceWithParameter]] = {
    DB.run {
      PersistenceSchema.testServices
        .join(PersistenceSchema.domainParameters).on(_.parameter === _.id)
        .filter(_._2.domain === domainId)
        .result
        .map { results =>
          results.map { result =>
            TestServiceWithParameter(result._1, result._2)
          }
        }
    }
  }

  def getTestServiceWithParameter(serviceId: Long): Future[TestServiceWithParameter] = {
    DB.run {
      PersistenceSchema.testServices
        .join(PersistenceSchema.domainParameters).on(_.parameter === _.id)
        .filter(_._1.id === serviceId)
        .result
        .head
        .map { result =>
          TestServiceWithParameter(result._1, result._2)
        }
    }
  }

  def getAvailableDomainParametersForTestServiceConversion(domainId: Long): Future[Seq[DomainParameter]] = {
    DB.run {
      PersistenceSchema.domainParameters
        .filter(_.domain === domainId)
        .filter(_.inTests === true)
        .filter(_.isTestService =!= true)
        .filter(_.kind === "SIMPLE")
        .result
    }
  }

  private def loadTestServiceForTest(receivedServiceData: TestServiceWithParameter): Future[TestServiceWithParameter] = {
    if (receivedServiceData.service.id == 0L) {
      // New test service - use provided data as-is.
      Future.successful(receivedServiceData)
    } else {
      // Existing service - we may need to load passwords if these are not replaced in the received data.
      if ((receivedServiceData.service.authBasicUsername.isEmpty || receivedServiceData.service.authBasicPassword.nonEmpty)
          && (receivedServiceData.service.authTokenUsername.isEmpty || receivedServiceData.service.authTokenPassword.nonEmpty)) {
        // Authentication is either disabled (both basic and token), or in both cases the passwords are provided in the received data.
        Future.successful(receivedServiceData)
      } else {
        // We have active authentication but passwords were not received. We need to load the stored password information.
        DB.run {
          PersistenceSchema.testServices
            .filter(_.id === receivedServiceData.service.id)
            .map(x => (x.authBasicPassword, x.authTokenPassword))
            .result
            .head
        }.map { result =>
          receivedServiceData.copy(
            service = receivedServiceData.service.copy(
              authBasicPassword = result._1.map(MimeUtil.decryptString),
              authTokenPassword = result._2.map(MimeUtil.decryptString)
            )
          )
        }
      }
    }
  }

  private def prepareTestServiceCallProperties(testService: TestServiceWithParameter): Properties = {
    val props = new Properties
    testService.service.authBasicUsername.foreach(props.put(PropertyConstants.AUTH_BASIC_USERNAME, _))
    testService.service.authBasicPassword.foreach(props.put(PropertyConstants.AUTH_BASIC_PASSWORD, _))
    testService.service.authTokenUsername.foreach(props.put(PropertyConstants.AUTH_USERNAMETOKEN_USERNAME, _))
    testService.service.authTokenPassword.foreach(props.put(PropertyConstants.AUTH_USERNAMETOKEN_PASSWORD, _))
    testService.service.authTokenPasswordType.foreach { x =>
      val passwordType = TestServiceAuthTokenPasswordType.apply(x) match {
        case TestServiceAuthTokenPasswordType.Digest => PropertyConstants.AUTH_USERNAMETOKEN_PASSWORDTYPE_VALUE_DIGEST
        case TestServiceAuthTokenPasswordType.Text => PropertyConstants.AUTH_USERNAMETOKEN_PASSWORDTYPE_VALUE_TEXT
        case _ => throw new IllegalStateException("Unexpected test service auth token password type [%s]".formatted(x))
      }
      props.put(PropertyConstants.AUTH_USERNAMETOKEN_PASSWORDTYPE, passwordType)
    }
    props
  }

  def testTestService(testService: TestServiceWithParameter): Future[ServiceTestResult] = {
    for {
      serviceDataToUse <- loadTestServiceForTest(testService)
      result <- {
        Future {
          val serviceUrl = URI.create(serviceDataToUse.parameter.value.get).toURL
          val callProperties = prepareTestServiceCallProperties(serviceDataToUse)
          val response = TestServiceType.apply(serviceDataToUse.service.serviceType) match {
            case TestServiceType.ValidationService =>
              val client = new RemoteValidationModuleClient(serviceUrl, callProperties)
              val wrapper = new com.gitb.vs.GetModuleDefinitionResponse
              wrapper.setModule(client.getModuleDefinition)
              new JAXBElement[com.gitb.vs.GetModuleDefinitionResponse](new QName("http://www.gitb.com/vs/v1/", "GetModuleDefinitionResponse"), classOf[com.gitb.vs.GetModuleDefinitionResponse], wrapper)
            case TestServiceType.MessagingService =>
              val client = new RemoteMessagingModuleClient(serviceUrl, callProperties)
              val wrapper = new com.gitb.ms.GetModuleDefinitionResponse
              wrapper.setModule(client.getModuleDefinition)
              new JAXBElement[com.gitb.ms.GetModuleDefinitionResponse](new QName("http://www.gitb.com/ms/v1/", "GetModuleDefinitionResponse"), classOf[com.gitb.ms.GetModuleDefinitionResponse], wrapper)
            case TestServiceType.ProcessingService =>
              val client = new RemoteProcessingModuleClient(serviceUrl, callProperties)
              val wrapper = new com.gitb.ps.GetModuleDefinitionResponse
              wrapper.setModule(client.getModuleDefinition)
              new JAXBElement[com.gitb.ps.GetModuleDefinitionResponse](new QName("http://www.gitb.com/ps/v1/", "GetModuleDefinitionResponse"), classOf[com.gitb.ps.GetModuleDefinitionResponse], wrapper)
            case _ => throw new IllegalArgumentException("Unknown test service type %s".formatted(serviceDataToUse.service.serviceType))
          }
          val bos = new ByteArrayOutputStream()
          XMLUtils.marshalToStream(response, bos)
          ServiceTestResult(success = true, Some(List(new String(bos.toByteArray, StandardCharsets.UTF_8))), Constants.MimeTypeXML)
        }.recover {
          case exception: Exception =>
            ServiceTestResult(success = false, Some(extractFailureDetails(exception)), Constants.MimeTypeTextPlain)
        }
      }
    } yield result
  }

}
