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

package controllers

import config.Configurations
import controllers.ConformanceService.{KeystoreInfo, TestSuiteUploadInfo}
import controllers.util.{Parameters, _}
import exceptions.{ErrorCodes, NotFoundException}
import managers._
import models.Enums.TestSuiteReplacementChoice.{PROCEED, TestSuiteReplacementChoice}
import models.Enums.{Result => _, _}
import models._
import models.prerequisites.PrerequisiteUtil
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils._
import utils.signature.SigUtils

import java.io.File
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.security.cert.{Certificate, CertificateExpiredException, CertificateNotYetValidException, X509Certificate}
import java.security.{KeyStore, NoSuchAlgorithmException}
import java.util.UUID
import javax.inject.Inject
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

object ConformanceService {

  case class KeystoreInfo(failure: Option[Result], keystorePassword: Option[String], keyPassword: Option[String], keystoreFile: Option[File])

  object TestSuiteUploadInfo {

    def withFailure(result: Result): TestSuiteUploadInfo = {
      TestSuiteUploadInfo(Some(result), None, None)
    }

    def withActions(actions: List[TestSuiteDeploymentAction]): TestSuiteUploadInfo = {
      TestSuiteUploadInfo(None, Some(actions), None)
    }

    def withResult(result: TestSuiteUploadResult): TestSuiteUploadInfo = {
      TestSuiteUploadInfo(None, None, Some(result))
    }

  }

  case class TestSuiteUploadInfo(failure: Option[Result], actions: Option[List[TestSuiteDeploymentAction]], uploadResult: Option[TestSuiteUploadResult])

}

class ConformanceService @Inject() (authorizedAction: AuthorizedAction,
                                    cc: ControllerComponents,
                                    reportManager: ReportManager,
                                    systemManager: SystemManager,
                                    endpointManager: EndPointManager,
                                    specificationManager: SpecificationManager,
                                    domainManager: DomainManager,
                                    communityManager: CommunityManager,
                                    conformanceManager: ConformanceManager,
                                    actorManager: ActorManager,
                                    testSuiteManager: TestSuiteManager,
                                    testResultManager: TestResultManager,
                                    testCaseManager: TestCaseManager,
                                    parameterManager: ParameterManager,
                                    authorizationManager: AuthorizationManager,
                                    communityLabelManager: CommunityLabelManager,
                                    repositoryUtils: RepositoryUtils)
                                   (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[ConformanceService])

  def getDomain(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomain(request, domainId).flatMap { _ =>
      domainManager.getDomains(Some(List(domainId))).map { domains =>
        val json = JsonUtil.jsDomain(domains.head, withApiKeys = true).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets the list of domains
   */
  def getDomains: Action[AnyContent] = authorizedAction.async { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
    authorizationManager.canViewDomains(request, ids, snapshotId).flatMap { _ =>
      domainManager.getDomains(ids, snapshotId).map { domains =>
        val withApiKeys = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.KEYS)
          .getOrElse(ids.exists(_.nonEmpty))
        val json = JsonUtil.jsDomains(domains, withApiKeys).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def searchDomains: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewAllDomains(request).flatMap { _ =>
      val filter = ParameterExtractor.optionalQueryParameter(request, Parameters.FILTER)
      val page = ParameterExtractor.extractPageNumber(request)
      val limit = ParameterExtractor.extractPageLimit(request)
      domainManager.searchDomains(page, limit, filter).map { result =>
        val json = JsonUtil.jsSearchResult(result, list => JsonUtil.jsDomains(list, withApiKeys = false)).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getDomainOfSpecification(specId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewDomainBySpecificationId(request, specId).flatMap { _ =>
      domainManager.getDomainOfSpecification(specId).map { domain =>
        val json = JsonUtil.jsDomain(domain, withApiKeys = false).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getDomainOfActor(actorId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewActor(request, actorId).flatMap { _ =>
      domainManager.getDomainOfActor(actorId).map { domain =>
        val json = JsonUtil.jsDomain(domain, withApiKeys = false).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets the domain of the given community
   */
  def getCommunityDomain: Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewDomainByCommunityId(request, communityId).flatMap { _ =>
      domainManager.getCommunityDomain(communityId).map { domain =>
        if (domain.isDefined) {
          val json = JsonUtil.jsDomain(domain.get, withApiKeys = false).toString()
          ResponseConstructor.constructJsonResponse(json)
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  /**
   * Gets the applicable domains for the given community (a specific domain or all domains).
   */
  def getCommunityDomains: Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewDomainByCommunityId(request, communityId).flatMap { _ =>
      domainManager.getCommunityDomain(communityId).flatMap { communityDomain =>
        val domainLookup = if (communityDomain.isDefined) {
          Future.successful {
            (List(communityDomain.get), Some(communityDomain.get.id))
          }
        } else {
          domainManager.getDomains(None).map { domains =>
            (domains, None)
          }
        }
        domainLookup.map { domainInfo =>
          val json = JsonUtil.jsCommunityDomains(domainInfo._1, domainInfo._2).toString()
          ResponseConstructor.constructJsonResponse(json)
        }
      }
    }
  }

  def searchCommunityDomains: Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewDomainByCommunityId(request, communityId).flatMap { _ =>
      val filter = ParameterExtractor.optionalQueryParameter(request, Parameters.FILTER)
      val page = ParameterExtractor.extractPageNumber(request)
      val limit = ParameterExtractor.extractPageLimit(request)
      domainManager.searchCommunityDomains(page, limit, filter, communityId).map { result =>
        val json = JsonUtil.jsSearchResult(result, list => JsonUtil.jsDomains(list, withApiKeys = false)).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets the list of specifications
   */
  def getSpecs: Action[AnyContent] = authorizedAction.async { request =>
    val ids = ParameterExtractor.extractLongIdsBodyParameter(request)
    val domainIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.DOMAIN_IDS)
    val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS)
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    val auth = if (domainIds.isDefined && domainIds.get.nonEmpty) {
      authorizationManager.canViewDomains(request, domainIds, snapshotId)
    } else {
      authorizationManager.canViewSpecifications(request, ids, snapshotId)
    }
    auth.flatMap { _ =>
      specificationManager.getSpecifications(ids, domainIds, groupIds, withGroups = false, snapshotId).map { result =>
        val json = JsonUtil.jsSpecifications(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  private def nameForBadge(badgeFile: File, baseName: String): String = {
    val extension = FilenameUtils.getExtension(badgeFile.getName)
    if (extension.isEmpty) {
      baseName
    } else {
      baseName + "." + extension.toLowerCase
    }
  }

  def getSpecification(specificationId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSpecification(request, specificationId).flatMap { _ =>
      specificationManager.getSpecificationById(specificationId).map { result =>
        val successBadge = repositoryUtils.getConformanceBadge(specificationId, None, None, TestResultStatus.SUCCESS.toString, exactMatch = true, forReport = false)
        val otherBadge = repositoryUtils.getConformanceBadge(specificationId, None, None, TestResultStatus.UNDEFINED.toString, exactMatch = true, forReport = false)
        val failureBadge = repositoryUtils.getConformanceBadge(specificationId, None, None, TestResultStatus.FAILURE.toString, exactMatch = true, forReport = false)
        val badgeStatus = BadgeStatus(
          successBadge.map(nameForBadge(_, "success")),
          failureBadge.map(nameForBadge(_, "failure")),
          otherBadge.map(nameForBadge(_, "other"))
        )
        val successBadgeForReport = repositoryUtils.getConformanceBadge(specificationId, None, None, TestResultStatus.SUCCESS.toString, exactMatch = true, forReport = true)
        val otherBadgeForReport = repositoryUtils.getConformanceBadge(specificationId, None, None, TestResultStatus.UNDEFINED.toString, exactMatch = true, forReport = true)
        val failureBadgeForReport = repositoryUtils.getConformanceBadge(specificationId, None, None, TestResultStatus.FAILURE.toString, exactMatch = true, forReport = true)
        val badgeStatusForReport = BadgeStatus(
          successBadgeForReport.map(nameForBadge(_, "success.report")),
          failureBadgeForReport.map(nameForBadge(_, "failure.report")),
          otherBadgeForReport.map(nameForBadge(_, "other.report"))
        )
        val json = JsonUtil.jsSpecification(result, withApiKeys = true, Some((badgeStatus, badgeStatusForReport))).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets the list of specifications
   */
  def getActors: Action[AnyContent] = authorizedAction.async { request =>
    val ids = ParameterExtractor.extractLongIdsBodyParameter(request)
    authorizationManager.canViewActors(request, ids).flatMap { _ =>
      val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
      actorManager.getActorsWithSpecificationId(ids, specificationIds).map { result =>
        val json = JsonUtil.jsActorsNonCase(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getActor(actorId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageActor(request, actorId).flatMap { _ =>
      val specificationId = ParameterExtractor.requiredQueryParameter(request, Parameters.SPEC).toLong
      actorManager.getActorsWithSpecificationId(Some(List(actorId)), Some(List(specificationId))).map { results =>
        val result = results.headOption
        if (result.isDefined) {
          val successBadge = repositoryUtils.getConformanceBadge(specificationId, Some(result.get.id), None, TestResultStatus.SUCCESS.toString, exactMatch = true, forReport = false)
          val otherBadge = repositoryUtils.getConformanceBadge(specificationId, Some(result.get.id), None, TestResultStatus.UNDEFINED.toString, exactMatch = true, forReport = false)
          val failureBadge = repositoryUtils.getConformanceBadge(specificationId, Some(result.get.id), None, TestResultStatus.FAILURE.toString, exactMatch = true, forReport = false)
          val badgeStatus = BadgeStatus(
            successBadge.map(nameForBadge(_, "success")),
            failureBadge.map(nameForBadge(_, "failure")),
            otherBadge.map(nameForBadge(_, "other"))
          )
          val successBadgeForReport = repositoryUtils.getConformanceBadge(specificationId, Some(result.get.id), None, TestResultStatus.SUCCESS.toString, exactMatch = true, forReport = true)
          val otherBadgeForReport = repositoryUtils.getConformanceBadge(specificationId, Some(result.get.id), None, TestResultStatus.UNDEFINED.toString, exactMatch = true, forReport = true)
          val failureBadgeForReport = repositoryUtils.getConformanceBadge(specificationId, Some(result.get.id), None, TestResultStatus.FAILURE.toString, exactMatch = true, forReport = true)
          val badgeStatusForReport = BadgeStatus(
            successBadgeForReport.map(nameForBadge(_, "success.report")),
            failureBadgeForReport.map(nameForBadge(_, "failure.report")),
            otherBadgeForReport.map(nameForBadge(_, "other.report"))
          )
          val json = JsonUtil.jsActor(result.get, Some((badgeStatus, badgeStatusForReport))).toString()
          ResponseConstructor.constructJsonResponse(json)
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def searchActors(): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    authorizationManager.canViewActors(request, None, snapshotId).flatMap { _ =>
      val domainIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.DOMAIN_IDS)
      val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS)
      val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
      actorManager.searchActors(domainIds, specificationIds, groupIds, snapshotId).map { result =>
        val json = JsonUtil.jsActorsNonCase(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def searchActorsInDomain(): Action[AnyContent] = authorizedAction.async { request =>
    val domainId = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    authorizationManager.canViewActorsByDomainId(request, domainId, snapshotId).flatMap { _ =>
      val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS)
      val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
      actorManager.searchActors(Some(List(domainId)), specificationIds, groupIds, snapshotId).map { result =>
        val json = JsonUtil.jsActorsNonCase(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getAvailableConformanceStatements(systemId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSystem(request, systemId).flatMap { _ =>
      val domainId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.DOMAIN_ID)
      conformanceManager.getAvailableConformanceStatements(domainId, systemId).map { result =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsConformanceStatementItemInfo(result).toString)
      }
    }
  }

  /**
   * Gets the specifications that are defined/tested in the platform
   */
  def getDomainSpecs(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSpecificationsByDomainId(request, domainId).flatMap { _ =>
      specificationManager.getSpecificationsWithGroups(domainId).map { specs =>
        val json = JsonUtil.jsSpecifications(specs).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getDomainSpecsWithPaging(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSpecificationsByDomainId(request, domainId).flatMap { _ =>
      val filter = ParameterExtractor.optionalQueryParameter(request, Parameters.FILTER)
      val page = ParameterExtractor.extractPageNumber(request)
      val limit = ParameterExtractor.extractPageLimit(request)
      specificationManager.getSpecificationsWithPaging(domainId, filter, page, limit).map { result =>
        val json = JsonUtil.jsSearchResult(result, JsonUtil.jsDomainSpecifications).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets actors defined  for the spec
   */
  def getSpecActors(specId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSpecification(request, specId).flatMap { _ =>
      actorManager.getActorsWithSpecificationId(None, Some(List(specId))).map { actors =>
        val json = JsonUtil.jsActorsNonCase(actors).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getSpecTestSuites(specId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSpecification(request, specId).flatMap { _ =>
      val filter = ParameterExtractor.optionalQueryParameter(request, Parameters.FILTER)
      val page = ParameterExtractor.extractPageNumber(request)
      val limit = ParameterExtractor.extractPageLimit(request)
      testSuiteManager.getTestSuitesWithSpecificationId(filter, page, limit, specId).map { result =>
        val json = JsonUtil.jsSearchResult(result, JsonUtil.jsTestSuitesList).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getSpecSharedTestSuites(specId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSpecification(request, specId).flatMap { _ =>
      testSuiteManager.getSharedTestSuitesWithSpecificationId(specId).map { result =>
        val json = JsonUtil.jsTestSuitesList(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def searchSharedTestSuites(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomain(request, domainId).flatMap { _ =>
      val filter = ParameterExtractor.optionalQueryParameter(request, Parameters.FILTER)
      val page = ParameterExtractor.extractPageNumber(request)
      val limit = ParameterExtractor.extractPageLimit(request)
      testSuiteManager.searchSharedTestSuitesWithDomainId(filter, page, limit, domainId).map { result =>
        val json = JsonUtil.jsSearchResult(result, JsonUtil.jsTestSuitesList).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getSharedTestSuites(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomain(request, domainId).flatMap { _ =>
      testSuiteManager.getSharedTestSuitesWithDomainId(domainId).map { testSuites =>
        val json = JsonUtil.jsTestSuitesList(testSuites).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def createDomain(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCreateDomainAsync(request).flatMap { _ =>
      val domain = ParameterExtractor.extractDomain(request)
      domainManager.createDomain(domain).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def updateDomain(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateDomain(request, domainId).flatMap { _ =>
      domainManager.checkDomainExists(domainId).flatMap { domainExists =>
        if (domainExists) {
          val shortName:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
          val fullName:String = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
          val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
          val reportMetadata:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.METADATA)
          domainManager.updateDomain(domainId, shortName, fullName, description, reportMetadata).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          communityLabelManager.getLabel(request, LabelType.Domain).map { domainLabel =>
            throw NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, domainLabel + " with ID '" + domainId + "' not found.")
          }
        }
      }
    }
  }

  def createActor(): Action[AnyContent] = authorizedAction.async { request =>
    val paramMap = ParameterExtractor.paramMap(request)
    val specificationId = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SPECIFICATION_ID).toLong
    authorizationManager.canCreateActor(request, specificationId).flatMap { _ =>
      val actor = ParameterExtractor.extractActor(paramMap)
      actorManager.checkActorExistsInSpecification(actor.actorId, specificationId, None).flatMap { actorExists =>
        if (actorExists) {
          communityLabelManager.getLabelsByUserId(ParameterExtractor.extractUserId(request)).map { labels =>
            ResponseConstructor.constructBadRequestResponse(500, communityLabelManager.getLabel(labels, LabelType.Actor)+" already exists for this ID in the " + communityLabelManager.getLabel(labels, LabelType.Specification, single = true, lowercase = true)+".")
          }
        } else {
          val badgeInfo = ParameterExtractor.extractBadges(request, paramMap, forReport = false)
          if (badgeInfo._2.nonEmpty) {
            Future.successful(badgeInfo._2.get)
          } else {
            val badgeInfoForReport = ParameterExtractor.extractBadges(request, paramMap, forReport = true)
            if (badgeInfoForReport._2.nonEmpty) {
              Future.successful(badgeInfoForReport._2.get)
            } else {
              val domainId = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.DOMAIN_ID).toLong
              actorManager.createActorWrapper(actor.toCaseObject(CryptoUtil.generateApiKey(), domainId), specificationId, BadgeInfo(badgeInfo._1.get, badgeInfoForReport._1.get)).map { _ =>
                ResponseConstructor.constructEmptyResponse
              }
            }
          }
        }
      }
    }
  }

  def createEndpoint(): Action[AnyContent] = authorizedAction.async { request =>
    val endpoint = ParameterExtractor.extractEndpoint(request)
    authorizationManager.canCreateEndpoint(request, endpoint.actor).flatMap { _ =>
      endpointManager.checkEndPointExistsForActor(endpoint.name, endpoint.actor, None).flatMap { endpointExists =>
        if (endpointExists) {
          communityLabelManager.getLabelsByUserId(ParameterExtractor.extractUserId(request)).map { labels =>
            ResponseConstructor.constructBadRequestResponse(500, communityLabelManager.getLabel(labels, LabelType.Endpoint)+" with this name already exists for the "+communityLabelManager.getLabel(labels, LabelType.Actor, single = true, lowercase = true))
          }
        } else{
          endpointManager.createEndpointWrapper(endpoint).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        }
      }
    }
  }

  def createParameter(): Action[AnyContent] = authorizedAction.async { request =>
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    authorizationManager.canManageActor(request, actorId).flatMap { _ =>
      val parameter = ParameterExtractor.extractParameter(request)
      if (parameter.endpoint == 0L) {
        // New endpoint.
        parameterManager.createParameterAndEndpoint(parameter, actorId).map { createdIds =>
          ResponseConstructor.constructJsonResponse(JsonUtil.jsEndpointId(createdIds._1).toString)
        }
      } else {
        // Existing endpoint.
        parameterManager.checkParameterExistsForEndpoint(parameter.name, parameter.endpoint, None).flatMap { exists =>
          if (exists) {
            communityLabelManager.getLabel(request, LabelType.Endpoint, single = true, lowercase = true).map { endpointLabel =>
              ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the " + endpointLabel + ".")
            }
          } else {
            parameterManager.createParameterWrapper(parameter).map { _ =>
              ResponseConstructor.constructJsonResponse(JsonUtil.jsEndpointId(parameter.endpoint).toString)
            }
          }
        }
      }
    }
  }

  def createSpecification(): Action[AnyContent] = authorizedAction.async { request =>
    val paramMap = ParameterExtractor.paramMap(request)
    val specification = ParameterExtractor.extractSpecification(paramMap)
    authorizationManager.canCreateSpecification(request, specification.domain).flatMap { _ =>
      val badgeInfo = ParameterExtractor.extractBadges(request, paramMap, forReport = false)
      if (badgeInfo._2.nonEmpty) {
        Future.successful(badgeInfo._2.get)
      } else {
        val badgeInfoForReport = ParameterExtractor.extractBadges(request, paramMap, forReport = true)
        if (badgeInfoForReport._2.nonEmpty) {
          Future.successful(badgeInfoForReport._2.get)
        } else {
          specificationManager.createSpecifications(specification, BadgeInfo(badgeInfo._1.get, badgeInfoForReport._1.get)).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        }
      }
    }
  }

  def getEndpointsForActor(actorId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewEndpoints(request, actorId).flatMap { _ =>
      endpointManager.getEndpointsForActor(actorId).map { endpoints =>
        val json = JsonUtil.jsEndpoints(endpoints).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getEndpoints: Action[AnyContent] = authorizedAction.async { request =>
    val ids = ParameterExtractor.extractLongIdsQueryParameter(request)
    authorizationManager.canViewEndpointsById(request, ids).flatMap { _ =>
      endpointManager.getEndpoints(ids).map { endpoints =>
        val json = JsonUtil.jsEndpoints(endpoints).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def deleteDomain(domain_id: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteDomain(request, domain_id).flatMap { _ =>
      domainManager.deleteDomain(domain_id).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  private def specsMatch(allowedIds: Set[Long], actions: List[TestSuiteDeploymentAction]): Boolean = {
    actions.foreach { action =>
      if (!allowedIds.contains(action.specification.get)) {
        return false
      }
    }
    true
  }

  def resolvePendingTestSuites(): Action[AnyContent] = authorizedAction.async { request =>
    for {
      // Extract parameters and inputs.
      domainId <- Future.successful(ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong)
      specificationIds <- Future.successful(ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SPEC_IDS))
      pendingFolderId <- Future.successful(ParameterExtractor.requiredBodyParameter(request, Parameters.PENDING_ID))
      sharedTestSuite <- Future.successful(specificationIds.isEmpty || specificationIds.get.isEmpty)
      // Authorisation check.
      _ <- {
        if (specificationIds.isDefined) {
          authorizationManager.canManageSpecifications(request, specificationIds.get)
        } else {
          authorizationManager.canManageDomain(request, domainId)
        }
      }
      // Initial checks and actions.
      uploadInfo <- {
        val overallActionStr = ParameterExtractor.requiredBodyParameter(request, Parameters.PENDING_ACTION)
        var overallAction: TestSuiteReplacementChoice = TestSuiteReplacementChoice.CANCEL
        if ("proceed".equals(overallActionStr)) {
          overallAction = TestSuiteReplacementChoice.PROCEED
        }
        if (overallAction == TestSuiteReplacementChoice.CANCEL) {
          testSuiteManager.cancelPendingTestSuiteActions(pendingFolderId, domainId, sharedTestSuite).map { result =>
            TestSuiteUploadInfo.withResult(result)
          }
        } else {
          val actionsStr = ParameterExtractor.optionalBodyParameter(request, Parameters.ACTIONS)
          if (actionsStr.isDefined) {
            val actions = JsonUtil.parseJsPendingTestSuiteActions(actionsStr.get)
            if (!sharedTestSuite && !specsMatch(specificationIds.get.toSet, actions.filter(_.specification.isDefined))) {
              Future.successful {
                TestSuiteUploadInfo.withFailure(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_PARAM, "Provided actions don't match selected specifications"))
              }
            } else {
              Future.successful {
                TestSuiteUploadInfo.withActions(actions)
              }
            }
          } else {
            if (sharedTestSuite) {
              // We would have this case if we had a new shared test suite with only validation warnings.
              Future.successful {
                TestSuiteUploadInfo.withActions(List(new TestSuiteDeploymentAction(None, PROCEED, updateTestSuite = true, updateActors = None, sharedTestSuite = true, testCaseUpdates = None)))
              }
            } else if (specificationIds.isDefined && specificationIds.get.nonEmpty) {
              // We can have this case if we had no needed confirmation for deployment to specifications.
              val actions = specificationIds.get.map { specId =>
                new TestSuiteDeploymentAction(Some(specId), TestSuiteReplacementChoice.PROCEED, updateTestSuite = false, updateActors = Some(false), sharedTestSuite = false, testCaseUpdates = None)
              }
              Future.successful {
                TestSuiteUploadInfo.withActions(actions)
              }
            } else {
              Future.successful {
                TestSuiteUploadInfo.withFailure(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_PARAM, "Either a shared test suite is expected or a set of specifications"))
              }
            }
          }
        }
      }
      // Preparation of the result based on the parsed actions.
      uploadInfo <- {
        if (uploadInfo.failure.isEmpty && uploadInfo.uploadResult.isEmpty) {
          if (uploadInfo.actions.isDefined) {
            testSuiteManager.completePendingTestSuiteActions(pendingFolderId, domainId, sharedTestSuite, uploadInfo.actions.get).map { result =>
              TestSuiteUploadInfo.withResult(result)
            }
          } else {
            throw new IllegalStateException("Expected to have actions to process")
          }
        } else {
          // Nothing to do. Propagate the earlier results.
          Future.successful {
            uploadInfo
          }
        }
      }
      // Preparation of the final response.
      result <- {
        Future.successful {
          if (uploadInfo.failure.isEmpty) {
            if (uploadInfo.uploadResult.isDefined) {
              val json = JsonUtil.jsTestSuiteUploadResult(uploadInfo.uploadResult.get).toString()
              ResponseConstructor.constructJsonResponse(json)
            } else {
              throw new IllegalStateException("Expected to have a result to process")
            }
          } else {
            uploadInfo.failure.get
          }
        }
      }
    } yield result
  }

  def deployTestSuiteToSpecifications(): Action[AnyContent] = authorizedAction.async { request =>
    val action = for {
      paramMap <- Future.successful(ParameterExtractor.paramMap(request))
      domainId <- Future.successful(ParameterExtractor.requiredBodyParameter(paramMap, Parameters.DOMAIN_ID).toLong)
      specIds <- Future.successful(ParameterExtractor.optionalLongCommaListBodyParameter(paramMap, Parameters.SPEC_IDS))
      sharedTestSuite <- Future.successful(ParameterExtractor.optionalBooleanBodyParameter(paramMap, Parameters.SHARED).getOrElse(false))
      _ <- {
        if (specIds.isDefined) {
          authorizationManager.canManageSpecifications(request, specIds.get)
        } else {
          authorizationManager.canManageDomain(request, domainId)
        }
      }
      response <- {
        var response:Result = null
        val testSuiteFileName = "ts_"+RandomStringUtils.secure().next(10, false, true)+".zip"
        ParameterExtractor.extractFiles(request).get(Parameters.FILE) match {
          case Some(testSuite) =>
            if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
              val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
              val scanResult = virusScanner.scan(testSuite.file)
              if (!ClamAVClient.isCleanReply(scanResult)) {
                response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Test suite failed virus scan.")
              }
            }
            if (response == null) {
              val file = Paths.get(
                repositoryUtils.getTempFolder().getAbsolutePath,
                RandomStringUtils.secure().next(10, false, true),
                testSuiteFileName
              ).toFile
              file.getParentFile.mkdirs()
              Files.move(testSuite.file.toPath, file.toPath, StandardCopyOption.REPLACE_EXISTING)
              val contentType = testSuite.contentType
              logger.debug("Test suite file uploaded - filename: [" + testSuiteFileName + "] content type: [" + contentType + "]")
              testSuiteManager.deployTestSuiteFromZipFile(domainId, specIds, sharedTestSuite, file).map { result =>
                val json = JsonUtil.jsTestSuiteUploadResult(result).toString()
                ResponseConstructor.constructJsonResponse(json)
              }
            } else {
              Future.successful(response)
            }
          case None =>
            Future.successful {
              ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_PARAMS, "[" + Parameters.FILE + "] parameter is missing.")
            }
        }
      }
    } yield response
    action.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def getConformanceStatus(actorId: Long, sutId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
    authorizationManager.canViewConformanceStatus(request, actorId, sutId, snapshotId).flatMap { _ =>
      conformanceManager.getConformanceStatus(actorId, sutId, None, includeDisabled = true, snapshotId).map { results =>
        if (results.isEmpty) {
          ResponseConstructor.constructEmptyResponse
        } else {
          val json: String = JsonUtil.jsConformanceStatus(results.get).toString
          ResponseConstructor.constructJsonResponse(json)
        }
      }
    }
  }

  def getConformanceStatusForTestSuiteExecution(actorId: Long, sutId: Long, testSuite: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewConformanceStatus(request, actorId, sutId).flatMap { _ =>
      conformanceManager.getConformanceStatus(actorId, sutId, Some(testSuite), includeDisabled = false).map { results =>
        if (results.isEmpty) {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "No details could be found for the provided parameters")
        } else {
          val json: String = JsonUtil.jsConformanceStatus(results.get).toString
          ResponseConstructor.constructJsonResponse(json)
        }
      }
    }
  }

  def getTestSuiteTestCaseForExecution(testCaseId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestSuiteByTestCaseId(request, testCaseId).flatMap { _ =>
      testCaseManager.getTestCase(testCaseId.toString).map { testCase =>
        if (testCase.isEmpty) {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "The requested test case could not be found.")
        } else if (testCase.get.isDisabled) {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "The requested test case is disabled.")
        } else {
          val json: String = JsonUtil.jsTestCase(testCase.get).toString
          ResponseConstructor.constructJsonResponse(json)
        }
      }
    }
  }

  private def getPageOrDefault(_page: Option[String] = None):Int = _page match {
    case Some(p) => p.toInt
    case None => Constants.defaultPage.toInt
  }

  private def getLimitOrDefault(_limit: Option[String] = None):Int  = _limit match {
    case Some(l) => l.toInt
    case None => Constants.defaultLimit.toInt
  }


  def getConformanceOverviewForOrganisation(organisationId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
    for {
      _ <- authorizationManager.canViewConformanceOverviewForOrganisation(request, organisationId, snapshotId)
      organisationIds <- Future.successful(Some(List(organisationId)))
      systemIds <- Future.successful(ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SYSTEM_IDS))
      page <- Future.successful(getPageOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.PAGE)))
      limit <- Future.successful(getLimitOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.LIMIT)))
      results <- {
        val fullResults = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL).toBoolean
        val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.DOMAIN_IDS)
        val specIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SPEC_IDS)
        val specGroupIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.GROUP_IDS)
        val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ACTOR_IDS)
        val status = ParameterExtractor.optionalListBodyParameter(request, Parameters.STATUS)
        val updateTimeBegin = ParameterExtractor.optionalBodyParameter(request, Parameters.UPDATE_TIME_BEGIN)
        val updateTimeEnd = ParameterExtractor.optionalBodyParameter(request, Parameters.UPDATE_TIME_END)
        val sortColumn = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_COLUMN)
        val sortOrder = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_ORDER)
        if (fullResults) {
          conformanceManager.getConformanceStatementsFull(domainIds, specIds, specGroupIds, actorIds,
            None, organisationIds, systemIds, None, None,
            status, updateTimeBegin, updateTimeEnd,
            sortColumn, sortOrder, snapshotId)
        } else {
          conformanceManager.getConformanceStatements(domainIds, specIds, specGroupIds, actorIds,
            None, organisationIds, systemIds, None, None,
            status, updateTimeBegin, updateTimeEnd,
            sortColumn, sortOrder, snapshotId)
        }
      }
      result <- {
        // Return only the requested page
        val count = results.size
        val pageResults = results.slice((page - 1) * limit, (page - 1) * limit + limit)
        val json = JsonUtil.jsConformanceResultFullList(SearchResult(pageResults, count), None).toString()
        Future.successful(ResponseConstructor.constructJsonResponse(json))
      }
    } yield result
  }


  def getConformanceOverview(): Action[AnyContent] = authorizedAction.async { request =>
    val communityIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.COMMUNITY_IDS)
    for {
      _ <- authorizationManager.canViewConformanceOverview(request, communityIds)
      organisationIds <- Future.successful(ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ORG_IDS))
      systemIds <- Future.successful(ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SYSTEM_IDS))
      page <- Future.successful(getPageOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.PAGE)))
      limit <- Future.successful(getLimitOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.LIMIT)))
      results <- {
        val snapshotId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.SNAPSHOT)
        val fullResults = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL).toBoolean
        val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.DOMAIN_IDS)
        val specIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SPEC_IDS)
        val specGroupIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.GROUP_IDS)
        val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ACTOR_IDS)
        val orgParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, Parameters.ORGANISATION_PARAMETERS))
        val sysParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_PARAMETERS))
        val status = ParameterExtractor.optionalListBodyParameter(request, Parameters.STATUS)
        val updateTimeBegin = ParameterExtractor.optionalBodyParameter(request, Parameters.UPDATE_TIME_BEGIN)
        val updateTimeEnd = ParameterExtractor.optionalBodyParameter(request, Parameters.UPDATE_TIME_END)
        val sortColumn = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_COLUMN)
        val sortOrder = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_ORDER)
        if (fullResults) {
          conformanceManager.getConformanceStatementsFull(domainIds, specIds, specGroupIds, actorIds,
            communityIds, organisationIds, systemIds, orgParameters, sysParameters,
            status, updateTimeBegin, updateTimeEnd,
            sortColumn, sortOrder, snapshotId)
        } else {
          conformanceManager.getConformanceStatements(domainIds, specIds, specGroupIds, actorIds,
            communityIds, organisationIds, systemIds, orgParameters, sysParameters,
            status, updateTimeBegin, updateTimeEnd,
            sortColumn, sortOrder, snapshotId)
        }
      }
      parameterData <- {
        val forExport = ParameterExtractor.optionalBodyParameter(request, Parameters.EXPORT).getOrElse("false").toBoolean
        if (forExport && communityIds.isDefined && communityIds.get.size == 1) {
          communityManager.getParameterInfo(communityIds.get.head, organisationIds, systemIds).map(Some(_))
        } else {
          Future.successful(None)
        }
      }
      result <- {
        // Return only the requested page
        val count = results.size
        val pageResults = results.slice((page - 1) * limit, (page - 1) * limit + limit)
        val json = JsonUtil.jsConformanceResultFullList(SearchResult(pageResults, count), parameterData).toString()
        Future.successful(ResponseConstructor.constructJsonResponse(json))
      }
    } yield result
  }

  def deleteTestResults(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.COMMUNITY_ID)
    authorizationManager.canDeleteTestResults(request, communityId).flatMap { _ =>
      val sessionIds = JsonUtil.parseStringArray(ParameterExtractor.requiredBodyParameter(request, Parameters.SESSION_IDS))
      testResultManager.deleteTestSessions(sessionIds).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def deleteAllObsoleteTestResults(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteAllObsoleteTestResults(request).flatMap { _ =>
      testResultManager.deleteAllObsoleteTestResults().map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def deleteObsoleteTestResultsForOrganisation(): Action[AnyContent] = authorizedAction.async { request =>
    val organisationId = ParameterExtractor.requiredQueryParameter(request, Parameters.ORGANIZATION_ID).toLong
    authorizationManager.canDeleteObsoleteTestResultsForOrganisation(request, organisationId).flatMap { _ =>
      testResultManager.deleteObsoleteTestResultsForOrganisationWrapper(organisationId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def deleteObsoleteTestResultsForCommunity(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canDeleteObsoleteTestResultsForCommunity(request, communityId).flatMap { _ =>
      testResultManager.deleteObsoleteTestResultsForCommunityWrapper(communityId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def deleteCommunityKeystore(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewConformanceCertificateSettings(request, communityId).flatMap { _ =>
      communityManager.deleteCommunityKeystore(communityId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def downloadCommunityKeystore(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewConformanceCertificateSettings(request, communityId).flatMap { _ =>
      communityManager.getCommunityKeystore(communityId, decryptKeys = false).map { keystoreInfo =>
        if (keystoreInfo.isDefined) {
          val tempFile = Files.createTempFile("itb", "store")
          try {
            Files.write(tempFile, Base64.decodeBase64(MimeUtil.getBase64FromDataURL(keystoreInfo.get.keystoreFile)))
          } catch {
            case e:Exception =>
              FileUtils.deleteQuietly(tempFile.toFile)
              throw new IllegalStateException("Unable to generate keystore file", e)
          }
          Ok.sendFile(
            content = tempFile.toFile,
            inline = false,
            onClose = () => { FileUtils.deleteQuietly(tempFile.toFile) }
          )
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getCommunityKeystoreInfo(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewConformanceCertificateSettings(request, communityId).flatMap { _ =>
      communityManager.getCommunityKeystoreType(communityId).map { storedKeystoreType =>
        if (storedKeystoreType.isDefined) {
          ResponseConstructor.constructJsonResponse(JsonUtil.jsCommunityKeystore(storedKeystoreType.get).toString())
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getConformanceCertificateSettings(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewConformanceCertificateSettings(request, communityId).flatMap { _ =>
      communityManager.getConformanceCertificateSettingsWrapper(communityId, defaultIfMissing = true, None).map { settings =>
        if (settings.isDefined) {
          val json = JsonUtil.jsConformanceSettings(settings.get)
          ResponseConstructor.constructJsonResponse(json.toString)
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getConformanceOverviewCertificateSettingsWithApplicableMessage(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewConformanceCertificateSettings(request, communityId).flatMap { _ =>
      val identifier = ParameterExtractor.optionalLongQueryParameter(request, Parameters.ID)
      val level = OverviewLevelType.withName(ParameterExtractor.requiredQueryParameter(request, Parameters.LEVEL))
      val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
      communityManager.getConformanceOverviewCertificateSettingsWrapper(communityId, defaultIfMissing = true, snapshotId, Some(level), identifier).map { settings =>
        if (settings.isDefined) {
          val json = JsonUtil.jsConformanceOverviewSettings(settings.get)
          ResponseConstructor.constructJsonResponse(json.toString)
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getResolvedMessageForConformanceOverviewCertificate(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewConformanceCertificateSettings(request, communityId).flatMap { _ =>
      val id = ParameterExtractor.requiredQueryParameter(request, Parameters.ID).toLong
      val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
      communityManager.getConformanceOverviewCertificateMessage(snapshotId.isDefined, id).flatMap { message =>
        if (message.isDefined) {
          if (message.get.indexOf("$") > 0) {
            // Resolve placeholders for message
            val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
            val domainId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.DOMAIN_ID)
            val groupId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.GROUP_ID)
            val specificationId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SPECIFICATION_ID)
            reportManager.resolveConformanceOverviewCertificateMessage(message.get, systemId, domainId, groupId, specificationId, snapshotId, communityId).map { message =>
              ResponseConstructor.constructStringResponse(message)
            }
          } else {
            Future.successful {
              ResponseConstructor.constructStringResponse(message.get)
            }
          }
        } else {
          Future.successful {
            ResponseConstructor.constructEmptyResponse
          }
        }
      }
    }
  }

  def getResolvedMessageForConformanceStatementCertificate(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewConformanceCertificateSettings(request, communityId).flatMap { _ =>
      val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
      communityManager.getConformanceStatementCertificateMessage(snapshotId, communityId).flatMap { message =>
        if (message.isDefined) {
          if (message.get.indexOf("$") > 0) {
            // Resolve placeholders for message
            val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
            val actorId = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID).toLong
            reportManager.resolveConformanceStatementCertificateMessage(message.get, actorId, systemId, communityId, snapshotId).map { message =>
              ResponseConstructor.constructStringResponse(message)
            }
          } else {
            Future.successful {
              ResponseConstructor.constructStringResponse(message.get)
            }
          }
        } else {
          Future.successful {
            ResponseConstructor.constructEmptyResponse
          }
        }
      }
    }
  }

  def getConformanceOverviewCertificateSettings(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewConformanceCertificateSettings(request, communityId).flatMap { _ =>
      communityManager.getConformanceOverviewCertificateSettingsWrapper(communityId, defaultIfMissing = true, None, None, None).map { settings =>
        if (settings.isDefined) {
          val json = JsonUtil.jsConformanceOverviewSettings(settings.get)
          ResponseConstructor.constructJsonResponse(json.toString)
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def saveCommunityKeystore(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val task = for {
      _ <- authorizationManager.canUpdateConformanceCertificateSettings(request, communityId)
      paramMap <- Future.successful(ParameterExtractor.paramMap(request))
      keystoreData <- {
        var response: Option[Result] = None
        val files = ParameterExtractor.extractFiles(request)
        var keystoreData: Option[String] = None
        if (files.contains(Parameters.FILE)) {
          if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
            val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
            val scanResult = virusScanner.scan(files(Parameters.FILE).file)
            if (!ClamAVClient.isCleanReply(scanResult)) {
              response = Some(ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Keystore file failed virus scan."))
            }
          }
          if (response.isEmpty) {
            keystoreData = Some(MimeUtil.getFileAsDataURL(files(Parameters.FILE).file, "application/octet-stream"))
          }
        }
        Future.successful((response, keystoreData))
      }
      result <- {
        if (keystoreData._1.isEmpty) {
          val keystorePassword = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.KEYSTORE_PASS)
          val keyPassword = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.KEY_PASS)
          val keystoreType = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.TYPE)
          communityManager.saveCommunityKeystore(communityId, keystoreType, keystoreData._2, keyPassword, keystorePassword).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful(keystoreData._1.get)
        }
      }
    } yield result
    task.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def conformanceOverviewCertificateEnabled(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewCommunityBasic(request, communityId).flatMap { _ =>
      val reportLevel = OverviewLevelType.withName(ParameterExtractor.requiredQueryParameter(request, Parameters.LEVEL))
      communityManager.conformanceOverviewCertificateEnabled(communityId, reportLevel).map { checkResult =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsExists(checkResult).toString())
      }
    }
  }

  def testCommunityKeystore(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val tempKeystorePath = Paths.get(repositoryUtils.getTempFolder().getAbsolutePath, UUID.randomUUID().toString)
    val task = for {
      _ <- authorizationManager.canUpdateConformanceCertificateSettings(request, communityId)
      paramMap <- Future.successful(ParameterExtractor.paramMap(request))
      keystoreInfo <- {
        var keystoreFile: Option[File] = None
        val files = ParameterExtractor.extractFiles(request)
        var response: Option[Result] = None
        if (files.contains(Parameters.FILE)) {
          keystoreFile = Some(files(Parameters.FILE).file)
          if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
            val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
            val scanResult = virusScanner.scan(keystoreFile.get)
            if (!ClamAVClient.isCleanReply(scanResult)) {
              response = Some(ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Keystore file failed virus scan."))
            }
          }
        }
        Future.successful {
          KeystoreInfo(response, None, None, keystoreFile)
        }
      }
      keystoreInfoToUse <- {
        if (keystoreInfo.failure.isEmpty) {
          val keystorePassword = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.KEYSTORE_PASS)
          val keyPassword = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.KEY_PASS)
          if (keystorePassword.isEmpty || keyPassword.isEmpty || keystoreInfo.keystoreFile.isEmpty) {
            communityManager.getCommunityKeystore(communityId, decryptKeys = true).map { storedSettings =>
              if (storedSettings.isEmpty) {
                throw new IllegalStateException("Missing keystore settings.")
              }
              val keystorePasswordToUse = keystorePassword.getOrElse(storedSettings.get.keystorePassword)
              val keyPasswordToUse = keyPassword.getOrElse(storedSettings.get.keyPassword)
              val keystoreFileToUse = keystoreInfo.keystoreFile.getOrElse {
                Files.write(tempKeystorePath, Base64.decodeBase64(MimeUtil.getBase64FromDataURL(storedSettings.get.keystoreFile)))
                tempKeystorePath.toFile
              }
              KeystoreInfo(None, Some(keystorePasswordToUse), Some(keyPasswordToUse), Some(keystoreFileToUse))
            }
          } else {
            Future.successful {
              KeystoreInfo(None, keystorePassword, keyPassword, keystoreInfo.keystoreFile)
            }
          }
        } else {
          Future.successful(keystoreInfo)
        }
      }
      result <- {
        if (keystoreInfoToUse.failure.isEmpty) {
          // We have all the information we need - proceed.
          var problem: Option[String] = None
          var level: Option[String] = None
          val keystoreType = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.TYPE)
          val keystore = KeyStore.getInstance(keystoreType)
          Using.resource(Files.newInputStream(keystoreInfoToUse.keystoreFile.get.toPath)) { input =>
            try {
              keystore.load(input, keystoreInfoToUse.keystorePassword.get.toCharArray)
            } catch {
              case _: NoSuchAlgorithmException =>
                problem = Some("The keystore defines an invalid integrity check algorithm.")
                level = Some("error")
              case _: Exception =>
                problem = Some("The keystore could not be opened.")
                level = Some("error")
            }
          }
          if (problem.isEmpty) {
            var certificate: Option[Certificate] = None
            try {
              certificate = Some(SigUtils.checkKeystore(keystore, keystoreInfoToUse.keyPassword.get.toCharArray))
              if (certificate == null) {
                problem = Some("A valid key could not be found in the keystore.")
                level = Some("error")
              }
            } catch {
              case _: Exception =>
                problem = Some("The key could not be read from the keystore.")
                level = Some("error")
            }
            if (problem.isEmpty) {
              certificate.get match {
                case x509Cert: X509Certificate =>
                  try {
                    SigUtils.checkCertificateValidity(x509Cert)
                    if (!SigUtils.checkCertificateUsage(x509Cert) || !SigUtils.checkCertificateExtendedUsage(x509Cert)) {
                      problem = Some("The provided certificate is not valid for signature use.")
                      level = Some("warning")
                    }
                  } catch {
                    case _: CertificateExpiredException =>
                      problem = Some("The contained certificate is expired.")
                      level = Some("error")
                    case _: CertificateNotYetValidException =>
                      problem = Some("The certificate is not yet valid.")
                      level = Some("error")
                  }
                case _ =>
              }
            }
          }
          Future.successful {
            if (problem.isEmpty) {
              ResponseConstructor.constructEmptyResponse
            } else {
              val json = JsonUtil.jsConformanceSettingsValidation(problem.get, level.get)
              ResponseConstructor.constructJsonResponse(json.toString)
            }
          }
        } else {
          Future.successful(keystoreInfoToUse.failure.get)
        }
      }
    } yield result
    task.andThen { _ =>
      // Clean up the uploaded file (if any)
      if (request.body.asMultipartFormData.isDefined) {
        request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
      }
      // Clean up the temporary generated file from the stored settings (if any)
      if (Files.exists(tempKeystorePath)) {
        FileUtils.deleteQuietly(tempKeystorePath.toFile)
      }
    }
  }

  def updateStatementConfiguration(): Action[AnyContent] = authorizedAction.async { request =>
    val paramMap = ParameterExtractor.paramMap(request)
    val system = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SYSTEM_ID).toLong
    val actor = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.ACTOR_ID).toLong
    val organisationParameters = ParameterExtractor.extractOrganisationParameterValues(paramMap, Parameters.ORGANISATION_PARAMETERS, optional = true)
    val systemParameters = ParameterExtractor.extractSystemParameterValues(paramMap, Parameters.SYSTEM_PARAMETERS, optional = true)
    val statementParameters = ParameterExtractor.extractStatementParameterValues(paramMap, Parameters.STATEMENT_PARAMETERS, optional = true, system)
    val task = for {
      _ <- authorizationManager.canManageStatementConfiguration(request, system, actor, organisationParameters.exists(_.nonEmpty), systemParameters.exists(_.nonEmpty), statementParameters.exists(_.nonEmpty))
      result <- {
        val organisationFiles = new mutable.HashMap[Long, FileInfo]()
        val systemFiles = new mutable.HashMap[Long, FileInfo]()
        val statementFiles = new mutable.HashMap[Long, FileInfo]()
        val files = ParameterExtractor.extractFiles(request)
        files.foreach { entry =>
          val parts = entry._1.split('_')
          val ownerId = parts(1).toLong
          if (parts(0) == "org") {
            organisationFiles += (ownerId -> entry._2)
          } else if (parts(0) == "sys") {
            systemFiles += (ownerId -> entry._2)
          } else if (parts(0) == "stm") {
            statementFiles += (ownerId -> entry._2)
          } else {
            throw new IllegalArgumentException("Invalid name for uploaded file [%s]".formatted(entry._1))
          }
        }
        if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(files.map(entry => entry._2.file))) {
          Future.successful {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
          }
        } else {
          val userId = ParameterExtractor.extractUserId(request)
          conformanceManager.updateStatementConfiguration(userId, system, actor, organisationParameters, systemParameters, statementParameters, organisationFiles.toMap, systemFiles.toMap, statementFiles.toMap).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        }
      }
    } yield result
    task.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def getStatementParameterValues(): Action[AnyContent] = authorizedAction.async { request =>
    val system = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewEndpointConfigurationsForSystem(request, system).flatMap { _ =>
      val actor = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID).toLong
      conformanceManager.getStatementParameterValues(system, actor).map { values =>
        val json: String = JsonUtil.jsParametersWithValues(values, includeValues = true).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def checkConfigurations(): Action[AnyContent] = authorizedAction.async { request =>
    val system = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    authorizationManager.canViewEndpointConfigurationsForSystem(request, system).flatMap { _ =>
      val actor = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID).toLong
      conformanceManager.getSystemConfigurationStatus(system, actor).map { status =>
        status.map { configStatus =>
          if (configStatus.endpointParameters.isDefined) {
            // Filter out values with missing prerequisites.
            configStatus.endpointParameters = Some(PrerequisiteUtil.withValidPrerequisites(configStatus.endpointParameters.get))
          }
          configStatus
        }
        val json = JsonUtil.jsSystemConfigurationEndpoints(status, addValues = false, isAdmin = false) // The isAdmin flag only affects whether or a hidden value will be added (i.e. not applicable is addValues is false)
        ResponseConstructor.constructJsonResponse(json.toString)
      }
    }
  }

  def getDocumentationForPreview(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canPreviewDocumentation(request).map { _ =>
      val documentation = HtmlUtil.sanitizeEditorContent(ParameterExtractor.requiredBodyParameter(request, Parameters.CONTENT))
      ResponseConstructor.constructStringResponse(documentation)
    }
  }

  def getTestCaseDocumentation(id: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestCase(request, id.toString).flatMap { _ =>
      testCaseManager.getTestCaseDocumentation(id).map { documentation =>
        if (documentation.isDefined) {
          ResponseConstructor.constructStringResponse(documentation.get)
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getTestSuiteDocumentation(id: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestSuite(request, id).flatMap { _ =>
      testSuiteManager.getTestSuiteDocumentation(id).map { documentation =>
        if (documentation.isDefined) {
          ResponseConstructor.constructStringResponse(documentation.get)
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getStatementParametersOfCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      conformanceManager.getStatementParametersByCommunityId(communityId).map { result =>
        val json = JsonUtil.jsStatementParametersMinimal(result).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def linkSharedTestSuite(): Action[AnyContent] = authorizedAction.async { request =>
    val testSuiteId = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_SUITE_ID).toLong
    val specificationIds = ParameterExtractor.requiredLongListBodyParameter(request, Parameters.SPEC_IDS)
    authorizationManager.canManageSpecifications(request, specificationIds).flatMap { _ =>
      testSuiteManager.prepareSharedTestSuiteLink(testSuiteId, specificationIds).map { result =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSuiteUploadResult(result).toString())
      }
    }
  }

  def confirmLinkSharedTestSuite(): Action[AnyContent] = authorizedAction.async { request =>
    val testSuiteId = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_SUITE_ID).toLong
    val specificationIds = ParameterExtractor.requiredLongListBodyParameter(request, Parameters.SPEC_IDS)
    authorizationManager.canManageSpecifications(request, specificationIds).flatMap { _ =>
      val actionsStr = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTIONS)
      val actions = JsonUtil.parseJsPendingTestSuiteActions(actionsStr)
      if (!specsMatch(specificationIds.toSet, actions.filter(_.specification.isDefined))) {
        Future.successful {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_PARAM, "Provided actions don't match selected specifications")
        }
      } else {
        testSuiteManager.linkSharedTestSuiteWrapper(testSuiteId, actions).map { result =>
          ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSuiteUploadResult(result).toString())
        }
      }
    }
  }

  def unlinkSharedTestSuite(): Action[AnyContent] = authorizedAction.async { request =>
    val testSuiteId = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_SUITE_ID).toLong
    val specificationIds = ParameterExtractor.requiredLongListBodyParameter(request, Parameters.SPEC_IDS)
    authorizationManager.canManageSpecifications(request, specificationIds).flatMap { _ =>
      testSuiteManager.unlinkSharedTestSuite(testSuiteId, specificationIds).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def createConformanceSnapshot(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      val label = ParameterExtractor.requiredBodyParameter(request, Parameters.LABEL)
      val publicLabel = ParameterExtractor.optionalBodyParameter(request, Parameters.PUBLIC_LABEL)
      val isPublic = ParameterExtractor.requiredBodyParameter(request, Parameters.PUBLIC).toBoolean
      conformanceManager.createConformanceSnapshot(communityId, label, publicLabel, isPublic).map { snapshot =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsConformanceSnapshot(snapshot, public = false, withApiKey = true).toString)
      }
    }
  }

  def getConformanceSnapshot(snapshotId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val public = ParameterExtractor.requiredQueryParameter(request, Parameters.PUBLIC).toBoolean
    val auth = if (public) {
      authorizationManager.canViewConformanceSnapshot(request, snapshotId)
    } else {
      authorizationManager.canManageConformanceSnapshot(request, snapshotId)
    }
    auth.flatMap { _ =>
      conformanceManager.getConformanceSnapshot(snapshotId).map { snapshot =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsConformanceSnapshot(snapshot, public, withApiKey = false).toString)
      }
    }
  }

  def getConformanceSnapshots(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    val public = ParameterExtractor.requiredQueryParameter(request, Parameters.PUBLIC).toBoolean
    for {
      _ <- {
        if (public) {
          authorizationManager.canViewCommunityBasic(request, communityId)
        } else {
          authorizationManager.canManageCommunity(request, communityId)
        }
      }
      result <- {
        val withApiKeys = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.KEYS).getOrElse(false)
        conformanceManager.getConformanceSnapshotsWithLatest(communityId, public).map { snapshotData =>
          ResponseConstructor.constructJsonResponse(JsonUtil.jsConformanceSnapshotList(snapshotData._2, snapshotData._1, public, withApiKeys).toString)
        }
      }
    } yield result
  }

  def setLatestConformanceStatusLabel(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      val label = ParameterExtractor.optionalBodyParameter(request, Parameters.LABEL)
      conformanceManager.setLatestConformanceStatusLabel(communityId, label).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def editConformanceSnapshot(snapshotId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageConformanceSnapshot(request, snapshotId).flatMap { _ =>
      val label = ParameterExtractor.requiredBodyParameter(request, Parameters.LABEL)
      val publicLabel = ParameterExtractor.optionalBodyParameter(request, Parameters.PUBLIC_LABEL)
      val isPublic = ParameterExtractor.requiredBodyParameter(request, Parameters.PUBLIC).toBoolean
      conformanceManager.editConformanceSnapshot(snapshotId, label, publicLabel, isPublic).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def deleteConformanceSnapshot(snapshotId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageConformanceSnapshot(request, snapshotId).flatMap { _ =>
      conformanceManager.deleteConformanceSnapshot(snapshotId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def conformanceBadge(systemKey: String, actorKey: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canLookupConformanceBadge(request).flatMap { _ =>
      conformanceManager.getConformanceBadge(systemKey: String, actorKey: String, None, forReport = false).map { badge =>
        if (badge.isDefined && badge.get.exists()) {
          Ok.sendFile(content = badge.get)
        } else {
          NotFound
        }
      }
    }
  }

  def conformanceBadgeForSnapshot(systemKey: String, actorKey: String, snapshotKey: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canLookupConformanceBadge(request).flatMap { _ =>
      conformanceManager.getConformanceBadge(systemKey: String, actorKey: String, Some(snapshotKey), forReport = false).map { badge =>
        if (badge.isDefined && badge.get.exists()) {
          Ok.sendFile(content = badge.get)
        } else {
          NotFound
        }
      }
    }
  }

  def conformanceBadgeReportPreview(status: String, systemId: Long, specificationId: Long, actorId: Long): Action[AnyContent] = authorizedAction.async { request =>
    returnConformanceBadgeForReportPreview(status, systemId, specificationId, actorId, None, request)
  }

  def conformanceBadgeReportPreviewForSnapshot(status: String, systemId: Long, specificationId: Long, actorId: Long, snapshotId: Long): Action[AnyContent] = authorizedAction.async { request =>
    returnConformanceBadgeForReportPreview(status, systemId, specificationId, actorId, Some(snapshotId), request)
  }

  def conformanceBadgeByIds(systemId: Long, actorId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystem(request, systemId).flatMap { _ =>
      val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
      val forReport = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.REPORT)
      conformanceManager.getConformanceBadgeByIds(systemId, actorId, snapshotId, forReport.getOrElse(false)).map { badge =>
        if (badge.isDefined && badge.get.exists()) {
          Ok.sendFile(content = badge.get)
        } else {
          NotFound
        }
      }
    }
  }

  private def returnConformanceBadgeForReportPreview(status: String, systemId: Long, specificationId: Long, actorId: Long, snapshotId: Option[Long], request: RequestWithAttributes[_]): Future[Result] = {
    for {
      _ <- {
        if (snapshotId.isDefined) {
          authorizationManager.canViewSystemInSnapshot(request, systemId, snapshotId.get)
        } else {
          authorizationManager.canViewSystem(request, systemId)
        }
      }
      result <- {
        val badge = repositoryUtils.getConformanceBadge(specificationId, Some(actorId), snapshotId, status, exactMatch = false, forReport = true)
        if (badge.isDefined && badge.get.exists()) {
          Future.successful(Ok.sendFile(content = badge.get))
        } else {
          Future.successful(NotFound)
        }
      }
    } yield result
  }

  def conformanceBadgeUrl(systemId: Long, actorId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewSystem(request, systemId).flatMap { _ =>
      val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
      conformanceManager.getConformanceBadgeUrl(systemId, actorId, snapshotId).map { url =>
        if (url.isDefined) {
          ResponseConstructor.constructStringResponse(url.get)
        } else {
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getConformanceStatementsForSystem(systemId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
    authorizationManager.canViewConformanceStatements(request, systemId, snapshotId).flatMap { _ =>
      val page = ParameterExtractor.extractPageNumber(request)
      val limit = ParameterExtractor.extractPageLimit(request)
      val searchCriteria = ParameterExtractor.extractConformanceStatementSearchCriteria(request)
      conformanceManager.getConformanceStatementsForSystemPaged(page, limit, searchCriteria, systemId, snapshotId).map { result =>
        val json = JsonUtil.jsSearchResult(result, JsonUtil.jsConformanceStatementItems).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getConformanceStatement(systemId: Long, actorId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val snapshotId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.SNAPSHOT)
    authorizationManager.canViewConformanceStatements(request, systemId, snapshotId).flatMap { _ =>
      conformanceManager.getConformanceStatementsForSystem(systemId, Some(actorId), snapshotId, withDescriptions = true, withResults = false).flatMap { result =>
        val conformanceStatement = result.headOption
        if (conformanceStatement.isDefined) {
          conformanceManager.getConformanceStatus(actorId, systemId, None, includeDisabled = true, snapshotId).flatMap { results =>
            if (results.isDefined) {
              val systemInfoTask = if (systemId >= 0) {
                systemManager.getSystemProfile(systemId)
              } else if (snapshotId.isDefined) {
                // This is a deleted system from a snapshot.
                conformanceManager.getSystemInfoFromConformanceSnapshot(systemId, snapshotId.get)
              } else {
                throw new IllegalStateException("The conformance statement's system could not be found.")
              }
              systemInfoTask.map { systemInfo =>
                val json = JsonUtil.jsConformanceStatement(conformanceStatement.get, results.get, systemInfo).toString()
                ResponseConstructor.constructJsonResponse(json)
              }
            } else {
              Future.successful {
                ResponseConstructor.constructEmptyResponse
              }
            }
          }
        } else {
          Future.successful {
            ResponseConstructor.constructEmptyResponse
          }
        }
      }
    }
  }

}
