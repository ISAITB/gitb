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

import authentication.ProfileResolver
import com.gitb.utils.HmacUtils
import config.Configurations
import controllers.util.{ParameterExtractor, RequestWithAttributes}
import exceptions.UnauthorizedAccessException
import models.Enums.{SelfRegistrationType, UserRole}
import models._
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.Files
import play.api.mvc.{AnyContent, MultipartFormData, RequestHeader}
import utils.RepositoryUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
                                     systemConfigurationManager: SystemConfigurationManager,
                                     domainManager: DomainManager,
                                     repositoryUtils: RepositoryUtils,
                                     profileResolver: ProfileResolver)
                                    (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AuthorizationManager])

  def canCheckCoreServiceHealth(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canViewOrganisationAutomationKeys(request: RequestWithAttributes[_], organisationId: Long): Future[Boolean] = {
    val check = if (Configurations.AUTOMATION_API_ENABLED) {
      canViewOrganisation(request, organisationId)
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User not allowed to view the organisation's automation keys"))
  }

  def canViewOrganisationAutomationKeysInSnapshot(request: RequestWithAttributes[_], organisationId: Long, snapshotId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      canViewOrganisation(request, userInfo, organisationId).flatMap { _ =>
        canViewConformanceSnapshot(request, userInfo, snapshotId)
      }
    }
    check.map(setAuthResult(request, _, "User not allowed to view the organisation's automation keys"))
  }

  def canLookupConformanceBadge(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = Future.successful(true)
    check.map(setAuthResult(request, _, "User not allowed to lookup conformance badges"))
  }

  private def checkApiKeyUpdateForOrganisation(request: RequestWithAttributes[_], organisation: Option[Organizations]): Future[Boolean] = {
    if (Configurations.AUTOMATION_API_ENABLED) {
      getUser(getRequestUserId(request)).flatMap { userInfo =>
        if (isTestBedAdmin(userInfo)) {
          // Anything goes for the Test Bed admin.
          Future.successful(true)
        } else if (userInfo.organization.isDefined) {
          if (organisation.isDefined) {
            if (isCommunityAdmin(userInfo)) {
              // The community admin can edit any settings within her community even if API usage is currently
              // not allowed for community members.
              Future.successful(organisation.get.community == userInfo.organization.get.community)
            } else if (isOrganisationAdmin(userInfo) && userInfo.organization.get.id == organisation.get.id) {
              // The organisation admin needs to be updating her own organisation and be part of a community that allows
              // the automation API.
              communityManager.checkCommunityAllowsAutomationApi(userInfo.organization.get.community)
            } else {
              Future.successful(false)
            }
          } else {
            Future.successful(false)
          }
        } else {
          Future.successful(false)
        }
      }
    } else {
      Future.successful(false)
    }
  }

  def canUpdateSystemApiKey(request: RequestWithAttributes[_], systemId: Long): Future[Boolean] = {
    val check = organizationManager.getOrganizationBySystemId(systemId).flatMap { organisation =>
      checkApiKeyUpdateForOrganisation(request, Some(organisation))
    }
    check.map(setAuthResult(request, _, "User not allowed to update system API key"))
  }

  def canUpdateOrganisationApiKey(request: RequestWithAttributes[_], organisationId: Long): Future[Boolean] = {
    val check = organizationManager.getById(organisationId).flatMap { organisation =>
      checkApiKeyUpdateForOrganisation(request, organisation)
    }
    check.map(setAuthResult(request, _, "User not allowed to update organisation API key"))
  }

  def canOrganisationUseAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = if (Configurations.AUTOMATION_API_ENABLED) {
      val apiKey = request.headers.get(Constants.AutomationHeader)
      if (apiKey.isDefined) {
        organizationManager.getByApiKey(apiKey.get).flatMap { organisation =>
          if (organisation.isDefined) {
            communityManager.getById(organisation.get.community).map { community =>
              community.isDefined && community.get.allowAutomationApi
            }
          } else {
            Future.successful(false)
          }
        }
      } else {
        Future.successful(false)
      }
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "You are not allowed to manage test sessions through the automation API"))
  }

  def canManageConfigurationThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = restApiEnabledAndValidCommunityKeyDefined(request)
    check.map(setAuthResult(request, _, "You are not allowed to manage configuration properties through the automation API"))
  }

  private def restApiEnabledAndValidCommunityKeyDefined(request: RequestWithAttributes[_]): Future[Boolean] = {
    if (Configurations.AUTOMATION_API_ENABLED) {
      val apiKey = request.headers.get(Constants.AutomationHeader)
      if (apiKey.isDefined) {
        // Validate the community API key.
        communityManager.getByApiKey(apiKey.get).map { community =>
          community.isDefined
        }
      } else {
        Future.successful(false)
      }
    } else {
      Future.successful(false)
    }
  }

  private def restApiEnabledAndValidMasterKeyDefined(request: RequestWithAttributes[_]): Future[Boolean] = {
    if (Configurations.AUTOMATION_API_ENABLED) {
      val apiKey = request.headers.get(Constants.AutomationHeader)
      if (apiKey.isDefined) {
        // Check to see that the API key is the master API key.
        systemConfigurationManager.getSystemConfigurationAsync(Constants.RestApiAdminKey).map { masterApiKey =>
          masterApiKey.flatMap(_.parameter).isDefined && masterApiKey.get.parameter.get == apiKey.get
        }
      } else {
        Future.successful(false)
      }
    } else {
      Future.successful(false)
    }
  }

  def canManageActorThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = restApiEnabledAndValidCommunityKeyDefined(request)
    check.map(setAuthResult(request, _, "You are not allowed to manage actors through the automation API"))
  }

  def canManageSpecificationThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = restApiEnabledAndValidCommunityKeyDefined(request)
    check.map(setAuthResult(request, _, "You are not allowed to manage specifications through the automation API"))
  }

  def canManageSpecificationGroupThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = restApiEnabledAndValidCommunityKeyDefined(request)
    check.map(setAuthResult(request, _, "You are not allowed to manage specification groups through the automation API"))
  }

  def canManageOrganisationThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = restApiEnabledAndValidCommunityKeyDefined(request)
    check.map(setAuthResult(request, _, "You are not allowed to manage organisations through the automation API"))
  }

  def canManageSystemThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = restApiEnabledAndValidCommunityKeyDefined(request)
    check.map(setAuthResult(request, _, "You are not allowed to manage systems through the automation API"))
  }

  def canManageAnyCommunityThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = restApiEnabledAndValidMasterKeyDefined(request)
    check.map(setAuthResult(request, _, "You are not allowed to perform system-level operations through the automation API"))
  }

  def canManageCommunityThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = restApiEnabledAndValidCommunityKeyDefined(request)
    check.map(setAuthResult(request, _, "You are not allowed to manage communities through the automation API"))
  }

  def canCreateDomainThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = restApiEnabledAndValidMasterKeyDefined(request)
    check.map(setAuthResult(request, _, "You are not allowed to perform system-level operations through the automation API"))
  }

  def canCreateCommunityThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = restApiEnabledAndValidMasterKeyDefined(request)
    check.map(setAuthResult(request, _, "You are not allowed to perform system-level operations through the automation API"))
  }

  def canDeleteDomainThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    canCreateDomainThroughAutomationApi(request)
  }

  def canDeleteCommunityThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    canCreateCommunityThroughAutomationApi(request)
  }

  def canUpdateDomainThroughAutomationApi(request: RequestWithAttributes[_], domainKey: Option[String]): Future[Boolean] = {
    val check = if (Configurations.AUTOMATION_API_ENABLED) {
      val apiKey = request.headers.get(Constants.AutomationHeader)
      if (apiKey.isDefined) {
        systemConfigurationManager.getSystemConfigurationAsync(Constants.RestApiAdminKey).flatMap { masterApiKey =>
          if (masterApiKey.flatMap(_.parameter).isDefined && masterApiKey.get.parameter.get == apiKey.get) {
            // The API key matches the master API key.
            Future.successful(true)
          } else {
            // The API key must match a community.
            communityManager.getByApiKey(apiKey.get).flatMap { community =>
              if (community.isDefined) {
                if (community.get.domain.isDefined) {
                  // We can only update the domain linked to the community.
                  if (domainKey.isDefined) {
                    // The provided domain key must match the key of the community's domain.
                    domainManager.getDomainByIdAsync(community.get.domain.get).map { domain =>
                      domainKey.get == domain.apiKey
                    }
                  } else {
                    // We implicitly match the community's domain.
                    Future.successful(true)
                  }
                } else {
                  // We can update any domain.
                  Future.successful(true)
                }
              } else {
                Future.successful(false)
              }
            }
          }
        }
      } else {
        Future.successful(false)
      }
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "You are not allowed to update this domain through the automation API"))
  }

  def canManageTestSuitesThroughAutomationApi(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = restApiEnabledAndValidCommunityKeyDefined(request)
    check.map(setAuthResult(request, _, "You are not allowed to manage test suites through the automation API"))
  }

  def canSelfRegister(request: RequestWithAttributes[_], organisation: Organizations, selfRegToken: Option[String], templateId: Option[Long]): Future[Boolean] = {
    val check = checkHasPrincipal(request, skipForNonSSO = true).flatMap { hasPrincipal =>
      if (Configurations.REGISTRATION_ENABLED && hasPrincipal) {
        communityManager.getById(organisation.community).flatMap { targetCommunity =>
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
                organizationManager.getById(templateId.get).map { targetTemplate =>
                  targetTemplate.isDefined && targetTemplate.get.community == targetCommunity.get.id && targetTemplate.get.template
                }
              } else {
                Future.successful(true)
              }
            } else {
              Future.successful(false)
            }
          } else {
            Future.successful(false)
          }
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User not allowed to self-register with the provided configuration"))
  }

  def canViewBreadcrumbLabels(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = checkIsAuthenticated(request)
    check.map(setAuthResult(request, _, "User not allowed to view breadcrumbs"))
  }

  def canViewSelfRegistrationOptions(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = Future.successful(Configurations.REGISTRATION_ENABLED)
    check.map(setAuthResult(request, _, "User not allowed to view self-registration options"))
  }

  def canMigrateAccount(request: RequestWithAttributes[AnyContent]): Future[Boolean] = {
    val check = checkHasPrincipal(request, skipForNonSSO = false).map { hasPrincipal =>
      hasPrincipal && Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD
    }
    check.map(setAuthResult(request, _, "Account migration not allowed"))
  }

  def canLinkFunctionalAccount(request: RequestWithAttributes[_], userId: Long): Future[Boolean] = {
    val check = getPrincipal(request).flatMap { principal =>
      if (principal != null) {
        userManager.getUserByIdAsync(userId).map { user =>
          user.isDefined && user.get.ssoEmail.isDefined && user.get.ssoEmail.get.toLowerCase == principal.email.toLowerCase
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "You cannot access the requested account"))
  }

  def canDisconnectFunctionalAccount(request: RequestWithAttributes[AnyContent]): Future[Boolean] = {
    canSelectFunctionalAccount(request, getRequestUserId(request))
  }

  def canViewUserFunctionalAccounts(request: RequestWithAttributes[AnyContent]): Future[Boolean] = {
    checkHasPrincipal(request, skipForNonSSO = false)
  }

  def canSelectFunctionalAccount(request: RequestWithAttributes[AnyContent], id: Long): Future[Boolean] = {
    val check = if (Configurations.DEMOS_ENABLED && Configurations.DEMOS_ACCOUNT == id) {
      Future.successful(true)
    } else {
      getPrincipal(request).flatMap { principal =>
        if (principal != null) {
          userManager.getUserByIdAsync(id).map { user =>
            user.isDefined && user.get.ssoUid.isDefined && user.get.ssoUid.get == principal.uid
          }
        } else {
          Future.successful(false)
        }
      }
    }
    check.map(setAuthResult(request, _, "You cannot access the requested account"))
  }

  def canViewActors(request: RequestWithAttributes[AnyContent], ids: Option[List[Long]]): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else {
        if (ids.isDefined) {
          getVisibleDomainForUser(userInfo).flatMap { domainLinkedToUser =>
            if (domainLinkedToUser.isDefined) {
              actorManager.getActorDomainIds(ids.get).map { domainIds =>
                if (domainIds.nonEmpty) {
                  domainIds.size == 1 && domainIds.head == domainLinkedToUser.get
                } else {
                  true
                }
              }
            } else {
              Future.successful(true)
            }
          }
        } else {
          Future.successful(true)
        }
      }
    }
    check.map(setAuthResult(request, _, "User cannot view the requested actor"))
  }

  def canViewActorsByDomainId(request: RequestWithAttributes[AnyContent], domainId: Long): Future[Boolean] = {
    canViewDomains(request, Some(List(domainId)))
  }

  private def canViewCommunityContent(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo) || isCommunityAdmin(userInfo)) {
        canManageCommunity(request, userInfo, communityId)
      } else {
        // Organisation users can view all systems in the community if enabled as a permission.
        organisationUserCanViewCommunityContent(request, userInfo, communityId)
      }
    }
  }

  private def organisationUserCanViewCommunityContent(request: RequestWithAttributes[_], userInfo: User, communityId: Long): Future[Boolean] = {
    val check = if (userInfo.organization.get.community == communityId) {
      // Requested community ID matches the community of the user.
      communityManager.getById(communityId).map { community =>
        // The comm
        community.isDefined && community.get.allowCommunityView
      }
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User cannot view the requested community"))
  }

  def canViewSystemsByCommunityId(request: RequestWithAttributes[AnyContent], communityId: Long): Future[Boolean] = {
    canViewCommunityContent(request, communityId)
  }

  def canDeleteOrganisationUser(request: RequestWithAttributes[_], userId: Long): Future[Boolean] = {
    canUpdateOrganisationUser(request, userId)
  }

  def canDeleteAdministrator(request: RequestWithAttributes[_], userId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        getUserOrganisation(userId).map { targetUserOrganisation =>
          targetUserOrganisation.exists(_.community == userInfo.organization.get.community)
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot delete the requested administrator"))
  }

  def canUpdateOrganisationUser(request: RequestWithAttributes[_], userId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        userManager.getById(userId).flatMap { user =>
          canManageOrganisationFull(request, userInfo, user.organization)
        }
      } else if (isOrganisationAdmin(userInfo)) {
        if (!Configurations.DEMOS_ENABLED || Configurations.DEMOS_ACCOUNT != userInfo.id) {
          userManager.getById(userId).flatMap { user =>
            if (userInfo.organization.isDefined && user.organization == userInfo.organization.get.id) {
              canUpdateOwnOrganisation(request, ignoreExistingTests = true)
            } else {
              Future.successful(false)
            }
          }
        } else {
          Future.successful(false)
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested user"))
  }

  def canUpdateCommunityAdministrator(request: RequestWithAttributes[_], userId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        // The target admin will be in the same organisation
        userManager.getById(userId).map { user =>
          userInfo.organization.isDefined && user.organization == userInfo.organization.get.id
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested administrator"))
  }

  def canUpdateTestBedAdministrator(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canCreateOrganisationUser(request: RequestWithAttributes[_], orgId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageOrganisationFull(request, userInfo, orgId)
    }
  }

  def canCreateCommunityAdministrator(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  def canCreateTestBedAdministrator(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canViewUser(request: RequestWithAttributes[_], userId: Long): Future[Boolean] = {
    canUpdateOrganisationUser(request, userId)
  }

  def canViewOwnOrganisationUser(request: RequestWithAttributes[_], userId: Long): Future[Boolean] = {
    checkIsAuthenticated(request)
  }

  def canViewOrganisationUsers(request: RequestWithAttributes[_], orgId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageOrganisationFull(request, userInfo, orgId)
    }
  }

  def canViewCommunityAdministrators(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  def canViewTestBedAdministrators(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canDownloadTestSuite(request: RequestWithAttributes[_], testSuiteId: Long): Future[Boolean] = {
    canManageTestSuite(request, testSuiteId)
  }

  def canViewAllTestSuites(request: RequestWithAttributes[_]): Future[Boolean] = {
    canViewAllTestResources(request)
  }

  def canMoveTestSuite(request: RequestWithAttributes[_], testSuiteId: Long, specificationId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        // Test Bed admin can do anything.
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        for {
          domainIdFromTestSuiteLookup <- testSuiteManager.getTestSuiteDomain(testSuiteId)
          domainIdFromSpecificationLookup <- domainManager.getDomainOfSpecification(specificationId).map(_.id)
          result <- {
            if (domainIdFromTestSuiteLookup == domainIdFromSpecificationLookup) {
              // For a community admin the test suite's domain must match the target specification's domain, and the domain must be manageable by the community admin.
              canManageDomain(request, domainIdFromTestSuiteLookup)
            } else {
              Future.successful(false)
            }
          }
        } yield result
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot move the requested test suite to the target specification"))
  }

  def canManageTestSuite(request: RequestWithAttributes[_], testSuiteId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        testSuiteManager.getById(testSuiteId).flatMap { testSuite =>
          if (testSuite.isDefined) {
            canManageDomain(request, userInfo, testSuite.get.domain)
          } else {
            Future.successful(false)
          }
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested test suite"))
  }

  def canDeleteTestSuite(request: RequestWithAttributes[_], testSuiteId: Long): Future[Boolean] = {
    canManageTestSuite(request, testSuiteId)
  }

  def canExecuteTestSession(request: RequestWithAttributes[_], sessionId: String, requireAdmin: Boolean = false): Future[Boolean] = {
    canManageTestSession(request, sessionId, requireAdmin, requireOwnTestSessionIfNotAdmin = true)
  }

  def canExecuteTestCase(request: RequestWithAttributes[_], test_id: String): Future[Boolean] = {
    canViewTestCase(request, test_id)
  }

  def canExecuteTestCases(request: RequestWithAttributes[AnyContent], testCaseIds: List[Long], specId: Long, systemId: Long, actorId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else {
        testCaseManager.getSpecificationsOfTestCases(testCaseIds).flatMap { specIds =>
          if (specIds.nonEmpty && specIds.contains(specId)) {
            canViewSpecifications(request, userInfo, Some(specIds.toList)).flatMap { _ =>
              canViewSystemsById(request, userInfo, Some(List(systemId)))
            }
          } else {
            Future.successful(false)
          }
        }
      }
    }
    check.map(setAuthResult(request, _, "Cannot view the requested test case(s)"))
  }

  def canGetBinaryFileMetadata(request: RequestWithAttributes[_]): Future[Boolean] = {
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

  def canViewSystemsById(request: RequestWithAttributes[_], userInfo: User, systemIds: Option[List[Long]]): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else {
      if (systemIds.isDefined && userInfo.organization.isDefined) {
        if (isCommunityAdmin(userInfo)) {
          // All the system's should be in organisations in the user's community.
          systemManager.getCommunityIdsOfSystems(systemIds.get).map { communityIds =>
            communityIds.size == 1 && userInfo.organization.exists(x => x.community == communityIds.head)
          }
        } else {
          // The systems should be in the user's organisation.
          systemManager.getSystemIdsForOrganization(userInfo.organization.get.id).map { orgSystemIds =>
            containsAll(systemIds.get, orgSystemIds)
          }
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User can't view the requested system(s)"))
  }

  def canViewSystemsById(request: RequestWithAttributes[_], systemIds: Option[List[Long]]): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canViewSystemsById(request, userInfo, systemIds)
    }
  }

  def canViewSystems(request: RequestWithAttributes[_], userInfo: User, orgId: Long): Future[Boolean] = {
    canViewOrganisation(request, userInfo, orgId)
  }

  def canViewSystems(request: RequestWithAttributes[_], orgId: Long, snapshotId: Option[Long] = None): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (orgId >= 0) {
        canViewSystems(request, userInfo, orgId)
      } else if (snapshotId.isDefined) {
        // Deleted organisation present in a snapshot
        canManageConformanceSnapshot(request, snapshotId.get)
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot view the systems of this organisation"))
  }

  def canViewEndpointConfigurationsForSystem(request: RequestWithAttributes[_], system: Long): Future[Boolean] = {
    canViewSystem(request, system)
  }

  def canManageStatementConfiguration(request: RequestWithAttributes[_], systemId: Long, actorId: Long, organisationUpdates: Boolean, systemUpdates: Boolean, statementUpdates: Boolean): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else {
        systemManager.getSystemById(systemId).flatMap { system =>
          if (system.isDefined) {
            if (isCommunityAdmin(userInfo)) {
              if (isOwnSystem(userInfo, system)) {
                Future.successful(true)
              } else {
                canManageOrganisationFull(request, userInfo, system.get.owner)
              }
            } else if (isOrganisationAdmin(userInfo)) {
              if (isOwnSystem(userInfo, system)) {
                if (!organisationUpdates && !systemUpdates && !statementUpdates) {
                  // No changes to properties.
                  Future.successful(true)
                } else {
                  communityManager.getById(userInfo.organization.get.community).flatMap { community =>
                    if (community.isDefined) {
                      if (community.get.allowPostTestOrganisationUpdates && community.get.allowPostTestSystemUpdates && community.get.allowPostTestStatementUpdates) {
                        // The community allows changes post-testing.
                        Future.successful(true)
                      } else {
                        testResultManager.testSessionsExistForSystemAndActors(systemId, List(actorId)).flatMap { testsExist =>
                          if (!testsExist) {
                            // No tests exist.
                            Future.successful(true)
                          } else {
                            // Check permissions and incoming updates.
                            Future.successful {
                              (community.get.allowPostTestOrganisationUpdates || !organisationUpdates) &&
                                (community.get.allowPostTestSystemUpdates || !systemUpdates) &&
                                (community.get.allowPostTestStatementUpdates || !statementUpdates)
                            }
                          }
                        }
                      }
                    } else {
                      Future.successful(false)
                    }
                  }
                }
              } else {
                Future.successful(false)
              }
            } else {
              Future.successful(false)
            }
          } else {
            Future.successful(false)
          }
        }
      }
    }
    check.map(setAuthResult(request, _, "User cannot manage statement configuration"))
  }

  def canDeleteConformanceStatement(request: RequestWithAttributes[_], systemId: Long, actorIds: Option[List[Long]]): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        // Own system or within community.
        systemManager.getSystemById(systemId).flatMap { system =>
          if (system.isDefined) {
            if (isOwnSystem(userInfo, system)) {
              Future.successful(true)
            } else {
              canManageOrganisationFull(request, userInfo, system.get.owner)
            }
          } else {
            Future.successful(false)
          }
        }
      } else if (isOrganisationAdmin(userInfo)) {
        communityManager.getById(userInfo.organization.get.community).flatMap { community =>
          if (community.isDefined && community.get.allowStatementManagement) {
            isOwnSystem(userInfo, systemId).flatMap { ownSystem =>
              // Has to be own system.
              if (ownSystem) {
                if (community.get.allowPostTestStatementUpdates) {
                  Future.successful(true)
                } else {
                  if (actorIds.isDefined && actorIds.get.nonEmpty) {
                    testResultManager.testSessionsExistForSystemAndActors(systemId, actorIds.get).map { exists =>
                      !exists
                    }
                  } else {
                    Future.successful(true)
                  }
                }
              } else {
                Future.successful(false)
              }
            }
          } else {
            Future.successful(false)
          }
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot delete the requested statement"))
  }

  def canViewConformanceStatements(request: RequestWithAttributes[_], systemId: Long, snapshotId: Option[Long]): Future[Boolean] = {
    if (snapshotId.isEmpty) {
      canViewSystem(request, systemId)
    } else {
      canViewSystemInSnapshot(request, systemId, snapshotId.get)
    }
  }

  def canCreateConformanceStatement(request: RequestWithAttributes[_], sutId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        // Own system or within community.
        systemManager.getSystemById(sutId).flatMap { system =>
          if (system.isDefined) {
            if (isOwnSystem(userInfo, system)) {
              Future.successful(true)
            } else {
              canManageOrganisationFull(request, userInfo, system.get.owner)
            }
          } else {
            Future.successful(false)
          }
        }
      } else {
        communityManager.getById(userInfo.organization.get.community).flatMap { community =>
          if (isOrganisationAdmin(userInfo) && community.isDefined && community.get.allowStatementManagement) {
            // Has to be own system.
            isOwnSystem(userInfo, sutId)
          } else {
            Future.successful(false)
          }
        }
      }
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested statement"))
  }

  def canViewSystem(request: RequestWithAttributes[_], sutId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        // Own system or within community.
        systemManager.getSystemById(sutId).flatMap { system =>
          if (system.isDefined) {
            if (isOwnSystem(userInfo, system)) {
              Future.successful(true)
            } else {
              canManageOrganisationFull(request, userInfo, system.get.owner)
            }
          } else {
            Future.successful(false)
          }
        }
      } else {
        // Has to be own system.
        isOwnSystem(userInfo, sutId)
      }
    }
    check.map(setAuthResult(request, _, "User cannot view the requested system"))
  }

  def canViewSystemInSnapshot(request: RequestWithAttributes[_], systemId: Long, snapshotId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else {
        if (userInfo.organization.isDefined) {
          if (isCommunityAdmin(userInfo)) {
            // This is a snapshot of the user's own community.
            conformanceManager.getConformanceSnapshot(snapshotId).map { snapshot =>
              snapshot.community == userInfo.organization.get.community
            }
          } else {
            // The requested system ID and the user's own organisation ID must exist as results in the specified snapshot.
            conformanceManager.existsInConformanceSnapshot(snapshotId, systemId, userInfo.organization.get.id)
          }
        } else {
          Future.successful(false)
        }
      }
    }
    check.map(setAuthResult(request, _, "User cannot view the requested system in the given conformance snapshot"))
  }

  def canUpdateSystem(request: RequestWithAttributes[_], systemId: Long, isDelete: Boolean = false): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        // Own system or within community.
        systemManager.getSystemById(systemId).flatMap { system =>
          if (system.isDefined) {
            if (isOwnSystem(userInfo, system)) {
              Future.successful(true)
            } else {
              canManageOrganisationFull(request, userInfo, system.get.owner)
            }
          } else {
            Future.successful(false)
          }
        }
      } else if (isOrganisationAdmin(userInfo) && userInfo.organization.isDefined) {
        // Has to be own system.
        isOwnSystem(userInfo, systemId).flatMap { ownSystem =>
          if (ownSystem) {
            // Check to see if special permissions are in place.
            communityManager.getById(userInfo.organization.get.community).flatMap { community =>
              if (community.isDefined) {
                if (!isDelete || community.get.allowSystemManagement) {
                  if (community.get.allowPostTestSystemUpdates) {
                    Future.successful(true)
                  } else {
                    testResultManager.testSessionsExistForSystem(systemId).map { exists =>
                      !exists
                    }
                  }
                } else {
                  Future.successful(false)
                }
              } else {
                Future.successful(false)
              }
            }
          } else {
            Future.successful(false)
          }
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot update the requested system"))
  }

  private def isOwnSystem(userInfo: User, system: Option[Systems]): Boolean = {
    system.isDefined && userInfo.organization.isDefined && system.get.owner == userInfo.organization.get.id
  }

  private def isOwnSystem(userInfo: User, systemId: Long): Future[Boolean] = {
    systemManager.getSystemById(systemId).map { system =>
      isOwnSystem(userInfo, system)
    }
  }

  def canManageSystem(request: RequestWithAttributes[_], systemId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageSystem(request, userInfo, systemId)
    }
  }

  def canManageSystem(request: RequestWithAttributes[_], userInfo: User, sutId: Long): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else if (isCommunityAdmin(userInfo)) {
      // Own system or within community.
      systemManager.getSystemById(sutId).flatMap { system =>
        if (system.isDefined) {
          if (isOwnSystem(userInfo, system)) {
            Future.successful(true)
          } else {
            canManageOrganisationFull(request, userInfo, system.get.owner)
          }
        } else {
          Future.successful(false)
        }
      }
    } else if (isOrganisationAdmin(userInfo)) {
      // Has to be own system.
      isOwnSystem(userInfo, sutId)
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested system"))
  }

  def canCreateSystem(request: RequestWithAttributes[_], orgId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        if (userInfo.organization.isDefined && userInfo.organization.get.id == orgId) {
          Future.successful(true)
        } else {
          organizationManager.getById(orgId).map { org =>
            org.isDefined && userInfo.organization.isDefined && org.get.community == userInfo.organization.get.community
          }
        }
      } else {
        communityManager.getById(userInfo.organization.get.community).map { community =>
          isOrganisationAdmin(userInfo) && community.isDefined && community.get.allowSystemManagement && userInfo.organization.isDefined && userInfo.organization.get.id == orgId
        }
      }
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested organisation"))
  }

  def canDeleteSystem(request: RequestWithAttributes[_], systemId: Long): Future[Boolean] = {
    canUpdateSystem(request, systemId, isDelete = true)
  }

  def canManageThemes(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canAccessThemeData(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = Future.successful(true)
    check.map(setAuthResult(request, _, "User cannot access theme"))
  }

  def canUpdateSystemConfigurationValues(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canManageSystemSettings(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canViewSystemConfigurationValues(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canManageAnyTestSession(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canUpdateSpecification(request: RequestWithAttributes[_], specId: Long): Future[Boolean] = {
    canManageSpecification(request, specId)
  }

  def canDeleteSpecification(request: RequestWithAttributes[_], specId: Long): Future[Boolean] = {
    canManageSpecification(request, specId)
  }

  def canViewAllTestCases(request: RequestWithAttributes[_]): Future[Boolean] = {
    canViewAllTestResources(request)
  }

  private def canViewAllTestResources(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (userInfo.organization.isDefined) {
        communityManager.getById(userInfo.organization.get.community).map { communityInfo =>
          communityInfo.isDefined && communityInfo.get.domain.isEmpty
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User not allowed to view all test resources"))
  }

  def canViewTestSuiteResource(request: RequestWithAttributes[_], testId: String): Future[Boolean] = {
    val check = if (isTestEngineCall(request)) {
      Future.successful(checkValidTestEngineCall(request, testId))
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "Not allowed to access requested resource"))
  }

  def canViewTestCase(request: RequestWithAttributes[_], testId: String): Future[Boolean] = {
    val check = if (isTestEngineCall(request)) {
      Future.successful {
        checkValidTestEngineCall(request, testId)
      }
    } else {
      getUser(getRequestUserId(request)).flatMap { userInfo =>
        if (isTestBedAdmin(userInfo)) {
          Future.successful(true)
        } else if (isCommunityAdmin(userInfo)) {
          testSuiteManager.getTestSuiteOfTestCase(testId.toLong).flatMap { testSuite =>
            canManageDomain(request, userInfo, testSuite.domain)
          }
        } else {
          conformanceManager.getSpecificationIdForTestCaseFromConformanceStatements(testId.toLong).flatMap { specId =>
            if (specId.isDefined) {
              canViewSpecifications(request, userInfo, Some(List(specId.get)))
            } else {
              Future.successful(false)
            }
          }
        }
      }
    }
    check.map(setAuthResult(request, _, "Cannot view requested test case"))
  }

  def canPreviewConformanceCertificateReport(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  def canViewConformanceCertificateReport(request: RequestWithAttributes[_], communityId: Long, snapshotId: Option[Long] = None): Future[Boolean] = {
    if (snapshotId.isDefined) {
      canManageConformanceSnapshot(request, snapshotId.get)
    } else {
      canManageCommunity(request, communityId)
    }
  }

  def canViewOwnConformanceCertificateReport(request: RequestWithAttributes[_], systemId: Long, snapshotId: Option[Long] = None): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else {
        if (isCommunityAdmin(userInfo)) {
          systemManager.getCommunityIdOfSystem(systemId).map { communityIdOfSystem =>
            userInfo.organization.isDefined && userInfo.organization.get.community == communityIdOfSystem
          }
        } else {
          // Organisation user.
          if (userInfo.organization.isDefined) {
            communityManager.getById(userInfo.organization.get.community).flatMap { community =>
              if (community.isDefined && community.get.allowCertificateDownload) {
                systemManager.getSystemById(systemId).flatMap { system =>
                  if (system.isDefined && userInfo.organization.get.id == system.get.owner) {
                    if (snapshotId.isDefined) {
                      conformanceManager.getConformanceSnapshot(snapshotId.get).map { snapshot =>
                        snapshot.community == community.get.id && snapshot.isPublic
                      }
                    } else {
                      Future.successful(true)
                    }
                  } else {
                    Future.successful(false)
                  }
                }
              } else {
                Future.successful(false)
              }
            }
          } else {
            Future.successful(false)
          }
        }
      }
    }
    check.map(setAuthResult(request, _, "User cannot download this conformance certificate"))
  }

  def canViewConformanceStatementReport(request: RequestWithAttributes[_], systemId: Long, snapshotId: Option[Long] = None): Future[Boolean] = {
    if (snapshotId.isDefined) {
      canViewSystemInSnapshot(request, systemId, snapshotId.get)
    } else {
      canViewSystem(request, systemId)
    }
  }

  def canManageTestSession(request: RequestWithAttributes[_], sessionId: String, requireAdmin: Boolean, requireOwnTestSessionIfNotAdmin: Boolean): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        // Within community.
        testResultManager.getCommunityIdForTestSession(sessionId).flatMap { result =>
          if (result.isDefined) {
            if (result.get._2.isDefined) {
              // There is a community ID defined. This would mean an executing or completed session.
              canManageCommunity(request, userInfo, result.get._2.get)
            } else {
              /*
               Existing session but without a community ID. This can only come up if the community has been deleted.
               In such a case only the test bed admin should be able to see this.
               */
              Future.successful(false)
            }
          } else {
            /*
            There is no test session recorded for this session ID. This could be because the test session is currently
            being configured.
             */
            Future.successful(true)
          }
        }
      } else {
        // Organisation user.
        if (requireAdmin || userInfo.organization.isEmpty) {
          Future.successful(false)
        } else {
          testResultManager.getOrganisationIdsForTestSession(sessionId).flatMap { result =>
            if (result.isDefined) {
              val organisationIdForSession = result.get._2
              val communityIdForSession = result.get._3
              if (organisationIdForSession.isDefined && communityIdForSession.isDefined) {
                if (userInfo.organization.get.id == organisationIdForSession.get) {
                  // The session belongs to the user's organisation.
                  Future.successful(true)
                } else {
                  if (requireOwnTestSessionIfNotAdmin) {
                    // The session must belong to the user's organisation.
                    Future.successful(false)
                  } else {
                    // The session must belong to the user's community.
                    organisationUserCanViewCommunityContent(request, userInfo, communityIdForSession.get)
                  }
                }
              } else {
                // This is an obsolete session no longer visible to the user.
                Future.successful(false)
              }
            } else {
              // There is no test session recorded for this session ID. This could be because the test session is currently being configured.
              Future.successful(true)
            }
          }
        }
      }
    }
    check.map(setAuthResult(request, _, "User cannot manage requested session"))
  }

  def canViewTestResultForSession(request: RequestWithAttributes[_], sessionId: String): Future[Boolean] = {
    canManageTestSession(request, sessionId, requireAdmin = false, requireOwnTestSessionIfNotAdmin = false)
  }

  def canViewCommunityTests(request: RequestWithAttributes[_], communityIds: Option[List[Long]]): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else {
        if (communityIds.isDefined && communityIds.get.size == 1) {
          // There should be only a single community requested.
          val communityId = communityIds.get.head
          if (isCommunityAdmin(userInfo)) {
            canManageCommunity(request, userInfo, communityId)
          } else {
            // The community must have enabled the option to view testing of other organisations.
            communityManager.getById(communityId).map { community =>
              community.isDefined && userInfo.organization.isDefined && community.get.id == userInfo.organization.get.community && community.get.allowCommunityView
            }
          }
        } else {
          Future.successful(false)
        }
      }
    }
    check.map(setAuthResult(request, _, "User cannot view the requested community"))
  }

  def canViewActiveTestsForCommunity(request: RequestWithAttributes[_], communityIds: Option[List[Long]]): Future[Boolean] = {
    canViewCommunities(request, communityIds)
  }

  def canViewTestResultsForOrganisation(request: RequestWithAttributes[_], organisationId: Long): Future[Boolean] = {
    canViewOrganisation(request, organisationId)
  }

  def canUpdateParameter(request: RequestWithAttributes[_], parameterId: Long): Future[Boolean] = {
    canManageParameter(request, parameterId)
  }

  private def canManageParameter(request: RequestWithAttributes[_], parameterId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        parameterManager.getParameterById(parameterId).flatMap { parameter =>
          if (parameter.isDefined) {
            canManageEndpoint(request, userInfo, parameter.get.endpoint)
          } else {
            Future.successful(false)
          }
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot manage parameter"))
  }

  def canDeleteParameter(request: RequestWithAttributes[_], parameterId: Long): Future[Boolean] = {
    canManageParameter(request, parameterId)
  }

  def canDeleteOrganisation(request: RequestWithAttributes[_], orgId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo) || isCommunityAdmin(userInfo)) {
        canManageOrganisationFull(request, userInfo, orgId)
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot delete the requested organisation"))
  }

  def canUpdateOrganisation(request: RequestWithAttributes[_], orgId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageOrganisationFull(request, userInfo, orgId)
    }
  }

  def canManageOrganisationBasic(request: RequestWithAttributes[_], orgId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageOrganisationBasic(request, userInfo, orgId)
    }
  }

  def canManageOrganisationBasic(request: RequestWithAttributes[_], userInfo: User, orgId: Long): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else if (isCommunityAdmin(userInfo)) {
      if (userInfo.organization.isDefined && userInfo.organization.get.id == orgId) {
        Future.successful(true)
      } else {
        organizationManager.getById(orgId).map { org =>
          org.isDefined && userInfo.organization.isDefined && org.get.community == userInfo.organization.get.community
        }
      }
    } else if (isOrganisationAdmin(userInfo)) {
      Future.successful(userInfo.organization.isDefined && userInfo.organization.get.id == orgId)
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested organisation"))
  }

  private def canManageOrganisationFull(request: RequestWithAttributes[_], userInfo: User, orgId: Long): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else if (isCommunityAdmin(userInfo)) {
      if (userInfo.organization.isDefined && userInfo.organization.get.id == orgId) {
        Future.successful(true)
      } else {
        organizationManager.getById(orgId).map { org =>
          org.isDefined && userInfo.organization.isDefined && org.get.community == userInfo.organization.get.community
        }
      }
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested organisation"))
  }

  def canCreateOrganisation(request: RequestWithAttributes[_], organisation: Organizations, otherOrganisation: Option[Long]): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        // Check we can manage the community of the organisation as well as the template organisation.
        canManageCommunity(request, userInfo, organisation.community).flatMap { ok =>
          if (ok) {
            if (otherOrganisation.isDefined) {
              organizationManager.getById(otherOrganisation.get).map { other =>
                other.isDefined && other.get.community == organisation.community
              }
            } else {
              Future.successful(true)
            }
          } else {
            Future.successful(false)
          }
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot create the provided organisation"))
  }

  def canViewOrganisationsByCommunity(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canViewCommunityContent(request, communityId)
  }

  def canViewOrganisation(request: RequestWithAttributes[_], userInfo: User, orgId: Long): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else {
      if (userInfo.organization.isDefined && userInfo.organization.get.id == orgId) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        canManageOrganisationFull(request, userInfo, orgId)
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot view the requested organisation"))
  }

  def canViewOrganisation(request: RequestWithAttributes[_], orgId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canViewOrganisation(request, userInfo, orgId)
    }
  }

  def canViewAllOrganisations(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canViewTestBedDefaultLegalNotice(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = Future.successful(true)
    check.map(setAuthResult(request, _, "User cannot view the default landing page"))
  }

  def canViewSystemResource(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = Future.successful(true)
    check.map(setAuthResult(request, _, "User cannot view the requested resource"))
  }

  def canViewDefaultLegalNotice(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canViewCommunityBasic(request, communityId)
  }

  def canManageLegalNotice(request: RequestWithAttributes[_], noticeId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      val load = () => { legalNoticeManager.getCommunityId(noticeId) }
      canManageCommunityArtifact(request, userInfo, load)
    }
  }

  def canManageLegalNotices(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  def canViewDefaultLandingPage(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canViewCommunityBasic(request, communityId)
  }

  def canManageLandingPage(request: RequestWithAttributes[_], pageId: Long): Future[Boolean] = {
    val load = () => landingPageManager.getCommunityId(pageId)
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageCommunityArtifactAsync(request, userInfo, load)
    }
  }

  def canManageLandingPages(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  def canManageTrigger(request: RequestWithAttributes[_], triggerId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      val load = () => { triggerManager.getCommunityId(triggerId) }
      canManageCommunityArtifact(request, userInfo, load)
    }
  }

  def canManageTriggers(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  def canManageCommunityResource(request: RequestWithAttributes[_], resourceId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      val load = () => {
        communityResourceManager.getCommunityId(resourceId)
      }
      canManageCommunityArtifact(request, userInfo, load)
    }
  }

  def canViewDefaultErrorTemplate(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canViewCommunityBasic(request, communityId)
  }

  def canManageErrorTemplate(request: RequestWithAttributes[_], templateId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      val load = () => { errorTemplateManager.getCommunityId(templateId) }
      canManageCommunityArtifact(request, userInfo, load)
    }
  }

  private def canManageCommunityArtifact(request: RequestWithAttributes[_], userInfo: User, loadFunction: () => Future[Long]): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else if (isCommunityAdmin(userInfo)) {
      loadFunction.apply().flatMap { id =>
        canManageCommunity(request, userInfo, id)
      }
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested community"))
  }

  private def canManageCommunityArtifactAsync(request: RequestWithAttributes[_], userInfo: User, loadFunction: () => Future[Long]): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else if (isCommunityAdmin(userInfo)) {
      loadFunction.apply().flatMap { communityId =>
        canManageCommunity(request, userInfo, communityId)
      }
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested community"))
  }

  def canManageErrorTemplates(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  private def canManageEndpoints(request: RequestWithAttributes[_], userInfo: User, ids: Option[List[Long]]): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else if (isCommunityAdmin(userInfo)) {
      if (ids.isDefined) {
        endpointManager.getDomainIdsOfEndpoints(ids.get).flatMap { domainIds =>
          canManageDomains(request, userInfo, domainIds.toSet)
        }
      } else {
        Future.successful(false)
      }
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User cannot manage requested endpoint"))
  }

  private def canManageEndpoint(request: RequestWithAttributes[_], userInfo: User, endPointId: Long): Future[Boolean] = {
    canManageEndpoints(request, userInfo, Some(List(endPointId)))
  }

  def canUpdateEndpoint(request: RequestWithAttributes[_], endPointId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageEndpoint(request, userInfo, endPointId)
    }
  }

  def canDeleteEndpoint(request: RequestWithAttributes[_], endPointId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageEndpoint(request, userInfo, endPointId)
    }
  }

  def canViewOwnCommunity(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkIsAuthenticated(request)
  }

  def canDeleteCommunity(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canUpdateCommunity(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageCommunity(request, userInfo, communityId)
    }
  }

  def canViewCommunityBasic(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        Future.successful(isOwnCommunity(userInfo, communityId) || communityId == Constants.DefaultCommunityId)
      } else {
        Future.successful(isOwnCommunity(userInfo, communityId))
      }
    }
    check.map(setAuthResult(request, _, "User cannot view the requested community"))
  }

  private def isOwnCommunity(userInfo: User, communityId: Long): Boolean = {
    userInfo.organization.isDefined && userInfo.organization.get.community == communityId
  }

  def canViewCommunityFull(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  def canCreateCommunity(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canViewAllCommunities(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canViewCommunities(request: RequestWithAttributes[_], communityIds: Option[List[Long]]): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        // There should be only a single community requested.
        if (communityIds.isDefined && communityIds.get.size == 1) {
          canManageCommunity(request, userInfo, communityIds.get.head)
        } else {
          Future.successful(false)
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot view the requested community"))
  }

  def canLogout(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = Future.successful(true)
    check.map(setAuthResult(request, _, "User cannot logout"))
  }

  def canCheckAnyUserEmail(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).map { userInfo =>
      isTestBedAdmin(userInfo) || isCommunityAdmin(userInfo) || isOrganisationAdmin(userInfo)
    }
    check.map(setAuthResult(request, _, "User cannot check user data"))
  }

  def canCheckUserEmail(request: RequestWithAttributes[_], organisationId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).map { userInfo =>
      isAnyAdminType(userInfo) && userInfo.organization.isDefined && userInfo.organization.get.id == organisationId
    }
    check.map(setAuthResult(request, _, "User cannot manage own organisation"))
  }

  def canCheckSystemAdminEmail(request: RequestWithAttributes[AnyContent]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canCheckCommunityAdminEmail(request: RequestWithAttributes[AnyContent], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  def canCheckOrganisationUserEmail(request: RequestWithAttributes[AnyContent], organisationId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageOrganisationFull(request, userInfo, organisationId)
    }
  }

  def canLogin(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = Future.successful(true)
    check.map(setAuthResult(request, _, "User not allowed to login"))
  }

  def canUpdateActor(request: RequestWithAttributes[_], actorId: Long): Future[Boolean] = {
    canManageActor(request, actorId)
  }

  def canDeleteActor(request: RequestWithAttributes[_], actorId: Long): Future[Boolean] = {
    canManageActor(request, actorId)
  }

  def canSubmitFeedback(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = Future.successful(true)
    check.map(setAuthResult(request, _, "User not allowed to submit feedback"))
  }

  def canViewConfiguration(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = Future.successful(true)
    check.map(setAuthResult(request, _, "User not allowed to view configuration"))
  }

  def canUpdateOwnProfile(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = checkIsAuthenticated(request).flatMap { authenticated =>
      if (authenticated) {
        val userId = getRequestUserId(request)
        Future.successful {
          !Configurations.AUTHENTICATION_SSO_ENABLED && (!Configurations.DEMOS_ENABLED || userId != Configurations.DEMOS_ACCOUNT)
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot edit own profile"))
  }

  def canViewOwnProfile(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkIsAuthenticated(request)
  }

  def canCreateUserInOwnOrganisation(request: RequestWithAttributes[_]): Future[Boolean] = {
    canUpdateOwnOrganisation(request, ignoreExistingTests = true)
  }

  def canViewOwnOrganisationUsers(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkIsAuthenticated(request)
  }

  def canUpdateOwnOrganisationAndLandingPage(request: RequestWithAttributes[_], landingPageId: Option[Long]): Future[Boolean] = {
    canUpdateOwnOrganisation(request, ignoreExistingTests = false).flatMap { check =>
      if (check) {
        Future.successful(true)
      } else {
        if (landingPageId.isEmpty || landingPageId.get == -1) {
          Future.successful(true)
        } else {
          canManageLandingPage(request, landingPageId.get)
        }
      }
    }
  }

  def canUpdateOwnOrganisation(request: RequestWithAttributes[_], ignoreExistingTests: Boolean): Future[Boolean] = {
    val userId = getRequestUserId(request)
    val check = if (!Configurations.DEMOS_ENABLED || userId != Configurations.DEMOS_ACCOUNT) {
      getUser(userId).flatMap { userInfo =>
        if (isTestBedAdmin(userInfo) || isCommunityAdmin(userInfo)) {
          Future.successful(true)
        } else if (isOrganisationAdmin(userInfo) && userInfo.organization.isDefined) {
          if (ignoreExistingTests) {
            Future.successful(true)
          } else {
            communityManager.getById(userInfo.organization.get.community).flatMap { community =>
              if (community.isDefined) {
                if (community.get.allowPostTestOrganisationUpdates) {
                  Future.successful(true)
                } else {
                  testResultManager.testSessionsExistForOrganisation(userInfo.organization.get.id).map { exists =>
                    !exists
                  }
                }
              } else {
                Future.successful(false)
              }
            }
          }
        } else {
          Future.successful(false)
        }
      }
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User cannot manage own organisation"))
  }

  def canViewOwnOrganisation(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkIsAuthenticated(request)
  }

  def canUpdateConformanceCertificateSettings(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  def canViewConformanceCertificateSettings(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  def canManageCommunity(request: RequestWithAttributes[_], userInfo: User, communityId: Long): Future[Boolean] = {
    val check = Future.successful(isTestBedAdmin(userInfo) || (isCommunityAdmin(userInfo) && userInfo.organization.isDefined && userInfo.organization.get.community == communityId))
    check.map(setAuthResult(request, _, "User cannot manage the requested community"))
  }

  def canViewConformanceSnapshot(request: RequestWithAttributes[_], snapshotId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canViewConformanceSnapshot(request, userInfo, snapshotId)
    }
  }

  def canViewConformanceSnapshot(request: RequestWithAttributes[_], userInfo: User, snapshotId: Long): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else if (userInfo.organization.isDefined) {
      conformanceManager.getConformanceSnapshot(snapshotId).map { snapshot =>
        if (isCommunityAdmin(userInfo)) {
          snapshot.community == userInfo.organization.get.community
        } else {
          snapshot.community == userInfo.organization.get.community && snapshot.isPublic
        }
      }
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User cannot view the requested conformance snapshot"))
  }

  def canManageConformanceSnapshot(request: RequestWithAttributes[_], snapshotId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageConformanceSnapshot(request, userInfo, snapshotId)
    }
  }

  private def canManageConformanceSnapshot(request: RequestWithAttributes[_], userInfo: User, snapshotId: Long): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else if (isCommunityAdmin(userInfo) && userInfo.organization.isDefined) {
      conformanceManager.getConformanceSnapshot(snapshotId).map { snapshot =>
        snapshot.community == userInfo.organization.get.community
      }
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested conformance snapshot"))
  }

  def canManageCommunity(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageCommunity(request, userInfo, communityId)
    }
  }

  def canManageOrganisationParameter(request: RequestWithAttributes[_], parameterId: Long): Future[Boolean] = {
    val check = communityManager.getOrganisationParameterById(parameterId).flatMap { parameter =>
      if (parameter.isDefined) {
        canManageCommunity(request, parameter.get.community)
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested parameter"))
  }

  def canManageSystemParameter(request: RequestWithAttributes[_], parameterId: Long): Future[Boolean] = {
    val check = communityManager.getSystemParameterById(parameterId).flatMap { parameter =>
      if (parameter.isDefined) {
        canManageCommunity(request, parameter.get.community)
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested parameter"))
  }

  def canDeleteObsoleteTestResultsForCommunity(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  def canDeleteObsoleteTestResultsForOrganisation(request: RequestWithAttributes[_], organisationId: Long): Future[Boolean] = {
    canManageOrganisationBasic(request, organisationId)
  }

  def canDeleteTestResults(request: RequestWithAttributes[_], communityId: Option[Long]): Future[Boolean] = {
    if (communityId.isDefined) {
      canManageCommunity(request, communityId.get)
    } else {
      checkTestBedAdmin(request)
    }
  }

  def canDeleteAllObsoleteTestResults(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canViewConformanceOverview(request: RequestWithAttributes[_], communityIds: Option[List[Long]], snapshotId: Option[Long] = None): Future[Boolean] = {
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

  def canManageDomainParameters(request: RequestWithAttributes[_], domainId: Long): Future[Boolean] = {
    canManageDomain(request, domainId)
  }

  def canViewDomainParametersForCommunity(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    canManageCommunity(request, communityId)
  }

  def canViewTestSuiteByTestCaseId(request: RequestWithAttributes[_], testCaseId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        testSuiteManager.getTestSuiteOfTestCase(testCaseId).flatMap { testSuite =>
          canManageDomain(request, userInfo, testSuite.domain)
        }
      } else {
        // There must be a conformance statement for this.
        conformanceManager.getSpecificationIdForTestCaseFromConformanceStatements(testCaseId).flatMap { specId =>
          if (specId.isDefined) {
            canViewSpecifications(request, userInfo, Some(List(specId.get)))
          } else {
            Future.successful(false)
          }
        }
      }
    }
    check.map(setAuthResult(request, _, "User cannot view the requested test suite"))
  }

  def canViewTestSuite(request: RequestWithAttributes[_], testSuiteId: Long): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        testSuiteManager.getById(testSuiteId).flatMap { testSuite =>
          if (testSuite.isDefined) {
            canManageDomain(request, userInfo, testSuite.get.domain)
          } else {
            Future.successful(false)
          }
        }
      } else {
        // There must be a conformance statement for this.
        conformanceManager.getSpecificationIdForTestSuiteFromConformanceStatements(testSuiteId).flatMap { specId =>
          if (specId.isDefined) {
            canViewSpecifications(request, userInfo, Some(List(specId.get)))
          } else {
            Future.successful(false)
          }
        }
      }
    }
    check.map(setAuthResult(request, _, "User doesn't have access to the requested test suite"))
  }

  def canViewConformanceStatus(request: RequestWithAttributes[_], actorId: Long, sutId: Long, snapshotId: Option[Long] = None): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else {
        if (snapshotId.isDefined) {
          canManageConformanceSnapshot(request, userInfo, snapshotId.get)
        } else {
          canViewSystemsById(request, userInfo, Some(List(sutId)))
        }
      }
    }
    check.map(setAuthResult(request, _, "User cannot view the requested conformance status"))
  }

  def canEditTestSuite(request: RequestWithAttributes[AnyContent], testSuiteId: Long): Future[Boolean] = {
    testSuiteManager.getById(testSuiteId).flatMap { testSuite =>
      canManageDomain(request, testSuite.get.domain)
    }
  }

  def canEditTestCase(request: RequestWithAttributes[AnyContent], testCaseId: Long): Future[Boolean] = {
    testSuiteManager.getTestSuiteOfTestCase(testCaseId).flatMap { testSuite =>
      canManageDomain(request, testSuite.domain)
    }
  }

  def canDeleteDomain(request: RequestWithAttributes[_], domain_id: Long): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canViewEndpointsById(request: RequestWithAttributes[_], ids: Option[List[Long]]): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageEndpoints(request, userInfo, ids)
    }
  }

  def canViewEndpoints(request: RequestWithAttributes[_], actorId: Long): Future[Boolean] = {
    canViewActor(request, actorId)
  }

  def canCreateSpecification(request: RequestWithAttributes[_], domain: Long): Future[Boolean] = {
    canUpdateDomain(request, domain)
  }

  def canManageActor(request: RequestWithAttributes[_], userInfo: User, actor: Long): Future[Boolean] = {
    specificationManager.getSpecificationOfActor(actor).flatMap { spec =>
      canManageDomain(request, userInfo, spec.domain)
    }
  }

  def canManageActor(request: RequestWithAttributes[_], actor: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageActor(request, userInfo, actor)
    }
  }

  def canCreateEndpoint(request: RequestWithAttributes[_], actor: Long): Future[Boolean] = {
    canManageActor(request, actor)
  }

  def canManageSpecification(request: RequestWithAttributes[_], specificationId: Long): Future[Boolean] = {
    specificationManager.getSpecificationById(specificationId).flatMap { spec =>
      canManageDomain(request, spec.domain)
    }
  }

  def canManageSpecificationGroup(request: RequestWithAttributes[_], groupId: Long): Future[Boolean] = {
    specificationManager.getSpecificationGroupById(groupId).flatMap { group =>
      canManageDomain(request, group.domain)
    }
  }

  def canManageSpecifications(request: RequestWithAttributes[_], specIds: List[Long]): Future[Boolean] = {
    val check = getUser(getRequestUserId(request)).flatMap { userInfo =>
      if (isTestBedAdmin(userInfo)) {
        Future.successful(true)
      } else if (isCommunityAdmin(userInfo)) {
        specificationManager.getSpecificationsById(specIds).flatMap { specs =>
          canManageDomains(request, userInfo, specs.map(_.domain).toSet)
        }
      } else {
        Future.successful(false)
      }
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested specifications"))
  }


  def canCreateActor(request: RequestWithAttributes[_], specificationId: Long): Future[Boolean] = {
    canManageSpecification(request, specificationId)
  }

  def canManageDomain(request: RequestWithAttributes[_], userInfo: User, domainId: Long): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else if (isCommunityAdmin(userInfo) && userInfo.organization.isDefined) {
      getCommunityDomain(userInfo.organization.get.community).map { communityDomain =>
        communityDomain.isEmpty || communityDomain.get == domainId
      }
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested domain"))
  }

  private def canManageDomains(request: RequestWithAttributes[_], userInfo: User, domainIds: Set[Long]): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else if (isCommunityAdmin(userInfo) && userInfo.organization.isDefined) {
      getCommunityDomain(userInfo.organization.get.community).map { communityDomain =>
        communityDomain.isEmpty || domainIds.isEmpty || (domainIds.size == 1 && domainIds.head == communityDomain.get)
      }
    } else {
      Future.successful(false)
    }
    check.map(setAuthResult(request, _, "User cannot manage the requested domain"))
  }

  def canManageDomain(request: RequestWithAttributes[_], domainId: Long): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canManageDomain(request, userInfo, domainId)
    }
  }

  def canApplySandboxDataMulti(request: RequestWithAttributes[MultipartFormData[Files.TemporaryFile]]): Future[Boolean] = {
    val check = Future.successful {
      Configurations.DATA_WEB_INIT_ENABLED && !repositoryUtils.getDataLockFile().exists()
    }
    check.map(setAuthResult(request, _, "Web-based data initialisation is not enabled"))
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

  def canUpdateDomain(request: RequestWithAttributes[_], domainId: Long): Future[Boolean] = {
    canManageDomain(request, domainId)
  }

  def checkTestBedAdmin(request: RequestWithAttributes[_]): Future[Boolean] = {
    val check = isTestBedAdmin(getRequestUserId(request))
    check.map(setAuthResult(request, _, "Only test bed administrators can perform this operation"))
  }

  def checkTestBedAdmin(request: RequestHeader): Future[Boolean] = {
    val check = isTestBedAdmin(getRequestUserId(request))
    check.map(checkAuthResult(_, "Only test bed administrators can perform this operation"))
  }

  def canCreateDomainAsync(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canCreateDomain(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canDeleteAnyDomain(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canViewActor(request: RequestWithAttributes[_], actor_id: Long): Future[Boolean] = {
    specificationManager.getSpecificationIdOfActor(actor_id).flatMap { specId =>
      canViewSpecifications(request, Some(List(specId)))
    }
  }

  def canViewActorsBySpecificationId(request: RequestWithAttributes[_], spec_id: Long): Future[Boolean] = {
    canViewSpecifications(request, Some(List(spec_id)))
  }

  def canViewSpecificationsByDomainId(request: RequestWithAttributes[_], domain_id: Long): Future[Boolean] = {
    canViewDomains(request, Some(List(domain_id)))
  }

  private def specificationsMatchDomain(specs: Iterable[Specifications], domainId: Long): Boolean = {
    for (elem <- specs) {
      if (elem.domain != domainId) {
        return false
      }
    }
    true
  }

  def canViewSpecifications(request: RequestWithAttributes[_], userInfo: User, ids: Option[List[Long]]): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else {
      getVisibleDomainForUser(userInfo).flatMap { userDomain =>
        if (userDomain.isDefined) {
          if (ids.isDefined && ids.get.nonEmpty) {
            specificationManager.getSpecifications(ids).map { specs =>
              specificationsMatchDomain(specs, userDomain.get)
            }
          } else {
            Future.successful(false)
          }
        } else {
          Future.successful(true)
        }
      }
    }
    check.map(setAuthResult(request, _, "User doesn't have access to the requested specification(s)"))
  }

  def canViewSpecifications(request: RequestWithAttributes[_], ids: Option[List[Long]]): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canViewSpecifications(request, userInfo, ids)
    }
  }

  def canViewDomainByCommunityId(request: RequestWithAttributes[_], communityId: Long): Future[Boolean] = {
    val userId = getRequestUserId(request)
    val check = isTestBedAdmin(userId).flatMap { isAdmin =>
      if (!isAdmin) {
        getUserOrganisation(userId).map { userInfo =>
          userInfo.isDefined && userInfo.get.community == communityId
        }
      } else {
        Future.successful(true)
      }
    }
    check.map(setAuthResult(request, _, "User doesn't have access to the requested domain(s)"))
  }

  def canViewDomainBySpecificationId(request: RequestWithAttributes[_], specId: Long): Future[Boolean] = {
    val userId = getRequestUserId(request)
    val check = isTestBedAdmin(userId).flatMap { isAdmin =>
      if (isAdmin) {
        Future.successful(true)
      } else {
        getVisibleDomainForUser(userId).flatMap { userDomain =>
          if (userDomain.isDefined) {
            specificationManager.getSpecificationById(specId).map { spec =>
              spec.domain == userDomain.get
            }
          } else {
            Future.successful(true)
          }
        }
      }
    }
    check.map(setAuthResult(request, _, "User doesn't have access to the requested domain(s)"))
  }

  def canViewDomains(request: RequestWithAttributes[_], userInfo: User, ids: Option[List[Long]]): Future[Boolean] = {
    val check = if (isTestBedAdmin(userInfo)) {
      Future.successful(true)
    } else {
      // Users are either linked to a community or not (in which case they view all domains).
      getVisibleDomainForUser(userInfo).flatMap { domainLinkedToUser =>
        if (domainLinkedToUser.isDefined) {
          // The list of domains should include only the user's domain.
          Future.successful(ids.isDefined && ids.get.size == 1 && ids.get.head == domainLinkedToUser.get)
        } else {
          Future.successful(true)
        }
      }
    }
    check.map(setAuthResult(request, _, "User doesn't have access to the requested domain(s)"))
  }

  def canViewAllDomains(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkTestBedAdmin(request)
  }

  def canViewDomains(request: RequestWithAttributes[_], ids: Option[List[Long]]): Future[Boolean] = {
    getUser(getRequestUserId(request)).flatMap { userInfo =>
      canViewDomains(request, userInfo, ids)
    }
  }

  def canPreviewDocumentation(request: RequestWithAttributes[_]): Future[Boolean] = {
    checkIsAuthenticated(request)
  }

  private def getUser(userId: Long): Future[User] = {
    userManager.getUserByIdAsync(userId).map { user =>
      if (user.isEmpty) {
        throw UnauthorizedAccessException("User not found")
      } else {
        user.get
      }
    }
  }

  private def getUserOrganisation(userId: Long): Future[Option[Organizations]] = {
    getUser(userId).map { user =>
      user.organization
    }
  }

  private def getCommunityDomain(communityId: Long): Future[Option[Long]] = {
    communityManager.getById(communityId).map { community =>
      if (community.isDefined) {
        community.get.domain
      } else {
        None
      }
    }
  }

  private def getVisibleDomainForUser(userInfo: User): Future[Option[Long]] = {
    if (userInfo.organization.isDefined) {
      getCommunityDomain(userInfo.organization.get.community)
    } else {
      Future.successful(None)
    }
  }

  private def getVisibleDomainForUser(userId: Long): Future[Option[Long]] = {
    getUser(userId).flatMap { userInfo =>
      getVisibleDomainForUser(userInfo)
    }
  }

  private def checkIsAuthenticated(request: RequestWithAttributes[_]): Future[Boolean] = {
    getRequestUserId(request)
    val check = checkHasPrincipal(request, skipForNonSSO = true)
    check.map(setAuthResult(request, _, "User is not authenticated"))
  }

  private def isTestBedAdmin(userId: Long): Future[Boolean] = {
    accountManager.isSystemAdmin(userId)
  }

  private def checkValidTestEngineCall(request: RequestWithAttributes[_], testId: String): Boolean = {
    val ok = HmacUtils.isTokenValid(request.headers.get(HmacUtils.HMAC_HEADER_TOKEN).get, testId, request.headers.get(HmacUtils.HMAC_HEADER_TIMESTAMP).get)
    setAuthResult(request, ok, "Test engine call rejected due to invalid HMAC.")
  }

  private def isTestEngineCall(request: RequestWithAttributes[_]): Boolean = {
    request.headers.get(HmacUtils.HMAC_HEADER_TOKEN).isDefined && request.headers.get(HmacUtils.HMAC_HEADER_TIMESTAMP).isDefined
  }

  private def getRequestUserId(request: RequestHeader): Long = {
    val userId = ParameterExtractor.extractOptionalUserId(request)
    if (userId.isEmpty) {
      throwError("User is not authenticated.")
      -1
    } else {
      userId.get
    }
  }

  def getUserIdFromRequest(request: RequestWithAttributes[_]): Future[Long] = {
    Future.successful {
      getRequestUserId(request)
    }
  }

  private def throwError(message: String): Unit = {
    val error = UnauthorizedAccessException(message)
    logger.warn("Unauthorised access detected ["+message+"]", error)
    throw error
  }

  private def checkAuthResult(ok: Boolean, message: String): Boolean = {
    if (!ok) {
      throwError(message)
    }
    ok
  }

  private def setAuthResult(request: RequestWithAttributes[_], ok: Boolean, message: String): Boolean = {
    if (!request.authorised) {
      request.authorised = true
    }
    checkAuthResult(ok, message)
  }

  def getAccountInfo(request: RequestWithAttributes[_]): Future[ActualUserInfo] = {
    getPrincipal(request).flatMap { accountInfo =>
      accountManager.getUserAccountsForUid(accountInfo.uid).map { userAccounts =>
        new ActualUserInfo(accountInfo.uid, accountInfo.email, accountInfo.name, userAccounts)
      }
    }
  }

  def getPrincipal(request: RequestWithAttributes[_]): Future[ActualUserInfo] = {
    Future.successful {
      profileResolver.resolveUserInfo(request).orNull
    }
  }

  private def checkHasPrincipal(request: RequestWithAttributes[_], skipForNonSSO: Boolean): Future[Boolean] = {
    val check = if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      getPrincipal(request).map { principal =>
        principal != null
      }
    } else {
      Future.successful(skipForNonSSO)
    }
    check.map(setAuthResult(request, _, "User is not authenticated"))
  }

}
