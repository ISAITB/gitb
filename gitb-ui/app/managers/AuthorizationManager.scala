package managers

import com.gitb.utils.HmacUtils
import controllers.util.{ParameterExtractor, RequestWithAttributes}
import exceptions.UnauthorizedAccessException
import javax.inject.{Inject, Singleton}
import models.Enums.UserRole
import models._
import org.slf4j.{Logger, LoggerFactory}
import persistence.AccountManager
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.Files
import play.api.mvc.{AnyContent, MultipartFormData}

object AuthorizationManager {
  val AUTHORIZATION_OK = "AUTH_OK"
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
                                     parameterManager: ParameterManager,
                                     testResultManager: TestResultManager
                                    ) extends BaseManager(dbConfigProvider) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthorizationManager])

  def canViewDomainsBySystemId(request: RequestWithAttributes[AnyContent], systemId: Long): Boolean = {
    canViewSystem(request, systemId)
  }

  def canViewSpecificationsBySystemId(request: RequestWithAttributes[AnyContent], systemId: Long): Boolean = {
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
    checkTestBedAdmin(request)
  }

  def canUpdateOrganisationUser(request: RequestWithAttributes[_], userId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      val user = userManager.getById(userId)
      ok = canManageOrganisationFull(request, userInfo, user.organization)
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
        ok = canManageSpecification(request, userInfo, testSuite.get.specification)
      }
    }
    setAuthResult(request, ok, "User cannot manage the requested test suite")
  }

  def canDeleteTestSuite(request: RequestWithAttributes[_], testSuiteId: Long):Boolean = {
    canManageTestSuite(request, testSuiteId)
  }

  def canExecuteTestSession(request: RequestWithAttributes[_], session_id: String):Boolean = {
    canManageTestSession(request, session_id)
  }

  def canExecuteTestCase(request: RequestWithAttributes[_], test_id: String):Boolean = {
    canViewTestCase(request, test_id)
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

  def canViewSystems(request: RequestWithAttributes[_], orgId: Long):Boolean = {
    canViewSystems(request, getUser(getRequestUserId(request)), orgId)
  }

  def canViewEndpointConfigurationsForSystem(request: RequestWithAttributes[_], system: Long):Boolean = {
    canViewSystem(request, system)
  }

  def canEditEndpointConfiguration(request: RequestWithAttributes[_], config: Configs):Boolean = {
    canManageSystem(request, getUser(getRequestUserId(request)), config.system)
  }

  def canDeleteEndpointConfiguration(request: RequestWithAttributes[_], systemId: Long, endpointId: Long):Boolean = {
    canManageSystem(request, getUser(getRequestUserId(request)), systemId)
  }

  def canViewEndpointConfigurations(request: RequestWithAttributes[_], systemId: Long, endpointId: Long):Boolean = {
    canViewSystem(request, systemId)
  }

  def canDeleteConformanceStatement(request: RequestWithAttributes[_], sut_id: Long):Boolean = {
    canManageSystem(request, getUser(getRequestUserId(request)), sut_id)
  }

  def canViewConformanceStatements(request: RequestWithAttributes[_], sut_id: Long):Boolean = {
    canViewSystem(request, sut_id)
  }

  def canCreateConformanceStatement(request: RequestWithAttributes[_], sut_id: Long):Boolean = {
    canManageSystem(request, getUser(getRequestUserId(request)), sut_id)
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

  def canUpdateSystem(request: RequestWithAttributes[_], sut_id: Long):Boolean = {
    canManageSystem(request, getUser(getRequestUserId(request)), sut_id)
  }

  private def isOwnSystem(userInfo: User, system: Option[Systems]): Boolean = {
    system.isDefined && userInfo.organization.isDefined && system.get.owner == userInfo.organization.get.id
  }

  private def isOwnSystem(userInfo: User, systemId: Long): Boolean = {
    isOwnSystem(userInfo, systemManager.getSystemById(systemId))
  }

  def canManageSystem(request: RequestWithAttributes[_], userInfo: User, sut_id: Long): Boolean = {
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
    } else if (isOrganisationAdmin(userInfo)) {
      // Has to be own system.
      ok = isOwnSystem(userInfo, sut_id)
    }
    setAuthResult(request, ok, "User cannot manage the requested system")
  }

  def canCreateSystem(request: RequestWithAttributes[_], orgId: Long):Boolean = {
    canManageOrganisationBasic(request, getUser(getRequestUserId(request)), orgId)
  }

  def canDeleteSystem(request: RequestWithAttributes[_], systemId: Long):Boolean = {
    canManageSystem(request, getUser(getRequestUserId(request)), systemId)
  }

  def canAccessThemeData(request: RequestWithAttributes[_]):Boolean = {
    val ok = true
    setAuthResult(request, ok, "User cannot access theme")
  }

  def canEditTheSessionAliveTime(request: RequestWithAttributes[_]):Boolean = {
    checkTestBedAdmin(request)
  }

  def canViewTheSessionAliveTime(request: RequestWithAttributes[_]):Boolean = {
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
    } else if (isCommunityAdmin(userInfo) && userInfo.organization.isDefined) {
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
        ok = canViewSpecifications(request, userInfo, Some(List(testSuite.specification)))
      } else {
        val specId = conformanceManager.getSpecificationIdForTestCaseFromConformanceStatements(testId.toLong)
        if (specId.isDefined) {
          ok = canViewSpecifications(request, userInfo, Some(List(specId.get)))
        }
      }
    }
    setAuthResult(request, ok, "Cannot view requested test case")
  }

  def canViewConformanceCertificateReport(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canViewConformanceStatementReport(request: RequestWithAttributes[_], systemId: String):Boolean = {
    canViewSystem(request, systemId.toLong)
  }

  def canManageTestSession(request: RequestWithAttributes[_], sessionId: String): Boolean = {
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
    setAuthResult(request, ok, "User cannot manage requested session")
  }

  def canViewTestResultForSession(request: RequestWithAttributes[_], sessionId: String):Boolean = {
    canManageTestSession(request, sessionId)
  }

  def canViewTestResultsForCommunity(request: RequestWithAttributes[_], communityIds: Option[List[Long]]):Boolean = {
    canViewCommunities(request, communityIds)
  }

  def canViewTestResultsForSystem(request: RequestWithAttributes[_], systemId: Long):Boolean = {
    canViewSystem(request, systemId)
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

  def canCheckUserEmail(request: RequestWithAttributes[_]):Boolean = {
    setAuthResult(request, isAnyAdminType(getUser(getRequestUserId(request))), "User cannot manage own organisation")
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
    checkIsAuthenticated(request)
  }

  def canViewConfiguration(request: RequestWithAttributes[_]):Boolean = {
    checkIsAuthenticated(request)
  }

  def canUpdateOwnProfile(request: RequestWithAttributes[_]):Boolean = {
    checkIsAuthenticated(request)
  }

  def canViewOwnProfile(request: RequestWithAttributes[_]):Boolean = {
    checkIsAuthenticated(request)
  }

  def canCreateUserInOwnOrganisation(request: RequestWithAttributes[_]):Boolean = {
    canUpdateOwnOrganisation(request)
  }

  def canViewOwnOrganisationnUsers(request: RequestWithAttributes[_]):Boolean = {
    checkIsAuthenticated(request)
  }

  def canUpdateOwnOrganisation(request: RequestWithAttributes[_]):Boolean = {
    setAuthResult(request, isAnyAdminType(getUser(getRequestUserId(request))), "User cannot manage own organisation")
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

  def canManageCommunity(request: RequestWithAttributes[_], userInfo: User, communityId: Long): Boolean = {
    var ok = false
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo) && userInfo.organization.isDefined && userInfo.organization.get.community == communityId) {
      ok = true
    }
    setAuthResult(request, ok, "User cannot manage the requested community")
  }

  def canManageCommunity(request: RequestWithAttributes[_], communityId: Long): Boolean = {
    canManageCommunity(request, getUser(getRequestUserId(request)), communityId)
  }

  def canDeleteObsoleteTestResultsForCommunity(request: RequestWithAttributes[_], communityId: Long):Boolean = {
    canManageCommunity(request, communityId)
  }

  def canDeleteObsoleteTestResultsForSystem(request: RequestWithAttributes[_], systemId: Long):Boolean = {
    canManageSystem(request, getUser(getRequestUserId(request)), systemId)
  }

  def canDeleteAllObsoleteTestResults(request: RequestWithAttributes[_]):Boolean = {
    checkTestBedAdmin(request)
  }

  def canViewConformanceOverview(request: RequestWithAttributes[_], communityIds: Option[List[Long]]):Boolean = {
    if (communityIds.isEmpty || (communityIds.isDefined && communityIds.get.size > 1)) {
      checkTestBedAdmin(request)
    } else {
      // Single community. This can also be a community admin.
      canManageCommunity(request, communityIds.get.head)
    }
  }

  def canManageDomainParameters(request: RequestWithAttributes[_], domainId: Long):Boolean = {
    canManageDomain(request, domainId)
  }

  def canViewTestSuiteByTestCaseId(request: RequestWithAttributes[_], testCaseId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
    } else if (isCommunityAdmin(userInfo)) {
      val testSuite = testSuiteManager.getTestSuiteOfTestCase(testCaseId)
      ok = canViewSpecifications(request, userInfo, Some(List(testSuite.specification)))
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
        ok = canViewSpecifications(request, userInfo, Some(List(testSuite.get.specification)))
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

  def canViewConformanceStatus(request: RequestWithAttributes[_], actorId: Long, sutId: Long):Boolean = {
    var ok = false
    val userInfo = getUser(getRequestUserId(request))
    if (isTestBedAdmin(userInfo)) {
      ok = true
      request.attributes += (AuthorizationManager.AUTHORIZATION_OK -> "")
    } else {
      ok = canViewSystemsById(request, userInfo, Some(List(sutId)))
    }
    setAuthResult(request, ok, "User cannot view the requested conformance status")
  }

  def canEditTestSuitesMulti(request: RequestWithAttributes[MultipartFormData[Files.TemporaryFile]], specification_id: Long):Boolean = {
    canManageSpecification(request, specification_id)
  }

  def canEditTestSuites(request: RequestWithAttributes[_], specification_id: Long):Boolean = {
    canManageSpecification(request, specification_id)
  }

  def canDeleteDomain(request: RequestWithAttributes[_], domain_id: Long):Boolean = {
    canUpdateDomain(request, domain_id)
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

  private def checkTestBedAdmin(request: RequestWithAttributes[_]): Boolean = {
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

  private def specificationsMatchDomain(specs: List[Specifications], domainId: Long): Boolean = {
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
          val specs = conformanceManager.getSpecifications(ids)
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

  def canViewDomains(request: RequestWithAttributes[_], ids: Option[List[Long]]):Boolean = {
    var ok = false
    val userId = getRequestUserId(request)
    if (isTestBedAdmin(userId)) {
      ok = true
    } else {
      // Users are either linked to a community or not (in which case they view all domains).
      val domainLinkedToUser = getVisibleDomainForUser(userId)
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

  private def getUser(userId: Long): User = {
    val user = userManager.getUserById(userId)
    user
  }

  private def getUserOrganisation(userId: Long): Option[Organizations] = {
    val user = getUser(userId)
    user.organization
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
    val ok = true
    setAuthResult(request, ok, "User is not authenticated")
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

  private def getRequestUserId(request: RequestWithAttributes[_]): Long = {
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
    if (ok) {
      if (!request.attributes.contains(AuthorizationManager.AUTHORIZATION_OK)) {
        request.attributes += (AuthorizationManager.AUTHORIZATION_OK -> "")
      }
    } else {
      throwError(message)
    }
    ok
  }
}