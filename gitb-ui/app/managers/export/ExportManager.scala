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

package managers.export

import com.gitb.xml
import com.gitb.xml.export.{TriggerDataType, TriggerEventType, TriggerServiceType, _}
import managers._
import models.Enums.{SelfRegistrationRestriction, _}
import models.{TestCases, Actors => _, Endpoints => _, Systems => _, _}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.{FileUtils, IOUtils}
import org.slf4j.{Logger, LoggerFactory}
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider
import utils.{JsonUtil, MimeUtil, RepositoryUtils}

import java.io.File
import java.nio.file.Files
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExportManager @Inject() (repositoryUtils: RepositoryUtils,
                               systemConfigurationManager: SystemConfigurationManager,
                               domainManager: DomainManager,
                               domainParameterManager: DomainParameterManager,
                               communityResourceManager: CommunityResourceManager,
                               triggerManager: TriggerManager,
                               communityManager: CommunityManager,
                               testSuiteManager: TestSuiteManager,
                               landingPageManager: LandingPageManager,
                               legalNoticeManager: LegalNoticeManager,
                               errorTemplateManager: ErrorTemplateManager,
                               specificationManager: SpecificationManager,
                               reportManager: ReportManager,
                               dbConfigProvider: DatabaseConfigProvider)
                              (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[ExportManager])
  private final val DEFAULT_CONTENT_TYPE = "application/octet-stream"

  import dbConfig.profile.api._

  private def encryptText(value: Option[String], isAlreadyEncrypted: Boolean, encryptionKey: Option[String]): String = {
    if (value.isDefined) {
      if (encryptionKey.isEmpty) {
        throw new IllegalArgumentException("No encryption key was provided to encrypt sensitive properties")
      }
      var valueToProcess = value.get
      if (isAlreadyEncrypted) {
        valueToProcess = MimeUtil.decryptString(valueToProcess)
      }
      MimeUtil.encryptString(valueToProcess, encryptionKey.get.toCharArray)
    } else {
      null
    }
  }

  private def encryptText(value: Option[String], encryptionKey: Option[String]): String = {
    encryptText(value, isAlreadyEncrypted = false, encryptionKey)
  }

  private def toExportedTestServiceType(modelType: models.Enums.TestServiceType.TestServiceType): com.gitb.xml.export.TestServiceType = {
    modelType match {
      case models.Enums.TestServiceType.ProcessingService => com.gitb.xml.export.TestServiceType.PROCESSING
      case models.Enums.TestServiceType.ValidationService => com.gitb.xml.export.TestServiceType.VALIDATION
      case models.Enums.TestServiceType.MessagingService => com.gitb.xml.export.TestServiceType.MESSAGING
      case _ => throw new IllegalArgumentException("Unknown test service type %s".formatted(modelType.id))
    }
  }

  private def toExportedTestServiceApiType(modelType: models.Enums.TestServiceApiType.TestServiceApiType): com.gitb.xml.export.TestServiceApiType = {
    modelType match {
      case models.Enums.TestServiceApiType.SoapApi => com.gitb.xml.export.TestServiceApiType.SOAP
      case models.Enums.TestServiceApiType.RestApi => com.gitb.xml.export.TestServiceApiType.REST
      case _ => throw new IllegalArgumentException("Unknown test service API type %s".formatted(modelType.id))
    }
  }

  private def toExportedTestServiceAuthTokenPasswordType(modelType: models.Enums.TestServiceAuthTokenPasswordType.TestServiceAuthTokenPasswordType): com.gitb.xml.export.TestServiceAuthTokenPasswordType = {
    modelType match {
      case models.Enums.TestServiceAuthTokenPasswordType.Digest => com.gitb.xml.export.TestServiceAuthTokenPasswordType.DIGEST
      case models.Enums.TestServiceAuthTokenPasswordType.Text => com.gitb.xml.export.TestServiceAuthTokenPasswordType.TEXT
      case _ => throw new IllegalArgumentException("Unknown test service auth token password type %s".formatted(modelType.id))
    }
  }

  private def propertyTypeForExport(modelType: String): PropertyType = {
    if ("BINARY".equals(modelType)) {
      PropertyType.BINARY
    } else if ("HIDDEN".equals(modelType) || "SECRET".equals(modelType)) {
      PropertyType.SECRET
    } else if ("SIMPLE".equals(modelType)) {
      PropertyType.SIMPLE
    } else {
      throw new IllegalStateException("Unknown property type ["+modelType+"]")
    }
  }

  def exportDomain(domainId: Long, exportSettings: ExportSettings): Future[com.gitb.xml.export.Export] = {
    exportDomainInternal(domainId, None, exportSettings).map { data =>
      val exportData = new Export
      exportData.setDomains(new Domains)
      exportData.getDomains.getDomain.add(data.exportedDomain.get)
      exportData
    }
  }

  private[export] def loadSharedTestSuites(domainId: Long): Future[Seq[models.TestSuites]] = {
    DB.run(PersistenceSchema.testSuites
      .filter(_.domain === domainId)
      .filter(_.shared)
      .result
    )
  }

  private[export] def loadThemes(): Future[Seq[models.theme.Theme]] = {
    DB.run(
      PersistenceSchema.themes
        .filter(_.custom === true)
        .result
    )
  }

  private[export] def loadSpecificationTestSuiteMap(domainId: Long): Future[Map[Long, List[models.TestSuites]]] = {
    DB.run(
      PersistenceSchema.testSuites
        .join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
        .filter(_._1.domain === domainId)
        .map(x => (x._2.specId, x._1)) // Spec ID, TestSuite
        .result
    ).map { results =>
      val specificationTestSuiteMap = mutable.Map[Long, ListBuffer[models.TestSuites]]()
      results.foreach { x =>
        var testSuites = specificationTestSuiteMap.get(x._1)
        if (testSuites.isEmpty) {
          testSuites = Some(new ListBuffer[models.TestSuites])
          specificationTestSuiteMap += (x._1 -> testSuites.get)
        }
        testSuites.get += x._2
      }
      specificationTestSuiteMap.view.mapValues(_.toList).toMap
    }
  }

  private[export] def loadEndpointParameterMap(domainId: Long): Future[Map[Long, List[models.Parameters]]] = {
    DB.run(
      PersistenceSchema.parameters
        .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
        .join(PersistenceSchema.actors).on(_._2.actor === _.id)
        .filter(_._2.domain === domainId)
        .map(x => x._1._1)
        .sortBy(x=> (x.endpoint.asc, x.displayOrder.asc, x.testKey.asc))
        .result
    ).map { results =>
      val endpointParameterMap = mutable.Map[Long, ListBuffer[models.Parameters]]()
      results.foreach { x =>
        var parameters = endpointParameterMap.get(x.endpoint)
        if (parameters.isEmpty) {
          parameters = Some(new ListBuffer[models.Parameters])
          endpointParameterMap += (x.endpoint -> parameters.get)
        }
        parameters.get += x
      }
      endpointParameterMap.view.mapValues(_.toList).toMap
    }
  }

  private[export] def loadActorEndpointMap(domainId: Long): Future[Map[Long, List[models.Endpoints]]] = {
    DB.run(
      PersistenceSchema.endpoints
        .join(PersistenceSchema.actors).on(_.actor === _.id)
        .filter(_._2.domain === domainId)
        .map(x => x._1)
        .result
    ).map { results =>
      val actorEndpointMap = mutable.Map[Long, ListBuffer[models.Endpoints]]()
      results.foreach { x =>
        var endpoints = actorEndpointMap.get(x.actor)
        if (endpoints.isEmpty) {
          endpoints = Some(new ListBuffer[models.Endpoints]())
          actorEndpointMap += (x.actor -> endpoints.get)
        }
        endpoints.get += x
      }
      actorEndpointMap.view.mapValues(_.toList).toMap
    }
  }

  private[export] def loadOrganisations(communityId: Long): Future[Seq[models.Organizations]] = {
    DB.run(PersistenceSchema.organizations.filter(_.community === communityId).filter(_.adminOrganization === false).result)
  }

  private[export] def loadCommunityAdministrators(communityId: Long): Future[Seq[models.Users]] = {
    DB.run(
      PersistenceSchema.users
        .join(PersistenceSchema.organizations).on(_.organization === _.id)
        .filter(_._2.community === communityId)
        .filter(_._2.adminOrganization === true)
        .filter(_._1.role === UserRole.CommunityAdmin.id.toShort)
        .map(x => x._1)
        .result
    )
  }

  private[export] def loadSystemAdministrators(): Future[Seq[models.Users]] = {
    DB.run(PersistenceSchema.users
      .filter(_.role === UserRole.SystemAdmin.id.toShort)
      .result
    )
  }

  private[export] def loadOrganisationProperties(communityId: Long): Future[Seq[models.OrganisationParameters]] = {
    DB.run(PersistenceSchema.organisationParameters.filter(_.community === communityId).result)
  }

  private[export] def loadSystemProperties(communityId: Long): Future[Seq[models.SystemParameters]] = {
    DB.run(PersistenceSchema.systemParameters.filter(_.community === communityId).result)
  }

  private[export] def loadOrganisationUserMap(communityId: Long): Future[Map[Long, List[models.Users]]] ={
    DB.run(
      PersistenceSchema.users
        .join(PersistenceSchema.organizations).on(_.organization === _.id)
        .filter(_._2.adminOrganization === false)
        .filter(_._2.community === communityId)
        .map(x => x._1)
        .result
    ).map { results =>
      val organisationUserMap: mutable.Map[Long, ListBuffer[models.Users]] = mutable.Map()
      results.foreach { x =>
        var users = organisationUserMap.get(x.organization)
        if (users.isEmpty) {
          users = Some(new ListBuffer[models.Users])
          organisationUserMap += (x.organization -> users.get)
        }
        users.get += x
      }
      organisationUserMap.view.mapValues(_.toList).toMap
    }
  }

  private [export] def loadOrganisationParameterValueMap(communityId: Long): Future[Map[Long, List[models.OrganisationParameterValues]]] = {
    DB.run(
      PersistenceSchema.organisationParameterValues
        .join(PersistenceSchema.organizations).on(_.organisation === _.id)
        .filter(_._2.adminOrganization === false)
        .filter(_._2.community === communityId)
        .map(x => x._1)
        .result
    ).map { results =>
      val organisationParameterValueMap: mutable.Map[Long, ListBuffer[models.OrganisationParameterValues]] = mutable.Map()
      results.foreach { x =>
        var organisationParameters = organisationParameterValueMap.get(x.organisation)
        if (organisationParameters.isEmpty) {
          organisationParameters = Some(new ListBuffer[OrganisationParameterValues])
          organisationParameterValueMap += (x.organisation -> organisationParameters.get)
        }
        organisationParameters.get += x
      }
      organisationParameterValueMap.view.mapValues(_.toList).toMap
    }
  }

  private[export] def loadOrganisationSystemMap(communityId:Long): Future[Map[Long, List[models.Systems]]] = {
    DB.run(
      PersistenceSchema.systems
        .join(PersistenceSchema.organizations).on(_.owner === _.id)
        .filter(_._2.adminOrganization === false)
        .filter(_._2.community === communityId)
        .map(x => x._1)
        .result
    ).map { results =>
      val organisationSystemMap: mutable.Map[Long, ListBuffer[models.Systems]] = mutable.Map()
      results.foreach { x =>
        var systems = organisationSystemMap.get(x.owner)
        if (systems.isEmpty) {
          systems = Some(new ListBuffer[models.Systems]())
          organisationSystemMap += (x.owner -> systems.get)
        }
        systems.get += x
      }
      organisationSystemMap.view.mapValues(_.toList).toMap
    }
  }

  private[export] def loadSystemStatementsMap(communityId: Long, domainId: Option[Long]): Future[Map[Long, List[(models.Specifications, models.Actors)]]] = {
    var query = PersistenceSchema.systemImplementsActors
      .join(PersistenceSchema.systems).on(_.systemId === _.id)
      .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
      .join(PersistenceSchema.actors).on(_._1._1.actorId === _.id)
      .join(PersistenceSchema.specificationHasActors).on(_._2.id === _.actorId)
      .join(PersistenceSchema.specifications).on(_._2.specId === _.id)
      .filter(_._1._1._1._2.adminOrganization === false)
      .filter(_._1._1._1._2.community === communityId)
    if (domainId.isDefined) {
      query = query.filter(_._2.domain === domainId.get)
    }
    DB.run(query
        .map(x => (x._1._1._1._1._2.id, x._2, x._1._1._2))
      .result
    ).map { results =>
      val systemStatementsMap: mutable.Map[Long, ListBuffer[(models.Specifications, models.Actors)]] = mutable.Map()
      results.foreach { x =>
        var statements = systemStatementsMap.get(x._1) // systemId
        if (statements.isEmpty) {
          statements = Some(new ListBuffer[(models.Specifications, models.Actors)])
          systemStatementsMap += (x._1 -> statements.get)
        }
        statements.get += ((x._2, x._3)) // (specification, actor)
      }
      systemStatementsMap.view.mapValues(_.toList).toMap
    }
  }

  private[export] def loadSystemConfigurationsMap(community: models.Communities): Future[Map[String, List[models.Configs]]] = {
    var query = PersistenceSchema.configs
      .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
      .join(PersistenceSchema.actors).on(_._2.actor === _.id)
      .join(PersistenceSchema.systems).on(_._1._1.system === _.id)
      .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
      .filter(_._2.adminOrganization === false)
      .filter(_._2.community === community.id)
    if (community.domain.isDefined) {
      query = query.filter(_._1._1._2.domain === community.domain.get)
    }
    DB.run(query
      .map(x => (x._1._1._1._1, x._1._2.id, x._1._1._2.id, x._1._1._1._2.actor))
      .result
    ).map { results =>
      val systemConfigurationsMap: mutable.Map[String, ListBuffer[models.Configs]] = mutable.Map()
      results.foreach { x =>
        val key = s"${x._4}_${x._1.endpoint}_${x._2}_${x._1.parameter}" // [Actor ID]_[Endpoint ID]_[System ID]_[Endpoint parameter ID]
        var configs = systemConfigurationsMap.get(key)
        if (configs.isEmpty) {
          configs = Some(new ListBuffer[Configs])
          systemConfigurationsMap += (key -> configs.get)
        }
        configs.get += x._1
      }
      systemConfigurationsMap.view.mapValues(_.toList).toMap
    }
  }

  private[export] def loadSystemParameterValues(communityId:Long): Future[Map[Long, List[models.SystemParameterValues]]] = {
    DB.run(
      PersistenceSchema.systemParameterValues
        .join(PersistenceSchema.systems).on(_.system === _.id)
        .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
        .filter(_._2.adminOrganization === false)
        .filter(_._2.community === communityId)
        .map(x => x._1._1)
        .result
    ).map { results =>
      val systemParameterValueMap: mutable.Map[Long, ListBuffer[models.SystemParameterValues]] = mutable.Map()
      results.foreach { x =>
        var systemParameters = systemParameterValueMap.get(x.system)
        if (systemParameters.isEmpty) {
          systemParameters = Some(new ListBuffer[SystemParameterValues])
          systemParameterValueMap += (x.system -> systemParameters.get)
        }
        systemParameters.get += x
      }
      systemParameterValueMap.view.mapValues(_.toList).toMap
    }
  }

  private[export] def loadSpecificationActorMap(domainId: Long): Future[Map[Long, List[models.Actors]]] = {
    DB.run(
      PersistenceSchema.actors
        .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
        .filter(_._1.domain === domainId)
        .map(x => (x._1, x._2.specId))
        .result
    ).map { results =>
      val specificationActorMap = mutable.Map[Long, ListBuffer[models.Actors]]()
      results.foreach { result =>
        var actors = specificationActorMap.get(result._2)
        if (actors.isEmpty) {
          actors = Some(new ListBuffer[models.Actors]())
          specificationActorMap += (result._2 -> actors.get)
        }
        actors.get += result._1
      }
      specificationActorMap.view.mapValues(_.toList).toMap
    }
  }

  private def toId(uniqueNumber: Int): String = {
    "_" + uniqueNumber
  }

  private def exportDomainInternal(domainId: Long, exportData: Option[DomainExportData], exportSettings: ExportSettings): Future[DomainExportInfo] = {
    for {
      data <- exportData.map(Future.successful).getOrElse(loadDomainExportData(domainId, exportSettings))
      result <- {
        val sequence = new IdGenerator
        val exportedActorMap: mutable.Map[Long, com.gitb.xml.export.Actor] = mutable.Map()
        val exportedEndpointParameterMap: mutable.Map[Long, com.gitb.xml.export.EndpointParameter] = mutable.Map()
        val exportedDomainParameterMap: mutable.Map[Long, com.gitb.xml.export.DomainParameter] = mutable.Map()
        val exportedSharedTestSuiteMap: mutable.Map[Long, com.gitb.xml.export.TestSuite] = mutable.Map()
        val exportedSpecificationMap: mutable.Map[Long, com.gitb.xml.export.Specification] = mutable.Map()
        val exportedSpecificationGroupMap: mutable.Map[Long, com.gitb.xml.export.SpecificationGroup] = mutable.Map()
        // Domain.
        val domain = data.domain.get
        val exportedDomain = new com.gitb.xml.export.Domain
        exportedDomain.setId(toId(sequence.next()))
        exportedDomain.setShortName(domain.shortname)
        exportedDomain.setFullName(domain.fullname)
        exportedDomain.setDescription(domain.description.orNull)
        exportedDomain.setReportMetadata(domain.reportMetadata.orNull)
        exportedDomain.setApiKey(domain.apiKey)
        // Shared test suites.
        if (exportSettings.testSuites) {
          val testSuites = data.sharedTestSuites.get
          if (testSuites.nonEmpty) {
            val sharedTestSuites = new com.gitb.xml.export.TestSuites
            testSuites.foreach { testSuite =>
              val exportedTestSuite = toExportedTestSuite(sequence, testSuite, None, data.testSuiteTestCaseGroupMap.get.getOrElse(testSuite.id, List.empty), data.testSuiteTestCaseMap.get.getOrElse(testSuite.id, List.empty))
              sharedTestSuites.getTestSuite.add(exportedTestSuite)
              exportedSharedTestSuiteMap += (testSuite.id -> exportedTestSuite)
            }
            exportedDomain.setSharedTestSuites(sharedTestSuites)
          }
        }
        // Specifications.
        if (exportSettings.specifications) {
          // Groups.
          val groups = data.specificationGroups.get
          if (groups.nonEmpty) {
            exportedDomain.setSpecificationGroups(new com.gitb.xml.export.SpecificationGroups)
            groups.foreach { group =>
              val exportedGroup = new com.gitb.xml.export.SpecificationGroup
              exportedGroup.setId(toId(sequence.next()))
              exportedGroup.setShortName(group.shortname)
              exportedGroup.setFullName(group.fullname)
              exportedGroup.setDescription(group.description.orNull)
              exportedGroup.setReportMetadata(group.reportMetadata.orNull)
              exportedGroup.setDisplayOrder(group.displayOrder)
              exportedGroup.setApiKey(group.apiKey)
              exportedDomain.getSpecificationGroups.getGroup.add(exportedGroup)
              exportedSpecificationGroupMap += (group.id -> exportedGroup)
            }
          }
          // Specs.
          val specifications = data.specifications.get
          if (specifications.nonEmpty) {
            exportedDomain.setSpecifications(new com.gitb.xml.export.Specifications)
            specifications.foreach { specification =>
              val exportedSpecification = new com.gitb.xml.export.Specification
              exportedSpecification.setId(toId(sequence.next()))
              exportedSpecification.setShortName(specification.shortname)
              exportedSpecification.setFullName(specification.fullname)
              exportedSpecification.setDescription(specification.description.orNull)
              exportedSpecification.setReportMetadata(specification.reportMetadata.orNull)
              exportedSpecification.setApiKey(specification.apiKey)
              exportedSpecification.setHidden(specification.hidden)
              exportedSpecification.setDisplayOrder(specification.displayOrder)
              if (specification.group.nonEmpty) {
                exportedSpecification.setGroup(exportedSpecificationGroupMap(specification.group.get))
              }
              exportedSpecification.setBadges(badgesInfo(
                repositoryUtils.getConformanceBadge(specification.id, None, None, TestResultStatus.SUCCESS.toString, exactMatch = true, forReport = false),
                repositoryUtils.getConformanceBadge(specification.id, None, None, TestResultStatus.UNDEFINED.toString, exactMatch = true, forReport = false),
                repositoryUtils.getConformanceBadge(specification.id, None, None, TestResultStatus.FAILURE.toString, exactMatch = true, forReport = false)
              ).orNull
              )
              exportedSpecification.setBadgesForReport(badgesInfo(
                repositoryUtils.getConformanceBadge(specification.id, None, None, TestResultStatus.SUCCESS.toString, exactMatch = true, forReport = true),
                repositoryUtils.getConformanceBadge(specification.id, None, None, TestResultStatus.UNDEFINED.toString, exactMatch = true, forReport = true),
                repositoryUtils.getConformanceBadge(specification.id, None, None, TestResultStatus.FAILURE.toString, exactMatch = true, forReport = true)
              ).orNull
              )
              exportedSpecificationMap += (specification.id -> exportedSpecification)
              // Actors
              if (exportSettings.actors && data.specificationActorMap.get.contains(specification.id)) {
                exportedSpecification.setActors(new com.gitb.xml.export.Actors)
                data.specificationActorMap.get(specification.id).foreach { actor =>
                  val exportedActor = new com.gitb.xml.export.Actor
                  exportedActor.setId(toId(sequence.next()))
                  exportedActor.setSpecification(exportedSpecification)
                  exportedActor.setActorId(actor.actorId)
                  exportedActor.setName(actor.name)
                  exportedActor.setApiKey(actor.apiKey)
                  exportedActor.setDescription(actor.description.orNull)
                  exportedActor.setReportMetadata(actor.reportMetadata.orNull)
                  if (actor.default.isDefined) {
                    exportedActor.setDefault(actor.default.get)
                  } else {
                    exportedActor.setDefault(false)
                  }
                  exportedActor.setHidden(actor.hidden)
                  exportedActor.setBadges(badgesInfo(
                    repositoryUtils.getConformanceBadge(specification.id, Some(actor.id), None, TestResultStatus.SUCCESS.toString, exactMatch = true, forReport = false),
                    repositoryUtils.getConformanceBadge(specification.id, Some(actor.id), None, TestResultStatus.UNDEFINED.toString, exactMatch = true, forReport = false),
                    repositoryUtils.getConformanceBadge(specification.id, Some(actor.id), None, TestResultStatus.FAILURE.toString, exactMatch = true, forReport = false)
                  ).orNull
                  )
                  exportedActor.setBadges(badgesInfo(
                    repositoryUtils.getConformanceBadge(specification.id, Some(actor.id), None, TestResultStatus.SUCCESS.toString, exactMatch = true, forReport = true),
                    repositoryUtils.getConformanceBadge(specification.id, Some(actor.id), None, TestResultStatus.UNDEFINED.toString, exactMatch = true, forReport = true),
                    repositoryUtils.getConformanceBadge(specification.id, Some(actor.id), None, TestResultStatus.FAILURE.toString, exactMatch = true, forReport = true)
                  ).orNull
                  )
                  // Endpoints.
                  if (exportSettings.endpoints && data.actorEndpointMap.get.contains(actor.id)) {
                    exportedActor.setEndpoints(new com.gitb.xml.export.Endpoints)
                    data.actorEndpointMap.get(actor.id).foreach { endpoint =>
                      val exportedEndpoint = new com.gitb.xml.export.Endpoint
                      exportedEndpoint.setId(toId(sequence.next()))
                      exportedEndpoint.setName(endpoint.name)
                      exportedEndpoint.setDescription(endpoint.desc.orNull)
                      // Endpoint parameters.
                      if (data.endpointParameterMap.get.contains(endpoint.id)) {
                        exportedEndpoint.setParameters(new com.gitb.xml.export.EndpointParameters)
                        data.endpointParameterMap.get(endpoint.id).foreach { parameter =>
                          val exportedParameter = new com.gitb.xml.export.EndpointParameter
                          exportedParameter.setId(toId(sequence.next()))
                          exportedParameter.setEndpoint(exportedEndpoint)
                          exportedParameter.setLabel(parameter.name)
                          exportedParameter.setName(parameter.testKey)
                          exportedParameter.setDescription(parameter.desc.orNull)
                          exportedParameter.setType(propertyTypeForExport(parameter.kind))
                          exportedParameter.setEditable(!parameter.adminOnly)
                          exportedParameter.setInTests(!parameter.notForTests)
                          exportedParameter.setRequired(parameter.kind.equals("R"))
                          exportedParameter.setHidden(parameter.hidden)
                          exportedParameter.setAllowedValues(parameter.allowedValues.orNull)
                          exportedParameter.setDisplayOrder(parameter.displayOrder)
                          exportedParameter.setDependsOn(parameter.dependsOn.orNull)
                          exportedParameter.setDependsOnValue(parameter.dependsOnValue.orNull)
                          exportedParameter.setDefaultValue(parameter.defaultValue.orNull)
                          exportedEndpointParameterMap += (parameter.id -> exportedParameter)
                          exportedEndpoint.getParameters.getParameter.add(exportedParameter)
                        }
                      }
                      exportedActor.getEndpoints.getEndpoint.add(exportedEndpoint)
                    }
                  }
                  exportedSpecification.getActors.getActor.add(exportedActor)
                  exportedActorMap += (actor.id -> exportedActor)
                }
              }
              if (exportSettings.testSuites && data.specificationTestSuiteMap.get.contains(specification.id)) {
                val testSuites = data.specificationTestSuiteMap.get(specification.id)
                val sharedTestSuites = testSuites.filter(_.shared)
                if (sharedTestSuites.nonEmpty) {
                  // Reference a test suite linked at domain level.
                  sharedTestSuites.foreach { testSuite =>
                    val exportedTestSuite = exportedSharedTestSuiteMap(testSuite.id)
                    exportedSpecification.getSharedTestSuites.add(exportedTestSuite)
                  }
                }
                val specificTestSuites = testSuites.filter(!_.shared)
                if (specificTestSuites.nonEmpty) {
                  exportedSpecification.setTestSuites(new com.gitb.xml.export.TestSuites)
                  specificTestSuites.foreach { testSuite =>
                    // Test suite specific to the specification.
                    val exportedTestSuite = toExportedTestSuite(sequence, testSuite, Some(exportedSpecification), data.testSuiteTestCaseGroupMap.get.getOrElse(testSuite.id, List.empty), data.testSuiteTestCaseMap.get.getOrElse(testSuite.id, List.empty))
                    exportedSpecification.getTestSuites.getTestSuite.add(exportedTestSuite)
                  }
                }
              }
              exportedDomain.getSpecifications.getSpecification.add(exportedSpecification)
            }
          }
        }
        // Domain parameters.
        if (exportSettings.domainParameters) {
          val domainParameters = data.domainParameters.get
          if (domainParameters.nonEmpty) {
            exportedDomain.setParameters(new com.gitb.xml.export.DomainParameters)
            domainParameters.foreach { parameter =>
              val exportedParameter = new com.gitb.xml.export.DomainParameter
              exportedParameter.setId(toId(sequence.next()))
              exportedParameter.setName(parameter.name)
              exportedParameter.setDescription(parameter.desc.orNull)
              exportedParameter.setType(propertyTypeForExport(parameter.kind))
              exportedParameter.setInTests(parameter.inTests)
              exportedParameter.setTestService(exportSettings.testServices && parameter.isTestService)
              if (exportedParameter.getType == PropertyType.SECRET) {
                exportedParameter.setValue(encryptText(parameter.value, isAlreadyEncrypted = true, exportSettings.encryptionKey))
              } else if (exportedParameter.getType == PropertyType.BINARY) {
                exportedParameter.setValue(MimeUtil.getFileAsDataURL(repositoryUtils.getDomainParameterFile(domain.id, parameter.id), parameter.contentType.getOrElse(DEFAULT_CONTENT_TYPE)))
              } else {
                exportedParameter.setValue(parameter.value.orNull)
              }
              exportedDomain.getParameters.getParameter.add(exportedParameter)
              exportedDomainParameterMap += (parameter.id -> exportedParameter)
            }
          }
          // Test services
          if (exportSettings.testServices) {
            val testServices = data.testServices.get
            if (testServices.nonEmpty) {
              exportedDomain.setTestServices(new com.gitb.xml.export.TestServices)
              testServices.foreach { testService =>
                val exportedTestService = new com.gitb.xml.export.TestService
                exportedTestService.setId(toId(sequence.next()))
                exportedTestService.setIdentifier(testService.identifier.orNull)
                exportedTestService.setVersion(testService.version.orNull)
                exportedTestService.setServiceType(toExportedTestServiceType(models.Enums.TestServiceType.apply(testService.serviceType)))
                exportedTestService.setApiType(toExportedTestServiceApiType(models.Enums.TestServiceApiType.apply(testService.apiType)))
                exportedTestService.setAuthBasicUsername(testService.authBasicUsername.orNull)
                exportedTestService.setAuthBasicPassword(encryptText(testService.authBasicPassword, isAlreadyEncrypted = true, exportSettings.encryptionKey))
                exportedTestService.setAuthTokenUsername(testService.authTokenUsername.orNull)
                exportedTestService.setAuthTokenPassword(encryptText(testService.authTokenPassword, isAlreadyEncrypted = true, exportSettings.encryptionKey))
                exportedTestService.setAuthTokenPasswordType(testService.authTokenPasswordType.map(x => toExportedTestServiceAuthTokenPasswordType(models.Enums.TestServiceAuthTokenPasswordType.apply(x))).orNull)
                exportedTestService.setParameter(exportedDomainParameterMap(testService.parameter))
                exportedDomain.getTestServices.getService.add(exportedTestService)
              }
            }
          }
        }
        Future.successful {
          DomainExportInfo(
            sequence.current(),
            exportedActorMap.toMap,
            exportedEndpointParameterMap.toMap,
            Some(exportedDomain),
            data.actorEndpointMap.getOrElse(Map.empty),
            data.endpointParameterMap.getOrElse(Map.empty),
            exportedDomainParameterMap.toMap,
            exportedSpecificationGroupMap.toMap,
            exportedSpecificationMap.toMap
          )
        }
      }
    } yield result
  }

  private def toExportedTestSuite(sequence:IdGenerator, testSuite: models.TestSuites, specificationToSet: Option[com.gitb.xml.export.Specification], testCaseGroups: List[models.TestCaseGroup], testCases: List[models.TestCases]):com.gitb.xml.export.TestSuite  = {
    val exportedTestSuite = new com.gitb.xml.export.TestSuite
    exportedTestSuite.setId(toId(sequence.next()))
    exportedTestSuite.setIdentifier(testSuite.identifier)
    exportedTestSuite.setShortName(testSuite.shortname)
    exportedTestSuite.setFullName(testSuite.fullname)
    exportedTestSuite.setVersion(testSuite.version)
    exportedTestSuite.setAuthors(testSuite.authors.orNull)
    exportedTestSuite.setKeywords(testSuite.keywords.orNull)
    exportedTestSuite.setDescription(testSuite.description.orNull)
    exportedTestSuite.setDocumentation(testSuite.documentation.orNull)
    exportedTestSuite.setHasDocumentation(testSuite.hasDocumentation)
    exportedTestSuite.setModificationDate(testSuite.modificationDate.orNull)
    exportedTestSuite.setOriginalDate(testSuite.originalDate.orNull)
    exportedTestSuite.setSpecification(specificationToSet.orNull)
    exportedTestSuite.setSpecReference(testSuite.specReference.orNull)
    exportedTestSuite.setSpecDescription(testSuite.specReference.orNull)
    exportedTestSuite.setSpecReference(testSuite.specReference.orNull)
    // Zip the test suite's resources to a temporary archive and convert it to a BASE64 string.
    val testTestSuitePath = testSuiteManager.extractTestSuite(testSuite, None)
    try {
      exportedTestSuite.setData(
        Base64.encodeBase64String(IOUtils.toByteArray(Files.newInputStream(testTestSuitePath)))
      )
    } finally {
      FileUtils.deleteQuietly(testTestSuitePath.toFile)
    }
    // Test case groups.
    val exportedGroupMap = mutable.HashMap[Long, com.gitb.xml.export.TestCaseGroup]()
    if (testCaseGroups.nonEmpty) {
      exportedTestSuite.setTestCaseGroups(new TestCaseGroups)
      testCaseGroups.foreach { group =>
        val exportedGroup = new com.gitb.xml.export.TestCaseGroup
        exportedGroup.setId(toId(sequence.next()))
        exportedGroup.setIdentifier(group.identifier)
        exportedGroup.setName(group.name.orNull)
        exportedGroup.setDescription(group.description.orNull)
        exportedGroup.setTestSuite(exportedTestSuite)
        exportedTestSuite.getTestCaseGroups.getTestCaseGroup.add(exportedGroup)
        exportedGroupMap += (group.id -> exportedGroup)
      }
    }
    // Test cases.
    if (testCases.nonEmpty) {
      exportedTestSuite.setTestCases(new xml.export.TestCases)
      testCases.foreach { testCase =>
        val exportedTestCase = new com.gitb.xml.export.TestCase
        exportedTestCase.setId(toId(sequence.next()))
        exportedTestCase.setIdentifier(testCase.identifier)
        exportedTestCase.setShortName(testCase.shortname)
        exportedTestCase.setFullName(testCase.fullname)
        exportedTestCase.setVersion(testCase.version)
        exportedTestCase.setDescription(testCase.description.orNull)
        exportedTestCase.setAuthors(testCase.authors.orNull)
        exportedTestCase.setKeywords(testCase.keywords.orNull)
        exportedTestCase.setModificationDate(testCase.modificationDate.orNull)
        exportedTestCase.setOriginalDate(testCase.originalDate.orNull)
        exportedTestCase.setTestCaseType(testCase.testCaseType)
        exportedTestCase.setTestSuiteOrder(testCase.testSuiteOrder)
        exportedTestCase.setDocumentation(testCase.documentation.orNull)
        exportedTestCase.setHasDocumentation(testCase.hasDocumentation)
        exportedTestCase.setTargetActors(testCase.targetActors.orNull)
        exportedTestCase.setTestSuite(exportedTestSuite)
        exportedTestCase.setSpecification(specificationToSet.orNull)
        exportedTestCase.setOptional(testCase.isOptional)
        exportedTestCase.setDisabled(testCase.isDisabled)
        exportedTestCase.setTags(testCase.tags.orNull)
        exportedTestCase.setSpecReference(testCase.specReference.orNull)
        exportedTestCase.setSpecDescription(testCase.specReference.orNull)
        exportedTestCase.setSpecReference(testCase.specReference.orNull)
        exportedTestCase.setGroup(testCase.group.flatMap(exportedGroupMap.get).orNull)
        // Test case path - remove first part which represents the test suite
        val firstPathSeparatorIndex = testCase.path.indexOf('/')
        if (firstPathSeparatorIndex != -1) {
          exportedTestCase.setPath(testCase.path.substring(firstPathSeparatorIndex))
        } else {
          exportedTestCase.setPath(testCase.path)
        }
        exportedTestSuite.getTestCases.getTestCase.add(exportedTestCase)
      }
    }
    exportedTestSuite
  }

  def exportSystemSettings(exportSettings: ExportSettings): Future[com.gitb.xml.export.Export] = {
    exportSystemSettingsInternal(None, exportSettings, None, None).map { data =>
      val exportData = new Export
      exportData.setSettings(data.exportedSettings)
      exportData
    }
  }

  private def toExportedCommunityResource(resource: models.CommunityResources, sequence: IdGenerator) = {
    val exportedResource = new CommunityResource
    exportedResource.setId(toId(sequence.next()))
    exportedResource.setName(resource.name)
    exportedResource.setDescription(resource.description.orNull)
    exportedResource.setContent(MimeUtil.getFileAsDataURL(repositoryUtils.getCommunityResource(resource.community, resource.id), DEFAULT_CONTENT_TYPE))
    exportedResource
  }

  private def exportSystemSettingsInternal(exportData: Option[SystemSettingsExportData], exportSettings: ExportSettings, exportedUserMap: Option[Map[Long, String]], latestSequenceId: Option[Int]): Future[SystemSettingsExportInfo] = {
    for {
      data <- exportData.map(Future.successful).getOrElse(loadSystemSettingsExportData(exportSettings))
      result <- {
        val sequence = IdGenerator(latestSequenceId.getOrElse(0))
        val exportedSettings = new com.gitb.xml.export.Settings
        // Resources
        if (exportSettings.systemResources) {
          val resources = data.systemResources.get
          if (resources.nonEmpty) {
            exportedSettings.setResources(new com.gitb.xml.export.CommunityResources)
            resources.foreach { resource =>
              exportedSettings.getResources.getResource.add(toExportedCommunityResource(resource, sequence))
            }
          }
        }
        // Themes
        if (exportSettings.themes) {
          val themes = data.themes.get
          if (themes.nonEmpty) {
            val exportedThemes = new com.gitb.xml.export.Themes
            themes.foreach { theme =>
              val exportedTheme = new com.gitb.xml.export.Theme
              exportedTheme.setId(toId(sequence.next()))
              exportedTheme.setKey(theme.key)
              exportedTheme.setDescription(theme.description.orNull)
              exportedTheme.setActive(theme.active)
              exportedTheme.setSeparatorTitleColor(theme.separatorTitleColor)
              exportedTheme.setModalTitleColor(theme.modalTitleColor)
              exportedTheme.setTableTitleColor(theme.tableTitleColor)
              exportedTheme.setCardTitleColor(theme.cardTitleColor)
              exportedTheme.setPageTitleColor(theme.pageTitleColor)
              exportedTheme.setHeadingColor(theme.headingColor)
              exportedTheme.setTabLinkColor(theme.tabLinkColor)
              exportedTheme.setFooterTextColor(theme.footerTextColor)
              exportedTheme.setHeaderBackgroundColor(theme.headerBackgroundColor)
              exportedTheme.setHeaderBorderColor(theme.headerBorderColor)
              exportedTheme.setHeaderSeparatorColor(theme.headerSeparatorColor)
              exportedTheme.setHeaderLogoPath(theme.headerLogoPath)
              if (!systemConfigurationManager.isBuiltInThemeResource(theme.headerLogoPath)) {
                exportedTheme.setHeaderLogoContent(MimeUtil.getFileAsDataURL(repositoryUtils.getThemeResource(theme.id, theme.headerLogoPath).get, DEFAULT_CONTENT_TYPE))
              }
              exportedTheme.setFooterBackgroundColor(theme.footerBackgroundColor)
              exportedTheme.setFooterBorderColor(theme.footerBorderColor)
              exportedTheme.setFooterLogoPath(theme.footerLogoPath)
              if (!systemConfigurationManager.isBuiltInThemeResource(theme.footerLogoPath)) {
                exportedTheme.setFooterLogoContent(MimeUtil.getFileAsDataURL(repositoryUtils.getThemeResource(theme.id, theme.footerLogoPath).get, DEFAULT_CONTENT_TYPE))
              }
              exportedTheme.setFooterLogoDisplay(theme.footerLogoDisplay)
              exportedTheme.setFaviconPath(theme.faviconPath)
              if (!systemConfigurationManager.isBuiltInThemeResource(theme.faviconPath)) {
                exportedTheme.setFaviconContent(MimeUtil.getFileAsDataURL(repositoryUtils.getThemeResource(theme.id, theme.faviconPath).get, DEFAULT_CONTENT_TYPE))
              }
              exportedTheme.setPrimaryButtonColor(theme.primaryButtonColor)
              exportedTheme.setPrimaryButtonLabelColor(theme.primaryButtonLabelColor)
              exportedTheme.setPrimaryButtonHoverColor(theme.primaryButtonHoverColor)
              exportedTheme.setPrimaryButtonActiveColor(theme.primaryButtonActiveColor)
              exportedTheme.setSecondaryButtonColor(theme.secondaryButtonColor)
              exportedTheme.setSecondaryButtonLabelColor(theme.secondaryButtonLabelColor)
              exportedTheme.setSecondaryButtonHoverColor(theme.secondaryButtonHoverColor)
              exportedTheme.setSecondaryButtonActiveColor(theme.secondaryButtonActiveColor)
              exportedThemes.getTheme.add(exportedTheme)
            }
            exportedSettings.setThemes(exportedThemes)
          }
        }
        // Default landing pages
        if (exportSettings.defaultLandingPages) {
          val richContents = data.landingPages.get
          if (richContents.nonEmpty) {
            exportedSettings.setLandingPages(new com.gitb.xml.export.LandingPages)
            richContents.foreach { content =>
              val exportedContent = toExportedLandingPage(sequence.next(), content)
              exportedSettings.getLandingPages.getLandingPage.add(exportedContent)
            }
          }
        }
        // Default legal notices
        if (exportSettings.defaultLegalNotices) {
          val richContents = data.legalNotices.get
          if (richContents.nonEmpty) {
            exportedSettings.setLegalNotices(new com.gitb.xml.export.LegalNotices)
            richContents.foreach { content =>
              val exportedContent = toExportedLegalNotice(sequence.next(), content)
              exportedSettings.getLegalNotices.getLegalNotice.add(exportedContent)
            }
          }
        }
        // Default error templates
        if (exportSettings.defaultErrorTemplates) {
          val richContents = data.errorTemplates.get
          if (richContents.nonEmpty) {
            exportedSettings.setErrorTemplates(new com.gitb.xml.export.ErrorTemplates)
            richContents.foreach { content =>
              val exportedContent = toExportedErrorTemplate(sequence.next(), content)
              exportedSettings.getErrorTemplates.getErrorTemplate.add(exportedContent)
            }
          }
        }
        // System administrators
        if (exportSettings.systemAdministrators) {
          val administrators = data.systemAdministrators.get
          if (administrators.nonEmpty) {
            exportedSettings.setAdministrators(new SystemAdministrators)
            administrators.foreach { user =>
              val exportedAdmin = new SystemAdministrator
              populateExportedUser(sequence.next(), user, exportedAdmin, exportSettings.encryptionKey)
              exportedSettings.getAdministrators.getAdministrator.add(exportedAdmin)
            }
          }
        }
        // System configurations
        if (exportSettings.systemConfigurations) {
          val systemConfigurations = data.systemConfigurations.get
          if (systemConfigurations.nonEmpty) {
            exportedSettings.setSystemConfigurations(new com.gitb.xml.export.SystemConfigurations)
            systemConfigurations.foreach { config =>
              var valueToSet = config.config.parameter
              if (valueToSet.isDefined) {
                if (config.config.name == Constants.DemoAccount) {
                  // Set the export ID for the demo account user.
                  if (exportedUserMap.isDefined) {
                    valueToSet = exportedUserMap.get.get(config.config.parameter.get.toLong)
                  } else {
                    valueToSet = None
                  }
                } else if (config.config.name == Constants.EmailSettings) {
                  // Encrypt the SMTP password (if defined).
                  var emailSettings = JsonUtil.parseJsEmailSettings(valueToSet.get)
                  if (emailSettings.authPassword.isDefined) {
                    emailSettings = emailSettings.withPassword(encryptText(emailSettings.authPassword, isAlreadyEncrypted = true, exportSettings.encryptionKey))
                    valueToSet = Some(JsonUtil.jsEmailSettings(emailSettings, maskPassword = false).toString())
                  }
                }
                if (valueToSet.isDefined) {
                  val exportedConfig = new SystemConfiguration
                  exportedConfig.setId(toId(sequence.next()))
                  exportedConfig.setName(config.config.name)
                  exportedConfig.setValue(valueToSet.get)
                  exportedSettings.getSystemConfigurations.getConfig.add(exportedConfig)
                }
              }
            }
          }
        }
        // Return results.
        Future.successful {
          SystemSettingsExportInfo(sequence.current(), exportedSettings)
        }
      }
    } yield result
  }

  private def toExportedLandingPage(idToUse: Int, content: models.LandingPages): com.gitb.xml.export.LandingPage = {
    val exportedContent = new com.gitb.xml.export.LandingPage
    exportedContent.setId(toId(idToUse))
    exportedContent.setName(content.name)
    exportedContent.setDescription(content.description.orNull)
    exportedContent.setContent(content.content)
    exportedContent.setDefault(content.default)
    exportedContent
  }

  private def toExportedLegalNotice(idToUse: Int, content: models.LegalNotices): com.gitb.xml.export.LegalNotice = {
    val exportedContent = new com.gitb.xml.export.LegalNotice
    exportedContent.setId(toId(idToUse))
    exportedContent.setName(content.name)
    exportedContent.setDescription(content.description.orNull)
    exportedContent.setContent(content.content)
    exportedContent.setDefault(content.default)
    exportedContent
  }

  private def toExportedErrorTemplate(idToUse: Int, content: models.ErrorTemplates): com.gitb.xml.export.ErrorTemplate = {
    val exportedContent = new com.gitb.xml.export.ErrorTemplate
    exportedContent.setId(toId(idToUse))
    exportedContent.setName(content.name)
    exportedContent.setDescription(content.description.orNull)
    exportedContent.setContent(content.content)
    exportedContent.setDefault(content.default)
    exportedContent
  }

  private def toExportedReportSetting(setting: models.CommunityReportSettings): com.gitb.xml.export.CommunityReportSetting = {
    val exportedSetting = new com.gitb.xml.export.CommunityReportSetting
    exportedSetting.setReportType(toExportedReportType(models.Enums.ReportType.apply(setting.reportType)))
    exportedSetting.setSignPdfs(setting.signPdfs)
    exportedSetting.setCustomPdfs(setting.customPdfs)
    exportedSetting.setCustomPdfsWithCustomXml(setting.customPdfsWithCustomXml)
    exportedSetting.setCustomPdfService(setting.customPdfService.orNull)
    exportedSetting
  }

  private def populateExportedUser(idToUse: Int, user: models.Users, exportedUser: com.gitb.xml.export.User, encryptionKey: Option[String]): Unit = {
    exportedUser.setId(toId(idToUse))
    exportedUser.setName(user.name)
    exportedUser.setEmail(user.email)
    exportedUser.setPassword(encryptText(Some(user.password), encryptionKey))
    exportedUser.setOnetimePassword(user.onetimePassword)
  }

  private def loadCommunityExportData(communityId: Long, exportSettings: ExportSettings): Future[(CommunityExportData, ExportSettings)] = {
    require(communityId != Constants.DefaultCommunityId, "The default community cannot be exported")
    for {
      community <- communityManager.getById(communityId)
      data <- {
        if (community.isEmpty) {
          logger.error("No community could be found for id ["+communityId+"]. Aborting export.")
          throw new IllegalStateException("The community requested for export could not be found.")
        }
        if (community.get.domain.isEmpty && exportSettings.domain) {
          logger.warn("Skipping domain-related information from community export as the requested community ["+communityId+"] is not linked to a domain.")
          exportSettings.domain = false
        }
        if (config.Configurations.AUTHENTICATION_SSO_ENABLED && (exportSettings.communityAdministrators || exportSettings.organisationUsers)) {
          logger.warn("Users can only be included when SSO is not enabled. Enforcing [admins, users] ["+exportSettings.communityAdministrators+", "+exportSettings.organisationUsers+"] to false")
          exportSettings.communityAdministrators = false
          exportSettings.organisationUsers = false
        }
        // Load all data
        loadIfApplicable(exportSettings.domain,
          () => loadDomainExportData(community.get.domain.get, exportSettings)
        ).zip(
          // Administrators
          loadIfApplicable(exportSettings.communityAdministrators,
            () => loadCommunityAdministrators(communityId)
          ).zip {
            // Conformance statement certificate
            loadIfApplicable(exportSettings.certificateSettings,
              () => communityManager.getConformanceCertificateSettingsWrapper(communityId, defaultIfMissing = false, None)
            ).zip(
              // Conformance overview certificate
              loadIfApplicable(exportSettings.certificateSettings,
                () => communityManager.getConformanceOverviewCertificateSettingsWrapper(communityId, defaultIfMissing = false, None, None, None)
              ).zip(
                // Signature settings
                loadIfApplicable(exportSettings.certificateSettings,
                  () => communityManager.getCommunityKeystore(communityId, decryptKeys = false)
                ).zip{
                  // Report settings
                  loadIfApplicable(exportSettings.certificateSettings,
                    () => reportManager.getAllReportSettings(communityId)
                  ).zip(
                    // Custom organisation properties
                    loadIfApplicable(exportSettings.customProperties,
                      () => loadOrganisationProperties(communityId)
                    ).zip(
                      // Custom system properties
                      loadIfApplicable(exportSettings.customProperties,
                        () => loadSystemProperties(communityId)
                      ).zip(
                        // Custom labels
                        loadIfApplicable(exportSettings.customLabels,
                          () => communityManager.getCommunityLabels(communityId)
                        ).zip(
                          // Landing pages
                          loadIfApplicable(exportSettings.landingPages,
                            () => landingPageManager.getLandingPagesByCommunity(communityId)
                          ).zip(
                            // Legal notices
                            loadIfApplicable(exportSettings.legalNotices,
                              () => legalNoticeManager.getLegalNoticesByCommunity(communityId)
                            ).zip(
                              // Error templates
                              loadIfApplicable(exportSettings.errorTemplates,
                                () => errorTemplateManager.getErrorTemplatesByCommunity(communityId)
                              ).zip(
                                // Triggers
                                loadIfApplicable(exportSettings.triggers,
                                  () => triggerManager.getTriggerAndDataByCommunityId(communityId)
                                ).zip(
                                  // Resources
                                  loadIfApplicable(exportSettings.resources,
                                    () => communityResourceManager.getCommunityResources(communityId)
                                  ).zip(
                                    // Organisations
                                    loadIfApplicable(exportSettings.organisations,
                                      () => loadOrganisations(communityId)
                                    ).zip(
                                      // Organisation users
                                      loadIfApplicable(exportSettings.organisations && exportSettings.organisationUsers,
                                        () => loadOrganisationUserMap(communityId)
                                      ).zip(
                                        // Organisation property values
                                        loadIfApplicable(exportSettings.organisations && exportSettings.customProperties && exportSettings.organisationPropertyValues,
                                          () => loadOrganisationParameterValueMap(communityId)
                                        ).zip(
                                          // Systems
                                          loadIfApplicable(exportSettings.organisations && exportSettings.systems,
                                            () => loadOrganisationSystemMap(communityId)
                                          ).zip(
                                            // System property values
                                            loadIfApplicable(exportSettings.organisations && exportSettings.systems && exportSettings.customProperties && exportSettings.systemPropertyValues,
                                              () => loadSystemParameterValues(communityId)
                                            ).zip(
                                              // Statements
                                              loadIfApplicable(exportSettings.organisations && exportSettings.systems && exportSettings.statements && exportSettings.domain,
                                                () => loadSystemStatementsMap(community.get.id, community.get.domain)
                                              ).zip(
                                                // Statement configurations
                                                loadIfApplicable(exportSettings.organisations && exportSettings.systems && exportSettings.statements && exportSettings.domain && exportSettings.statementConfigurations,
                                                  () => loadSystemConfigurationsMap(community.get)
                                                ).zip(
                                                  // System settings
                                                  loadIfApplicable(exportSettings.hasSystemSettings(),
                                                    () => loadSystemSettingsExportData(exportSettings)
                                                  )
                                                )
                                              )
                                            )
                                          )
                                        )
                                      )
                                    )
                                  )
                                )
                              )
                            )
                          )
                        )
                      )
                    )
                  )
                }
              )
            )
          }
        ).map { results =>
          (CommunityExportData(
            community = community,
            domainExportData = results._1,
            administrators = results._2._1,
            statementCertificateSettings = results._2._2._1,
            overviewCertificateSettings = results._2._2._2._1,
            keystore = results._2._2._2._2._1,
            reportSettings = results._2._2._2._2._2._1,
            organisationProperties = results._2._2._2._2._2._2._1,
            systemProperties = results._2._2._2._2._2._2._2._1,
            labels = results._2._2._2._2._2._2._2._2._1,
            landingPages = results._2._2._2._2._2._2._2._2._2._1,
            legalNotices = results._2._2._2._2._2._2._2._2._2._2._1,
            errorTemplates = results._2._2._2._2._2._2._2._2._2._2._2._1,
            triggers = results._2._2._2._2._2._2._2._2._2._2._2._2._1,
            resources = results._2._2._2._2._2._2._2._2._2._2._2._2._2._1,
            organisations = results._2._2._2._2._2._2._2._2._2._2._2._2._2._2._1,
            organisationUserMap = results._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._1,
            organisationParameterValueMap = results._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._1,
            organisationSystemMap = results._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._1,
            systemParameterValueMap = results._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._1,
            systemStatementsMap = results._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._1,
            systemConfigurationsMap = results._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._1,
            systemSettings = results._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2._2
          ), exportSettings)
        }
      }
    } yield data
  }

  private def loadDomainExportData(domainId: Long, exportSettings: ExportSettings): Future[DomainExportData] = {
    // Load export data with zipping to load in parallel
    // Domain
    domainManager.getDomainById(domainId).zip(
      // Shared test suites.
      loadIfApplicable(exportSettings.testSuites,
        () => loadSharedTestSuites(domainId)
      ).zip(
        // Specification groups.
        loadIfApplicable(exportSettings.specifications,
          () => specificationManager.getSpecificationGroups(domainId)
        ).zip(
          // Specifications
          loadIfApplicable(exportSettings.specifications,
            () => specificationManager.getSpecifications(domainId, withGroups = false)
          ).zip(
            // Specification actors
            loadIfApplicable(exportSettings.specifications && exportSettings.actors,
              () => loadSpecificationActorMap(domainId)
            ).zip(
              // Actor endpoints
              loadIfApplicable(exportSettings.specifications && exportSettings.actors && exportSettings.endpoints,
                () => loadActorEndpointMap(domainId)
              ).zip(
                // Endpoint parameters.
                loadIfApplicable(exportSettings.specifications && exportSettings.actors && exportSettings.endpoints,
                  () => loadEndpointParameterMap(domainId)
                ).zip(
                  // Test suites.
                  loadIfApplicable(exportSettings.specifications && exportSettings.testSuites,
                    () => loadActorTestSuiteMap(domainId)
                  ).zip(
                    // Test case groups.
                    loadIfApplicable(exportSettings.specifications && exportSettings.testSuites,
                      () => loadTestCaseGroupMap(domainId)
                    ).zip(
                      // Test suite test cases
                      loadIfApplicable(exportSettings.specifications && exportSettings.testSuites,
                        () => loadTestSuiteTestCaseMap(domainId)
                      ).zip(
                        // Specification test suites
                        loadIfApplicable(exportSettings.specifications && exportSettings.testSuites,
                          () => loadSpecificationTestSuiteMap(domainId)
                        ).zip(
                          // Domain parameters
                          loadIfApplicable(exportSettings.domainParameters,
                            () => domainParameterManager.getDomainParameters(domainId)
                          )
                        ).zip(
                          // Test services
                          loadIfApplicable(exportSettings.domainParameters && exportSettings.testServices,
                            () => domainParameterManager.getTestServices(domainId)
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    ).map { results =>
      DomainExportData(
        domain = Some(results._1),
        sharedTestSuites =          results._2._1,
        specificationGroups =       results._2._2._1,
        specifications =            results._2._2._2._1,
        specificationActorMap =     results._2._2._2._2._1,
        actorEndpointMap =          results._2._2._2._2._2._1,
        endpointParameterMap =      results._2._2._2._2._2._2._1,
        testSuiteActorMap =         results._2._2._2._2._2._2._2._1,
        testSuiteTestCaseGroupMap = results._2._2._2._2._2._2._2._2._1,
        testSuiteTestCaseMap =      results._2._2._2._2._2._2._2._2._2._1,
        specificationTestSuiteMap = results._2._2._2._2._2._2._2._2._2._2._1._1,
        domainParameters =          results._2._2._2._2._2._2._2._2._2._2._1._2,
        testServices =              results._2._2._2._2._2._2._2._2._2._2._2
      )
    }
  }

  private def loadSystemSettingsExportData(exportSettings: ExportSettings): Future[SystemSettingsExportData] = {
    // Load export data with zipping to load in parallel
    // Resources
    loadIfApplicable(exportSettings.systemResources,
      () => communityResourceManager.getCommunityResources(Constants.DefaultCommunityId)
    ).zip(
      // Themes
      loadIfApplicable(exportSettings.themes,
        () => loadThemes()
      ).zip(
        // Default landing pages
        loadIfApplicable(exportSettings.defaultLandingPages,
          () => landingPageManager.getLandingPagesByCommunity(Constants.DefaultCommunityId)
        ).zip(
          // Default legal notices
          loadIfApplicable(exportSettings.defaultLegalNotices,
            () => legalNoticeManager.getLegalNoticesByCommunity(Constants.DefaultCommunityId)
          ).zip(
            // Default error templates
            loadIfApplicable(exportSettings.defaultErrorTemplates,
              () => errorTemplateManager.getErrorTemplatesByCommunity(Constants.DefaultCommunityId)
            ).zip(
              // System administrators
              loadIfApplicable(exportSettings.systemAdministrators,
                () => loadSystemAdministrators()
              ).zip(
                // System configurations
                loadIfApplicable(exportSettings.systemConfigurations,
                  () => systemConfigurationManager.getEditableSystemConfigurationValues(onlyPersisted = true)
                )
              )
            )
          )
        )
      )
    ).map { results =>
      SystemSettingsExportData(
        systemResources = results._1,
        themes = results._2._1,
        landingPages = results._2._2._1,
        legalNotices = results._2._2._2._1,
        errorTemplates = results._2._2._2._2._1,
        systemAdministrators = results._2._2._2._2._2._1,
        systemConfigurations = results._2._2._2._2._2._2
      )
    }
  }

  private def loadTestSuiteTestCaseMap(domainId: Long): Future[Map[Long, List[models.TestCases]]] = {
    DB.run(
      PersistenceSchema.testCases
        .join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
        .join(PersistenceSchema.testSuites).on(_._2.testsuite === _.id)
        .filter(_._2.domain === domainId)
        .map(x => (x._1._2.testsuite, x._1._1))
        .result
    ).map { results =>
      val testSuiteTestCaseMap = mutable.Map[Long, ListBuffer[models.TestCases]]()
      results.foreach { x =>
        var testCases = testSuiteTestCaseMap.get(x._1)
        if (testCases.isEmpty) {
          testCases = Some(new ListBuffer[TestCases])
          testSuiteTestCaseMap += (x._1 -> testCases.get)
        }
        testCases.get += x._2
      }
      testSuiteTestCaseMap.view.mapValues(_.toList).toMap
    }
  }

  private def loadTestCaseGroupMap(domainId: Long): Future[Map[Long, List[models.TestCaseGroup]]] = {
    DB.run(
      PersistenceSchema.testCaseGroups
        .join(PersistenceSchema.testSuites).on(_.testSuite === _.id)
        .filter(_._2.domain === domainId)
        .map(x => (x._1.id, x._1))
        .result
    ).map { results =>
      val testSuiteTestCaseGroupMap = mutable.Map[Long, ListBuffer[models.TestCaseGroup]]()
      results.foreach { x =>
        var testCaseGroups = testSuiteTestCaseGroupMap.get(x._1)
        if (testCaseGroups.isEmpty) {
          testCaseGroups = Some(new ListBuffer[models.TestCaseGroup])
          testSuiteTestCaseGroupMap += (x._2.testSuite -> testCaseGroups.get)
        }
        testCaseGroups.get += x._2
      }
      testSuiteTestCaseGroupMap.view.mapValues(_.toList).toMap
    }
  }

  private def loadActorTestSuiteMap(domainId: Long): Future[Map[Long, List[Long]]] = {
    DB.run(
      PersistenceSchema.testSuites
        .join(PersistenceSchema.testSuiteHasActors).on(_.id === _.testsuite)
        .filter(_._1.domain === domainId)
        .map(x => x._2)
        .result
    ).map { results =>
      val testSuiteActorMap = mutable.Map[Long, ListBuffer[Long]]()
      results.foreach { x =>
        var actors = testSuiteActorMap.get(x._1) // Test suite ID
        if (actors.isEmpty) {
          actors = Some(new ListBuffer[Long])
          testSuiteActorMap += (x._1 -> actors.get)
        }
        actors.get += x._2 // Actor ID
      }
      testSuiteActorMap.view.mapValues(_.toList).toMap
    }
  }

  def exportCommunity(communityId: Long, exportSettings: ExportSettings): Future[com.gitb.xml.export.Export] = {
    for {
      dataResults <- loadCommunityExportData(communityId, exportSettings)
      domainExportInfo <- {
        if (dataResults._2.domain) {
          val domain = dataResults._1.domainExportData.get.domain.get
          exportDomainInternal(domain.id, dataResults._1.domainExportData, exportSettings.withoutSystemSettings()).map(Some(_))
        } else {
          Future.successful(None)
        }
      }
      result <- {
        val data = dataResults._1
        val community = data.community
        val exportSettings = dataResults._2
        // Create export
        val exportData = new Export
        val communityData = new com.gitb.xml.export.Community
        val exportedUserMap = new mutable.HashMap[Long, String]() // Map of DB user ID to XML user ID
        val idSequence = IdGenerator()
        if (exportSettings.domain) {
          // Add domain data as part of the community export.
          idSequence.reset(domainExportInfo.get.latestSequenceId)
          if (domainExportInfo.exists(_.exportedDomain.isDefined)) {
            exportData.setDomains(new Domains)
            exportData.getDomains.getDomain.add(domainExportInfo.get.exportedDomain.get)
            communityData.setDomain(domainExportInfo.get.exportedDomain.get)
          }
        }
        exportData.setCommunities(new com.gitb.xml.export.Communities)
        exportData.getCommunities.getCommunity.add(communityData)
        // Community basic info.
        communityData.setId(toId(idSequence.next()))
        communityData.setShortName(community.get.shortname)
        communityData.setFullName(community.get.fullname)
        communityData.setSupportEmail(community.get.supportEmail.orNull)
        communityData.setDescription(community.get.description.orNull)
        communityData.setApiKey(community.get.apiKey)
        communityData.setAllowCertificateDownload(community.get.allowCertificateDownload)
        communityData.setAllowStatementManagement(community.get.allowStatementManagement)
        communityData.setAllowSystemManagement(community.get.allowSystemManagement)
        communityData.setAllowPostTestOrganisationUpdates(community.get.allowPostTestOrganisationUpdates)
        communityData.setAllowPostTestSystemUpdates(community.get.allowPostTestSystemUpdates)
        communityData.setAllowPostTestStatementUpdates(community.get.allowPostTestStatementUpdates)
        communityData.setAllowAutomationApi(community.get.allowAutomationApi)
        communityData.setAllowCommunityView(community.get.allowCommunityView)
        communityData.setInteractionNotification(community.get.interactionNotification)
        // Self registration information.
        communityData.setSelfRegistrationSettings(new SelfRegistrationSettings)
        SelfRegistrationType.apply(community.get.selfRegType) match {
          case SelfRegistrationType.NotSupported => communityData.getSelfRegistrationSettings.setMethod(SelfRegistrationMethod.NOT_SUPPORTED)
          case SelfRegistrationType.PublicListing => communityData.getSelfRegistrationSettings.setMethod(SelfRegistrationMethod.PUBLIC)
          case SelfRegistrationType.PublicListingWithToken => communityData.getSelfRegistrationSettings.setMethod(SelfRegistrationMethod.PUBLIC_WITH_TOKEN)
          case SelfRegistrationType.Token => communityData.getSelfRegistrationSettings.setMethod(SelfRegistrationMethod.TOKEN)
        }
        communityData.getSelfRegistrationSettings.setNotifications(community.get.selfRegNotification)
        communityData.getSelfRegistrationSettings.setToken(community.get.selfRegToken.orNull)
        communityData.getSelfRegistrationSettings.setTokenHelpText(community.get.selfRegTokenHelpText.orNull)
        SelfRegistrationRestriction.apply(community.get.selfRegRestriction) match {
          case SelfRegistrationRestriction.NoRestriction => communityData.getSelfRegistrationSettings.setRestriction(com.gitb.xml.export.SelfRegistrationRestriction.NO_RESTRICTION)
          case SelfRegistrationRestriction.UserEmail => communityData.getSelfRegistrationSettings.setRestriction(com.gitb.xml.export.SelfRegistrationRestriction.USER_EMAIL)
          case SelfRegistrationRestriction.UserEmailDomain => communityData.getSelfRegistrationSettings.setRestriction(com.gitb.xml.export.SelfRegistrationRestriction.USER_EMAIL_DOMAIN)
        }
        communityData.getSelfRegistrationSettings.setForceTemplateSelection(community.get.selfRegForceTemplateSelection)
        communityData.getSelfRegistrationSettings.setForceRequiredProperties(community.get.selfRegForceRequiredProperties)
        // Administrators.
        if (exportSettings.communityAdministrators) {
          val administrators = data.administrators.get
          if (administrators.nonEmpty) {
            communityData.setAdministrators(new CommunityAdministrators)
            administrators.foreach { user =>
              val exportedAdmin = new CommunityAdministrator
              populateExportedUser(idSequence.next(), user, exportedAdmin, exportSettings.encryptionKey)
              communityData.getAdministrators.getAdministrator.add(exportedAdmin)
              exportedUserMap += (user.id -> exportedAdmin.getId)
            }
          }
        }
        // Certificate settings.
        if (exportSettings.certificateSettings) {
          // Conformance statement certificate.
          val certificateSettings = data.statementCertificateSettings.get
          if (certificateSettings.isDefined) {
            communityData.setConformanceCertificateSettings(new ConformanceCertificateSettings)
            communityData.getConformanceCertificateSettings.setAddTitle(certificateSettings.get.includeTitle)
            communityData.getConformanceCertificateSettings.setAddDetails(certificateSettings.get.includeDetails)
            communityData.getConformanceCertificateSettings.setAddMessage(certificateSettings.get.includeMessage)
            communityData.getConformanceCertificateSettings.setAddTestCases(certificateSettings.get.includeTestCases)
            communityData.getConformanceCertificateSettings.setAddResultOverview(certificateSettings.get.includeTestStatus)
            communityData.getConformanceCertificateSettings.setAddSignature(certificateSettings.get.includeSignature)
            communityData.getConformanceCertificateSettings.setAddPageNumbers(certificateSettings.get.includePageNumbers)
            communityData.getConformanceCertificateSettings.setMessage(certificateSettings.get.message.orNull)
            communityData.getConformanceCertificateSettings.setTitle(certificateSettings.get.title.orNull)
          }
          // Conformance overview certificate.
          val certificateOverviewSettings = data.overviewCertificateSettings.get
          if (certificateOverviewSettings.isDefined) {
            communityData.setConformanceOverviewCertificateSettings(new ConformanceOverviewCertificateSettings)
            communityData.getConformanceOverviewCertificateSettings.setAddTitle(certificateOverviewSettings.get.settings.includeTitle)
            communityData.getConformanceOverviewCertificateSettings.setAddDetails(certificateOverviewSettings.get.settings.includeDetails)
            communityData.getConformanceOverviewCertificateSettings.setAddMessage(certificateOverviewSettings.get.settings.includeMessage)
            communityData.getConformanceOverviewCertificateSettings.setAddStatementList(certificateOverviewSettings.get.settings.includeStatements)
            communityData.getConformanceOverviewCertificateSettings.setAddStatementDetails(certificateOverviewSettings.get.settings.includeStatementDetails)
            communityData.getConformanceOverviewCertificateSettings.setAddResultOverview(certificateOverviewSettings.get.settings.includeStatementStatus)
            communityData.getConformanceOverviewCertificateSettings.setAddSignature(certificateOverviewSettings.get.settings.includeSignature)
            communityData.getConformanceOverviewCertificateSettings.setAddPageNumbers(certificateOverviewSettings.get.settings.includePageNumbers)
            communityData.getConformanceOverviewCertificateSettings.setEnableAggregateLevel(certificateOverviewSettings.get.settings.enableAllLevel)
            communityData.getConformanceOverviewCertificateSettings.setEnableDomainLevel(certificateOverviewSettings.get.settings.enableDomainLevel)
            communityData.getConformanceOverviewCertificateSettings.setEnableSpecificationGroupLevel(certificateOverviewSettings.get.settings.enableGroupLevel)
            communityData.getConformanceOverviewCertificateSettings.setEnableSpecificationLevel(certificateOverviewSettings.get.settings.enableSpecificationLevel)
            communityData.getConformanceOverviewCertificateSettings.setTitle(certificateOverviewSettings.get.settings.title.orNull)
            // Messages.
            if (certificateOverviewSettings.get.messages.nonEmpty) {
              communityData.getConformanceOverviewCertificateSettings.setMessages(new ConformanceOverviewCertificateMessages)
              certificateOverviewSettings.get.messages.foreach { message =>
                val exportedMessage = new com.gitb.xml.export.ConformanceOverviewCertificateMessage
                exportedMessage.setMessage(message.message)
                var includeInExport = false
                OverviewLevelType.apply(message.messageType) match {
                  case OverviewLevelType.OrganisationLevel =>
                    exportedMessage.setMessageType(com.gitb.xml.export.ConformanceOverviewCertificateMessageType.ALL)
                    includeInExport = true
                  case OverviewLevelType.DomainLevel =>
                    exportedMessage.setMessageType(com.gitb.xml.export.ConformanceOverviewCertificateMessageType.DOMAIN)
                    if (message.domain.isDefined) {
                      if (domainExportInfo.isDefined && domainExportInfo.get.exportedDomain.isDefined) {
                        exportedMessage.setIdentifier(domainExportInfo.get.exportedDomain.get.getId)
                        includeInExport = true
                      }
                    } else {
                      includeInExport = true
                    }
                  case OverviewLevelType.SpecificationGroupLevel =>
                    exportedMessage.setMessageType(com.gitb.xml.export.ConformanceOverviewCertificateMessageType.SPECIFICATION_GROUP)
                    if (message.group.isDefined) {
                      if (domainExportInfo.isDefined && domainExportInfo.get.exportedSpecificationGroupMap.contains(message.group.get)) {
                        exportedMessage.setIdentifier(domainExportInfo.get.exportedSpecificationGroupMap(message.group.get))
                        includeInExport = true
                      }
                    } else {
                      includeInExport = true
                    }
                  case OverviewLevelType.SpecificationLevel =>
                    exportedMessage.setMessageType(com.gitb.xml.export.ConformanceOverviewCertificateMessageType.SPECIFICATION)
                    if (message.specification.isDefined) {
                      if (domainExportInfo.isDefined && domainExportInfo.get.exportedSpecificationMap.contains(message.specification.get)) {
                        exportedMessage.setIdentifier(domainExportInfo.get.exportedSpecificationMap(message.specification.get))
                        includeInExport = true
                      }
                    } else {
                      includeInExport = true
                    }
                }
                if (includeInExport) {
                  communityData.getConformanceOverviewCertificateSettings.getMessages.getMessage.add(exportedMessage)
                }
              }
            }
          }
          // Signature settings.
          val keystore = data.keystore.get
          if (keystore.isDefined) {
            communityData.setSignatureSettings(new SignatureSettings)
            communityData.getSignatureSettings.setKeystore(keystore.get.keystoreFile)
            // These values are stored encrypted using the master password that is unique per instance.
            communityData.getSignatureSettings.setKeyPassword(encryptText(Some(keystore.get.keyPassword), isAlreadyEncrypted = true, exportSettings.encryptionKey))
            communityData.getSignatureSettings.setKeystorePassword(encryptText(Some(keystore.get.keystorePassword), isAlreadyEncrypted = true, exportSettings.encryptionKey))
            keystore.get.keystoreType match {
              case "PKCS_12" => communityData.getSignatureSettings.setKeystoreType(KeystoreType.PKCS_12)
              case "JCEKS" => communityData.getSignatureSettings.setKeystoreType(KeystoreType.JCEKS)
              case _ => communityData.getSignatureSettings.setKeystoreType(KeystoreType.JKS)
            }
          }
          // Report stylesheets.
          if (repositoryUtils.hasCommunityReportStylesheets(communityId)) {
            communityData.setReportStylesheets(new CommunityReportStylesheets)
            addCommunityStylesheetToExport(communityId, models.Enums.ReportType.ConformanceOverviewCertificate, communityData.getReportStylesheets)
            addCommunityStylesheetToExport(communityId, models.Enums.ReportType.ConformanceStatementCertificate, communityData.getReportStylesheets)
            addCommunityStylesheetToExport(communityId, models.Enums.ReportType.ConformanceOverviewReport, communityData.getReportStylesheets)
            addCommunityStylesheetToExport(communityId, models.Enums.ReportType.ConformanceStatementReport, communityData.getReportStylesheets)
            addCommunityStylesheetToExport(communityId, models.Enums.ReportType.TestCaseReport, communityData.getReportStylesheets)
            addCommunityStylesheetToExport(communityId, models.Enums.ReportType.TestStepReport, communityData.getReportStylesheets)
          }
          // Report settings.
          val reportSettings = data.reportSettings.get
          if (reportSettings.nonEmpty) {
            communityData.setReportSettings(new com.gitb.xml.export.CommunityReportSettings)
            reportSettings.foreach { setting =>
              communityData.getReportSettings.getReportSetting.add(toExportedReportSetting(setting))
            }
          }
        }
        // Custom member properties.
        val exportedOrganisationPropertyMap: mutable.Map[Long, OrganisationProperty] = mutable.Map()
        val exportedSystemPropertyMap: mutable.Map[Long, SystemProperty] = mutable.Map()
        if (exportSettings.customProperties) {
          val organisationProperties = data.organisationProperties.get
          if (organisationProperties.nonEmpty) {
            communityData.setOrganisationProperties(new OrganisationProperties)
            organisationProperties.foreach { property =>
              val exportedProperty = new OrganisationProperty()
              exportedProperty.setId(toId(idSequence.next()))
              exportedProperty.setLabel(property.name)
              exportedProperty.setName(property.testKey)
              exportedProperty.setDescription(property.description.orNull)
              exportedProperty.setType(propertyTypeForExport(property.kind))
              exportedProperty.setRequired(property.use.equals("R"))
              exportedProperty.setEditable(!property.adminOnly)
              exportedProperty.setInExports(property.inExports)
              exportedProperty.setInTests(!property.notForTests)
              exportedProperty.setInSelfRegistration(property.inSelfRegistration)
              exportedProperty.setHidden(property.hidden)
              exportedProperty.setAllowedValues(property.allowedValues.orNull)
              exportedProperty.setDisplayOrder(property.displayOrder)
              exportedProperty.setDependsOn(property.dependsOn.orNull)
              exportedProperty.setDependsOnValue(property.dependsOnValue.orNull)
              exportedProperty.setDefaultValue(property.defaultValue.orNull)
              communityData.getOrganisationProperties.getProperty.add(exportedProperty)
              exportedOrganisationPropertyMap += (property.id -> exportedProperty)
            }
          }
          val systemProperties = data.systemProperties.get
          if (systemProperties.nonEmpty) {
            communityData.setSystemProperties(new SystemProperties)
            systemProperties.foreach { property =>
              val exportedProperty = new SystemProperty()
              exportedProperty.setId(toId(idSequence.next()))
              exportedProperty.setLabel(property.name)
              exportedProperty.setName(property.testKey)
              exportedProperty.setDescription(property.description.orNull)
              exportedProperty.setType(propertyTypeForExport(property.kind))
              exportedProperty.setRequired(property.use.equals("R"))
              exportedProperty.setEditable(!property.adminOnly)
              exportedProperty.setInExports(property.inExports)
              exportedProperty.setInTests(!property.notForTests)
              exportedProperty.setHidden(property.hidden)
              exportedProperty.setAllowedValues(property.allowedValues.orNull)
              exportedProperty.setDisplayOrder(property.displayOrder)
              exportedProperty.setDependsOn(property.dependsOn.orNull)
              exportedProperty.setDependsOnValue(property.dependsOnValue.orNull)
              exportedProperty.setDefaultValue(property.defaultValue.orNull)
              communityData.getSystemProperties.getProperty.add(exportedProperty)
              exportedSystemPropertyMap += (property.id -> exportedProperty)
            }
          }
        }
        // Custom labels.
        if (exportSettings.customLabels) {
          val labels = data.labels.get
          if (labels.nonEmpty) {
            communityData.setCustomLabels(new CustomLabels())
            labels.foreach { label =>
              val exportedLabel = new CustomLabel()
              LabelType.apply(label.labelType) match {
                case LabelType.Domain => exportedLabel.setLabelType(CustomLabelType.DOMAIN)
                case LabelType.Specification => exportedLabel.setLabelType(CustomLabelType.SPECIFICATION)
                case LabelType.Actor => exportedLabel.setLabelType(CustomLabelType.ACTOR)
                case LabelType.Endpoint => exportedLabel.setLabelType(CustomLabelType.ENDPOINT)
                case LabelType.Organisation => exportedLabel.setLabelType(CustomLabelType.ORGANISATION)
                case LabelType.System => exportedLabel.setLabelType(CustomLabelType.SYSTEM)
                case LabelType.SpecificationInGroup => exportedLabel.setLabelType(CustomLabelType.SPECIFICATION_IN_GROUP)
                case LabelType.SpecificationGroup => exportedLabel.setLabelType(CustomLabelType.SPECIFICATION_GROUP)
              }
              exportedLabel.setId(toId(idSequence.next()))
              exportedLabel.setFixedCasing(label.fixedCase)
              exportedLabel.setSingularForm(label.singularForm)
              exportedLabel.setPluralForm(label.pluralForm)
              communityData.getCustomLabels.getLabel.add(exportedLabel)
            }
          }
        }
        // Landing pages.
        val exportedLandingPageMap: mutable.Map[Long, com.gitb.xml.export.LandingPage] = mutable.Map()
        if (exportSettings.landingPages) {
          val richContents = data.landingPages.get
          if (richContents.nonEmpty) {
            communityData.setLandingPages(new com.gitb.xml.export.LandingPages)
            richContents.foreach { content =>
              val exportedContent = toExportedLandingPage(idSequence.next(), content)
              communityData.getLandingPages.getLandingPage.add(exportedContent)
              exportedLandingPageMap += (content.id -> exportedContent)
            }
          }
        }
        // Legal notices.
        val exportedLegalNoticeMap: mutable.Map[Long, com.gitb.xml.export.LegalNotice] = mutable.Map()
        if (exportSettings.legalNotices) {
          val richContents = data.legalNotices.get
          if (richContents.nonEmpty) {
            communityData.setLegalNotices(new com.gitb.xml.export.LegalNotices)
            richContents.foreach { content =>
              val exportedContent = toExportedLegalNotice(idSequence.next(), content)
              communityData.getLegalNotices.getLegalNotice.add(exportedContent)
              exportedLegalNoticeMap += (content.id -> exportedContent)
            }
          }
        }
        // Error templates.
        val exportedErrorTemplateMap: mutable.Map[Long, com.gitb.xml.export.ErrorTemplate] = mutable.Map()
        if (exportSettings.errorTemplates) {
          val richContents = data.errorTemplates.get
          if (richContents.nonEmpty) {
            communityData.setErrorTemplates(new com.gitb.xml.export.ErrorTemplates)
            richContents.foreach { content =>
              val exportedContent = toExportedErrorTemplate(idSequence.next(), content)
              communityData.getErrorTemplates.getErrorTemplate.add(exportedContent)
              exportedErrorTemplateMap += (content.id -> exportedContent)
            }
          }
        }
        // Triggers
        if (exportSettings.triggers) {
          val triggers = data.triggers.get
          if (triggers.nonEmpty) {
            communityData.setTriggers(new com.gitb.xml.export.Triggers)
            triggers.foreach { trigger =>
              val exportedTrigger = new com.gitb.xml.export.Trigger
              exportedTrigger.setId(toId(idSequence.next()))
              exportedTrigger.setName(trigger.trigger.name)
              exportedTrigger.setDescription(trigger.trigger.description.orNull)
              exportedTrigger.setActive(trigger.trigger.active)
              exportedTrigger.setUrl(trigger.trigger.url)
              exportedTrigger.setOperation(trigger.trigger.operation.orNull)
              models.Enums.TriggerEventType.apply(trigger.trigger.eventType) match {
                case models.Enums.TriggerEventType.OrganisationCreated => exportedTrigger.setEventType(TriggerEventType.ORGANISATION_CREATED)
                case models.Enums.TriggerEventType.OrganisationUpdated => exportedTrigger.setEventType(TriggerEventType.ORGANISATION_UPDATED)
                case models.Enums.TriggerEventType.SystemCreated => exportedTrigger.setEventType(TriggerEventType.SYSTEM_CREATED)
                case models.Enums.TriggerEventType.SystemUpdated => exportedTrigger.setEventType(TriggerEventType.SYSTEM_UPDATED)
                case models.Enums.TriggerEventType.ConformanceStatementCreated => exportedTrigger.setEventType(TriggerEventType.CONFORMANCE_STATEMENT_CREATED)
                case models.Enums.TriggerEventType.ConformanceStatementUpdated => exportedTrigger.setEventType(TriggerEventType.CONFORMANCE_STATEMENT_UPDATED)
                case models.Enums.TriggerEventType.TestSessionSucceeded => exportedTrigger.setEventType(TriggerEventType.TEST_SESSION_SUCCEEDED)
                case models.Enums.TriggerEventType.TestSessionFailed => exportedTrigger.setEventType(TriggerEventType.TEST_SESSION_FAILED)
                case models.Enums.TriggerEventType.ConformanceStatementSucceeded => exportedTrigger.setEventType(TriggerEventType.CONFORMANCE_STATEMENT_SUCCEEDED)
                case models.Enums.TriggerEventType.TestSessionStarted => exportedTrigger.setEventType(TriggerEventType.TEST_SESSION_STARTED)
              }
              models.Enums.TriggerServiceType.apply(trigger.trigger.serviceType) match {
                case models.Enums.TriggerServiceType.GITB => exportedTrigger.setServiceType(TriggerServiceType.GITB)
                case models.Enums.TriggerServiceType.JSON => exportedTrigger.setServiceType(TriggerServiceType.JSON)
              }
              if (trigger.data.isDefined && trigger.data.get.nonEmpty) {
                exportedTrigger.setDataItems(new TriggerDataItems)
                trigger.data.get.foreach { dataItem =>
                  val dataType = models.Enums.TriggerDataType.apply(dataItem.dataType)
                  // Check to ensure we have the dependent properties exported (if applicable).
                  if ((dataType != models.Enums.TriggerDataType.OrganisationParameter && dataType != models.Enums.TriggerDataType.SystemParameter && dataType != models.Enums.TriggerDataType.DomainParameter) ||
                    ((dataType == models.Enums.TriggerDataType.OrganisationParameter || dataType == models.Enums.TriggerDataType.SystemParameter) && exportSettings.customProperties) ||
                    (dataType == models.Enums.TriggerDataType.DomainParameter && exportSettings.domain && exportSettings.domainParameters) ||
                    (dataType == models.Enums.TriggerDataType.StatementParameter && exportSettings.endpoints)
                  ) {
                    var addItem = true
                    val exportedDataItem = new TriggerDataItem
                    exportedDataItem.setId(toId(idSequence.next()))
                    dataType match {
                      case models.Enums.TriggerDataType.Community => exportedDataItem.setDataType(TriggerDataType.COMMUNITY)
                      case models.Enums.TriggerDataType.Organisation => exportedDataItem.setDataType(TriggerDataType.ORGANISATION)
                      case models.Enums.TriggerDataType.System => exportedDataItem.setDataType(TriggerDataType.SYSTEM)
                      case models.Enums.TriggerDataType.Specification => exportedDataItem.setDataType(TriggerDataType.SPECIFICATION)
                      case models.Enums.TriggerDataType.Actor => exportedDataItem.setDataType(TriggerDataType.ACTOR)
                      case models.Enums.TriggerDataType.TestSession => exportedDataItem.setDataType(TriggerDataType.TEST_SESSION)
                      case models.Enums.TriggerDataType.TestReport => exportedDataItem.setDataType(TriggerDataType.TEST_REPORT)
                      case models.Enums.TriggerDataType.OrganisationParameter =>
                        if (exportedOrganisationPropertyMap.contains(dataItem.dataId)) {
                          exportedDataItem.setDataType(TriggerDataType.ORGANISATION_PARAMETER)
                          exportedDataItem.setData(exportedOrganisationPropertyMap(dataItem.dataId))
                        } else {
                          addItem = false
                        }
                      case models.Enums.TriggerDataType.SystemParameter =>
                        if (exportedSystemPropertyMap.contains(dataItem.dataId)) {
                          exportedDataItem.setDataType(TriggerDataType.SYSTEM_PARAMETER)
                          exportedDataItem.setData(exportedSystemPropertyMap(dataItem.dataId))
                        } else {
                          addItem = false
                        }
                      case models.Enums.TriggerDataType.DomainParameter =>
                        if (domainExportInfo.isDefined && domainExportInfo.get.exportedDomainParameterMap.contains(dataItem.dataId)) {
                          exportedDataItem.setDataType(TriggerDataType.DOMAIN_PARAMETER)
                          exportedDataItem.setData(domainExportInfo.get.exportedDomainParameterMap(dataItem.dataId))
                        } else {
                          addItem = false
                        }
                      case models.Enums.TriggerDataType.StatementParameter =>
                        if (domainExportInfo.isDefined && domainExportInfo.get.exportedEndpointParameterMap.contains(dataItem.dataId)) {
                          exportedDataItem.setDataType(TriggerDataType.STATEMENT_PARAMETER)
                          exportedDataItem.setData(domainExportInfo.get.exportedEndpointParameterMap(dataItem.dataId))
                        } else {
                          addItem = false
                        }
                    }
                    if (addItem) {
                      exportedTrigger.getDataItems.getTriggerDataItem.add(exportedDataItem)
                    }
                  }
                }
                if (exportedTrigger.getDataItems != null && exportedTrigger.getDataItems.getTriggerDataItem.isEmpty) {
                  exportedTrigger.setDataItems(null)
                }
              }
              if (trigger.fireExpressions.isDefined && trigger.fireExpressions.get.nonEmpty) {
                exportedTrigger.setFireExpressions(new TriggerFireExpressions)
                trigger.fireExpressions.get.foreach { expression =>
                  val exportedFireExpression = new com.gitb.xml.export.TriggerFireExpression
                  exportedFireExpression.setId(toId(idSequence.next()))
                  exportedFireExpression.setExpression(expression.expression)
                  models.Enums.TriggerFireExpressionType.apply(expression.expressionType) match {
                    case models.Enums.TriggerFireExpressionType.TestCaseIdentifier => exportedFireExpression.setExpressionType(com.gitb.xml.export.TriggerFireExpressionType.TEST_CASE_IDENTIFIER)
                    case models.Enums.TriggerFireExpressionType.TestSuiteIdentifier => exportedFireExpression.setExpressionType(com.gitb.xml.export.TriggerFireExpressionType.TEST_SUITE_IDENTIFIER)
                    case models.Enums.TriggerFireExpressionType.ActorIdentifier => exportedFireExpression.setExpressionType(com.gitb.xml.export.TriggerFireExpressionType.ACTOR_IDENTIFIER)
                    case models.Enums.TriggerFireExpressionType.SpecificationName => exportedFireExpression.setExpressionType(com.gitb.xml.export.TriggerFireExpressionType.SPECIFICATION_NAME)
                    case models.Enums.TriggerFireExpressionType.SystemName => exportedFireExpression.setExpressionType(com.gitb.xml.export.TriggerFireExpressionType.SYSTEM_NAME)
                    case models.Enums.TriggerFireExpressionType.OrganisationName => exportedFireExpression.setExpressionType(com.gitb.xml.export.TriggerFireExpressionType.ORGANISATION_NAME)
                  }
                  exportedFireExpression.setNotMatch(expression.notMatch)
                  exportedTrigger.getFireExpressions.getTriggerFireExpression.add(exportedFireExpression)
                }
              }
              communityData.getTriggers.getTrigger.add(exportedTrigger)
            }
          }
        }
        // Resources
        if (exportSettings.resources) {
          val resources = data.resources.get
          if (resources.nonEmpty) {
            communityData.setResources(new com.gitb.xml.export.CommunityResources)
            resources.foreach { resource =>
              communityData.getResources.getResource.add(toExportedCommunityResource(resource, idSequence))
            }
          }
        }
        // Organisations.
        if (exportSettings.organisations) {
          val organisations = data.organisations.get
          if (organisations.nonEmpty) {
            communityData.setOrganisations(new Organisations)
            organisations.foreach { organisation =>
              val exportedOrganisation = new Organisation
              exportedOrganisation.setId(toId(idSequence.next()))
              require(!organisation.adminOrganization, "The community's admin organisation should not be exportable")
              exportedOrganisation.setAdmin(organisation.adminOrganization)
              exportedOrganisation.setShortName(organisation.shortname)
              exportedOrganisation.setFullName(organisation.fullname)
              exportedOrganisation.setTemplate(organisation.template)
              exportedOrganisation.setTemplateName(organisation.templateName.orNull)
              exportedOrganisation.setApiKey(organisation.apiKey.orNull)
              if (exportSettings.organisationUsers && !organisation.adminOrganization && data.organisationUserMap.get.contains(organisation.id)) {
                exportedOrganisation.setUsers(new com.gitb.xml.export.Users)
                data.organisationUserMap.get(organisation.id).foreach { user =>
                  val exportedUser = new OrganisationUser
                  populateExportedUser(idSequence.next(), user, exportedUser, exportSettings.encryptionKey)
                  exportedUserMap += (user.id -> exportedUser.getId)
                  UserRole.apply(user.role) match {
                    case UserRole.VendorAdmin => exportedUser.setRole(OrganisationRoleType.ORGANISATION_ADMIN)
                    case _ => exportedUser.setRole(OrganisationRoleType.ORGANISATION_USER)
                  }
                  exportedOrganisation.getUsers.getUser.add(exportedUser)
                }
              }
              if (exportSettings.landingPages && organisation.landingPage.isDefined) {
                exportedOrganisation.setLandingPage(exportedLandingPageMap(organisation.landingPage.get))
              }
              if (exportSettings.legalNotices && organisation.legalNotice.isDefined) {
                exportedOrganisation.setLegalNotice(exportedLegalNoticeMap(organisation.legalNotice.get))
              }
              if (exportSettings.errorTemplates && organisation.errorTemplate.isDefined) {
                exportedOrganisation.setErrorTemplate(exportedErrorTemplateMap(organisation.errorTemplate.get))
              }
              // Organisation property values.
              if (exportSettings.customProperties && exportSettings.organisationPropertyValues && data.organisationParameterValueMap.get.contains(organisation.id)) {
                exportedOrganisation.setPropertyValues(new OrganisationPropertyValues)
                data.organisationParameterValueMap.get(organisation.id).foreach { parameter =>
                  val exportedProperty = new OrganisationPropertyValue
                  exportedProperty.setId(toId(idSequence.next()))
                  exportedProperty.setProperty(exportedOrganisationPropertyMap(parameter.parameter))
                  if (exportedProperty.getProperty.getType == PropertyType.SECRET) {
                    exportedProperty.setValue(encryptText(Some(parameter.value), isAlreadyEncrypted = true, exportSettings.encryptionKey))
                  } else if (exportedProperty.getProperty.getType == PropertyType.BINARY) {
                    exportedProperty.setValue(MimeUtil.getFileAsDataURL(repositoryUtils.getOrganisationPropertyFile(parameter.parameter, organisation.id), parameter.contentType.getOrElse(DEFAULT_CONTENT_TYPE)))
                  } else {
                    exportedProperty.setValue(parameter.value)
                  }
                  exportedOrganisation.getPropertyValues.getProperty.add(exportedProperty)
                }
              }
              // Systems.
              if (exportSettings.systems && data.organisationSystemMap.get.contains(organisation.id)) {
                exportedOrganisation.setSystems(new com.gitb.xml.export.Systems)
                data.organisationSystemMap.get(organisation.id).foreach { system =>
                  val exportedSystem = new com.gitb.xml.export.System
                  exportedSystem.setId(toId(idSequence.next()))
                  exportedSystem.setShortName(system.shortname)
                  exportedSystem.setFullName(system.fullname)
                  exportedSystem.setDescription(system.description.orNull)
                  exportedSystem.setVersion(system.version.orNull)
                  exportedSystem.setApiKey(system.apiKey)
                  exportedSystem.setBadgeKey(system.badgeKey)
                  // System property values.
                  if (exportSettings.customProperties && exportSettings.systemPropertyValues && data.systemParameterValueMap.get.contains(system.id)) {
                    exportedSystem.setPropertyValues(new SystemPropertyValues)
                    data.systemParameterValueMap.get(system.id).foreach { parameter =>
                      val exportedProperty = new SystemPropertyValue
                      exportedProperty.setId(toId(idSequence.next()))
                      exportedProperty.setProperty(exportedSystemPropertyMap(parameter.parameter))
                      if (exportedProperty.getProperty.getType == PropertyType.SECRET) {
                        exportedProperty.setValue(encryptText(Some(parameter.value), isAlreadyEncrypted = true, exportSettings.encryptionKey))
                      } else if (exportedProperty.getProperty.getType == PropertyType.BINARY) {
                        exportedProperty.setValue(MimeUtil.getFileAsDataURL(repositoryUtils.getSystemPropertyFile(parameter.parameter, system.id), parameter.contentType.getOrElse(DEFAULT_CONTENT_TYPE)))
                      } else {
                        exportedProperty.setValue(parameter.value)
                      }
                      exportedSystem.getPropertyValues.getProperty.add(exportedProperty)
                    }
                  }
                  // Conformance statements.
                  if (exportSettings.domain && exportSettings.statements && data.systemStatementsMap.get.contains(system.id) && domainExportInfo.isDefined) {
                    exportedSystem.setStatements(new ConformanceStatements)
                    data.systemStatementsMap.get(system.id).foreach { x =>
                      val exportedStatement = new com.gitb.xml.export.ConformanceStatement
                      exportedStatement.setId(toId(idSequence.next()))
                      exportedStatement.setActor(domainExportInfo.get.exportedActorMap(x._2.id))
                      if (exportSettings.statementConfigurations) {
                        // From the statement's actor get its endpoints.
                        if (domainExportInfo.get.actorEndpointMap.contains(x._2.id)) {
                          domainExportInfo.get.actorEndpointMap(x._2.id).foreach { endpoint =>
                            // For the endpoint get the parameters.
                            if (domainExportInfo.get.endpointParameterMap.contains(endpoint.id)) {
                              domainExportInfo.get.endpointParameterMap(endpoint.id).foreach { parameter =>
                                // Get the system configurations for the parameter.
                                val key = s"${endpoint.actor}_${parameter.endpoint}_${system.id}_${parameter.id}"
                                if (data.systemConfigurationsMap.get.contains(key)) {
                                  if (exportedStatement.getConfigurations == null) {
                                    exportedStatement.setConfigurations(new com.gitb.xml.export.Configurations)
                                  }
                                  data.systemConfigurationsMap.get(key).foreach { config =>
                                    val exportedConfiguration = new com.gitb.xml.export.Configuration
                                    exportedConfiguration.setId(toId(idSequence.next()))
                                    exportedConfiguration.setParameter(domainExportInfo.get.exportedEndpointParameterMap(config.parameter))
                                    if (exportedConfiguration.getParameter.getType == PropertyType.SECRET) {
                                      exportedConfiguration.setValue(encryptText(Some(config.value), isAlreadyEncrypted = true, exportSettings.encryptionKey))
                                    } else if (exportedConfiguration.getParameter.getType == PropertyType.BINARY) {
                                      exportedConfiguration.setValue(MimeUtil.getFileAsDataURL(repositoryUtils.getStatementParameterFile(config.parameter, config.system), config.contentType.getOrElse(DEFAULT_CONTENT_TYPE)))
                                    } else {
                                      exportedConfiguration.setValue(config.value)
                                    }
                                    exportedStatement.getConfigurations.getConfiguration.add(exportedConfiguration)
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                      exportedSystem.getStatements.getStatement.add(exportedStatement)
                    }
                  }
                  exportedOrganisation.getSystems.getSystem.add(exportedSystem)
                }
              }
              communityData.getOrganisations.getOrganisation.add(exportedOrganisation)
            }
          }
        }
        Future.successful {
          (exportData, exportedUserMap.toMap, idSequence.next())
        }
      }
      // Add system settings as part of the community export.
      result <- {
        val exportSettings = dataResults._2
        if (exportSettings.hasSystemSettings()) {
          exportSystemSettingsInternal(dataResults._1.systemSettings, exportSettings, Some(result._2), Some(result._3)).map { systemSettingsExportInfo =>
            result._1.setSettings(systemSettingsExportInfo.exportedSettings)
            result._1
          }
        } else {
          Future.successful(result._1)
        }
      }
    } yield result
  }

  private def toExportedReportType(reportType: models.Enums.ReportType.ReportType): com.gitb.xml.export.ReportType = {
    reportType match {
      case models.Enums.ReportType.ConformanceOverviewReport => com.gitb.xml.export.ReportType.CONFORMANCE_OVERVIEW
      case models.Enums.ReportType.ConformanceStatementReport => com.gitb.xml.export.ReportType.CONFORMANCE_STATEMENT
      case models.Enums.ReportType.TestCaseReport => com.gitb.xml.export.ReportType.TEST_CASE
      case models.Enums.ReportType.TestStepReport => com.gitb.xml.export.ReportType.TEST_STEP
      case models.Enums.ReportType.ConformanceOverviewCertificate => com.gitb.xml.export.ReportType.CONFORMANCE_OVERVIEW_CERTIFICATE
      case models.Enums.ReportType.ConformanceStatementCertificate => com.gitb.xml.export.ReportType.CONFORMANCE_STATEMENT_CERTIFICATE
      case _ => throw new IllegalArgumentException("Unknown report type %s".formatted(reportType.id))
    }
  }

  private def addCommunityStylesheetToExport(communityId: Long, reportType: models.Enums.ReportType.ReportType, exportData: CommunityReportStylesheets): Unit = {
    val stylesheet = repositoryUtils.getCommunityReportStylesheet(communityId, reportType)
    if (stylesheet.isDefined) {
      val exportStylesheet = new CommunityReportStylesheet
      exportStylesheet.setReportType(toExportedReportType(reportType))
      exportStylesheet.setContent(Files.readString(stylesheet.get))
      exportData.getStylesheet.add(exportStylesheet)
    }
  }

  private def badgesInfo(successBadge: Option[File], otherBadge: Option[File], failureBadge: Option[File]): Option[ConformanceBadges] = {
    var result: Option[ConformanceBadges] = None
    if (successBadge.isDefined || otherBadge.isDefined || failureBadge.isDefined) {
      val badges = new ConformanceBadges
      if (successBadge.isDefined) {
        val badge = new ConformanceBadge()
        badge.setName(successBadge.get.getName)
        badge.setContent(MimeUtil.getFileAsDataURL(successBadge.get, DEFAULT_CONTENT_TYPE))
        badges.setSuccess(badge)
      }
      if (otherBadge.isDefined) {
        val badge = new ConformanceBadge()
        badge.setName(otherBadge.get.getName)
        badge.setContent(MimeUtil.getFileAsDataURL(otherBadge.get, DEFAULT_CONTENT_TYPE))
        badges.setOther(badge)
      }
      if (failureBadge.isDefined) {
        val badge = new ConformanceBadge()
        badge.setName(failureBadge.get.getName)
        badge.setContent(MimeUtil.getFileAsDataURL(failureBadge.get, DEFAULT_CONTENT_TYPE))
        badges.setFailure(badge)
      }
      result = Some(badges)
    }
    result
  }

  def exportDeletions(communityKeys: List[String], domainKeys: List[String]): Future[com.gitb.xml.export.Export] = {
    Future.successful {
      val exportData = new Export
      exportData.setDeletions(new Deletions)
      communityKeys.foreach { key =>
        exportData.getDeletions.getCommunity.add(key)
      }
      domainKeys.foreach { key =>
        exportData.getDeletions.getDomain.add(key)
      }
      exportData
    }
  }

}
