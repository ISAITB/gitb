package managers.export

import java.nio.file.Files

import com.gitb.xml.export._
import javax.inject.{Inject, Singleton}
import managers._
import models.Enums.{LabelType, SelfRegistrationRestriction, SelfRegistrationType}
import models.{Actors => _, Endpoints => _, Systems => _, _}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.{FileUtils, IOUtils}
import org.slf4j.{Logger, LoggerFactory}
import persistence.db._
import play.api.db.slick.DatabaseConfigProvider
import utils.MimeUtil

import scala.collection.mutable.ListBuffer

@Singleton
class ExportManager @Inject() (communityManager: CommunityManager, conformanceManager: ConformanceManager, testSuiteManager: TestSuiteManager, landingPageManager: LandingPageManager, legalNoticeManager: LegalNoticeManager, errorTemplateManager: ErrorTemplateManager, organisationManager: OrganizationManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[ExportManager])

  import dbConfig.profile.api._

  private def encryptText(value: Option[String], encryptionKey: Option[String]): String = {
    if (value.isDefined) {
      if (encryptionKey.isEmpty) {
        throw new IllegalArgumentException("No encryption key was provided to encrypt sensitive properties")
      }
      MimeUtil.encryptString(value.get, encryptionKey.get.toCharArray)
    } else {
      null
    }
  }

  private def propertyTypeForExport(modelType: String): PropertyType = {
    if ("BINARY".equals(modelType)) {
      PropertyType.BINARY
    } else if ("SECRET".equals(modelType)) {
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

  private def exportDomainInternal(domainId: Long, exportSettings: ExportSettings): DomainExportInfo = {
    var idSequence: Int = 0
    val specificationActorMap: scala.collection.mutable.Map[Long, ListBuffer[models.Actors]] = scala.collection.mutable.Map()
    val actorEndpointMap: scala.collection.mutable.Map[Long, ListBuffer[models.Endpoints]] = scala.collection.mutable.Map()
    val endpointParameterMap: scala.collection.mutable.Map[Long, ListBuffer[models.Parameters]] = scala.collection.mutable.Map()
    val specificationTestSuiteMap: scala.collection.mutable.Map[Long, ListBuffer[models.TestSuites]] = scala.collection.mutable.Map()
    if (exportSettings.specifications) {
      if (exportSettings.actors) {
        // Actors.
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
        if (exportSettings.endpoints) {
          // Endpoints.
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
          // Endpoint parameters.
          exec(PersistenceSchema.parameters
            .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
            .join(PersistenceSchema.actors).on(_._2.actor === _.id)
            .filter(_._2.domain === domainId)
            .map(x => x._1._1)
            .result
          ).foreach { x =>
            var parameters = endpointParameterMap.get(x.endpoint)
            if (parameters.isEmpty) {
              parameters = Some(new ListBuffer[models.Parameters])
              endpointParameterMap += (x.endpoint -> parameters.get)
            }
            parameters.get += x
          }
        }
      }
      // Test suites.
      if (exportSettings.testSuites) {
        exec(PersistenceSchema.testSuites
          .join(PersistenceSchema.specifications)
          .filter(_._2.domain === domainId)
          .map(x => x._1)
          .result
        ).foreach { x =>
          var testSuites = specificationTestSuiteMap.get(x.specification)
          if (testSuites.isEmpty) {
            testSuites = Some(new ListBuffer[models.TestSuites])
            specificationTestSuiteMap += (x.specification -> testSuites.get)
          }
          testSuites.get += x
        }
      }
    }
    val exportedActorMap: scala.collection.mutable.Map[Long, com.gitb.xml.export.Actor] = scala.collection.mutable.Map()
    val exportedEndpointParameterMap: scala.collection.mutable.Map[Long, com.gitb.xml.export.EndpointParameter] = scala.collection.mutable.Map()
    // Domain.
    val domain = conformanceManager.getById(domainId)
    val exportedDomain = new com.gitb.xml.export.Domain
    idSequence += 1
    exportedDomain.setId(idSequence.toString)
    exportedDomain.setShortName(domain.shortname)
    exportedDomain.setFullName(domain.fullname)
    exportedDomain.setDescription(domain.description.orNull)
    // Specifications.
    if (exportSettings.specifications) {
      val specifications = conformanceManager.getSpecifications(domain.id)
      if (specifications.nonEmpty) {
        exportedDomain.setSpecifications(new com.gitb.xml.export.Specifications)
        specifications.foreach { specification =>
          val exportedSpecification = new com.gitb.xml.export.Specification
          exportedSpecification.setShortName(specification.shortname)
          exportedSpecification.setFullName(specification.fullname)
          exportedSpecification.setDescription(specification.description.orNull)
          exportedSpecification.setHidden(specification.hidden)
          if (exportSettings.actors && specificationActorMap.contains(specification.id)) {
            exportedSpecification.setActors(new com.gitb.xml.export.Actors)
            specificationActorMap(specification.id).foreach { actor =>
              val exportedActor = new com.gitb.xml.export.Actor
              idSequence += 1
              exportedActor.setId(idSequence.toString)
              exportedActor.setActorId(actor.actorId)
              exportedActor.setName(actor.name)
              exportedActor.setDescription(actor.description.orNull)
              if (actor.default.isDefined) {
                exportedActor.setDefault(actor.default.get)
              } else {
                exportedActor.setDefault(false)
              }
              exportedActor.setHidden(actor.hidden)
              // Endpoints.
              if (exportSettings.endpoints && actorEndpointMap.get(actor.id).isDefined) {
                exportedActor.setEndpoints(new com.gitb.xml.export.Endpoints)
                actorEndpointMap(actor.id).foreach { endpoint =>
                  val exportedEndpoint = new com.gitb.xml.export.Endpoint
                  exportedEndpoint.setName(endpoint.name)
                  exportedEndpoint.setDescription(endpoint.desc.orNull)
                  // Endpoint parameters.
                  if (endpointParameterMap.get(endpoint.id).isDefined) {
                    exportedEndpoint.setParameters(new com.gitb.xml.export.EndpointParameters)
                    endpointParameterMap(endpoint.id).foreach { parameter =>
                      val exportedParameter = new com.gitb.xml.export.EndpointParameter
                      idSequence += 1
                      exportedParameter.setId(idSequence.toString)
                      exportedParameter.setName(parameter.name)
                      exportedParameter.setDescription(parameter.desc.orNull)
                      exportedParameter.setType(propertyTypeForExport(parameter.kind))
                      exportedParameter.setEditable(!parameter.adminOnly)
                      exportedParameter.setInTests(!parameter.notForTests)
                      exportedParameter.setRequired(parameter.kind.equals("R"))
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
            exportedSpecification.setTestSuites(new com.gitb.xml.export.TestSuites)
            specificationTestSuiteMap(specification.id).foreach { testSuite =>
              // Zip the test suite's resources to a temporary archive and convert it to a BASE64 string.
              val testTestSuitePath = testSuiteManager.extractTestSuite(testSuite, specification, None)
              try {
                exportedSpecification.getTestSuites.setTestSuite(
                  Base64.encodeBase64String(IOUtils.toByteArray(Files.newInputStream(testTestSuitePath)))
                )
              } finally {
                FileUtils.deleteQuietly(testTestSuitePath.toFile)
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
          exportedParameter.setName(parameter.name)
          exportedParameter.setDescription(parameter.desc.orNull)
          exportedParameter.setType(propertyTypeForExport(parameter.kind))
          if (exportedParameter.getType == PropertyType.SECRET) {
            exportedParameter.setValue(encryptText(parameter.value, exportSettings.encryptionKey))
          } else {
            exportedParameter.setValue(parameter.value.orNull)
          }
          exportedDomain.getParameters.getParameter.add(exportedParameter)
        }
      }
    }
    DomainExportInfo(idSequence, exportedActorMap, exportedEndpointParameterMap, exportedDomain)
  }

  def exportCommunity(communityId: Long, exportSettings: ExportSettings): com.gitb.xml.export.Export = {
    val community = communityManager.getById(communityId)
    if (community.isEmpty) {
      logger.error("No community could be found for id ["+communityId+"]. Aborting export.")
      throw new IllegalStateException("The community requested for export could not be found.")
    }
    if (community.get.domain.isEmpty && exportSettings.domain) {
      logger.warn("Skipping domain-related information from community export as the requested community ["+communityId+"] is not linked to a domain.")
      exportSettings.domain = false
    }
    val exportData = new Export
    var domainExportInfo: DomainExportInfo = null
    var idSequence: Int = 0
    if (community.get.domain.isDefined && exportSettings.domain) {
      domainExportInfo = exportDomainInternal(community.get.domain.get, exportSettings)
      idSequence = domainExportInfo.latestSequenceId
      exportData.setDomains(new Domains)
      exportData.getDomains.getDomain.add(domainExportInfo.exportedDomain)
    }
    val communityData = new com.gitb.xml.export.Community
    exportData.setCommunities(new com.gitb.xml.export.Communities)
    exportData.getCommunities.getCommunity.add(communityData)



    // Collect data for subsequent use (single queries versus a query per item).
    val specificationActorMap: scala.collection.mutable.Map[Long, ListBuffer[models.Actors]] = scala.collection.mutable.Map()
    val actorEndpointMap: scala.collection.mutable.Map[Long, ListBuffer[models.Endpoints]] = scala.collection.mutable.Map()
    val endpointParameterMap: scala.collection.mutable.Map[Long, ListBuffer[models.Parameters]] = scala.collection.mutable.Map()
    val specificationTestSuiteMap: scala.collection.mutable.Map[Long, ListBuffer[models.TestSuites]] = scala.collection.mutable.Map()
    // Community basic info.
    communityData.setShortName(community.get.shortname)
    communityData.setFullName(community.get.fullname)
    communityData.setSupportEmail(community.get.supportEmail.orNull)
    communityData.setDescription(community.get.description.orNull)
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
    SelfRegistrationRestriction.apply(community.get.selfRegRestriction) match {
      case SelfRegistrationRestriction.NoRestriction => communityData.getSelfRegistrationSettings.setRestriction(com.gitb.xml.export.SelfRegistrationRestriction.NO_RESTRICTION)
      case SelfRegistrationRestriction.UserEmail => communityData.getSelfRegistrationSettings.setRestriction(com.gitb.xml.export.SelfRegistrationRestriction.USER_EMAIL)
      case SelfRegistrationRestriction.UserEmailDomain => communityData.getSelfRegistrationSettings.setRestriction(com.gitb.xml.export.SelfRegistrationRestriction.USER_EMAIL_DOMAIN)
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
          communityData.getConformanceCertificateSettings.getSignature.setKeyPassword(encryptText(certificateSettings.get.keyPassword, exportSettings.encryptionKey))
          communityData.getConformanceCertificateSettings.getSignature.setKeystorePassword(encryptText(certificateSettings.get.keystorePassword, exportSettings.encryptionKey))
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
      val organisationProperties = exec(PersistenceSchema.organisationParameters.filter(_.community === communityId).result)
      if (organisationProperties.nonEmpty) {
        communityData.setOrganisationProperties(new OrganisationProperties)
        organisationProperties.foreach { property =>
          val exportedProperty = new OrganisationProperty()
          idSequence += 1
          exportedProperty.setId(idSequence.toString)
          exportedProperty.setLabel(property.name)
          exportedProperty.setName(property.testKey)
          exportedProperty.setDescription(property.description.orNull)
          exportedProperty.setType(propertyTypeForExport(property.kind))
          exportedProperty.setRequired(property.use.equals("R"))
          exportedProperty.setEditable(!property.adminOnly)
          exportedProperty.setInExports(property.inExports)
          exportedProperty.setInTests(!property.notForTests)
          exportedProperty.setInSelfRegistration(property.inSelfRegistration)
          communityData.getOrganisationProperties.getProperty.add(exportedProperty)
          exportedOrganisationPropertyMap += (property.id -> exportedProperty)
        }
      }
      val systemProperties = exec(PersistenceSchema.systemParameters.filter(_.community === communityId).result)
      if (systemProperties.nonEmpty) {
        communityData.setSystemProperties(new SystemProperties)
        systemProperties.foreach { property =>
          val exportedProperty = new SystemProperty()
          idSequence += 1
          exportedProperty.setId(idSequence.toString)
          exportedProperty.setLabel(property.name)
          exportedProperty.setName(property.testKey)
          exportedProperty.setDescription(property.description.orNull)
          exportedProperty.setType(propertyTypeForExport(property.kind))
          exportedProperty.setRequired(property.use.equals("R"))
          exportedProperty.setEditable(!property.adminOnly)
          exportedProperty.setInExports(property.inExports)
          exportedProperty.setInTests(!property.notForTests)
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
          }
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
          exportedContent.setId(idSequence.toString)
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
          exportedContent.setId(idSequence.toString)
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
          exportedContent.setId(idSequence.toString)
          exportedContent.setName(content.name)
          exportedContent.setDescription(content.description.orNull)
          exportedContent.setContent(content.content)
          exportedContent.setDefault(content.default)
          communityData.getErrorTemplates.getErrorTemplate.add(exportedContent)
          exportedErrorTemplateMap += (content.id -> exportedContent)
        }
      }
    }
    // Collect data for subsequent use (single queries versus a query per item).
    val organisationParameterValueMap: scala.collection.mutable.Map[Long, ListBuffer[models.OrganisationParameterValues]] = scala.collection.mutable.Map()
    val organisationSystemMap: scala.collection.mutable.Map[Long, ListBuffer[models.Systems]] = scala.collection.mutable.Map()
    val systemParameterValueMap: scala.collection.mutable.Map[Long, ListBuffer[models.SystemParameterValues]] = scala.collection.mutable.Map()
    val systemStatementsMap: scala.collection.mutable.Map[Long, ListBuffer[Long]] = scala.collection.mutable.Map()
    val systemConfigurationsMap: scala.collection.mutable.Map[String, ListBuffer[models.Configs]] = scala.collection.mutable.Map()
    if (exportSettings.organisations) {
      if (exportSettings.customProperties) {
        if (exportSettings.organisationPropertyValues) {
          exec(PersistenceSchema.organisationParameterValues
            .join(PersistenceSchema.organizations).on(_.organisation === _.id)
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
        }
        if (exportSettings.systemPropertyValues && exportSettings.systems) {
          exec(PersistenceSchema.systemParameterValues
            .join(PersistenceSchema.systems).on(_.system === _.id)
            .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
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
        }
      }
      if (exportSettings.systems) {
        exec(PersistenceSchema.systems
          .join(PersistenceSchema.organizations).on(_.owner === _.id)
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
        if (exportSettings.statements && exportSettings.domain) {
          var query = PersistenceSchema.systemImplementsActors
            .join(PersistenceSchema.systems).on(_.systemId === _.id)
            .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
            .join(PersistenceSchema.actors).on(_._1._1.actorId === _.id)
            .filter(_._1._2.community === communityId)
          if (community.get.domain.isDefined) {
            query = query.filter(_._2.domain === community.get.domain.get)
          }
          exec(query
            .map(x => x._1._1._1)
            .result
          ).foreach { x =>
            var statements = systemStatementsMap.get(x._1) // systemId
            if (statements.isEmpty) {
              statements = Some(new ListBuffer[Long])
              systemStatementsMap += (x._1 -> statements.get)
            }
            statements.get += x._3 // actorId
          }
          if (exportSettings.statementConfigurations) {
            var query = PersistenceSchema.configs
              .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
              .join(PersistenceSchema.actors).on(_._2.actor === _.id)
              .join(PersistenceSchema.systems).on(_._1._1.system === _.id)
              .join(PersistenceSchema.organizations).on(_._2.owner === _.id)
              .filter(_._2.community === communityId)
            if (community.get.domain.isDefined) {
              query = query.filter(_._1._1._2.domain === community.get.domain.get)
            }
            exec(query
              .map(x => (x._1._1._1._1, x._1._2.id))
              .result
            ).foreach { x =>
              val key = x._2+"_"+x._1.parameter // [System ID]_[Parameter ID]
              var configs = systemConfigurationsMap.get(key)
              if (configs.isEmpty) {
                configs = Some(new ListBuffer[Configs])
                systemConfigurationsMap += (key -> configs.get)
              }
              configs.get += x._1
            }
          }
        }
      }
    }
    // Organisations.
    if (exportSettings.organisations) {
      val organisations = organisationManager.getOrganizationsByCommunity(communityId)
      if (organisations.nonEmpty) {
        communityData.setOrganisations(new Organisations)
        organisations.foreach { organisation =>
          val exportedOrganisation = new Organisation
          exportedOrganisation.setShortName(organisation.shortname)
          exportedOrganisation.setFullName(organisation.fullname)
          exportedOrganisation.setTemplate(organisation.template)
          exportedOrganisation.setTemplateName(organisation.templateName.orNull)
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
              exportedProperty.setProperty(exportedOrganisationPropertyMap(parameter.parameter))
              if (exportedProperty.getProperty.getType == PropertyType.SECRET) {
                exportedProperty.setValue(encryptText(Some(parameter.value), exportSettings.encryptionKey))
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
              exportedSystem.setShortName(system.shortname)
              exportedSystem.setFullName(system.fullname)
              exportedSystem.setDescription(system.description.orNull)
              exportedSystem.setVersion(system.version)
              // System property values.
              if (exportSettings.customProperties && exportSettings.systemPropertyValues && systemParameterValueMap.contains(system.id)) {
                exportedSystem.setPropertyValues(new SystemPropertyValues)
                systemParameterValueMap(system.id).foreach { parameter =>
                  val exportedProperty = new SystemPropertyValue
                  exportedProperty.setProperty(exportedSystemPropertyMap(parameter.parameter))
                  if (exportedProperty.getProperty.getType == PropertyType.SECRET) {
                    exportedProperty.setValue(encryptText(Some(parameter.value), exportSettings.encryptionKey))
                  } else {
                    exportedProperty.setValue(parameter.value)
                  }
                  exportedSystem.getPropertyValues.getProperty.add(exportedProperty)
                }
              }
              // Conformance statements.
              if (exportSettings.domain && exportSettings.statements && systemStatementsMap.contains(system.id)) {
                exportedSystem.setStatements(new ConformanceStatements)
                systemStatementsMap(system.id).foreach { actorId =>
                  val exportedStatement = new com.gitb.xml.export.ConformanceStatement
                  exportedStatement.setActor(domainExportInfo.exportedActorMap(actorId))
                  if (exportSettings.statementConfigurations) {
                    // From the statement's actor get its endpoints.
                    if (actorEndpointMap.contains(actorId)) {
                      actorEndpointMap(actorId).foreach { endpoint =>
                        // For the endpoint get the parameters.
                        if (endpointParameterMap.contains(endpoint.id)) {
                          endpointParameterMap(endpoint.id).foreach { parameter =>
                            // Get the system configurations for the parameter.
                            val key = system.id+"_"+parameter.id
                            if (systemConfigurationsMap.contains(key)) {
                              exportedStatement.setConfigurations(new com.gitb.xml.export.Configurations)
                              systemConfigurationsMap(key).foreach { config =>
                                val exportedConfiguration = new com.gitb.xml.export.Configuration
                                exportedConfiguration.setParameter(domainExportInfo.exportedEndpointParameterMap(config.parameter))
                                if (exportedConfiguration.getParameter.getType == PropertyType.SECRET) {
                                  exportedConfiguration.setValue(encryptText(Some(config.value), exportSettings.encryptionKey))
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
