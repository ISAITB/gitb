package managers

import com.gitb.utils.HmacUtils
import config.Configurations
import controllers.util.{ParameterExtractor, RequestWithAttributes}
import exceptions.UnauthorizedAccessException
import models.Enums.{SelfRegistrationType, UserRole}
import models._
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.profile.ProfileManager
import org.pac4j.play.PlayWebContext
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.Files
import play.api.mvc.{AnyContent, MultipartFormData}
import utils.RepositoryUtils

import javax.inject.{Inject, Singleton}
import scala.collection.mutable

object AuthorizationManager {
  val AUTHORIZATION_CHECKED = "AUTH_CHECKED"
}

@Singleton
class AuthorizationManager @Inject()(dbConfigProvider: DatabaseConfigProvider,
                                     accountManager: AccountManager,
                                     userManager: UserManager,
                                     communityManager: CommunityManager,
                                     specificationManager: SpecificationManager,
                                     conformanceManager: ConformanceManager,
                                     endpointManager: EndPointManager,
                                     testSuiteManager: TestSuiteManager,
                                     systemManager: SystemManager,
                                     organizationManager: OrganizationManager,
                                     testCaseManager: TestCaseManager,
                                     errorTemplateManager: ErrorTemplateManager,
                                     landingPageManager: LandingPageManager,
                                     legalNoticeManager: LegalNoticeManager,
                                     triggerManager: TriggerManager,
                                     communityResourceManager: CommunityResourceManager,
                                     parameterManager: ParameterManager,
                                     testResultManager: TestResultManager,
                                     actorManager: ActorManager,
                                     repositoryUtils: RepositoryUtils,
                                     playSessionStore: SessionStore
                                    ) extends BaseManager(dbConfigProvider) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthorizationManager])

  def canViewOrganisationAutomationKeys(request: RequestWithAttributes[_], organisationId: Long): Boolean = {
    val ok = canViewOrganisation(request, organisationId) && Configurations.AUTOMATION_API_ENABLED
    setAuthResult(request, ok, "User not allowed to view the organisation's automation keys")
  }

  def canLookupConformanceBadge(request: RequestWithAttributes[_]): Boolean = {
    setAuthResult(request, ok = true, "User not allowed to lookup conformance badges")
  }

  private def checkApiKeyUpdateForOrganisation(request: RequestWithAttributes[_], organisation: Option[Organizations]): Boolean = {
    var ok = false
    if (Configurations.AUTOMATION_API_ENABLED) {
      val userId = getRequestUserId(request)
      val userInfo = getUser(userId)
      if (isTestBedAdmin(userInfo)) {
        // Anything goes for the Test Bed admin.
        ok = true
      } else if (userInfo.organization.isDefined) {
        if (organisation.isDefined) {
          if (isCommunityAdmin(userInfo)) {
            // The community admin can edit any settings within her community even if API usage is currently
            // not allowed for community members.
            if (organisation.get.community == userInfo.organization.get.community) {
              ok = true
            }
          } else if (isOrganisationAdmin(userInfo)
            && userInfo.organization.get.id == organisation.get.id
            && communityManager.checkCommunityAllowsAutomationApi(userInfo.organization.get.community)) {
            // The organisation admin needs to be updating her own organisation and be part of a community that allows
            // the automation API.
            ok = true
          }
        }
      }
    }
    ok
  }

  def canUpdateSystemApiKey(request: RequestWithAttributes[_], systemId: Long): Boolean = {
    val organisation = organizationManager.getOrganizationBySystemId(systemId)
    val ok = checkApiKeyUpdateForOrganisation(request, Some(organisation))
    setAuthResult(request, ok, "User not allowed to update system API key")
  }

  def canUpdateOrganisationApiKey(request: RequestWithAttributes[_], organisationId: Long): Boolean = {
    val organisation = organizationManager.getById(organisationId)
    val ok = checkApiKeyUpdateForOrganisation(request, organisation)
    setAuthResult(request, ok, "User not allowed to update organisation API key")
  }

  def canManageSessionsThroughAutomationApi(request: RequestWithAttributes[_]): Boolean = {
    var ok = false
    if (Configurations.AUTOMATION_API_ENABLED) {
      val apiKey = request.headers.get(Constants.AutomationHeader)
      if (apiKey.isDefined) {
        // Check to see that the api key identifies an organisation in a community that allows API usage.
        val organisation = organizationManager.getByApiKey(apiKey.get)
        if (organisation.isDefined) {
          val community = communityManager.getById(organisation.get.community)
          if (community.isDefined && community.get.allowAutomationApi) {
            ok = true
          }
        }
      }
    }
    setAuthResult(request, ok, "You are not allowed to manage test sessions through the automation API")
  }

  def canManageTestSuitesThroughAutomationApi(request: RequestWithAttributes[_]): Boolean = {
    var ok = false
    if (Configurations.AUTOMATION_API_ENABLED) {
      val apiKey = request.headers.get(Constants.AutomationHeader)
      if (apiKey.isDefined) {
        // Check to see that the API key identifies a community that allows API usage.
        val community = communityManager.getByApiKey(apiKey.get)
        if (community.isDefined) {
          ok = true
        }
      }
    }
    setAuthResult(request, ok, "You are not allowed to manage test suites through the automation API")
  }

  def canSelfRegister(request: RequestWithAttributes[_], organisation: Organizations, organisationAdmin: Users, selfRegToken: Option[String], templateId: Option[Long]): Boolean = {
    var ok = false
    if (Configurations.REGISTRATION_ENABLED && checkHasPrincipal(request, skipForNonSSO = true)) {
      val targetCommunity = communityManager.getById(organisation.community)
      if (targetCommunity.isDefined) {
        var communityOk = false
        if (targetCommunity.get.selfRegType == SelfRegistrationType.PublicListing.id.toShort) {
          communityOk = true
        } else if (targetCommunity.get.selfRegType == SelfRegistrationType.PublicListingWithToken.id.toShort) {
          if (selfRegToken.isDefined) {
            communityOk = true
          }
        }
        if (communityOk) {
          if (templateId.isDefined) {
            val targetTemplate = organizationManager.getById(templateId.get)
            if (targetTemplate.isDefined && targetTemplate.get.community == targetCommunity.get.id && targetTemplate.get.template) {
              ok = true
            }
          } else {
            ok = true
          }
        }
      }
      setAuthResult(request, ok, "User not allowed to self-register with the provided configuration")
    }
    ok
  }

  def canViewBreadcrumbLabels(request: RequestWithAttributes[_]): Boolean = {
    val ok = checkIsAuthenticated(request)
    setAuthResult(request, ok, "User not allowed to view breadcrumbs")
  }

  def canViewSelfRegistrationOptions(request: RequestWithAttributes[_]): Boolean = {
    val ok = Configurations.REGISTRATION_ENABLED
    setAuthResult(request, ok, "User not allowed to view self-registration options")
  }

  def canMigrateAccount(request: RequestWithAttributes[AnyContent]) = {
    var ok = checkHasPrincipal(request, false)
    if (Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD) {
      ok = true
    }
    setAuthResult(request, ok, "Account migration not allowed")
  }

  def canLinkFunctionalAccount(request: RequestWithAttributes[_], userId: Long): Boolean = {
    var ok = false
    val principal = getPrincipal(request)
    if (principal != null) {
      val user = userManager.getUserById(userId)
      if (user.isDefined) {
        ok = user.get.ssoEmail.isDefined && user.get.ssoEmail.get.toLowerCase == principal.email.toLowerCase
      }
    }
    setAuthResult(request, ok, "You cannot access the requested account")
  }

  def canDisconnectFunctionalAccount(request: RequestWithAttributes[AnyContent]): Boolean = {
    canSelectFunctionalAccount(request, getRequestUserId(request))
  }

  def canViewUserFunctionalAccounts(request: RequestWithAttributes[AnyContent]): Boolean = {
    checkHasPrincipal(request, false)
  }

  def canSelectFunctionalAccount(request: RequestWithAttributes[AnyContent], id: Long): Boolean = {
    var ok = false
    if (Configurations.DEMOS_ENABLED && Configurations.DEMOS_ACCOUNT == id) {
      ok = true
    } else {
      val principal = getPrincipal(request)
      if (principal != null) {
        val user = userManager.getUserById(id)
        if (user.isDefined) {
          ok = user.get.ssoUid.isDefined && user.get.ssoUid.get == principal.uid
        }
      }
    }
    setAuthResult(request, ok, "You cannot access the requested account")
  }

  def canViewActors(request: RequestWithAttributes[AnyContent], ids: Option[List[Long]]): Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      if (ids.isDefined) {
        val domainLinkedToUser = getVisibleDomainForUser(userInfo)
        if (domainLinkedToUser.isDefined) {
          var domainIds: Set[Long] = Set()
          ids.get.foreach { actorId =>
            val actor = actorManager.getById(actorId)
            if (actor.isDefined) {
              domainIds += actor.get.domain
            }
          }
          if (domainIds.nonEmpty) {
            if (domainIds.size == 1) {
              ok = domainIds.head == domainLinkedToUser.get
            } else {
              ok = false
            }
          } else {
            ok = true
          }
        } else {
          ok = true
        }
      } else {
        ok = true
      }
    }
    setAuthResult(request, ok, "User cannot view the requested actor")
  }

  def canViewDomainsBySystemId(request: RequestWithAttributes[AnyContent], systemId: Long): Boolean = {
    canViewSystem(request, systemId)
  }

  def canViewSpecificationsBySystemId(request: RequestWithAttributes[AnyContent], systemId: Long): Boolean = {
    canViewSystem(request, systemId)
  }

  def canViewActorsBySystemId(request: RequestWithAttributes[AnyContent], systemId: Long): Boolean = {
    canViewSystem(request, systemId)
  }

  def canViewActorsByDomainId(request: RequestWithAttributes[AnyContent], domainId: Long): Boolean = {
    canViewDomains(request, Some(List(domainId)))
  }

  def canViewSystemsByCommunityId(request: RequestWithAttributes[AnyContent], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canViewTestCasesByCommunityId(request: RequestWithAttributes[AnyContent], communityId: Long): Boolean = {
    canManageCommunity(request, communityId)
  }

  def canViewTestCasesBySystemId(request: RequestWithAttributes[AnyContent], systemId: Long): Boolean = {
    canViewSystem(request, systemId)
  }

  def canViewTestSuitesBySystemId(request: RequestWithAttributes[AnyContent], systemId: Long): Boolean = {
    canViewSystem(request, systemId)
  }

  def canViewTestSuitesByCommunityId(request: RequestWithAttributes[AnyContent], communityId: Long): Boolean = {
    canManageCommunity(request, communityId)
  }

  def canDeleteOrganisationUser(request: RequestWithAttributes[_], userId: Long):Boolean = {
    canUpdateOrganisationUser(request, userId)
  }

  def canDeleteAdministrator(request: RequestWithAttributes[_], userId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      val targetUserOrganisation = getUserOrganisation(userId).get
      ok = targetUserOrganisation.community == userInfo.organization.get.community
    }
    setAuthResult(request, ok, "User cannot delete the requested administrator")
  }

  def canUpdateOrganisationUser(request: RequestWithAttributes[_], userId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      val user = userManager.getById(userId)
      ok = canManageOrganisationFull(request, userInfo, user.organization)
    } else if (isOrganisationAdmin(userInfo)) {
      if (!Configurations.DEMOS_ENABLED || Configurations.DEMOS_ACCOUNT != userInfo.id) {
        val user = userManager.getById(userId)
        if (userInfo.organization.isDefined && user.organization == userInfo.organization.get.id) {
          ok = canUpdateOwnOrganisation(request, ignoreExistingTests = true)
        }
      }
    }
    setAuthResult(request, ok, "User cannot manage the requested user")
  }

  def canUpdateCommunityAdministrator(request: RequestWithAttributes[_], userId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      // The target admin will be in the same organisation
      val user = userManager.getById(userId)
      ok = userInfo.organization.isDefined && user.organization == userInfo.organization.get.id
    }
    setAuthResult(request, ok, "User cannot manage the requested administrator")
  }

  def canUpdateTestBedAdministrator(request: RequestWithAttributes[_]):Boolean = {
    checkTestBedAdmin(request)
  }

  def canCreateOrganisationUser(request: RequestWithAttributes[_], orgId: Long):Boolean = {
    canManageOrganisationFull(request, getUser(getRequestUserId(request)), orgId)
  }

  def canCreateCommunityAdministrator(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canCreateTestBedAdministrator(request: RequestWithAttributes[_]):Boolean = {
    checkTestBedAdmin(request)
  }

  def canViewUser(request: RequestWithAttributes[_], userId: Long):Boolean = {
    canUpdateOrganisationUser(request, userId)
  }

  def canViewOwnOrganisationUser(request: RequestWithAttributes[_], userId: Long): Boolean = {
    checkIsAuthenticated(request)
  }

  def canViewOrganisationUsers(request: RequestWithAttributes[_], orgId: Long):Boolean = {
    canManageOrganisationFull(request, getUser(getRequestUserId(request)), orgId)
  }

  def canViewCommunityAdministrators(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canViewTestBedAdministrators(request: RequestWithAttributes[_]):Boolean = {
    checkTestBedAdmin(request)
  }

  def canDownloadTestSuite(request: RequestWithAttributes[_], testSuiteId: Long):Boolean = {
    canManageTestSuite(request, testSuiteId)
  }

  def canViewAllTestSuites(request: RequestWithAttributes[_]): Boolean = {
    canViewAllTestResources(request)
  }

  def canManageTestSuite(request: RequestWithAttributes[_], testSuiteId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      val testSuite = testSuiteManager.getById(testSuiteId)
      if (testSuite.isDefined) {
        ok = canManageDomain(request, userInfo, testSuite.get.domain)
      }
    }
    setAuthResult(request, ok, "User cannot manage the requested test suite")
  }

  def canDeleteTestSuite(request: RequestWithAttributes[_], testSuiteId: Long):Boolean = {
    canManageTestSuite(request, testSuiteId)
  }

  def canExecuteTestSession(request: RequestWithAttributes[_], sessionId: String, requireAdmin: Boolean = false):Boolean = {
    canManageTestSession(request, sessionId, requireAdmin)
  }

  def canExecuteTestCase(request: RequestWithAttributes[_], test_id: String):Boolean = {
    canViewTestCase(request, test_id)
  }

  def canExecuteTestCases(request: RequestWithAttributes[AnyContent], testCaseIds: List[Long], specId: Long, systemId: Long, actorId: Long): Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      val specIds = testCaseManager.getSpecificationsOfTestCases(testCaseIds)
      if (specIds.nonEmpty && specIds.contains(specId)) {
        ok = canViewSpecifications(request, userInfo, Some(specIds.toList)) && canViewSystemsById(request, userInfo, Some(List(systemId)))
      }
    }
    setAuthResult(request, ok, "Cannot view the requested test case(s)")
  }

  def canGetBinaryFileMetadata(request: RequestWithAttributes[_]):Boolean = {
    checkIsAuthenticated(request)
  }

  private def containsAll(toCheck: List[Long], toLookIn: Set[Long]): Boolean = {
    toCheck.foreach(valueToCheck => {
      if (!toLookIn(valueToCheck)) {
        return false
      }
    })
    true
  }

  def canViewSystemsById(request: RequestWithAttributes[_], userInfo: User, systemIds: Option[List[Long]]):Boolean = {
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      if (systemIds.isDefined) {
        if (isCommunityAdmin(userInfo)) {
          // All the system's should be in organisations in the user's community.
          val systems = systemManager.getSystems(systemIds)
          systems.foreach(system => {
            ok = canViewOrganisation(request, userInfo, system.owner)
          })
        } else {
          // The systems should be in the user's organisation.
          if (userInfo.organization.isDefined) {
            val orgSystemIds = systemManager.getSystemIdsForOrganization(userInfo.organization.get.id)
            ok = containsAll(systemIds.get, orgSystemIds)
          }
        }
      }
    }
    setAuthResult(request, ok, "User can't view the requested system(s)")
  }

  def canViewSystemsById(request: RequestWithAttributes[_], systemIds: Option[List[Long]]):Boolean = {
    canViewSystemsById(request, getUser(getRequestUserId(request)), systemIds)
  }

  def canViewSystems(request: RequestWithAttributes[_], userInfo: User, orgId: Long):Boolean = {
    canViewOrganisation(request, userInfo, orgId)
  }

  def canViewSystems(request: RequestWithAttributes[_], orgId: Long, snapshotId: Option[Long] = None):Boolean = {
    var ok = false
    if (orgId >= 0) {
      ok = canViewSystems(request, getUser(getRequestUserId(request)), orgId)
    } else if (snapshotId.isDefined) {
      // Deleted organisation present in a snapshot
      ok = canManageConformanceSnapshot(request, snapshotId.get)
    }
    setAuthResult(request, ok, "User cannot view the systems of this organisation")
  }

  def canViewEndpointConfigurationsForSystem(request: RequestWithAttributes[_], system: Long):Boolean = {
    canViewSystem(request, system)
  }

  def canManageSystemButCanAlsoEditParameter(request: RequestWithAttributes[_], userInfo: User, parameterId: Long): Boolean = {
    var ok = false
    if (isCommunityAdmin(userInfo) || isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      val parameter = parameterManager.getParameterById(parameterId)
      ok = parameter.isDefined && !parameter.get.adminOnly
    }
    setAuthResult(request, ok, "User cannot edit this parameter")
  }

  private def canManageEndpointConfiguration(request: RequestWithAttributes[_], systemId: Long, endpointId: Long, parameterId: Long): Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      val system = systemManager.getSystemById(systemId)
      if (system.isDefined) {
        if (isCommunityAdmin(userInfo)) {
          ok = isOwnSystem(userInfo, system) || canManageOrganisationFull(request, userInfo, system.get.owner)
        } else if (isOrganisationAdmin(userInfo)) {
          if (isOwnSystem(userInfo, system)) {
            val parameter = parameterManager.getParameterById(parameterId)
            if (parameter.isDefined && !parameter.get.adminOnly) {
              val community = communityManager.getById(userInfo.organization.get.community)
              if (community.isDefined) {
                if (community.get.allowPostTestStatementUpdates) {
                  ok = true
                } else {
                  val endpoint = endpointManager.getById(endpointId)
                  ok = !testResultManager.testSessionsExistForSystemAndActors(systemId, List(endpoint.actor))
                }
              }
            }
          }
        }
      }
    }
    setAuthResult(request, ok, "User cannot manage provided parameters")
  }

  def canEditEndpointConfiguration(request: RequestWithAttributes[_], config: Configs):Boolean = {
    canManageEndpointConfiguration(request, config.system, config.endpoint, config.parameter)
  }

  def canDeleteEndpointConfiguration(request: RequestWithAttributes[_], systemId: Long, endpointId: Long, parameterId: Long):Boolean = {
    canManageEndpointConfiguration(request, systemId, endpointId, parameterId)
  }

  def canViewEndpointConfigurations(request: RequestWithAttributes[_], systemId: Long, endpointId: Long):Boolean = {
    canViewSystem(request, systemId)
  }

  def canDeleteConformanceStatement(request: RequestWithAttributes[_], systemId: Long, actorIds: Option[List[Long]]):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      // Own system or within community.
      val system = systemManager.getSystemById(systemId)
      if (system.isDefined) {
        ok = isOwnSystem(userInfo, system) || canManageOrganisationFull(request, userInfo, system.get.owner)
      }
    } else if (isOrganisationAdmin(userInfo)) {
      val community = communityManager.getById(userInfo.organization.get.community)
      if (community.isDefined && community.get.allowStatementManagement) {
        // Has to be own system.
        if (isOwnSystem(userInfo, systemId)) {
          if (community.get.allowPostTestStatementUpdates) {
            ok = true
          } else {
            if (actorIds.isDefined && actorIds.get.nonEmpty) {
              ok = !testResultManager.testSessionsExistForSystemAndActors(systemId, actorIds.get)
            } else {
              ok = true
            }
          }
        }
      }
    }
    setAuthResult(request, ok, "User cannot delete the requested statement")
  }

  def canViewConformanceStatements(request: RequestWithAttributes[_], systemId: Long, snapshotId: Option[Long]):Boolean = {
    if (snapshotId.isEmpty) {
      canViewSystem(request, systemId)
    } else {
      canViewSystemInSnapshot(request, systemId, snapshotId.get)
    }
  }

  def canCreateConformanceStatement(request: RequestWithAttributes[_], sut_id: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      // Own system or within community.
      val system = systemManager.getSystemById(sut_id)
      if (system.isDefined) {
        ok = isOwnSystem(userInfo, system) || canManageOrganisationFull(request, userInfo, system.get.owner)
      }
    } else {
      val community = communityManager.getById(userInfo.organization.get.community)
      if (isOrganisationAdmin(userInfo) && community.isDefined && community.get.allowStatementManagement) {
        // Has to be own system.
        ok = isOwnSystem(userInfo, sut_id)
      }
    }
    setAuthResult(request, ok, "User cannot manage the requested statement")
  }

  def canViewSystem(request: RequestWithAttributes[_], sut_id: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      // Own system or within community.
      val system = systemManager.getSystemById(sut_id)
      if (system.isDefined) {
        ok = isOwnSystem(userInfo, system) || canManageOrganisationFull(request, userInfo, system.get.owner)
      }
    } else {
      // Has to be own system.
      ok = isOwnSystem(userInfo, sut_id)
    }
    setAuthResult(request, ok, "User cannot view the requested system")
  }

  def canViewSystemInSnapshot(request: RequestWithAttributes[_], systemId: Long, snapshotId: Long): Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      if (userInfo.organization.isDefined) {
        if (isCommunityAdmin(userInfo)) {
          // This is a snapshot of the user's own community.
          val snapshot = conformanceManager.getConformanceSnapshot(snapshotId)
          ok = snapshot.community == userInfo.organization.get.community
        } else {
          // The requested system ID and the user's own organisation ID must exist as results in the specified snapshot.
          ok = conformanceManager.existsInConformanceSnapshot(snapshotId, systemId, userInfo.organization.get.id)
        }
      }
    }
    setAuthResult(request, ok, "User cannot view the requested system in the given conformance snapshot")
  }

  def canUpdateSystem(request: RequestWithAttributes[_], systemId: Long, isDelete: Boolean = false):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      // Own system or within community.
      val system = systemManager.getSystemById(systemId)
      if (system.isDefined) {
        ok = isOwnSystem(userInfo, system) || canManageOrganisationFull(request, userInfo, system.get.owner)
      }
    } else if (isOrganisationAdmin(userInfo) && userInfo.organization.isDefined) {
      // Has to be own system.
      if (isOwnSystem(userInfo, systemId)) {
        // Check to see if special permissions are in place.
        val community = communityManager.getById(userInfo.organization.get.community)
        if (community.isDefined) {
          if (!isDelete || community.get.allowSystemManagement) {
            if (community.get.allowPostTestSystemUpdates) {
              ok = true
            } else {
              ok = !testResultManager.testSessionsExistForSystem(systemId)
            }
          }
        }
      }
    }
    setAuthResult(request, ok, "User cannot update the requested system")
  }

  private def isOwnSystem(userInfo: User, system: Option[Systems]): Boolean = {
    system.isDefined && userInfo.organization.isDefined && system.get.owner == userInfo.organization.get.id
  }

  private def isOwnSystem(userInfo: User, systemId: Long): Boolean = {
    isOwnSystem(userInfo, systemManager.getSystemById(systemId))
  }

  def canManageSystem(request: RequestWithAttributes[_], systemId: Long): Boolean = {
    canManageSystem(request, getUser(getRequestUserId(request)), systemId)
  }

  def canManageSystem(request: RequestWithAttributes[_], userInfo: User, sut_id: Long): Boolean = {
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      // Own system or within community.
      val system = systemManager.getSystemById(sut_id)
      if (system.isDefined) {
        ok = isOwnSystem(userInfo, system) || canManageOrganisationFull(request, userInfo, system.get.owner)
      }
    } else if (isOrganisationAdmin(userInfo)) {
      // Has to be own system.
      ok = isOwnSystem(userInfo, sut_id)
    }
    setAuthResult(request, ok, "User cannot manage the requested system")
  }

  def canCreateSystem(request: RequestWithAttributes[_], orgId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      ok = userInfo.organization.isDefined && userInfo.organization.get.id == orgId
      if (!ok) {
        val org = organizationManager.getById(orgId)
        ok = org.isDefined && userInfo.organization.isDefined && org.get.community == userInfo.organization.get.community
      }
    } else {
      val community = communityManager.getById(userInfo.organization.get.community)
      if (isOrganisationAdmin(userInfo) && community.isDefined && community.get.allowSystemManagement) {
        ok = userInfo.organization.isDefined && userInfo.organization.get.id == orgId
      }
    }
    setAuthResult(request, ok, "User cannot manage the requested organisation")
  }

  def canDeleteSystem(request: RequestWithAttributes[_], systemId: Long):Boolean = {
    canUpdateSystem(request, systemId, isDelete = true)
  }

  def canManageThemes(request: RequestWithAttributes[_]): Boolean = {
    checkTestBedAdmin(request)
  }

  def canAccessThemeData(request: RequestWithAttributes[_]):Boolean = {
    val ok = true
    setAuthResult(request, ok, "User cannot access theme")
  }

  def canUpdateSystemConfigurationValues(request: RequestWithAttributes[_]): Boolean = {
    checkTestBedAdmin(request)
  }

  def canManageSystemSettings(request: RequestWithAttributes[_]): Boolean = {
    checkTestBedAdmin(request)
  }

  def canViewSystemConfigurationValues(request: RequestWithAttributes[_]): Boolean = {
    checkTestBedAdmin(request)
  }

  def canManageAnyTestSession(request: RequestWithAttributes[_]):Boolean = {
    checkTestBedAdmin(request)
  }

  def canUpdateSpecification(request: RequestWithAttributes[_], specId: Long):Boolean = {
    canManageSpecification(request, specId)
  }

  def canDeleteSpecification(request: RequestWithAttributes[_], specId: Long):Boolean = {
    canManageSpecification(request, specId)
  }

  def canViewAllTestCases(request: RequestWithAttributes[_]):Boolean = {
    canViewAllTestResources(request)
  }

  def canViewAllTestResources(request: RequestWithAttributes[_]): Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (userInfo.organization.isDefined) {
      val communityInfo = communityManager.getById(userInfo.organization.get.community)
      ok = communityInfo.isDefined && communityInfo.get.domain.isEmpty
    }
    setAuthResult(request, ok, "User not allowed to view all test resources")
  }

  def canViewTestSuiteResource(request: RequestWithAttributes[_], testId: String):Boolean = {
    var ok = false
    if (isTestEngineCall(request)) {
      ok = checkValidTestEngineCall(request, testId)
    }
    setAuthResult(request, ok, "Not allowed to access requested resource")
  }

  def canViewTestCase(request: RequestWithAttributes[_], testId: String): Boolean = {
    var ok = false
    if (isTestEngineCall(request)) {
      ok = checkValidTestEngineCall(request, testId)
    } else {
      val userInfo = getUser(getRequestUserId(request))
      if (isTestBedAdmin(userInfo)) {
        ok = true
      } else if (isCommunityAdmin(userInfo)) {
        val testSuite = testSuiteManager.getTestSuiteOfTestCase(testId.toLong)
        ok = canManageDomain(request, userInfo, testSuite.domain)
      } else {
        val specId = conformanceManager.getSpecificationIdForTestCaseFromConformanceStatements(testId.toLong)
        if (specId.isDefined) {
          ok = canViewSpecifications(request, userInfo, Some(List(specId.get)))
        }
      }
    }
    setAuthResult(request, ok, "Cannot view requested test case")
  }

  def canPreviewConformanceCertificateReport(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canViewConformanceCertificateReport(request: RequestWithAttributes[_], communityId: Long, snapshotId: Option[Long] = None):Boolean = {
    if (snapshotId.isDefined) {
      canManageConformanceSnapshot(request, snapshotId.get)
    } else {
      canManageCommunity(request, communityId)
    }
  }

  def canViewOwnConformanceCertificateReport(request: RequestWithAttributes[_], systemId: Long, snapshotId: Option[Long] = None):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      if (isCommunityAdmin(userInfo)) {
        val communityIdOfSystem = systemManager.getCommunityIdOfSystem(systemId)
        if (userInfo.organization.isDefined && userInfo.organization.get.community == communityIdOfSystem) {
          ok = true
        }
      } else {
        // Organisation user.
        if (userInfo.organization.isDefined) {
          val community = communityManager.getById(userInfo.organization.get.community)
          if (community.isDefined && community.get.allowCertificateDownload) {
            val system = systemManager.getSystemById(systemId)
            if (system.isDefined && userInfo.organization.get.id == system.get.owner) {
              if (snapshotId.isDefined) {
                val snapshot = conformanceManager.getConformanceSnapshot(snapshotId.get)
                ok = snapshot.community == community.get.id && snapshot.isPublic
              } else {
                ok = true
              }
            }
          }
        }
      }
    }
    setAuthResult(request, ok, "User cannot download this conformance certificate")
  }

  def canViewConformanceStatementReport(request: RequestWithAttributes[_], systemId: Long, snapshotId: Option[Long] = None):Boolean = {
    if (snapshotId.isDefined) {
      canManageConformanceSnapshot(request, snapshotId.get)
    } else {
      canViewSystem(request, systemId)
    }
  }

  def canManageTestSession(request: RequestWithAttributes[_], sessionId: String, requireAdmin: Boolean): Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      // Within community.
      val result = testResultManager.getCommunityIdForTestSession(sessionId)
      if (result.isDefined) {
        if (result.get._2.isDefined) {
          // There is a community ID defined. This would mean an executing or completed session.
          ok = canManageCommunity(request, userInfo, result.get._2.get)
        } else {
          /*
           Existing session but without a community ID. This can only come up if the community has been deleted.
           In such a case only the test bed admin should be able to see this.
            */
          ok = false
        }
      } else {
          /*
          There is no test session recorded for this session ID. This could be because the test session is currently
          being configured.
           */
        ok = true
      }
    } else {
      // Own test session.
      if (requireAdmin) {
        ok = false
      } else {
        val result = testResultManager.getOrganisationIdForTestSession(sessionId)
        if (result.isDefined) {
          if (result.get._2.isDefined) {
            // There is an organisation ID defined. This needs to match the user's organisation ID.
            ok = userInfo.organization.isDefined && result.get._2.get == userInfo.organization.get.id
          } else {
            // No organisation ID. This is an obsolete session no longer visible to the user.
            ok = false
          }
        } else {
          /*
          There is no test session recorded for this session ID. This could be because the test session is currently
          being configured.
           */
          ok = true
        }
      }
    }
    setAuthResult(request, ok, "User cannot manage requested session")
  }

  def canViewTestResultForSession(request: RequestWithAttributes[_], sessionId: String):Boolean = {
    canManageTestSession(request, sessionId, requireAdmin = false)
  }

  def canViewTestResultsForCommunity(request: RequestWithAttributes[_], communityIds: Option[List[Long]]):Boolean = {
    canViewCommunities(request, communityIds)
  }

  def canViewTestResultsForOrganisation(request: RequestWithAttributes[_], organisationId: Long):Boolean = {
    canViewOrganisation(request, organisationId)
  }

  def canUpdateParameter(request: RequestWithAttributes[_], parameterId: Long):Boolean = {
    canManageParameter(request, parameterId)
  }

  def canManageParameter(request: RequestWithAttributes[_], parameterId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      val parameter = parameterManager.getParameterById(parameterId)
      if (parameter.isDefined) {
        ok = canManageEndpoint(request, userInfo, parameter.get.endpoint)
      }
    }
    setAuthResult(request, ok, "User cannot manage parameter")
  }

  def canDeleteParameter(request: RequestWithAttributes[_], parameterId: Long):Boolean = {
    canManageParameter(request, parameterId)
  }

  def canDeleteOrganisation(request: RequestWithAttributes[_], orgId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo) || isCommunityAdmin(userInfo)) {
      ok = canManageOrganisationFull(request, userInfo, orgId)
    }
    setAuthResult(request, ok, "User cannot delete the requested organisation")
  }

  def canUpdateOrganisation(request: RequestWithAttributes[_], orgId: Long):Boolean = {
    canManageOrganisationFull(request, getUser(getRequestUserId(request)), orgId)
  }

  def canManageOrganisationBasic(request: RequestWithAttributes[_], orgId: Long):Boolean = {
    canManageOrganisationBasic(request, getUser(getRequestUserId(request)), orgId)
  }

  def canManageOrganisationBasic(request: RequestWithAttributes[_], userInfo: User, orgId: Long):Boolean = {
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      ok = userInfo.organization.isDefined && userInfo.organization.get.id == orgId
      if (!ok) {
        val org = organizationManager.getById(orgId)
        ok = org.isDefined && userInfo.organization.isDefined && org.get.community == userInfo.organization.get.community
      }
    } else if (isOrganisationAdmin(userInfo)) {
      ok = userInfo.organization.isDefined && userInfo.organization.get.id == orgId
    }
    setAuthResult(request, ok, "User cannot manage the requested organisation")
  }

  def canManageOrganisationFull(request: RequestWithAttributes[_], userInfo: User, orgId: Long):Boolean = {
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      ok = userInfo.organization.isDefined && userInfo.organization.get.id == orgId
      if (!ok) {
        val org = organizationManager.getById(orgId)
        ok = org.isDefined && userInfo.organization.isDefined && org.get.community == userInfo.organization.get.community
      }
    }
    setAuthResult(request, ok, "User cannot manage the requested organisation")
  }

  def canCreateOrganisation(request: RequestWithAttributes[_], organisation: Organizations, otherOrganisation: Option[Long]):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      // Check we can manage the community of the organisation as well as the template organisation.
      ok = canManageCommunity(request, userInfo, organisation.community)
      if (ok && otherOrganisation.isDefined) {
        val other = organizationManager.getById(otherOrganisation.get)
        ok = other.isDefined && other.get.community == organisation.community
      }
    }
    setAuthResult(request, ok, "User cannot create the provided organisation")
  }

  def canViewOrganisationsByCommunity(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canViewOrganisation(request: RequestWithAttributes[_], userInfo: User, orgId: Long):Boolean = {
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      ok = userInfo.organization.isDefined && userInfo.organization.get.id == orgId
      if (!ok && isCommunityAdmin(userInfo)) {
        ok = canManageOrganisationFull(request, userInfo, orgId)
      }
    }
    setAuthResult(request, ok, "User cannot view the requested organisation")
  }

  def canViewOrganisation(request: RequestWithAttributes[_], orgId: Long):Boolean = {
    canViewOrganisation(request, getUser(getRequestUserId(request)), orgId)
  }

  def canViewAllOrganisations(request: RequestWithAttributes[_]):Boolean = {
    checkTestBedAdmin(request)
  }

  def canViewTestBedDefaultLegalNotice(request: RequestWithAttributes[_]):Boolean = {
    val ok = true
    setAuthResult(request, ok, "User cannot view the default landing page")
  }

  def canViewDefaultLegalNotice(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canViewCommunityBasic(request, communityId)
  }

  def canManageLegalNotice(request: RequestWithAttributes[_], noticeId: Long):Boolean = {
    val load = () => {legalNoticeManager.getCommunityId(noticeId)}
    canManageCommunityArtifact(request, getUser(getRequestUserId(request)), load)
  }

  def canManageLegalNotices(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canViewDefaultLandingPage(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canViewCommunityBasic(request, communityId)
  }

  def canManageLandingPage(request: RequestWithAttributes[_], pageId: Long):Boolean = {
    val load = () => {landingPageManager.getCommunityId(pageId)}
    canManageCommunityArtifact(request, getUser(getRequestUserId(request)), load)
  }

  def canManageLandingPages(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canManageTrigger(request: RequestWithAttributes[_], triggerId: Long):Boolean = {
    val load = () => {triggerManager.getCommunityId(triggerId)}
    canManageCommunityArtifact(request, getUser(getRequestUserId(request)), load)
  }

  def canManageTriggers(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canManageCommunityResource(request: RequestWithAttributes[_], resourceId: Long): Boolean = {
    val load = () => {
      communityResourceManager.getCommunityId(resourceId)
    }
    canManageCommunityArtifact(request, getUser(getRequestUserId(request)), load)
  }

  def canViewDefaultErrorTemplate(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canViewCommunityBasic(request, communityId)
  }

  def canManageErrorTemplate(request: RequestWithAttributes[_], templateId: Long):Boolean = {
    val load = () => {errorTemplateManager.getCommunityId(templateId)}
    canManageCommunityArtifact(request, getUser(getRequestUserId(request)), load)
  }

  def canManageCommunityArtifact(request: RequestWithAttributes[_], userInfo: User, loadFunction: () => Long): Boolean = {
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      ok = canManageCommunity(request, userInfo, loadFunction.apply())
    }
    setAuthResult(request, ok, "User cannot manage the requested community")
  }

  def canManageErrorTemplates(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canManageEndpoints(request: RequestWithAttributes[_], userInfo: User, ids: Option[List[Long]]):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      if (ids.isDefined) {
        ids.get.foreach( endpointId => {
          val endpoint = endpointManager.getById(endpointId)
          ok = canManageActor(request, userInfo, endpoint.actor)
        })
      }
    }
    setAuthResult(request, ok, "User cannot manage requested endpoint")
  }

  def canManageEndpoint(request: RequestWithAttributes[_], userInfo: User, endPointId: Long):Boolean = {
    canManageEndpoints(request, userInfo, Some(List(endPointId)))
  }

  def canUpdateEndpoint(request: RequestWithAttributes[_], endPointId: Long):Boolean = {
    canManageEndpoint(request, getUser(getRequestUserId(request)), endPointId)
  }

  def canDeleteEndpoint(request: RequestWithAttributes[_], endPointId: Long):Boolean = {
    canManageEndpoint(request, getUser(getRequestUserId(request)), endPointId)
  }

  def canViewOwnCommunity(request: RequestWithAttributes[_]):Boolean = {
    checkIsAuthenticated(request)
  }

  def canDeleteCommunity(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    checkTestBedAdmin(request)
  }

  def canUpdateCommunity(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, getUser(getRequestUserId(request)), communityId)
  }

  def canViewCommunityBasic(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      ok = isOwnCommunity(userInfo, communityId) || communityId == Constants.DefaultCommunityId
    } else {
      ok = isOwnCommunity(userInfo, communityId)
    }
    setAuthResult(request, ok, "User cannot view the requested community")
  }

  private def isOwnCommunity(userInfo: User, communityId: Long): Boolean = {
    userInfo.organization.isDefined && userInfo.organization.get.community == communityId
  }

  def canViewCommunityFull(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canCreateCommunity(request: RequestWithAttributes[_]):Boolean = {
    checkTestBedAdmin(request)
  }

  def canViewCommunities(request: RequestWithAttributes[_], communityIds: Option[List[Long]]):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      // There should be only a single community requested.
      if (communityIds.isDefined && communityIds.get.size == 1) {
        ok = canManageCommunity(request, userInfo, communityIds.get.head)
      }
    }
    setAuthResult(request, ok, "User cannot view the requested community")
  }

  def canLogout(request: RequestWithAttributes[_]):Boolean = {
    val ok = true
    setAuthResult(request, ok, "User cannot logout")
  }

  def canCheckAnyUserEmail(request: RequestWithAttributes[_]):Boolean = {
    val userInfo = getUser(getRequestUserId(request))
    setAuthResult(request, isTestBedAdmin(userInfo) || isCommunityAdmin(userInfo) || isOrganisationAdmin((userInfo)), "User cannot check verify user data")
  }

  def canCheckUserEmail(request: RequestWithAttributes[_], organisationId: Long):Boolean = {
    val userInfo = getUser(getRequestUserId(request))
    val ok = isAnyAdminType(userInfo) && userInfo.organization.isDefined && userInfo.organization.get.id == organisationId
    setAuthResult(request, ok, "User cannot manage own organisation")
  }

  def canCheckSystemAdminEmail(request: RequestWithAttributes[AnyContent]):Boolean = {
    checkTestBedAdmin(request)
  }

  def canCheckCommunityAdminEmail(request: RequestWithAttributes[AnyContent], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canCheckOrganisationUserEmail(request: RequestWithAttributes[AnyContent], organisationId: Long):Boolean = {
    canManageOrganisationFull(request, getUser(getRequestUserId(request)), organisationId)
  }

  def canLogin(request: RequestWithAttributes[_]):Boolean = {
    val ok = true
    setAuthResult(request, ok, "User not allowed to login")
  }

  def canUpdateActor(request: RequestWithAttributes[_], actorId: Long):Boolean = {
    canManageActor(request, actorId)
  }

  def canDeleteActor(request: RequestWithAttributes[_], actorId: Long):Boolean = {
    canManageActor(request, actorId)
  }

  def canSubmitFeedback(request: RequestWithAttributes[_]):Boolean = {
    val ok = true
    setAuthResult(request, ok, "User not allowed to submit feedback")
  }

  def canViewConfiguration(request: RequestWithAttributes[_]):Boolean = {
    val ok = true
    setAuthResult(request, ok, "User not allowed to view configuration")
  }

  def canUpdateOwnProfile(request: RequestWithAttributes[_]):Boolean = {
    var ok = checkIsAuthenticated(request)
    if (ok) {
      val userId = getRequestUserId(request)
      ok = !Configurations.AUTHENTICATION_SSO_ENABLED &&
        (!Configurations.DEMOS_ENABLED || userId != Configurations.DEMOS_ACCOUNT)
    }
    setAuthResult(request, ok, "User cannot edit own profile")
  }

  def canViewOwnProfile(request: RequestWithAttributes[_]):Boolean = {
    checkIsAuthenticated(request)
  }

  def canCreateUserInOwnOrganisation(request: RequestWithAttributes[_]):Boolean = {
    canUpdateOwnOrganisation(request, ignoreExistingTests = true)
  }

  def canViewOwnOrganisationnUsers(request: RequestWithAttributes[_]):Boolean = {
    checkIsAuthenticated(request)
  }

  def canUpdateOwnOrganisationAndLandingPage(request: RequestWithAttributes[_], landingPageId: Option[Long]):Boolean = {
    canUpdateOwnOrganisation(request, ignoreExistingTests = false) && (landingPageId.isEmpty || landingPageId.get == -1 || canManageLandingPage(request, landingPageId.get))
  }

  def canUpdateOwnOrganisation(request: RequestWithAttributes[_], ignoreExistingTests: Boolean):Boolean = {
    var ok = false
    val userId = getRequestUserId(request)
    if (!Configurations.DEMOS_ENABLED || userId != Configurations.DEMOS_ACCOUNT) {
      val userInfo = getUser(userId)
      if (isTestBedAdmin(userInfo) || isCommunityAdmin(userInfo)) {
        ok = true
      } else if (isOrganisationAdmin(userInfo) && userInfo.organization.isDefined) {
        if (ignoreExistingTests) {
          ok = true
        } else {
          val community = communityManager.getById(userInfo.organization.get.community)
          if (community.isDefined) {
            if (community.get.allowPostTestOrganisationUpdates) {
              ok = true
            } else {
              ok = !testResultManager.testSessionsExistForOrganisation(userInfo.organization.get.id)
            }
          }
        }
      }
    }
    setAuthResult(request, ok, "User cannot manage own organisation")
  }

  def canViewOwnOrganisation(request: RequestWithAttributes[_]):Boolean = {
    checkIsAuthenticated(request)
  }

  def canUpdateConformanceCertificateSettings(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canViewConformanceCertificateSettings(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canCheckPendingTestSessionInteractions(request: RequestWithAttributes[_], communityId: Option[Long]): Boolean = {
    if (communityId.isEmpty) {
      checkTestBedAdmin(request)
    } else {
      canManageCommunity(request, communityId.get)
    }
  }

  def canManageCommunity(request: RequestWithAttributes[_], userInfo: User, communityId: Long): Boolean = {
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo) && userInfo.organization.isDefined && userInfo.organization.get.community == communityId) {
      ok = true
    }
    setAuthResult(request, ok, "User cannot manage the requested community")
  }

  def canViewConformanceSnapshot(request: RequestWithAttributes[_], snapshotId: Long): Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (userInfo.organization.isDefined) {
      val snapshot = conformanceManager.getConformanceSnapshot(snapshotId)
      if (isCommunityAdmin(userInfo)) {
        ok = snapshot.community == userInfo.organization.get.community
      } else {
        ok = snapshot.community == userInfo.organization.get.community && snapshot.isPublic
      }
    }
    setAuthResult(request, ok, "User cannot view the requested conformance snapshot")
  }

  def canManageConformanceSnapshot(request: RequestWithAttributes[_], snapshotId: Long): Boolean = {
    canManageConformanceSnapshot(request, getUser(getRequestUserId(request)), snapshotId)
  }

  private def canManageConformanceSnapshot(request: RequestWithAttributes[_], userInfo: User, snapshotId: Long): Boolean = {
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo) && userInfo.organization.isDefined) {
      val snapshot = conformanceManager.getConformanceSnapshot(snapshotId)
      ok = snapshot.community == userInfo.organization.get.community
    }
    setAuthResult(request, ok, "User cannot manage the requested conformance snapshot")
  }

  def canManageCommunity(request: RequestWithAttributes[_], communityId: Long): Boolean = {
    canManageCommunity(request, getUser(getRequestUserId(request)), communityId)
  }

  def canManageOrganisationParameter(request: RequestWithAttributes[_], parameterId: Long): Boolean = {
    val parameter = communityManager.getOrganisationParameterById(parameterId)
    var ok = false
    if (parameter.isDefined) {
      ok = canManageCommunity(request, parameter.get.community)
    }
    setAuthResult(request, ok, "User cannot manage the requested parameter")
  }

  def canManageSystemParameter(request: RequestWithAttributes[_], parameterId: Long): Boolean = {
    val parameter = communityManager.getSystemParameterById(parameterId)
    var ok = false
    if (parameter.isDefined) {
      ok = canManageCommunity(request, parameter.get.community)
    }
    setAuthResult(request, ok, "User cannot manage the requested parameter")
  }

  def canDeleteObsoleteTestResultsForCommunity(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canDeleteObsoleteTestResultsForOrganisation(request: RequestWithAttributes[_], organisationId: Long):Boolean = {
    canManageOrganisationBasic(request, organisationId)
  }

  def canDeleteTestResults(request: RequestWithAttributes[_], communityId: Option[Long]):Boolean = {
    if (communityId.isDefined) {
      canManageCommunity(request, communityId.get)
    } else {
      checkTestBedAdmin(request)
    }
  }

  def canDeleteAllObsoleteTestResults(request: RequestWithAttributes[_]):Boolean = {
    checkTestBedAdmin(request)
  }

  def canViewConformanceOverview(request: RequestWithAttributes[_], communityIds: Option[List[Long]], snapshotId: Option[Long] = None):Boolean = {
    if (communityIds.isEmpty || (communityIds.isDefined && communityIds.get.size > 1)) {
      checkTestBedAdmin(request)
    } else {
      // Single community. This can also be a community admin.
      if (snapshotId.isDefined) {
        canManageConformanceSnapshot(request, snapshotId.get)
      } else {
        canManageCommunity(request, communityIds.get.head)
      }
    }
  }

  def canManageDomainParameters(request: RequestWithAttributes[_], domainId: Long):Boolean = {
    canManageDomain(request, domainId)
  }

  def canViewDomainParametersForCommunity(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canViewTestSuiteByTestCaseId(request: RequestWithAttributes[_], testCaseId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      val testSuite = testSuiteManager.getTestSuiteOfTestCase(testCaseId)
      ok = canManageDomain(request, userInfo, testSuite.domain)
    } else {
      // There must be a conformance statement for this.
      val specId = conformanceManager.getSpecificationIdForTestCaseFromConformanceStatements(testCaseId)
      if (specId.isDefined) {
        ok = canViewSpecifications(request, userInfo, Some(List(specId.get)))
      }
    }
    setAuthResult(request, ok, "User cannot view the requested test suite")
  }

  def canViewTestSuite(request: RequestWithAttributes[_], testSuiteId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      val testSuite = testSuiteManager.getById(testSuiteId)
      if (testSuite.isDefined) {
        ok = canManageDomain(request, userInfo, testSuite.get.domain)
      }
    } else {
      // There must be a conformance statement for this.
      val specId = conformanceManager.getSpecificationIdForTestSuiteFromConformanceStatements(testSuiteId)
      if (specId.isDefined) {
        ok = canViewSpecifications(request, userInfo, Some(List(specId.get)))
      }
    }
    setAuthResult(request, ok, "User doesn't have access to the requested test suite")
  }

  def canViewConformanceStatus(request: RequestWithAttributes[_], actorId: Long, sutId: Long, snapshotId: Option[Long] = None):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      if (snapshotId.isDefined) {
        ok = canManageConformanceSnapshot(request, userInfo, snapshotId.get)
      } else {
        ok = canViewSystemsById(request, userInfo, Some(List(sutId)))
      }
    }
    setAuthResult(request, ok, "User cannot view the requested conformance status")
  }

  def canEditTestSuitesMulti(request: RequestWithAttributes[MultipartFormData[Files.TemporaryFile]], specification_id: Long):Boolean = {
    canManageSpecification(request, specification_id)
  }

  def canEditTestSuite(request: RequestWithAttributes[AnyContent], testSuiteId: Long):Boolean = {
    val testSuite = testSuiteManager.getById(testSuiteId)
    canManageDomain(request, testSuite.get.domain)
  }

  def canEditTestCase(request: RequestWithAttributes[AnyContent], testCaseId: Long):Boolean = {
    val testSuite = testSuiteManager.getTestSuiteOfTestCase(testCaseId)
    canManageDomain(request, testSuite.domain)
  }

  def canEditTestSuites(request: RequestWithAttributes[_], specification_id: Long):Boolean = {
    canManageSpecification(request, specification_id)
  }

  def canDeleteDomain(request: RequestWithAttributes[_], domain_id: Long):Boolean = {
    checkTestBedAdmin(request)
  }

  def canViewEndpointsById(request: RequestWithAttributes[_], ids: Option[List[Long]]):Boolean = {
    canManageEndpoints(request, getUser(getRequestUserId(request)), ids)
  }

  def canViewEndpoints(request: RequestWithAttributes[_], actorId: Long):Boolean = {
    canViewActor(request, actorId)
  }

  def canCreateSpecification(request: RequestWithAttributes[_], domain: Long):Boolean = {
    canUpdateDomain(request, domain)
  }

  def canCreateParameter(request: RequestWithAttributes[_], endpointId: Long):Boolean = {
    val endpoint = endpointManager.getById(endpointId)
    canCreateEndpoint(request, endpoint.actor)
  }

  def canManageActor(request: RequestWithAttributes[_], userInfo: User, actor: Long):Boolean = {
    val spec = specificationManager.getSpecificationOfActor(actor)
    canManageDomain(request, userInfo, spec.domain)
  }

  def canManageActor(request: RequestWithAttributes[_], actor: Long):Boolean = {
    canManageActor(request, getUser(getRequestUserId(request)), actor)
  }

  def canCreateEndpoint(request: RequestWithAttributes[_], actor: Long):Boolean = {
    canManageActor(request, actor)
  }

  def canManageSpecification(request: RequestWithAttributes[_], userInfo: User, specificationId: Long): Boolean = {
    val spec = specificationManager.getSpecificationById(specificationId)
    canManageDomain(request, userInfo, spec.domain)
  }

  def canManageSpecification(request: RequestWithAttributes[_], specificationId: Long): Boolean = {
    val spec = specificationManager.getSpecificationById(specificationId)
    canManageDomain(request, spec.domain)
  }

  def canManageSpecificationGroup(request: RequestWithAttributes[_], groupId: Long): Boolean = {
    val group = specificationManager.getSpecificationGroupById(groupId)
    canManageDomain(request, group.domain)
  }

  def canManageSpecifications(request: RequestWithAttributes[_], specIds: List[Long]): Boolean = {
    val userInfo = getUser(getRequestUserId(request))
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      if (isCommunityAdmin(userInfo)) {
        val specDomainIds = mutable.Set[Long]()
        val specs = specificationManager.getSpecificationsById(specIds)
        specs.foreach(spec => specDomainIds += spec.domain)
        specDomainIds.foreach{ domainId =>
          canManageDomain(request, userInfo, domainId)
        }
        ok = true
      }
    }
    setAuthResult(request, ok, "User cannot manage the requested specifications")
  }


  def canCreateActor(request: RequestWithAttributes[_], specificationId: Long):Boolean = {
    canManageSpecification(request, specificationId)
  }

  def canManageDomain(request: RequestWithAttributes[_], userInfo: User, domainId: Long):Boolean = {
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      if (isCommunityAdmin(userInfo) && userInfo.organization.isDefined) {
        val communityDomain = getCommunityDomain(userInfo.organization.get.community)
        if (communityDomain.isDefined && communityDomain.get == domainId) {
          ok = true
        }
      }
    }
    setAuthResult(request, ok, "User cannot manage the requested domain")
  }

  def canManageDomain(request: RequestWithAttributes[_], domainId: Long):Boolean = {
    canManageDomain(request, getUser(getRequestUserId(request)), domainId)
  }

  def canApplySandboxDataMulti(request: RequestWithAttributes[MultipartFormData[Files.TemporaryFile]]): Boolean = {
    setAuthResult(request, Configurations.DATA_WEB_INIT_ENABLED && !repositoryUtils.getDataLockFile().exists(), "Web-based data initialisation is not enabled")
  }

  private def isCommunityAdmin(userInfo: User): Boolean = {
    userInfo.role == UserRole.CommunityAdmin.id.toShort
  }

  private def isOrganisationAdmin(userInfo: User): Boolean = {
    userInfo.role == UserRole.VendorAdmin.id.toShort
  }

  private def isTestBedAdmin(userInfo: User): Boolean = {
    userInfo.role == UserRole.SystemAdmin.id.toShort
  }

  private def isAnyAdminType(userInfo: User): Boolean = {
    isTestBedAdmin(userInfo) || isCommunityAdmin(userInfo) || isOrganisationAdmin(userInfo)
  }

  def canUpdateDomain(request: RequestWithAttributes[_], domainId: Long):Boolean = {
    canManageDomain(request, domainId)
  }

  def checkTestBedAdmin(request: RequestWithAttributes[_]): Boolean = {
    val ok = isTestBedAdmin(getRequestUserId(request))
    setAuthResult(request, ok, "Only test bed administrators can perform this operation")
  }

  def canCreateDomain(request: RequestWithAttributes[_]):Boolean = {
    checkTestBedAdmin(request)
  }

  def canViewActor(request: RequestWithAttributes[_], actor_id: Long): Boolean = {
    val specId = specificationManager.getSpecificationIdOfActor(actor_id)
    canViewSpecifications(request, Some(List(specId)))
  }

  def canViewTestCasesByActorId(request: RequestWithAttributes[_], actor_id: Long):Boolean = {
    canViewActor(request, actor_id)
  }

  def canViewTestSuitesBySpecificationId(request: RequestWithAttributes[_], spec_id: Long):Boolean = {
    canViewSpecifications(request, Some(List(spec_id)))
  }

  def canViewActorsBySpecificationId(request: RequestWithAttributes[_], spec_id: Long):Boolean = {
    canViewSpecifications(request, Some(List(spec_id)))
  }

  def canViewSpecificationsByDomainId(request: RequestWithAttributes[_], domain_id: Long):Boolean = {
    canViewDomains(request, Some(List(domain_id)))
  }

  def canViewActorsBySpecificationIds(request: RequestWithAttributes[_], ids: Option[List[Long]]):Boolean = {
    canViewSpecifications(request, ids)
  }

  private def specificationsMatchDomain(specs: Iterable[Specifications], domainId: Long): Boolean = {
    for (elem <- specs) {
      if (elem.domain != domainId) {
        return false
      }
    }
    true
  }

  def canViewSpecifications(request: RequestWithAttributes[_], userInfo: User, ids: Option[List[Long]]):Boolean = {
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      val userDomain = getVisibleDomainForUser(userInfo)
      if (userDomain.isDefined) {
        if (ids.isDefined && ids.get.nonEmpty) {
          val specs = specificationManager.getSpecifications(ids)
          ok = specificationsMatchDomain(specs, userDomain.get)
        }
      } else {
        ok = true
      }
    }
    setAuthResult(request, ok, "User doesn't have access to the requested specification(s)")
  }

  def canViewSpecifications(request: RequestWithAttributes[_], ids: Option[List[Long]]):Boolean = {
    canViewSpecifications(request, getUser(getRequestUserId(request)), ids)
  }

  def canViewDomainByCommunityId(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    var ok = false
    val userId = getRequestUserId(request)
    if (isTestBedAdmin(userId)) {
      ok = true
    } else {
      val userInfo = getUserOrganisation(userId)
      if (userInfo.isDefined && userInfo.get.community == communityId) {
        ok = true
      }
    }
    setAuthResult(request, ok, "User doesn't have access to the requested domain(s)")
  }

  def canViewDomainBySpecificationId(request: RequestWithAttributes[_], specId: Long):Boolean = {
    var ok = false
    val userId = getRequestUserId(request)
    if (isTestBedAdmin(userId)) {
      ok = true
    } else {
      val userDomain = getVisibleDomainForUser(userId)
      if (userDomain.isDefined) {
        val spec = specificationManager.getSpecificationById(specId)
        if (spec.domain == userDomain.get) {
          ok = true
        }
      } else {
        ok = true
      }
    }
    setAuthResult(request, ok, "User doesn't have access to the requested domain(s)")
  }

  def canViewDomains(request: RequestWithAttributes[_], userInfo: User, ids: Option[List[Long]]):Boolean = {
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else {
      // Users are either linked to a community or not (in which case they view all domains).
      val domainLinkedToUser = getVisibleDomainForUser(userInfo)
      if (domainLinkedToUser.isDefined) {
        // The list of domains should include only the user's domain.
        if (ids.isDefined && ids.get.size == 1 && ids.get.head == domainLinkedToUser.get) {
          ok = true
        }
      } else {
        ok = true
      }
    }
    setAuthResult(request, ok, "User doesn't have access to the requested domain(s)")
  }

  def canViewDomains(request: RequestWithAttributes[_], ids: Option[List[Long]]):Boolean = {
    canViewDomains(request, getUser(getRequestUserId(request)), ids)
  }

  def canPreviewDocumentation(request: RequestWithAttributes[_]):Boolean = {
    checkIsAuthenticated(request)
  }

  private def getUser(userId: Long): User = {
    val user = userManager.getUserById(userId)
    if (user.isEmpty) {
      throw UnauthorizedAccessException("User not found")
    } else {
      user.get
    }
  }

  private def getUserOrganisation(userId: Long): Option[Organizations] = {
    getUser(userId).organization
  }

  private def getCommunityDomain(communityId: Long): Option[Long] = {
    val community = communityManager.getById(communityId)
    if (community.isDefined) {
      community.get.domain
    } else {
      None
    }
  }

  private def getVisibleDomainForUser(userInfo: User): Option[Long] = {
    var userDomain: Option[Long] = null
    if (userInfo.organization.isDefined) {
      userDomain = getCommunityDomain(userInfo.organization.get.community)
    } else {
      userDomain = None
    }
    userDomain
  }

  private def getVisibleDomainForUser(userId: Long): Option[Long] = {
    getVisibleDomainForUser(getUser(userId))
  }

  private def checkIsAuthenticated(request: RequestWithAttributes[_]): Boolean = {
    getRequestUserId(request)
    var ok = false
    if (checkHasPrincipal(request, true)) {
      ok = true
      setAuthResult(request, ok, "User is not authenticated")
    }
    ok
  }

  private def isTestBedAdmin(userId: Long): Boolean = {
    accountManager.isSystemAdmin(userId)
  }

  private def checkValidTestEngineCall(request: RequestWithAttributes[_], testId: String): Boolean = {
    val ok = HmacUtils.isTokenValid(request.headers.get(HmacUtils.HMAC_HEADER_TOKEN).get, testId, request.headers.get(HmacUtils.HMAC_HEADER_TIMESTAMP).get)
    setAuthResult(request, ok, "Test engine call rejected due to invalid HMAC.")
  }

  private def isTestEngineCall(request: RequestWithAttributes[_]): Boolean = {
    request.headers.get(HmacUtils.HMAC_HEADER_TOKEN).isDefined && request.headers.get(HmacUtils.HMAC_HEADER_TIMESTAMP).isDefined
  }

  def getRequestUserId(request: RequestWithAttributes[_]): Long = {
    val userId = ParameterExtractor.extractOptionalUserId(request)
    if (userId.isEmpty) {
      throwError("User is not authenticated.")
      -1
    } else {
      userId.get
    }
  }

  private def throwError(message: String): Unit = {
    val error = UnauthorizedAccessException(message)
    logger.warn("Unauthorised access detected ["+message+"]", error)
    throw error
  }

  private def setAuthResult(request: RequestWithAttributes[_], ok: Boolean, message: String): Boolean = {
    if (!request.attributes.contains(AuthorizationManager.AUTHORIZATION_CHECKED)) {
      request.attributes += (AuthorizationManager.AUTHORIZATION_CHECKED -> "")
    }
    if (!ok) {
      throwError(message)
    }
    ok
  }

  def getAccountInfo(request: RequestWithAttributes[_]): ActualUserInfo = {
    val accountInfo = getPrincipal(request)
    val userAccounts = accountManager.getUserAccountsForUid(accountInfo.uid)
    val userInfo = new ActualUserInfo(accountInfo.uid, accountInfo.email, accountInfo.firstName, accountInfo.lastName, userAccounts)
    userInfo
  }

  def getPrincipal(request: RequestWithAttributes[_]): ActualUserInfo = {
    var userInfo: ActualUserInfo = null
    val webContext = new PlayWebContext(request)
    val profileManager = new ProfileManager(webContext, playSessionStore)
    val profile = profileManager.getProfile()
    if (profile.isEmpty) {
      logger.error("Lookup for a real user's data failed due to a missing profile.")
    } else {
      val uid = profile.get().getId
      val userAttributes = profile.get().getAttributes
      var email: String = null
      var firstName: String = null
      var lastName: String = null
      if (userAttributes != null) {
        email = userAttributes.get("email").asInstanceOf[String]
        firstName = userAttributes.get("firstName").asInstanceOf[String]
        lastName = userAttributes.get("lastName").asInstanceOf[String]
      }
      if (uid == null || email == null || firstName == null || lastName == null) {
        logger.error("User profile did not contain expected information [" + uid + "][" + email + "][" + firstName + "][" + lastName + "]")
      } else {
        userInfo = new ActualUserInfo(uid, email, firstName, lastName)
      }
    }
    userInfo
  }

  private def checkHasPrincipal(request: RequestWithAttributes[_], skipForNonSSO: Boolean): Boolean = {
    var ok = false
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      val principal = getPrincipal(request)
      if (principal != null) {
        ok = true
      }
    } else {
      if (skipForNonSSO) {
        ok = true
      } else {
        ok = false
      }
    }
    setAuthResult(request, ok, "User is not authenticated")
  }

}
