package managers

import actors.events.OrganisationUpdatedEvent
import javax.inject.{Inject, Singleton}
import models.Enums.UserRole
import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by VWYNGAET on 26/10/2016.
 */
@Singleton
class OrganizationManager @Inject() (systemManager: SystemManager, testResultManager: TestResultManager, triggerHelper: TriggerHelper, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("OrganizationManager")

  /**
    * Checks if organization exists (ignoring the default)
    */
  def checkOrganizationExists(orgId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.organizations.filter(_.id =!= Constants.DefaultOrganizationId).filter(_.id === orgId).result.headOption)
    firstOption.isDefined
  }

  /**
    * Gets all organizations
    */
  def getOrganizations(): List[Organizations] = {
    //1) Get all organizations except the default organization for system administrators
    val organizations = exec(PersistenceSchema.organizations
      .sortBy(_.shortname.asc)
      .result.map(_.toList))
    organizations
  }

  def getOrganisationTemplates(communityId: Long): Option[List[SelfRegTemplate]] = {
    val result = exec(PersistenceSchema.organizations
      .filter(_.community === communityId)
      .filter(_.template === true)
      .sortBy(_.templateName).result
    ).map(x => new SelfRegTemplate(x.id, x.templateName.get)).toList
    if (result.isEmpty) {
      None
    } else {
      Some(result)
    }
  }

  /**
    * Gets organizations with specified community
    */
  def getOrganizationsByCommunity(communityId: Long): List[Organizations] = {
    val organizations = exec(PersistenceSchema.organizations.filter(_.adminOrganization === false).filter(_.community === communityId)
      .sortBy(_.shortname.asc)
      .result.map(_.toList))
    organizations
  }

  def getById(id: Long): Option[Organizations] = {
    exec(PersistenceSchema.organizations.filter(_.id === id).result.headOption)
  }

  /**
    * Gets organization with specified id
    */
  def getOrganizationById(orgId: Long): Organization = {
    val o = exec(PersistenceSchema.organizations.filter(_.id === orgId).result.head)
    val l = exec(PersistenceSchema.landingPages.filter(_.id === o.landingPage).result.headOption)
    val n = exec(PersistenceSchema.legalNotices.filter(_.id === o.legalNotice).result.headOption)
    val e = exec(PersistenceSchema.errorTemplates.filter(_.id === o.errorTemplate).result.headOption)
    val organization = new Organization(o, l.orNull, n.orNull, e.orNull)
    organization
  }

  def getOrganizationBySystemId(systemId: Long): Organizations = {
    exec(PersistenceSchema.organizations
      .join(PersistenceSchema.systems).on(_.id === _.owner)
      .filter(_._2.id === systemId)
      .map(x => x._1)
      .result.head)
  }

  private def copyTestSetup(fromOrganisation: Long, toOrganisation: Long, copyOrganisationParameters: Boolean, copySystemParameters: Boolean, copyStatementParameters: Boolean): DBIO[Option[List[SystemCreationDbInfo]]] = {
    val actions = new ListBuffer[DBIO[Option[List[SystemCreationDbInfo]]]]()
    val systems = systemManager.getSystemsByOrganization(fromOrganisation)
    systems.foreach { otherSystem =>
      actions += (
        for {
          newSystemId <- systemManager.registerSystem(Systems(0L, otherSystem.shortname, otherSystem.fullname, otherSystem.description, otherSystem.version, toOrganisation), None, None, None)
          createdSystemInfo <- {
            DBIO.successful(Some(List[SystemCreationDbInfo](new SystemCreationDbInfo(newSystemId, None))))
          }
          linkedActorIds <- systemManager.copyTestSetup(otherSystem.id, newSystemId, copySystemParameters, copyStatementParameters)
          _ <- {
            createdSystemInfo.get.head.linkedActorIds = Some(linkedActorIds)
            DBIO.successful(())
          }
        } yield createdSystemInfo
      )
    }
    if (copyOrganisationParameters) {
      actions += PersistenceSchema.organisationParameterValues.filter(_.organisation === toOrganisation).delete andThen DBIO.successful(None)
      actions += (
        (for {
          otherValues <- PersistenceSchema.organisationParameterValues.filter(_.organisation === fromOrganisation).result.map(_.toList)
          _ <- {
            val copyActions = new ListBuffer[DBIO[_]]()
            otherValues.map(otherValue => {
              copyActions += (PersistenceSchema.organisationParameterValues += OrganisationParameterValues(toOrganisation, otherValue.parameter, otherValue.value))
            })
            DBIO.seq(copyActions.map(a => a): _*)
          }
        } yield()) andThen DBIO.successful(None)
      )
    }
    DBIO.fold(actions, None) {
      (aggregated, current) => {
        Some(aggregated.getOrElse(List[SystemCreationDbInfo]()) ++ current.getOrElse(List[SystemCreationDbInfo]()))
      }
    }
  }

  def createOrganizationInTrans(organization: Organizations, otherOrganisationId: Option[Long], propertyValues: Option[List[OrganisationParameterValues]], copyOrganisationParameters: Boolean, copySystemParameters: Boolean, copyStatementParameters: Boolean): DBIO[OrganisationCreationDbInfo] = {
    for {
      newOrganisationId <- PersistenceSchema.insertOrganization += organization
      _ <- {
        if (propertyValues.isDefined && (otherOrganisationId.isEmpty || !copyOrganisationParameters)) {
          saveOrganisationParameterValues(newOrganisationId, organization.community, isAdmin = true, propertyValues.get)
        } else {
          DBIO.successful(())
        }
      }
      createdSystemsInfo <- {
        if (otherOrganisationId.isDefined) {
          copyTestSetup(otherOrganisationId.get, newOrganisationId, copyOrganisationParameters, copySystemParameters, copyStatementParameters)
        } else {
          DBIO.successful(Some(List[SystemCreationDbInfo]()))
        }
      }
      createdOrganisationInfo <- DBIO.successful(new OrganisationCreationDbInfo(newOrganisationId, createdSystemsInfo))
    } yield createdOrganisationInfo
  }

  /**
    * Creates new organization
    */
  def createOrganization(organization: Organizations, otherOrganisationId: Option[Long], propertyValues: Option[List[OrganisationParameterValues]], copyOrganisationParameters: Boolean, copySystemParameters: Boolean, copyStatementParameters: Boolean) = {
    val orgInfo = exec(
      createOrganizationInTrans(organization, otherOrganisationId, propertyValues, copyOrganisationParameters, copySystemParameters, copyStatementParameters).transactionally
    )
    triggerHelper.triggersFor(organization.community, orgInfo)
    orgInfo.organisationId
  }

  def isTemplateNameUnique(templateName: String, communityId: Long, organisationIdToIgnore: Option[Long]): Boolean = {
    var q = PersistenceSchema.organizations
      .filter(_.community === communityId)
      .filter(_.template === true)
      .filter(_.templateName === templateName)
    if (organisationIdToIgnore.isDefined) {
      q = q.filter(_.id =!= organisationIdToIgnore.get)
    }
    val result = exec(q.result.headOption)
    result.isEmpty
  }

  def updateOwnOrganization(userId: Long, shortName: String, fullName: String, propertyValues: Option[List[OrganisationParameterValues]]) = {
    val user = exec(PersistenceSchema.users.filter(_.id === userId).result.head)
    val organisation = exec(PersistenceSchema.organizations.filter(_.id === user.organization).result.head)

    val actions = new ListBuffer[DBIO[_]]()
    val q = for {o <- PersistenceSchema.organizations if o.id === user.organization} yield (o.shortname, o.fullname)
    actions += q.update(shortName, fullName)
    if (propertyValues.isDefined) {
      val isAdmin = user.role == UserRole.SystemAdmin.id.toShort || user.role == UserRole.CommunityAdmin.id.toShort
      actions += saveOrganisationParameterValues(user.organization, organisation.community, isAdmin, propertyValues.get)
    }
    exec(DBIO.seq(actions.map(a => a): _*).transactionally)
    triggerHelper.publishTriggerEvent(new OrganisationUpdatedEvent(organisation.community, organisation.id))
  }

  def updateOrganizationInternal(orgId: Long, shortName: String, fullName: String, landingPageId: Option[Long], legalNoticeId: Option[Long], errorTemplateId: Option[Long], otherOrganisation: Option[Long], template: Boolean, templateName: Option[String], propertyValues: Option[List[OrganisationParameterValues]], copyOrganisationParameters: Boolean, copySystemParameters: Boolean, copyStatementParameters: Boolean): DBIO[Option[List[SystemCreationDbInfo]]] = {
    for {
      org <- PersistenceSchema.organizations.filter(_.id === orgId).result.headOption
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (org.isDefined) {
          var templateNameToSet: Option[String] = null
          if (template) {
            templateNameToSet = templateName
          } else {
            templateNameToSet = None
          }
          val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.shortname, o.fullname, o.landingPage, o.legalNotice, o.errorTemplate, o.template, o.templateName)
          actions += q.update(shortName, fullName, landingPageId, legalNoticeId, errorTemplateId, template, templateNameToSet)
          if (!shortName.isEmpty && !org.get.shortname.equals(shortName)) {
            actions += testResultManager.updateForUpdatedOrganisation(orgId, shortName)
          }
        }
        toDBIO(actions)
      }
      createdSystemInfo <- {
        if (otherOrganisation.isDefined) {
          for {
            // Replace the test setup for the organisation with the one from the provided one.
            _ <- systemManager.deleteSystemByOrganization(orgId)
            createdSystemInfo <- copyTestSetup(otherOrganisation.get, orgId, copyOrganisationParameters, copySystemParameters, copyStatementParameters)
          } yield createdSystemInfo
        } else {
          DBIO.successful(Some(List[SystemCreationDbInfo]()))
        }
      }
      _ <- {
        if (propertyValues.isDefined && (otherOrganisation.isEmpty || !copyOrganisationParameters)) {
          saveOrganisationParameterValues(orgId, org.get.community, isAdmin = true, propertyValues.get)
        } else {
          DBIO.successful(())
        }
      }
    } yield createdSystemInfo
  }

  private def getCommunityIdByOrganisationId(organisationId: Long): Long = {
    exec(PersistenceSchema.organizations.filter(_.id === organisationId).map(x => x.community).result.head)
  }

  def updateOrganization(orgId: Long, shortName: String, fullName: String, landingPageId: Option[Long], legalNoticeId: Option[Long], errorTemplateId: Option[Long], otherOrganisation: Option[Long], template: Boolean, templateName: Option[String], propertyValues: Option[List[OrganisationParameterValues]], copyOrganisationParameters: Boolean, copySystemParameters: Boolean, copyStatementParameters: Boolean): Unit = {
    val createdSystemInfo = exec(updateOrganizationInternal(orgId, shortName, fullName, landingPageId, legalNoticeId, errorTemplateId, otherOrganisation, template, templateName, propertyValues, copyOrganisationParameters, copySystemParameters, copyStatementParameters).transactionally)
    val communityId = getCommunityIdByOrganisationId(orgId)
    triggerHelper.publishTriggerEvent(new OrganisationUpdatedEvent(communityId, orgId))
    triggerHelper.triggersFor(communityId, createdSystemInfo)
  }

  /**
    * Deletes organization by community
    */
  def deleteOrganizationByCommunity(communityId: Long) = {
    testResultManager.updateForDeletedOrganisationByCommunityId(communityId) andThen
      (for {
        list <- PersistenceSchema.organizations.filter(_.community === communityId).result
        _ <- DBIO.seq(list.map { org =>
          deleteOrganization(org.id)
        }: _*)
      } yield ())
  }

  def deleteOrganization(orgId: Long) = {
    testResultManager.updateForDeletedOrganisation(orgId) andThen
      deleteUserByOrganization(orgId) andThen
      systemManager.deleteSystemByOrganization(orgId) andThen
      PersistenceSchema.organisationParameterValues.filter(_.organisation === orgId).delete andThen
      PersistenceSchema.organizations.filter(_.id === orgId).delete andThen
      DBIO.successful(())
  }

  /**
    * Deletes organization with specified id
    */
  def deleteOrganizationWrapper(orgId: Long) {
    exec(deleteOrganization(orgId).transactionally)
  }

  /**
    * Deletes all users with specified organization
    */
  def deleteUserByOrganization(orgId: Long) = {
    PersistenceSchema.users.filter(_.organization === orgId).delete
  }

  def getOrganisationParameterValues(orgId: Long): List[OrganisationParametersWithValue] = {
    val communityId = getById(orgId).get.community
    exec(PersistenceSchema.organisationParameters
      .joinLeft(PersistenceSchema.organisationParameterValues).on((p, v) => p.id === v.parameter && v.organisation === orgId)
      .filter(_._1.community === communityId)
      .sortBy(x => (x._1.displayOrder.asc, x._1.name.asc))
      .map(x => (x._1, x._2))
      .result
    ).toList.map(r => new OrganisationParametersWithValue(r._1, r._2))
  }

  private def prerequisitesSatisfied(isAdmin: Boolean, parameterToCheck: OrganisationParameters, statusMap: mutable.Map[Long, Boolean], definitionMap: Map[String, OrganisationParameters], providedValueMap: Map[Long, OrganisationParameterValues], existingValueMap: Map[Long, OrganisationParameterValues], checkedParameters: mutable.Set[Long]): Boolean = {
    var satisfied = false
    if (statusMap.contains(parameterToCheck.id)) {
      satisfied = statusMap(parameterToCheck.id)
    } else {
      if (!checkedParameters.contains(parameterToCheck.id)) {
        checkedParameters += parameterToCheck.id
        // The above check is added to avoid circular dependencies that would never complete.
        if (parameterToCheck.dependsOn.isDefined) {
          if (parameterToCheck.dependsOnValue.isDefined) {
            if (definitionMap.contains(parameterToCheck.dependsOn.get)) {
              val parentParameter = definitionMap(parameterToCheck.dependsOn.get)
              if (prerequisitesSatisfied(isAdmin, parentParameter, statusMap, definitionMap, providedValueMap, existingValueMap, checkedParameters)) {
                var parentValue = providedValueMap.get(parentParameter.id)
                if (parentValue.isEmpty && !isAdmin && parentParameter.adminOnly && parentParameter.hidden) {
                  parentValue = existingValueMap.get(parentParameter.id)
                }
                if (parentValue.isDefined && parentValue.get.value != null && parentValue.get.value.equals(parameterToCheck.dependsOnValue.get)) {
                  satisfied = true
                }
              }
            }
          }
        } else {
          satisfied = true
        }
      }
      statusMap += (parameterToCheck.id -> satisfied)
    }
    satisfied
  }

  def saveOrganisationParameterValues(orgId: Long, communityId: Long, isAdmin: Boolean, values: List[OrganisationParameterValues]): DBIO[_] = {
    saveOrganisationParameterValues(orgId, communityId, isAdmin, isSelfRegistration = false, values, requireMandatoryPropertyValues = false)
  }

  def saveOrganisationParameterValues(orgId: Long, communityId: Long, isAdmin: Boolean, isSelfRegistration: Boolean, values: List[OrganisationParameterValues], requireMandatoryPropertyValues: Boolean): DBIO[_] = {
    for {
      // Create a map of the provided parameters
      providedParameters <- DBIO.successful(values.map(x => (x.parameter, x)).toMap)
      // Load existing parameter values (needed to check prerequisites that are admin-only and hidden - only needed if we're enforcing required values.
      existingSimpleValues <- {
        var existingSimpleValues: Option[Map[Long, OrganisationParameterValues]] = None
        if (requireMandatoryPropertyValues && !isSelfRegistration) {
          // We only load these if we're not doing a self-registration. In a self-registration there will never be previously existing values.
          existingSimpleValues = Some(
            exec(
              PersistenceSchema.organisationParameterValues
                .join(PersistenceSchema.organisationParameters).on(_.parameter === _.id)
                .filter(_._1.organisation === orgId)
                .filter(_._2.kind === "SIMPLE")
                .map(x => x._1)
                .result
            ).map(x => (x.parameter, x)).toMap
          )
        }
        DBIO.successful(existingSimpleValues)
      }
      // Load parameter definitions for the organisation's community.
      parameterDefinitions <- PersistenceSchema.organisationParameters.filter(_.community === communityId).result
      // Create also a map based on the key.
      parameterDefinitionMap <- DBIO.successful(parameterDefinitions.map(x => (x.testKey, x)).toMap)
      // Make checks and updates.
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        val prerequisiteStatusMap = mutable.Map[Long, Boolean]()
        val checkPrerequisitesSet = mutable.Set[Long]()
        parameterDefinitions.foreach { parameterDefinition =>
          if (requireMandatoryPropertyValues && "R".equals(parameterDefinition.use) && !providedParameters.contains(parameterDefinition.id) && prerequisitesSatisfied(isAdmin, parameterDefinition, prerequisiteStatusMap, parameterDefinitionMap, providedParameters, existingSimpleValues.get, checkPrerequisitesSet)) {
            if (isAdmin || (!parameterDefinition.adminOnly && !parameterDefinition.hidden && (!isSelfRegistration || parameterDefinition.inSelfRegistration))) {
              // No need to check this case before as the UI normally enforces this. Only way we could reach this is if a request is tampered.
              // Admins (if forced) should provide all values. Non-admins if forced would only be expected to provide values for parameters that are editable by them (non-admin, non-hidden).
              throw new IllegalStateException("Required parameter ["+parameterDefinition.testKey+"] missing")
            }
          }
          if ((!parameterDefinition.adminOnly && !parameterDefinition.hidden) || isAdmin) {
            val matchedProvidedParameter = providedParameters.get(parameterDefinition.id)
            if (matchedProvidedParameter.isDefined) {
              // Create or update
              if (parameterDefinition.kind != "SECRET" || (parameterDefinition.kind == "SECRET" && matchedProvidedParameter.get.value != "")) {
                // Special case: No update for secret parameters that are defined but not updated.
                actions += PersistenceSchema.organisationParameterValues.filter(_.parameter === parameterDefinition.id).filter(_.organisation === orgId).delete
                actions += (PersistenceSchema.organisationParameterValues += matchedProvidedParameter.get.withOrgId(orgId))
              }
            } else {
              // Delete existing (if present)
              actions += PersistenceSchema.organisationParameterValues.filter(_.parameter === parameterDefinition.id).filter(_.organisation === orgId).delete
            }
          }
        }
        toDBIO(actions)
      }
    } yield()
  }

  def saveOrganisationParameterValuesWrapper(userId: Long, orgId: Long, values: List[OrganisationParameterValues]) = {
    val userRole: Short = exec(PersistenceSchema.users.filter(_.id === userId).map(x => x.role).result).head
    val isAdmin: Boolean = userRole == UserRole.CommunityAdmin.id.toShort || userRole == UserRole.SystemAdmin.id.toShort
    val organisation = getById(orgId)
    exec(saveOrganisationParameterValues(orgId, organisation.get.community, isAdmin, values).transactionally)
  }
}