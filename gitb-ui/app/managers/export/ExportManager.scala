package managers.export

import com.gitb.xml
import com.gitb.xml.export._
import managers._
import models.Enums.{LabelType, SelfRegistrationRestriction, SelfRegistrationType, UserRole}
import models.{TestCases, Actors => _, Endpoints => _, Systems => _, _}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.{FileUtils, IOUtils}
import org.slf4j.{Logger, LoggerFactory}
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider
import utils.{MimeUtil, RepositoryUtils}

import java.nio.file.Files
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

@Singleton
class ExportManager @Inject() (repositoryUtils: RepositoryUtils, communityResourceManager: CommunityResourceManager, triggerManager: TriggerManager, communityManager: CommunityManager, conformanceManager: ConformanceManager, testSuiteManager: TestSuiteManager, landingPageManager: LandingPageManager, legalNoticeManager: LegalNoticeManager, errorTemplateManager: ErrorTemplateManager, specificationManager: SpecificationManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

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

  def exportDomain(domainId: Long, exportSettings: ExportSettings): com.gitb.xml.export.Export = {
    val exportData = new Export
    exportData.setDomains(new Domains)
    exportData.getDomains.getDomain.add(exportDomainInternal(domainId, exportSettings).exportedDomain)
    exportData
  }

  private[export] def loadSharedTestSuites(domainId: Long): Seq[models.TestSuites] = {
    exec(PersistenceSchema.testSuites
      .filter(_.domain === domainId)
      .filter(_.shared)
      .result
    )
  }

  private[export] def loadSpecificationTestSuiteMap(domainId: Long): scala.collection.mutable.Map[Long, ListBuffer[models.TestSuites]] = {
    val specificationTestSuiteMap = scala.collection.mutable.Map[Long, ListBuffer[models.TestSuites]]()
    exec(PersistenceSchema.testSuites
      .join(PersistenceSchema.specificationHasTestSuites).on(_.id === _.testSuiteId)
      .filter(_._1.domain === domainId)
      .map(x => (x._2.specId, x._1)) // Spec ID, TestSuite
      .result
    ).foreach { x =>
      var testSuites = specificationTestSuiteMap.get(x._1)
      if (testSuites.isEmpty) {
        testSuites = Some(new ListBuffer[models.TestSuites])
        specificationTestSuiteMap += (x._1 -> testSuites.get)
      }
      testSuites.get += x._2
    }
    specificationTestSuiteMap
  }

  private[export] def loadEndpointParameterMap(domainId: Long): scala.collection.mutable.Map[Long, ListBuffer[models.Parameters]] = {
    val endpointParameterMap = scala.collection.mutable.Map[Long, ListBuffer[models.Parameters]]()
    exec(PersistenceSchema.parameters
      .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
      .join(PersistenceSchema.actors).on(_._2.actor === _.id)
      .filter(_._2.domain === domainId)
      .map(x => x._1._1)
      .sortBy(x=> (x.endpoint.asc, x.displayOrder.asc, x.testKey.asc))
      .result
    ).foreach { x =>
      var parameters = endpointParameterMap.get(x.endpoint)
      if (parameters.isEmpty) {
        parameters = Some(new ListBuffer[models.Parameters])
        endpointParameterMap += (x.endpoint -> parameters.get)
      }
      parameters.get += x
    }
    endpointParameterMap
  }

  private[export] def loadActorEndpointMap(domainId: Long): scala.collection.mutable.Map[Long, ListBuffer[models.Endpoints]] = {
    val actorEndpointMap = scala.collection.mutable.Map[Long, ListBuffer[models.Endpoints]]()
    exec(PersistenceSchema.endpoints
      .join(PersistenceSchema.actors).on(_.actor === _.id)
      .filter(_._2.domain === domainId)
      .map(x => x._1)
      .result
    ).foreach { x =>
      var endpoints = actorEndpointMap.get(x.actor)
      if (endpoints.isEmpty) {
        endpoints = Some(new ListBuffer[models.Endpoints]())
        actorEndpointMap += (x.actor -> endpoints.get)
      }
      endpoints.get += x
    }
    actorEndpointMap
  }

  private[export] def loadOrganisations(communityId: Long): Seq[models.Organizations] = {
    exec(PersistenceSchema.organizations.filter(_.community === communityId).filter(_.adminOrganization === false).result)
  }

  private[export] def loadAdministrators(communityId: Long): Seq[models.Users] = {
    exec(PersistenceSchema.users
      .join(PersistenceSchema.organizations).on(_.organization === _.id)
      .filter(_._2.community === communityId)
      .filter(_._2.adminOrganization === true)
      .filter(_._1.role === UserRole.CommunityAdmin.id.toShort)
      .map(x => x._1)
      .result
    )
  }

  private[export] def loadOrganisationProperties(communityId: Long): Seq[models.OrganisationParameters] = {
    exec(PersistenceSchema.organisationParameters.filter(_.community === communityId).result)
  }

  private[export] def loadSystemProperties(communityId: Long): Seq[models.SystemParameters] = {
    exec(PersistenceSchema.systemParameters.filter(_.community === communityId).result)
  }

  private[export] def loadOrganisationUserMap(communityId: Long): scala.collection.mutable.Map[Long, ListBuffer[models.Users]] ={
    val organisationUserMap: scala.collection.mutable.Map[Long, ListBuffer[models.Users]] = scala.collection.mutable.Map()
    exec(PersistenceSchema.users
      .join(PersistenceSchema.organizations).on(_.organization === _.id)
      .filter(_._2.adminOrganization === false)
      .filter(_._2.community === communityId)
      .map(x => x._1)
      .result
    ).foreach { x =>
      var users = organisationUserMap.get(x.organization)
      if (users.isEmpty) {
        users = Some(new ListBuffer[models.Users])
        organisationUserMap += (x.organization -> users.get)
      }
      users.get += x
    }
    organisationUserMap
  }

  private [export] def loadOrganisationParameterValueMap(communityId: Long): scala.collection.mutable.Map[Long, ListBuffer[models.OrganisationParameterValues]] = {
    val organisationParameterValueMap: scala.collection.mutable.Map[Long, ListBuffer[models.OrganisationParameterValues]] = scala.collection.mutable.Map()
    exec(PersistenceSchema.organisationParameterValues
      .join(PersistenceSchema.organizations).on(_.organisation === _.id)
      .filter(_._2.adminOrganization === false)
      .filter(_._2.community === communityId)
      .map(x => x._1)
      .result
    ).foreach { x =>
      var organisationParameters = organisationParameterValueMap.get(x.organisation)
      if (organisationParameters.isEmpty) {
        organisationParameters = Some(new ListBuffer[OrganisationParameterValues])
        organisationParameterValueMap += (x.organisation -> organisationParameters.get)
      }
      organisationParameters.get += x
    }
    organisationParameterValueMap
  }

  private[export] def loadOrganisationSystemMap(communityId:Long): scala.collection.mutable.Map[Long, ListBuffer[models.Systems]] = {
    val organisationSystemMap: scala.collection.mutable.Map[Long, ListBuffer[models.Systems]] = scala.collection.mutable.Map()
    exec(PersistenceSchema.systems
      .join(PersistenceSchema.organizations).on(_.owner === _.id)
      .filter(_._2.adminOrganization === false)
      .filter(_._2.community === communityId)
      .map(x => x._1)
      .result
    ).foreach { x =>
      var systems = organisationSystemMap.get(x.owner)
      if (systems.isEmpty) {
        systems = Some(new ListBuffer[models.Systems]())
        organisationSystemMap += (x.owner -> systems.get)
      }
      systems.get += x
    }
    organisationSystemMap
  }

  private[export] def loadSystemStatementsMap(communityId: Long, domainId: Option[Long]): scala.collection.mutable.Map[Long, ListBuffer[(models.Specifications, models.Actors)]] = {
    val systemStatementsMap: scala.collection.mutable.Map[Long, ListBuffer[(models.Specifications, models.Actors)]] = scala.collection.mutable.Map()
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
    exec(query
        .map(x => (x._1._1._1._1._2.id, x._2, x._1._1._2))
      .result
    ).foreach { x =>
      var statements = systemStatementsMap.get(x._1) // systemId
      if (statements.isEmpty) {
        statements = Some(new ListBuffer[(models.Specifications, models.Actors)])
        systemStatementsMap += (x._1 -> statements.get)
      }
      statements.get += ((x._2, x._3)) // (specification, actor)
    }
    systemStatementsMap
  }

  private[export] def loadSystemConfigurationsMap(community: models.Communities): scala.collection.mutable.Map[String, ListBuffer[models.Configs]] = {
    val systemConfigurationsMap: scala.collection.mutable.Map[String, ListBuffer[models.Configs]] = scala.collection.mutable.Map()
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
    exec(query
      .map(x => (x._1._1._1._1, x._1._2.id, x._1._1._2.id, x._1._1._1._2.actor))
      .result
    ).foreach { x =>
      val key = s"${x._4}_${x._1.endpoint}_${x._2}_${x._1.parameter}" // [Actor ID]_[Endpoint ID]_[System ID]_[Endpoint parameter ID]
      var configs = systemConfigurationsMap.get(key)
      if (configs.isEmpty) {
        configs = Some(new ListBuffer[Configs])
        systemConfigurationsMap += (key -> configs.get)
      }
      configs.get += x._1
    }
    systemConfigurationsMap
  }

  private[export] def loadSystemParameterValues(communityId:Long): scala.collection.mutable.Map[Long, ListBuffer[models.SystemParameterValues]] = {
    val systemParameterValueMap: scala.collection.mutable.Map[Long, ListBuffer[models.SystemParameterValues]] = scala.collection.mutable.Map()
    exec(PersistenceSchema.systemParameterValues
      .join(PersistenceSchema.systems).on(_.system === _.id)
      .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
      .filter(_._2.adminOrganization === false)
      .filter(_._2.community === communityId)
      .map(x => x._1._1)
      .result
    ).foreach { x =>
      var systemParameters = systemParameterValueMap.get(x.system)
      if (systemParameters.isEmpty) {
        systemParameters = Some(new ListBuffer[SystemParameterValues])
        systemParameterValueMap += (x.system -> systemParameters.get)
      }
      systemParameters.get += x
    }
    systemParameterValueMap
  }

  private[export] def loadSpecificationActorMap(domainId: Long): scala.collection.mutable.Map[Long, ListBuffer[models.Actors]] = {
    val specificationActorMap = scala.collection.mutable.Map[Long, ListBuffer[models.Actors]]()
    exec(PersistenceSchema.actors
      .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
      .filter(_._1.domain === domainId)
      .map(x => (x._1, x._2.specId))
      .result
    ).foreach { x =>
      var actors = specificationActorMap.get(x._2)
      if (actors.isEmpty) {
        actors = Some(new ListBuffer[models.Actors]())
        specificationActorMap += (x._2 -> actors.get)
      }
      actors.get += x._1
    }
    specificationActorMap
  }

  private def toId(uniqueNumber: Int): String = {
    "_" + uniqueNumber
  }

  private def exportDomainInternal(domainId: Long, exportSettings: ExportSettings): DomainExportInfo = {
    val sequence = new IdGenerator
    var specificationActorMap: scala.collection.mutable.Map[Long, ListBuffer[models.Actors]] = null
    var actorEndpointMap: scala.collection.mutable.Map[Long, ListBuffer[models.Endpoints]] = null
    var endpointParameterMap: scala.collection.mutable.Map[Long, ListBuffer[models.Parameters]] = null
    var specificationTestSuiteMap: scala.collection.mutable.Map[Long, ListBuffer[models.TestSuites]] = null
    var testSuiteTestCaseMap: scala.collection.mutable.Map[Long, ListBuffer[models.TestCases]] = null
    var testSuiteActorMap: scala.collection.mutable.Map[Long, ListBuffer[Long]] = null
    if (exportSettings.specifications) {
      if (exportSettings.actors) {
        // Actors.
        specificationActorMap = loadSpecificationActorMap(domainId)
        if (exportSettings.endpoints) {
          // Endpoints.
          actorEndpointMap = loadActorEndpointMap(domainId)
          // Endpoint parameters.
          endpointParameterMap = loadEndpointParameterMap(domainId)
        }
      }
      // Test suites.
      if (exportSettings.testSuites) {
        testSuiteActorMap = scala.collection.mutable.Map[Long, ListBuffer[Long]]()
        testSuiteTestCaseMap = scala.collection.mutable.Map[Long, ListBuffer[models.TestCases]]()
        specificationTestSuiteMap = loadSpecificationTestSuiteMap(domainId)
        exec(PersistenceSchema.testSuites
            .join(PersistenceSchema.testSuiteHasActors).on(_.id === _.testsuite)
            .filter(_._1.domain === domainId)
            .map(x => x._2)
            .result
        ).foreach { x =>
          var actors = testSuiteActorMap.get(x._1) // Test suite ID
          if (actors.isEmpty) {
            actors = Some(new ListBuffer[Long])
            testSuiteActorMap += (x._1 -> actors.get)
          }
          actors.get += x._2 // Actor ID
        }
        // Test cases.
        exec(PersistenceSchema.testCases
          .join(PersistenceSchema.testSuiteHasTestCases).on(_.id === _.testcase)
          .join(PersistenceSchema.testSuites).on(_._2.testsuite === _.id)
          .filter(_._2.domain === domainId)
          .map(x => (x._1._2.testsuite, x._1._1))
          .result
        ).foreach { x =>
          var testCases = testSuiteTestCaseMap.get(x._1)
          if (testCases.isEmpty) {
            testCases = Some(new ListBuffer[TestCases])
            testSuiteTestCaseMap += (x._1 -> testCases.get)
          }
          testCases.get += x._2
        }
      }
    }
    val exportedActorMap: scala.collection.mutable.Map[Long, com.gitb.xml.export.Actor] = scala.collection.mutable.Map()
    val exportedEndpointParameterMap: scala.collection.mutable.Map[Long, com.gitb.xml.export.EndpointParameter] = scala.collection.mutable.Map()
    val exportedDomainParameterMap: scala.collection.mutable.Map[Long, com.gitb.xml.export.DomainParameter] = scala.collection.mutable.Map()
    val exportedSharedTestSuiteMap: scala.collection.mutable.Map[Long, com.gitb.xml.export.TestSuite] = scala.collection.mutable.Map()
    // Domain.
    val domain = conformanceManager.getById(domainId)
    val exportedDomain = new com.gitb.xml.export.Domain
    exportedDomain.setId(toId(sequence.next()))
    exportedDomain.setShortName(domain.shortname)
    exportedDomain.setFullName(domain.fullname)
    exportedDomain.setDescription(domain.description.orNull)
    // Shared test suites.
    if (exportSettings.testSuites) {
      val testSuites = loadSharedTestSuites(domain.id)
      if (testSuites.nonEmpty) {
        val sharedTestSuites = new com.gitb.xml.export.TestSuites
        testSuites.foreach { testSuite =>
          val exportedTestSuite = toExportedTestSuite(sequence, testSuite, None, testSuiteTestCaseMap.getOrElse(testSuite.id, ListBuffer.empty).toList)
          sharedTestSuites.getTestSuite.add(exportedTestSuite)
          exportedSharedTestSuiteMap += (testSuite.id -> exportedTestSuite)
        }
        exportedDomain.setSharedTestSuites(sharedTestSuites)
      }
    }
    // Specifications.
    if (exportSettings.specifications) {
      // Groups.
      val exportedGroupMap = new mutable.HashMap[Long, com.gitb.xml.export.SpecificationGroup]()
      val groups = specificationManager.getSpecificationGroups(domain.id)
      if (groups.nonEmpty) {
        exportedDomain.setSpecificationGroups(new com.gitb.xml.export.SpecificationGroups)
        groups.foreach { group =>
          val exportedGroup = new com.gitb.xml.export.SpecificationGroup
          exportedGroup.setId(toId(sequence.next()))
          exportedGroup.setShortName(group.shortname)
          exportedGroup.setFullName(group.fullname)
          exportedGroup.setDescription(group.description.orNull)
          exportedGroup.setDisplayOrder(group.displayOrder)
          exportedDomain.getSpecificationGroups.getGroup.add(exportedGroup)
          exportedGroupMap += (group.id -> exportedGroup)
        }
      }
      // Specs.
      val specifications = conformanceManager.getSpecifications(domain.id, withGroups = false)
      if (specifications.nonEmpty) {
        exportedDomain.setSpecifications(new com.gitb.xml.export.Specifications)
        specifications.foreach { specification =>
          val exportedSpecification = new com.gitb.xml.export.Specification
          exportedSpecification.setId(toId(sequence.next()))
          exportedSpecification.setShortName(specification.shortname)
          exportedSpecification.setFullName(specification.fullname)
          exportedSpecification.setDescription(specification.description.orNull)
          exportedSpecification.setApiKey(specification.apiKey)
          exportedSpecification.setHidden(specification.hidden)
          exportedSpecification.setDisplayOrder(specification.displayOrder)
          if (specification.group.nonEmpty) {
            exportedSpecification.setGroup(exportedGroupMap(specification.group.get))
          }
          // Actors
          if (exportSettings.actors && specificationActorMap.contains(specification.id)) {
            exportedSpecification.setActors(new com.gitb.xml.export.Actors)
            specificationActorMap(specification.id).foreach { actor =>
              val exportedActor = new com.gitb.xml.export.Actor
              exportedActor.setId(toId(sequence.next()))
              exportedActor.setSpecification(exportedSpecification)
              exportedActor.setActorId(actor.actorId)
              exportedActor.setName(actor.name)
              exportedActor.setApiKey(actor.apiKey)
              exportedActor.setDescription(actor.description.orNull)
              if (actor.default.isDefined) {
                exportedActor.setDefault(actor.default.get)
              } else {
                exportedActor.setDefault(false)
              }
              exportedActor.setHidden(actor.hidden)
              // Endpoints.
              if (exportSettings.endpoints && actorEndpointMap.contains(actor.id)) {
                exportedActor.setEndpoints(new com.gitb.xml.export.Endpoints)
                actorEndpointMap(actor.id).foreach { endpoint =>
                  val exportedEndpoint = new com.gitb.xml.export.Endpoint
                  exportedEndpoint.setId(toId(sequence.next()))
                  exportedEndpoint.setName(endpoint.name)
                  exportedEndpoint.setDescription(endpoint.desc.orNull)
                  // Endpoint parameters.
                  if (endpointParameterMap.contains(endpoint.id)) {
                    exportedEndpoint.setParameters(new com.gitb.xml.export.EndpointParameters)
                    endpointParameterMap(endpoint.id).foreach { parameter =>
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
          if (exportSettings.testSuites && specificationTestSuiteMap.contains(specification.id)) {
            val testSuites = specificationTestSuiteMap(specification.id)
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
                val exportedTestSuite = toExportedTestSuite(sequence, testSuite, Some(exportedSpecification), testSuiteTestCaseMap.getOrElse(testSuite.id, ListBuffer.empty).toList)
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
      val domainParameters = conformanceManager.getDomainParameters(domain.id)
      if (domainParameters.nonEmpty) {
        exportedDomain.setParameters(new com.gitb.xml.export.DomainParameters)
        domainParameters.foreach { parameter =>
          val exportedParameter = new com.gitb.xml.export.DomainParameter
          exportedParameter.setId(toId(sequence.next()))
          exportedParameter.setName(parameter.name)
          exportedParameter.setDescription(parameter.desc.orNull)
          exportedParameter.setType(propertyTypeForExport(parameter.kind))
          exportedParameter.setInTests(parameter.inTests)
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
    }
    DomainExportInfo(sequence.current(), exportedActorMap, exportedEndpointParameterMap, exportedDomain, actorEndpointMap, endpointParameterMap, exportedDomainParameterMap)
  }

  private def toExportedTestSuite(sequence:IdGenerator, testSuite: models.TestSuites, specificationToSet: Option[com.gitb.xml.export.Specification], testCases: List[models.TestCases]):com.gitb.xml.export.TestSuite  = {
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
    // Zip the test suite's resources to a temporary archive and convert it to a BASE64 string.
    val testTestSuitePath = testSuiteManager.extractTestSuite(testSuite, None)
    try {
      exportedTestSuite.setData(
        Base64.encodeBase64String(IOUtils.toByteArray(Files.newInputStream(testTestSuitePath)))
      )
    } finally {
      FileUtils.deleteQuietly(testTestSuitePath.toFile)
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

  def exportCommunity(communityId: Long, exportSettings: ExportSettings): com.gitb.xml.export.Export = {
    require(communityId != Constants.DefaultCommunityId, "The default community cannot be exported")
    val community = communityManager.getById(communityId)
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
    val exportData = new Export
    val communityData = new com.gitb.xml.export.Community
    var domainExportInfo: DomainExportInfo = null
    var idSequence: Int = 0
    if (community.get.domain.isDefined && exportSettings.domain) {
      domainExportInfo = exportDomainInternal(community.get.domain.get, exportSettings)
      idSequence = domainExportInfo.latestSequenceId
      exportData.setDomains(new Domains)
      exportData.getDomains.getDomain.add(domainExportInfo.exportedDomain)
      communityData.setDomain(domainExportInfo.exportedDomain)
    }
    exportData.setCommunities(new com.gitb.xml.export.Communities)
    exportData.getCommunities.getCommunity.add(communityData)
    // Community basic info.
    idSequence += 1
    communityData.setId(toId(idSequence))
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
    // Self registration information.
    communityData.setSelfRegistrationSettings(new SelfRegistrationSettings)
    SelfRegistrationType.apply(community.get.selfRegType) match {
      case SelfRegistrationType.NotSupported => communityData.getSelfRegistrationSettings.setMethod(SelfRegistrationMethod.NOT_SUPPORTED)
      case SelfRegistrationType.PublicListing => communityData.getSelfRegistrationSettings.setMethod(SelfRegistrationMethod.PUBLIC)
      case SelfRegistrationType.PublicListingWithToken => communityData.getSelfRegistrationSettings.setMethod(SelfRegistrationMethod.PUBLIC_WITH_TOKEN)
      case SelfRegistrationType.Token => communityData.getSelfRegistrationSettings.setMethod(SelfRegistrationMethod.TOKEN)
    }
    communityData.getSelfRegistrationSettings.setNotifications(community.get.selfregNotification)
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
      val administrators = loadAdministrators(communityId)
      if (administrators.nonEmpty) {
        communityData.setAdministrators(new CommunityAdministrators)
        administrators.foreach { user =>
          val exportedAdmin = new CommunityAdministrator
          idSequence += 1
          exportedAdmin.setId(toId(idSequence))
          exportedAdmin.setName(user.name)
          exportedAdmin.setEmail(user.email)
          exportedAdmin.setPassword(encryptText(Some(user.password), exportSettings.encryptionKey))
          exportedAdmin.setOnetimePassword(user.onetimePassword)
          communityData.getAdministrators.getAdministrator.add(exportedAdmin)
        }
      }
    }
    // Certificate settings.
    if (exportSettings.certificateSettings) {
      val certificateSettings = conformanceManager.getConformanceCertificateSettingsWrapper(communityId)
      if (certificateSettings.isDefined) {
        communityData.setConformanceCertificateSettings(new ConformanceCertificateSettings)
        communityData.getConformanceCertificateSettings.setAddDetails(certificateSettings.get.includeDetails)
        communityData.getConformanceCertificateSettings.setAddMessage(certificateSettings.get.includeMessage)
        communityData.getConformanceCertificateSettings.setAddTestCases(certificateSettings.get.includeTestCases)
        communityData.getConformanceCertificateSettings.setAddResultOverview(certificateSettings.get.includeTestStatus)
        communityData.getConformanceCertificateSettings.setAddSignature(certificateSettings.get.includeSignature)
        communityData.getConformanceCertificateSettings.setMessage(certificateSettings.get.message.orNull)
        communityData.getConformanceCertificateSettings.setTitle(certificateSettings.get.title.orNull)
        // Keystore settings.
        if (certificateSettings.get.keystorePassword.isDefined
          && certificateSettings.get.keyPassword.isDefined
          && certificateSettings.get.keystoreType.isDefined
          && certificateSettings.get.keystoreFile.isDefined) {
          communityData.getConformanceCertificateSettings.setSignature(new SignatureSettings)
          communityData.getConformanceCertificateSettings.getSignature.setKeystore(certificateSettings.get.keystoreFile.get)
          // These values are stored encrypted using the master password that is unique per instance.
          communityData.getConformanceCertificateSettings.getSignature.setKeyPassword(encryptText(certificateSettings.get.keyPassword, isAlreadyEncrypted = true, exportSettings.encryptionKey))
          communityData.getConformanceCertificateSettings.getSignature.setKeystorePassword(encryptText(certificateSettings.get.keystorePassword, isAlreadyEncrypted = true, exportSettings.encryptionKey))
          certificateSettings.get.keystoreType.get match {
            case "PKCS_12" => communityData.getConformanceCertificateSettings.getSignature.setKeystoreType(KeystoreType.PKCS_12)
            case "JCEKS" => communityData.getConformanceCertificateSettings.getSignature.setKeystoreType(KeystoreType.JCEKS)
            case _ => communityData.getConformanceCertificateSettings.getSignature.setKeystoreType(KeystoreType.JKS)
          }
        }
      }
    }
    // Custom member properties.
    val exportedOrganisationPropertyMap: scala.collection.mutable.Map[Long, OrganisationProperty] = scala.collection.mutable.Map()
    val exportedSystemPropertyMap: scala.collection.mutable.Map[Long, SystemProperty] = scala.collection.mutable.Map()
    if (exportSettings.customProperties) {
      val organisationProperties = loadOrganisationProperties(communityId)
      if (organisationProperties.nonEmpty) {
        communityData.setOrganisationProperties(new OrganisationProperties)
        organisationProperties.foreach { property =>
          val exportedProperty = new OrganisationProperty()
          idSequence += 1
          exportedProperty.setId(toId(idSequence))
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
      val systemProperties = loadSystemProperties(community.get.id)
      if (systemProperties.nonEmpty) {
        communityData.setSystemProperties(new SystemProperties)
        systemProperties.foreach { property =>
          val exportedProperty = new SystemProperty()
          idSequence += 1
          exportedProperty.setId(toId(idSequence))
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
      val labels = communityManager.getCommunityLabels(communityId)
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
          idSequence += 1
          exportedLabel.setId(toId(idSequence))
          exportedLabel.setFixedCasing(label.fixedCase)
          exportedLabel.setSingularForm(label.singularForm)
          exportedLabel.setPluralForm(label.pluralForm)
          communityData.getCustomLabels.getLabel.add(exportedLabel)
        }
      }
    }
    // Landing pages.
    val exportedLandingPageMap: scala.collection.mutable.Map[Long, com.gitb.xml.export.LandingPage] = scala.collection.mutable.Map()
    if (exportSettings.landingPages) {
      val richContents = landingPageManager.getLandingPagesByCommunity(communityId)
      if (richContents.nonEmpty) {
        communityData.setLandingPages(new com.gitb.xml.export.LandingPages)
        richContents.foreach { content =>
          val exportedContent = new com.gitb.xml.export.LandingPage
          idSequence += 1
          exportedContent.setId(toId(idSequence))
          exportedContent.setName(content.name)
          exportedContent.setDescription(content.description.orNull)
          exportedContent.setContent(content.content)
          exportedContent.setDefault(content.default)
          communityData.getLandingPages.getLandingPage.add(exportedContent)
          exportedLandingPageMap += (content.id -> exportedContent)
        }
      }
    }
    // Legal notices.
    val exportedLegalNoticeMap: scala.collection.mutable.Map[Long, com.gitb.xml.export.LegalNotice] = scala.collection.mutable.Map()
    if (exportSettings.legalNotices) {
      val richContents = legalNoticeManager.getLegalNoticesByCommunity(communityId)
      if (richContents.nonEmpty) {
        communityData.setLegalNotices(new com.gitb.xml.export.LegalNotices)
        richContents.foreach { content =>
          val exportedContent = new com.gitb.xml.export.LegalNotice
          idSequence += 1
          exportedContent.setId(toId(idSequence))
          exportedContent.setName(content.name)
          exportedContent.setDescription(content.description.orNull)
          exportedContent.setContent(content.content)
          exportedContent.setDefault(content.default)
          communityData.getLegalNotices.getLegalNotice.add(exportedContent)
          exportedLegalNoticeMap += (content.id -> exportedContent)
        }
      }
    }
    // Error templates.
    val exportedErrorTemplateMap: scala.collection.mutable.Map[Long, com.gitb.xml.export.ErrorTemplate] = scala.collection.mutable.Map()
    if (exportSettings.errorTemplates) {
      val richContents = errorTemplateManager.getErrorTemplatesByCommunity(communityId)
      if (richContents.nonEmpty) {
        communityData.setErrorTemplates(new com.gitb.xml.export.ErrorTemplates)
        richContents.foreach { content =>
          val exportedContent = new com.gitb.xml.export.ErrorTemplate
          idSequence += 1
          exportedContent.setId(toId(idSequence))
          exportedContent.setName(content.name)
          exportedContent.setDescription(content.description.orNull)
          exportedContent.setContent(content.content)
          exportedContent.setDefault(content.default)
          communityData.getErrorTemplates.getErrorTemplate.add(exportedContent)
          exportedErrorTemplateMap += (content.id -> exportedContent)
        }
      }
    }
    // Triggers
    if (exportSettings.triggers) {
      val triggers = triggerManager.getTriggerAndDataByCommunityId(communityId)
      if (triggers.nonEmpty) {
        communityData.setTriggers(new com.gitb.xml.export.Triggers)
        triggers.foreach { trigger =>
          val exportedTrigger = new com.gitb.xml.export.Trigger
          idSequence += 1
          exportedTrigger.setId(toId(idSequence))
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
                val exportedDataItem = new TriggerDataItem
                idSequence += 1
                exportedDataItem.setId(toId(idSequence))
                dataType match {
                  case models.Enums.TriggerDataType.Community => exportedDataItem.setDataType(TriggerDataType.COMMUNITY)
                  case models.Enums.TriggerDataType.Organisation => exportedDataItem.setDataType(TriggerDataType.ORGANISATION)
                  case models.Enums.TriggerDataType.System => exportedDataItem.setDataType(TriggerDataType.SYSTEM)
                  case models.Enums.TriggerDataType.Specification => exportedDataItem.setDataType(TriggerDataType.SPECIFICATION)
                  case models.Enums.TriggerDataType.Actor => exportedDataItem.setDataType(TriggerDataType.ACTOR)
                  case models.Enums.TriggerDataType.TestSession => exportedDataItem.setDataType(TriggerDataType.TEST_SESSION)
                  case models.Enums.TriggerDataType.TestReport => exportedDataItem.setDataType(TriggerDataType.TEST_REPORT)
                  case models.Enums.TriggerDataType.OrganisationParameter =>
                    exportedDataItem.setDataType(TriggerDataType.ORGANISATION_PARAMETER)
                    exportedDataItem.setData(exportedOrganisationPropertyMap(dataItem.dataId))
                  case models.Enums.TriggerDataType.SystemParameter =>
                    exportedDataItem.setDataType(TriggerDataType.SYSTEM_PARAMETER)
                    exportedDataItem.setData(exportedSystemPropertyMap(dataItem.dataId))
                  case models.Enums.TriggerDataType.DomainParameter =>
                    exportedDataItem.setDataType(TriggerDataType.DOMAIN_PARAMETER)
                    exportedDataItem.setData(domainExportInfo.exportedDomainParameterMap(dataItem.dataId))
                  case models.Enums.TriggerDataType.StatementParameter =>
                    exportedDataItem.setDataType(TriggerDataType.STATEMENT_PARAMETER)
                    exportedDataItem.setData(domainExportInfo.exportedEndpointParameterMap(dataItem.dataId))
                }
                exportedTrigger.getDataItems.getTriggerDataItem.add(exportedDataItem)
              }
            }
            if (exportedTrigger.getDataItems != null && exportedTrigger.getDataItems.getTriggerDataItem.isEmpty) {
              exportedTrigger.setDataItems(null)
            }
          }
          communityData.getTriggers.getTrigger.add(exportedTrigger)
        }
      }
    }
    // Resources
    if (exportSettings.resources) {
      val resources = communityResourceManager.getCommunityResources(communityId)
      if (resources.nonEmpty) {
        communityData.setResources(new com.gitb.xml.export.CommunityResources)
        resources.foreach { resource =>
          val exportedResource = new CommunityResource
          idSequence += 1
          exportedResource.setId(toId(idSequence))
          exportedResource.setName(resource.name)
          exportedResource.setDescription(resource.description.orNull)
          exportedResource.setContent(MimeUtil.getFileAsDataURL(repositoryUtils.getCommunityResource(communityId, resource.id), DEFAULT_CONTENT_TYPE))
          communityData.getResources.getResource.add(exportedResource)
        }
      }
    }
    // Collect data for subsequent use (single queries versus a query per item).
    var organisationParameterValueMap: scala.collection.mutable.Map[Long, ListBuffer[models.OrganisationParameterValues]] = scala.collection.mutable.Map()
    var organisationSystemMap: scala.collection.mutable.Map[Long, ListBuffer[models.Systems]] = scala.collection.mutable.Map()
    var systemParameterValueMap: scala.collection.mutable.Map[Long, ListBuffer[models.SystemParameterValues]] = scala.collection.mutable.Map()
    var systemStatementsMap: scala.collection.mutable.Map[Long, ListBuffer[(models.Specifications, models.Actors)]] = scala.collection.mutable.Map() // System ID to [specification, actor]
    var systemConfigurationsMap: scala.collection.mutable.Map[String, ListBuffer[models.Configs]] = scala.collection.mutable.Map() // [Actor ID]_[Endpoint ID]_[System ID]_[Endpoint parameter ID]
    var organisationUserMap: scala.collection.mutable.Map[Long, ListBuffer[models.Users]] = scala.collection.mutable.Map()
    if (exportSettings.organisations) {
      if (exportSettings.organisationUsers) {
        organisationUserMap = loadOrganisationUserMap(communityId)
      }
      if (exportSettings.customProperties) {
        if (exportSettings.organisationPropertyValues) {
          organisationParameterValueMap = loadOrganisationParameterValueMap(communityId)
        }
        if (exportSettings.systemPropertyValues && exportSettings.systems) {
          systemParameterValueMap = loadSystemParameterValues(communityId)
        }
      }
      if (exportSettings.systems) {
        organisationSystemMap = loadOrganisationSystemMap(communityId)
        if (exportSettings.statements && exportSettings.domain) {
          systemStatementsMap = loadSystemStatementsMap(community.get.id, community.get.domain)
          if (exportSettings.statementConfigurations) {
            systemConfigurationsMap = loadSystemConfigurationsMap(community.get)
          }
        }
      }
    }
    // Organisations.
    if (exportSettings.organisations) {
      val organisations = loadOrganisations(communityId)
      if (organisations.nonEmpty) {
        communityData.setOrganisations(new Organisations)
        organisations.foreach { organisation =>
          val exportedOrganisation = new Organisation
          idSequence += 1
          exportedOrganisation.setId(toId(idSequence))
          require(!organisation.adminOrganization, "The community's admin organisation should not be exportable")
          exportedOrganisation.setAdmin(organisation.adminOrganization)
          exportedOrganisation.setShortName(organisation.shortname)
          exportedOrganisation.setFullName(organisation.fullname)
          exportedOrganisation.setTemplate(organisation.template)
          exportedOrganisation.setTemplateName(organisation.templateName.orNull)
          exportedOrganisation.setApiKey(organisation.apiKey.orNull)
          if (exportSettings.organisationUsers && !organisation.adminOrganization && organisationUserMap.contains(organisation.id)) {
            exportedOrganisation.setUsers(new com.gitb.xml.export.Users)
            organisationUserMap(organisation.id).foreach { user =>
              val exportedUser = new OrganisationUser
              idSequence += 1
              exportedUser.setId(toId(idSequence))
              exportedUser.setName(user.name)
              exportedUser.setEmail(user.email)
              exportedUser.setPassword(encryptText(Some(user.password), exportSettings.encryptionKey))
              exportedUser.setOnetimePassword(user.onetimePassword)
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
          if (exportSettings.customProperties && exportSettings.organisationPropertyValues && organisationParameterValueMap.contains(organisation.id)) {
            exportedOrganisation.setPropertyValues(new OrganisationPropertyValues)
            organisationParameterValueMap(organisation.id).foreach { parameter =>
              val exportedProperty = new OrganisationPropertyValue
              idSequence += 1
              exportedProperty.setId(toId(idSequence))
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
          if (exportSettings.systems && organisationSystemMap.contains(organisation.id)) {
            exportedOrganisation.setSystems(new com.gitb.xml.export.Systems)
            organisationSystemMap(organisation.id).foreach { system =>
              val exportedSystem = new com.gitb.xml.export.System
              idSequence += 1
              exportedSystem.setId(toId(idSequence))
              exportedSystem.setShortName(system.shortname)
              exportedSystem.setFullName(system.fullname)
              exportedSystem.setDescription(system.description.orNull)
              exportedSystem.setVersion(system.version.orNull)
              exportedSystem.setApiKey(system.apiKey.orNull)
              // System property values.
              if (exportSettings.customProperties && exportSettings.systemPropertyValues && systemParameterValueMap.contains(system.id)) {
                exportedSystem.setPropertyValues(new SystemPropertyValues)
                systemParameterValueMap(system.id).foreach { parameter =>
                  val exportedProperty = new SystemPropertyValue
                  idSequence += 1
                  exportedProperty.setId(toId(idSequence))
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
              if (exportSettings.domain && exportSettings.statements && systemStatementsMap.contains(system.id)) {
                exportedSystem.setStatements(new ConformanceStatements)
                systemStatementsMap(system.id).foreach { x =>
                  val exportedStatement = new com.gitb.xml.export.ConformanceStatement
                  idSequence += 1
                  exportedStatement.setId(toId(idSequence))
                  exportedStatement.setActor(domainExportInfo.exportedActorMap(x._2.id))
                  if (exportSettings.statementConfigurations) {
                    // From the statement's actor get its endpoints.
                    if (domainExportInfo.actorEndpointMap.contains(x._2.id)) {
                      domainExportInfo.actorEndpointMap(x._2.id).foreach { endpoint =>
                        // For the endpoint get the parameters.
                        if (domainExportInfo.endpointParameterMap.contains(endpoint.id)) {
                          domainExportInfo.endpointParameterMap(endpoint.id).foreach { parameter =>
                            // Get the system configurations for the parameter.
                            val key = s"${endpoint.actor}_${parameter.endpoint}_${system.id}_${parameter.id}"
                            if (systemConfigurationsMap.contains(key)) {
                              if (exportedStatement.getConfigurations == null) {
                                exportedStatement.setConfigurations(new com.gitb.xml.export.Configurations)
                              }
                              systemConfigurationsMap(key).foreach { config =>
                                val exportedConfiguration = new com.gitb.xml.export.Configuration
                                idSequence += 1
                                exportedConfiguration.setId(toId(idSequence))
                                exportedConfiguration.setParameter(domainExportInfo.exportedEndpointParameterMap(config.parameter))
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
    exportData
  }

}
