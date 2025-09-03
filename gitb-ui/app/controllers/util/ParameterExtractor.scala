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

package controllers.util

import config.Configurations
import exceptions.{ErrorCodes, InvalidRequestException}
import models.Enums._
import controllers.util.ParameterNames
import models.automation.TestServiceSearchCriteria
import models.statement.{AvailableStatementsSearchCriteria, ConformanceStatementSearchCriteria}
import models.theme.{Theme, ThemeFiles}
import models.{Actor, Badges, Communities, CommunityReportSettings, CommunityResources, Configs, Constants, Domain, DomainParameter, Endpoints, Enums, ErrorTemplates, FileInfo, LandingPages, LegalNotices, NamedFile, OrganisationParameterValues, Organizations, Parameters, SpecificationGroups, Specifications, SystemParameterValues, Systems, TestService, TestServiceWithParameter, Trigger, TriggerData, TriggerFireExpression, Triggers, Users}
import org.apache.commons.lang3.StringUtils
import play.api.mvc._
import utils.{ClamAVClient, CryptoUtil, HtmlUtil, JsonUtil, MimeUtil}

import java.awt.Color
import java.io.File
import java.net.URI
import java.util.concurrent.ThreadLocalRandom
import scala.collection.mutable.ListBuffer

object ParameterExtractor {

  private final val imageMimeTypes = Set("image/png", "image/x-png", "image/jpeg", "image/gif", "image/svg+xml", "image/vnd.microsoft.icon", "image/x-icon")
  private final val xslMimeTypes = Set("text/xml", Constants.MimeTypeXML, "text/xsl", "application/xslt+xml")

  def paramMap(request: Request[AnyContent]): Option[Map[String, Seq[String]]] = {
    var paramMap: Option[Map[String, Seq[String]]] = None
    if (request.body.asMultipartFormData.isDefined) {
      paramMap = Some(request.body.asMultipartFormData.get.dataParts)
    } else {
      paramMap = request.body.asFormUrlEncoded
    }
    paramMap
  }

  def extractFiles(request: Request[AnyContent]): Map[String, FileInfo] = {
    val fileMap = scala.collection.mutable.Map[String, FileInfo]()
    if (request.body.asMultipartFormData.isDefined) {
      request.body.asMultipartFormData.get.files.foreach { file =>
        fileMap += (file.key -> new FileInfo(file.key, file.filename, file.contentType, file.ref))
      }
    }
    fileMap.iterator.toMap
  }

  def extractConformanceStatementSearchCriteria(request: Request[AnyContent]): ConformanceStatementSearchCriteria = {
    ConformanceStatementSearchCriteria(
      filterText = ParameterExtractor.optionalQueryParameter(request, ParameterNames.FILTER).filter(s => !s.isBlank),
      succeeded = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.SUCCEEDED).getOrElse(true),
      failed = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.FAILED).getOrElse(true),
      incomplete = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.INCOMPLETE).getOrElse(true)
    )
  }

  def extractPageNumber(request:Request[AnyContent]): Long = {
    ParameterExtractor.optionalQueryParameter(request, ParameterNames.PAGE) match {
      case Some(v) => v.toLong
      case _ => 1L
    }
  }

  def extractPageLimit(request:Request[AnyContent]): Long = {
    ParameterExtractor.optionalQueryParameter(request, ParameterNames.LIMIT) match {
      case Some(v) => v.toLong
      case None => 10L
    }
  }

  def requiredQueryParameter(request:Request[AnyContent], parameter:String):String = {
    val param = request.getQueryString(parameter)
    if(param.isEmpty)
      throw InvalidRequestException(ErrorCodes.MISSING_PARAMS, "Parameter '" + parameter + "' is missing in the request")
    param.get
  }

  def optionalQueryParameter(request:Request[AnyContent], parameter:String):Option[String] = {
    val param = request.getQueryString(parameter)
    param
  }

  def optionalBooleanQueryParameter(request:Request[AnyContent], parameter:String):Option[Boolean] = {
    val param = request.getQueryString(parameter)
    if (param.isDefined) {
      Some(param.get.toBoolean)
    } else {
      None
    }
  }

  def optionalLongQueryParameter(request:Request[AnyContent], parameter:String):Option[Long] = {
    val param = request.getQueryString(parameter)
    if (param.isDefined) {
      Some(param.get.toLong)
    } else {
      None
    }
  }

  def extractOrganisationParameterValues(paramMap: Option[Map[String, Seq[String]]], parameterName: String, optional: Boolean): Option[List[OrganisationParameterValues]] = {
    var values: Option[List[OrganisationParameterValues]] = None
    if (optional) {
      val valuesJson = optionalBodyParameter(paramMap, parameterName)
      if (valuesJson.isDefined) {
        values = Some(JsonUtil.parseJsOrganisationParameterValues(valuesJson.get))
      }
    } else {
      val valuesJson = requiredBodyParameter(paramMap, parameterName)
      values = Some(JsonUtil.parseJsOrganisationParameterValues(valuesJson))
    }
    values
  }

  def extractSystemParameterValues(paramMap:Option[Map[String, Seq[String]]], parameterName: String, optional: Boolean): Option[List[SystemParameterValues]] = {
    var values: Option[List[SystemParameterValues]] = None
    if (optional) {
      val valuesJson = optionalBodyParameter(paramMap, parameterName)
      if (valuesJson.isDefined) {
        values = Some(JsonUtil.parseJsSystemParameterValues(valuesJson.get))
      }
    } else {
      val valuesJson = requiredBodyParameter(paramMap, parameterName)
      values = Some(JsonUtil.parseJsSystemParameterValues(valuesJson))
    }
    values
  }

  def extractStatementParameterValues(paramMap:Option[Map[String, Seq[String]]], parameterName: String, optional: Boolean, systemId: Long): Option[List[Configs]] = {
    var values: Option[List[Configs]] = None
    if (optional) {
      val valuesJson = optionalBodyParameter(paramMap, parameterName)
      if (valuesJson.isDefined) {
        values = Some(JsonUtil.parseJsConfigs(valuesJson.get, systemId))
      }
    } else {
      val valuesJson = requiredBodyParameter(paramMap, parameterName)
      values = Some(JsonUtil.parseJsConfigs(valuesJson, systemId))
    }
    values
  }

  def virusPresentInNamedFiles(values: Iterable[NamedFile]): Option[NamedFile] = {
    if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
      val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
      values.foreach { value =>
        // Check for virus. Do this regardless of the type of parameter as this can be changed later on.
        val scanResult = virusScanner.scan(value.file)
        if (!ClamAVClient.isCleanReply(scanResult)) {
          return Some(value)
        }
      }
    }
    None // No virus
  }

  def virusPresentInFiles(values: Iterable[File]): Boolean = {
    val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
    values.foreach { value =>
      // Check for virus. Do this regardless of the type of parameter as this can be changed later on.
      val scanResult = virusScanner.scan(value)
      if (!ClamAVClient.isCleanReply(scanResult)) {
        return true
      }
    }
    false
  }

  def optionalBodyParameterMulti(body: MultipartFormData[_], parameter:String): Option[String] = {
    var result: Option[String] = None
    val param = body.dataParts.get(parameter)
    if (param.isDefined) {
      val seq = param.get
      if (seq.nonEmpty) {
        result = Some(seq.head)
      }
    }
    result
  }

  def requiredBodyParameterMulti(body: MultipartFormData[_], parameter:String):String = {
    requiredBodyParameter(Some(body.dataParts), parameter)
  }

  def requiredBodyParameterMulti(request:Request[MultipartFormData[_]], parameter:String):String = {
    requiredBodyParameterMulti(request.body, parameter)
  }

  def requiredBodyParameter(request:Request[AnyContent], parameter:String):String = {
    requiredBodyParameter(request.body.asFormUrlEncoded, parameter)
  }

  def requiredBodyParameter(paramMap: Option[Map[String, Seq[String]]], parameter:String):String = {
    try {
      paramMap.get(parameter).head
    } catch {
      case _:NoSuchElementException =>
        throw InvalidRequestException(ErrorCodes.MISSING_PARAMS, "Parameter '" + parameter + "' is missing in the request")
    }
  }

  def optionalBodyParameter(paramMap: Option[Map[String, Seq[String]]], parameter:String): Option[String] = {
    try {
      paramMap.get(parameter).headOption
    } catch {
      case _:NoSuchElementException =>
        None
    }
  }

  def optionalArrayBodyParameter(paramMap: Option[Map[String, Seq[String]]], parameter: String): Option[Seq[String]] = {
    if (paramMap.isDefined && paramMap.get.contains(parameter)) {
      Some(paramMap.get(parameter).toList)
    } else {
      None
    }
  }

  def optionalLongBodyParameter(paramMap: Option[Map[String, Seq[String]]], parameter:String): Option[Long] = {
    try {
      val paramString = paramMap.get(parameter).headOption
      if (paramString.isDefined) {
        Some(paramString.get.toLong)
      } else {
        None
      }
    } catch {
      case _:NoSuchElementException =>
        None
    }
  }

  def optionalLongCommaListBodyParameter(paramMap: Option[Map[String, Seq[String]]], parameter: String): Option[List[Long]] = {
    try {
      val paramString = paramMap.get.get(parameter)
      if (paramString.isDefined) {
        Some(paramString.get.head.split(',').filter(_ != "").map(_.toLong).toList)
      } else {
        None
      }
    } catch {
      case _: NoSuchElementException =>
        None
    }
  }

  def optionalBodyParameter(request:Request[AnyContent], parameter:String):Option[String] = {
    optionalBodyParameter(request.body.asFormUrlEncoded, parameter)
  }

  def optionalLongBodyParameter(request:Request[AnyContent], parameter:String):Option[Long] = {
    optionalLongBodyParameter(request.body.asFormUrlEncoded, parameter)
  }

  def optionalLongBodyParameterMulti(request:Request[AnyContent], parameter:String):Option[Long] = {
    optionalLongBodyParameter(Some(request.body.asMultipartFormData.get.dataParts), parameter)
  }

  def optionalBooleanBodyParameter(request:Request[AnyContent], parameter:String):Option[Boolean] = {
    try {
      val paramList = request.body.asFormUrlEncoded.get(parameter)
      if(paramList.nonEmpty){
        Some(paramList.head.toBoolean)
      } else{
        None
      }
    } catch {
      case _:NoSuchElementException =>
        None
    }
  }

  def optionalBooleanBodyParameter(paramMap: Option[Map[String, Seq[String]]], parameter: String): Option[Boolean] = {
    try {
      val paramString = paramMap.get(parameter).headOption
      if (paramString.isDefined) {
        Some(paramString.get.toBoolean)
      } else {
        None
      }
    } catch {
      case _: NoSuchElementException =>
        None
    }
  }

  def optionalShortBodyParameter(request:Request[AnyContent], parameter:String):Option[Short] = {
    try {
      val paramList = request.body.asFormUrlEncoded.get(parameter)
      if(paramList.nonEmpty){
        Some(paramList.head.toShort)
      } else{
        None
      }
    } catch {
      case _:NoSuchElementException =>
        None
    }
  }

  def optionalIntBodyParameter(request:Request[AnyContent], parameter:String):Option[Int] = {
    try {
      val paramList = request.body.asFormUrlEncoded.get(parameter)
      if(paramList.nonEmpty){
        Some(paramList.head.toInt)
      } else{
        None
      }
    } catch {
      case _:NoSuchElementException =>
        None
    }
  }

  def extractOptionalUserId(request:RequestHeader):Option[Long] = {
    val userId = request.headers.get(ParameterNames.USER_ID)
    if (userId.isDefined) {
      Some(userId.get.toLong)
    } else {
      None
    }
  }

  def extractUserId(request:RequestHeader):Long = {
    request.headers.get(ParameterNames.USER_ID).get.toLong
  }

  def extractOrganizationInfo(paramMap: Option[Map[String, Seq[String]]]): Organizations = {
    val vendorSname = requiredBodyParameter(paramMap, ParameterNames.VENDOR_SNAME)
    val vendorFname = requiredBodyParameter(paramMap, ParameterNames.VENDOR_FNAME)
    val communityId = requiredBodyParameter(paramMap, ParameterNames.COMMUNITY_ID).toLong
    val landingPageId:Option[Long] = optionalLongBodyParameter(paramMap, ParameterNames.LANDING_PAGE_ID)
    val legalNoticeId:Option[Long] = optionalLongBodyParameter(paramMap, ParameterNames.LEGAL_NOTICE_ID)
    val errorTemplateId:Option[Long] = optionalLongBodyParameter(paramMap, ParameterNames.ERROR_TEMPLATE_ID)
    var template:Boolean = false
    var templateName: Option[String] = None
    if (Configurations.REGISTRATION_ENABLED) {
      template = optionalBodyParameter(paramMap, ParameterNames.TEMPLATE).getOrElse("false").toBoolean
      if (template) {
        templateName = optionalBodyParameter(paramMap, ParameterNames.TEMPLATE_NAME)
      }
    }
    Organizations(0L, vendorSname, vendorFname, OrganizationType.Vendor.id.toShort, adminOrganization = false, landingPageId, legalNoticeId, errorTemplateId, template = template, templateName, None, communityId)
  }

  def validCommunitySelfRegType(selfRegType: Short): Boolean = {
    selfRegType == SelfRegistrationType.NotSupported.id.toShort || selfRegType == SelfRegistrationType.PublicListing.id.toShort || selfRegType == SelfRegistrationType.PublicListingWithToken.id.toShort || selfRegType == SelfRegistrationType.Token.id.toShort
  }

  def extractCommunityReportSettings(paramMap:  Option[Map[String, Seq[String]]], communityId: Long): CommunityReportSettings = {
    val reportType = ReportType.apply(ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.TYPE).toShort)
    val signPdfReports = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.SIGN_PDF_REPORTS).toBoolean
    val useCustomPdfReports = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.USE_CUSTOM_PDF_REPORTS).toBoolean
    val useCustomPdfReportsWithCustomXml = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.USE_CUSTOM_PDFS_WITH_CUSTOM_XML).toBoolean
    val customPdfService = ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.CUSTOM_PDF_SERVICE)
    CommunityReportSettings(reportType.id.toShort, signPdfReports, useCustomPdfReports, useCustomPdfReportsWithCustomXml, customPdfService.filter(StringUtils.isNotBlank), communityId)
  }

  def extractCommunityInfo(request:Request[AnyContent]):Communities = {
    val sname = requiredBodyParameter(request, ParameterNames.COMMUNITY_SNAME)
    val fname = requiredBodyParameter(request, ParameterNames.COMMUNITY_FNAME)
    val email = optionalBodyParameter(request, ParameterNames.COMMUNITY_EMAIL)
    val description = optionalBodyParameter(request, ParameterNames.DESCRIPTION)
    val allowCertificateDownload = requiredBodyParameter(request, ParameterNames.ALLOW_CERTIFICATE_DOWNLOAD).toBoolean
    val allowStatementManagement = requiredBodyParameter(request, ParameterNames.ALLOW_STATEMENT_MANAGEMENT).toBoolean
    val allowSystemManagement = requiredBodyParameter(request, ParameterNames.ALLOW_SYSTEM_MANAGEMENT).toBoolean
    val allowPostTestOrganisationUpdate = requiredBodyParameter(request, ParameterNames.ALLOW_POST_TEST_ORG_UPDATE).toBoolean
    val allowPostTestSystemUpdate = requiredBodyParameter(request, ParameterNames.ALLOW_POST_TEST_SYS_UPDATE).toBoolean
    val allowPostTestStatementUpdate = requiredBodyParameter(request, ParameterNames.ALLOW_POST_TEST_STM_UPDATE).toBoolean
    var allowAutomationApi = false
    if (Configurations.AUTOMATION_API_ENABLED) {
      allowAutomationApi = requiredBodyParameter(request, ParameterNames.ALLOW_AUTOMATION_API).toBoolean
    }
    val allowCommunityView = requiredBodyParameter(request, ParameterNames.ALLOW_COMMUNITY_VIEW).toBoolean
    val interactionNotification = requiredBodyParameter(request, ParameterNames.COMMUNITY_INTERACTION_NOTIFICATION).toBoolean
    var selfRegType: Short = SelfRegistrationType.NotSupported.id.toShort
    var selfRegRestriction: Short = SelfRegistrationRestriction.NoRestriction.id.toShort
    var selfRegToken: Option[String] = None
    var selfRegTokenHelpText: Option[String] = None
    var selfRegNotification: Boolean = false
    var selfRegForceTemplateSelection: Boolean = false
    var selfRegForceRequiredProperties: Boolean = false
    if (Configurations.REGISTRATION_ENABLED) {
      selfRegType = requiredBodyParameter(request, ParameterNames.COMMUNITY_SELFREG_TYPE).toShort
      if (!validCommunitySelfRegType(selfRegType)) {
        throw new IllegalArgumentException("Unsupported value ["+selfRegType+"] for self-registration type")
      }
      selfRegToken = optionalBodyParameter(request, ParameterNames.COMMUNITY_SELFREG_TOKEN)
      selfRegTokenHelpText = optionalBodyParameter(request, ParameterNames.COMMUNITY_SELFREG_TOKEN_HELP_TEXT)
      if (selfRegTokenHelpText.isDefined) {
        selfRegTokenHelpText = Some(HtmlUtil.sanitizeEditorContent(selfRegTokenHelpText.get))
      }
      if (selfRegType == SelfRegistrationType.Token.id.toShort || selfRegType == SelfRegistrationType.PublicListingWithToken.id.toShort) {
        if (selfRegToken.isEmpty || StringUtils.isBlank(selfRegToken.get)) {
          throw new IllegalArgumentException("Missing self-registration token")
        }
      } else {
        selfRegToken = None
        selfRegTokenHelpText = None
      }
      if (selfRegType != SelfRegistrationType.NotSupported.id.toShort) {
        selfRegForceTemplateSelection = requiredBodyParameter(request, ParameterNames.COMMUNITY_SELFREG_FORCE_TEMPLATE).toBoolean
        selfRegForceRequiredProperties = requiredBodyParameter(request, ParameterNames.COMMUNITY_SELFREG_FORCE_PROPERTIES).toBoolean
        if (Configurations.EMAIL_ENABLED) {
          selfRegNotification = requiredBodyParameter(request, ParameterNames.COMMUNITY_SELFREG_NOTIFICATION).toBoolean
        }
        if (Configurations.AUTHENTICATION_SSO_ENABLED) {
          selfRegRestriction = ParameterExtractor.requiredBodyParameter(request, ParameterNames.COMMUNITY_SELFREG_RESTRICTION).toShort
        }
      }
    } else {
      selfRegType = SelfRegistrationType.NotSupported.id.toShort
    }

    val domainId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, ParameterNames.DOMAIN_ID)
    Communities(
      0L, sname, fname, email, selfRegType, selfRegToken, selfRegTokenHelpText, selfRegNotification, interactionNotification, description,
      selfRegRestriction, selfRegForceTemplateSelection, selfRegForceRequiredProperties,
      allowCertificateDownload, allowStatementManagement, allowSystemManagement,
      allowPostTestOrganisationUpdate, allowPostTestSystemUpdate, allowPostTestStatementUpdate, allowAutomationApi, allowCommunityView,
      CryptoUtil.generateApiKey(), None, domainId
    )
  }

  def extractSystemAdminInfo(request:Request[AnyContent]):Users = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      val ssoEmail = requiredBodyParameter(request, ParameterNames.USER_EMAIL).trim
      getUserInfoForSSO(ssoEmail, UserRole.SystemAdmin.id.toShort)
    } else {
      val name = requiredBodyParameter(request, ParameterNames.USER_NAME)
      val email = requiredBodyParameter(request, ParameterNames.USER_EMAIL).trim
      val password = requiredBodyParameter(request, ParameterNames.PASSWORD).trim
      Users(0L, name, email, CryptoUtil.hashPassword(password), onetimePassword = true, UserRole.SystemAdmin.id.toShort, 0L, None, None, UserSSOStatus.NotMigrated.id.toShort)
    }
  }

  def extractCommunityAdminInfo(request:Request[AnyContent]):Users = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      val ssoEmail = requiredBodyParameter(request, ParameterNames.USER_EMAIL).trim
      getUserInfoForSSO(ssoEmail, UserRole.CommunityAdmin.id.toShort)
    } else {
      val name = requiredBodyParameter(request, ParameterNames.USER_NAME)
      val email = requiredBodyParameter(request, ParameterNames.USER_EMAIL).trim
      val password = requiredBodyParameter(request, ParameterNames.PASSWORD).trim
      Users(0L, name, email, CryptoUtil.hashPassword(password), onetimePassword = true, UserRole.CommunityAdmin.id.toShort, 0L, None, None, UserSSOStatus.NotMigrated.id.toShort)
    }
  }

  def extractAdminInfo(paramMap:Option[Map[String, Seq[String]]], ssoEmailToForce: Option[String], passwordIsOneTime: Option[Boolean]):Users = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      var ssoEmail: String = null
      if (ssoEmailToForce.isDefined) {
        ssoEmail = ssoEmailToForce.get.trim
      } else {
        ssoEmail = requiredBodyParameter(paramMap, ParameterNames.USER_EMAIL).trim
      }
      getUserInfoForSSO(ssoEmail, UserRole.VendorAdmin.id.toShort)
    } else {
      val name = requiredBodyParameter(paramMap, ParameterNames.USER_NAME)
      val email = requiredBodyParameter(paramMap, ParameterNames.USER_EMAIL).trim
      val password = requiredBodyParameter(paramMap, ParameterNames.PASSWORD).trim
      Users(0L, name, email, CryptoUtil.hashPassword(password), passwordIsOneTime.getOrElse(true), UserRole.VendorAdmin.id.toShort, 0L, None, None, UserSSOStatus.NotMigrated.id.toShort)
    }
  }

  def extractAdminInfo(request:Request[AnyContent]):Users = {
    extractAdminInfo(ParameterExtractor.paramMap(request), None, None)
  }

  private def getUserInfoForSSO(ssoEmail: String, role: Short): Users = {
    val randomPart = ThreadLocalRandom.current.nextInt(10000000, 99999999 + 1)
    val name = s"User [$randomPart]"
    val email = s"$randomPart@itb.ec.europa.eu"
    val password = randomPart.toString
    Users(0L, name, email, CryptoUtil.hashPassword(password), onetimePassword = true, role, 0L, None, Some(ssoEmail), UserSSOStatus.NotLinked.id.toShort)
  }

  def extractUserInfo(request:Request[AnyContent]):Users = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      val ssoEmail = requiredBodyParameter(request, ParameterNames.USER_EMAIL).trim
      getUserInfoForSSO(ssoEmail, UserRole.VendorUser.id.toShort)
    } else {
      val name = requiredBodyParameter(request, ParameterNames.USER_NAME)
      val email = requiredBodyParameter(request, ParameterNames.USER_EMAIL).trim
      val password = requiredBodyParameter(request, ParameterNames.PASSWORD).trim
      Users(0L, name, email, CryptoUtil.hashPassword(password), onetimePassword = true, UserRole.VendorUser.id.toShort, 0L, None, None, UserSSOStatus.NotMigrated.id.toShort)
    }
  }

  def extractSystemWithOrganizationInfo(paramMap:Option[Map[String, Seq[String]]]):Systems = {
    val sname = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.SYSTEM_SNAME)
    val fname = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.SYSTEM_FNAME)
    val descr = ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.SYSTEM_DESC)
    val version = ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.SYSTEM_VERSION)
    val owner:Long = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.ORGANIZATION_ID).toLong
    Systems(0L, sname, fname, descr, version, CryptoUtil.generateApiKey(), "", owner)
  }

	def extractDomain(request:Request[AnyContent]):Domain = {
		val sname = ParameterExtractor.requiredBodyParameter(request, ParameterNames.SHORT_NAME)
		val fname = ParameterExtractor.requiredBodyParameter(request, ParameterNames.FULL_NAME)
		val descr = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DESC)
    val reportMetadata = ParameterExtractor.optionalBodyParameter(request, ParameterNames.METADATA)
		Domain(0L, sname, fname, descr, reportMetadata, CryptoUtil.generateApiKey())
	}

	def extractSpecification(paramMap:Option[Map[String, Seq[String]]]): Specifications = {
		val sname = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.SHORT_NAME)
		val fname = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.FULL_NAME)
		val descr = ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.DESC)
    val reportMetadata = ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.METADATA)
    val hidden = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.HIDDEN).toBoolean
		val domain = ParameterExtractor.optionalLongBodyParameter(paramMap, ParameterNames.DOMAIN_ID)
    val group = ParameterExtractor.optionalLongBodyParameter(paramMap, ParameterNames.GROUP_ID)

    Specifications(0L, sname, fname, descr, reportMetadata, hidden, CryptoUtil.generateApiKey(), domain.getOrElse(0L), 0, group)
	}

  def extractSpecificationGroup(request: Request[AnyContent]): SpecificationGroups = {
    val sname = ParameterExtractor.requiredBodyParameter(request, ParameterNames.SHORT_NAME)
    val fname = ParameterExtractor.requiredBodyParameter(request, ParameterNames.FULL_NAME)
    val descr = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DESC)
    val reportMetadata = ParameterExtractor.optionalBodyParameter(request, ParameterNames.METADATA)
    val domain = ParameterExtractor.requiredBodyParameter(request, ParameterNames.DOMAIN_ID).toLong

    SpecificationGroups(0L, sname, fname, descr, reportMetadata, 0, CryptoUtil.generateApiKey(), domain)
  }

  def extractActor(paramMap:Option[Map[String, Seq[String]]]):Actor = {
    val id:Long = ParameterExtractor. optionalBodyParameter(paramMap, ParameterNames.ID) match {
      case Some(i) => i.toLong
      case _ => 0L
    }
		val actorId:String = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.ACTOR_ID)
		val name:String = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.NAME)
		val description:Option[String] = ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.DESCRIPTION)
    val reportMetadata:Option[String] = ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.METADATA)
    var default:Option[Boolean] = None
    val defaultStr = ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.ACTOR_DEFAULT)
    if (defaultStr.isDefined) {
      default = Some(defaultStr.get.toBoolean)
    } else {
      default = Some(false)
    }
    val hidden = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.HIDDEN).toBoolean
    var displayOrder:Option[Short] = None
    val displayOrderStr = ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.DISPLAY_ORDER)
    if (displayOrderStr.isDefined) {
      displayOrder = Some(displayOrderStr.get.toShort)
    }
    new Actor(id, actorId, name, description, reportMetadata, default, hidden, displayOrder, None, None, None, None)
	}

  def extractEndpoint(request:Request[AnyContent]):Endpoints = {
    val id:Long = ParameterExtractor.optionalBodyParameter(request, ParameterNames.ID) match {
      case Some(i) => i.toLong
      case _ => 0L
    }
    val name:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.NAME)
    val desc:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DESC)
    val actorId:Long = ParameterExtractor.requiredBodyParameter(request, ParameterNames.ACTOR_ID).toLong
    Endpoints(id, name, desc, actorId)
  }

  def extractParameter(request:Request[AnyContent]):models.Parameters = {
      val id:Long = ParameterExtractor.optionalBodyParameter(request, ParameterNames.ID) match {
      case Some(i) => i.toLong
      case _ => 0L
    }
    val name:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.NAME)
    val testKey:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.TEST_KEY)
    val desc:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DESC)
    val use:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.USE)
    val kind:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.KIND)
    val endpointId:Long = ParameterExtractor.optionalBodyParameter(request, ParameterNames.ENDPOINT_ID).map(_.toLong).getOrElse(0L)
    val adminOnly = ParameterExtractor.requiredBodyParameter(request, ParameterNames.ADMIN_ONLY).toBoolean
    val notForTests = ParameterExtractor.requiredBodyParameter(request, ParameterNames.NOT_FOR_TESTS).toBoolean
    var hidden = ParameterExtractor.requiredBodyParameter(request, ParameterNames.HIDDEN).toBoolean
    val allowedValues:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.ALLOWED_VALUES)
    var dependsOn:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DEPENDS_ON)
    var dependsOnValue:Option[String] = None
    if (dependsOn.isDefined && dependsOn.get.trim.isEmpty) {
      dependsOn = None
    }
    if (dependsOn.isDefined) {
      dependsOnValue = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DEPENDS_ON_VALUE)
    }
    if (dependsOnValue.isDefined && dependsOnValue.get.trim.isEmpty) {
      dependsOnValue = None
    }
    if (!adminOnly) {
      hidden = false
    }
    val defaultValue = determineDefaultParameterValue(ParameterExtractor.optionalBodyParameter(request, ParameterNames.DEFAULT_VALUE), kind, allowedValues)
    models.Parameters(id, name, testKey, desc, use, kind, adminOnly, notForTests, hidden, allowedValues, 0, dependsOn, dependsOnValue, defaultValue, endpointId)
  }

  def extractOrganisationParameter(request:Request[AnyContent]):models.OrganisationParameters = {
    val id:Long = ParameterExtractor.optionalBodyParameter(request, ParameterNames.ID) match {
      case Some(i) => i.toLong
      case _ => 0L
    }
    val name:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.NAME)
    val testKey:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.TEST_KEY)
    val desc:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DESC)
    val use:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.USE)
    val kind:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.KIND)
    val communityId:Long = ParameterExtractor.requiredBodyParameter(request, ParameterNames.COMMUNITY_ID).toLong
    val adminOnly = ParameterExtractor.requiredBodyParameter(request, ParameterNames.ADMIN_ONLY).toBoolean
    val notForTests = ParameterExtractor.requiredBodyParameter(request, ParameterNames.NOT_FOR_TESTS).toBoolean
    val inExports:Boolean = (kind == "SIMPLE") && ParameterExtractor.requiredBodyParameter(request, ParameterNames.IN_EXPORTS).toBoolean
    val inSelfRegistration: Boolean = Configurations.REGISTRATION_ENABLED && (!adminOnly) && ParameterExtractor.requiredBodyParameter(request, ParameterNames.IN_SELFREG).toBoolean
    var hidden: Boolean = ParameterExtractor.requiredBodyParameter(request, ParameterNames.HIDDEN).toBoolean
    val allowedValues:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.ALLOWED_VALUES)
    var dependsOn:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DEPENDS_ON)
    var dependsOnValue:Option[String] = None
    if (dependsOn.isDefined && dependsOn.get.trim.isEmpty) {
      dependsOn = None
    }
    if (dependsOn.isDefined) {
      dependsOnValue = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DEPENDS_ON_VALUE)
    }
    if (dependsOnValue.isDefined && dependsOnValue.get.trim.isEmpty) {
      dependsOnValue = None
    }
    if (!adminOnly) {
      hidden = false
    }
    val defaultValue = determineDefaultParameterValue(ParameterExtractor.optionalBodyParameter(request, ParameterNames.DEFAULT_VALUE), kind, allowedValues)
    models.OrganisationParameters(id, name, testKey, desc, use, kind, adminOnly, notForTests, inExports, inSelfRegistration, hidden, allowedValues, 0, dependsOn, dependsOnValue, defaultValue, communityId)
  }

  private def determineDefaultParameterValue(defaultValue: Option[String], kind: String, allowedValues: Option[String]): Option[String] = {
    var defaultValueToUse = defaultValue
    if (defaultValueToUse.isDefined) {
      if (!kind.equals("SIMPLE")) {
        defaultValueToUse = None
      }
    }
    if (defaultValueToUse.isDefined && allowedValues.isDefined) {
      val allowed = JsonUtil.parseAllowedParameterValues(allowedValues.get)
      if (!allowed.keySet.contains(defaultValueToUse.get)) {
        defaultValueToUse = None
      }
    }
    defaultValueToUse
  }

  def extractSystemParameter(request:Request[AnyContent]):models.SystemParameters = {
    val id:Long = ParameterExtractor.optionalBodyParameter(request, ParameterNames.ID) match {
      case Some(i) => i.toLong
      case _ => 0L
    }
    val name:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.NAME)
    val testKey:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.TEST_KEY)
    val desc:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DESC)
    val use:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.USE)
    val kind:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.KIND)
    val communityId:Long = ParameterExtractor.requiredBodyParameter(request, ParameterNames.COMMUNITY_ID).toLong
    val adminOnly = ParameterExtractor.requiredBodyParameter(request, ParameterNames.ADMIN_ONLY).toBoolean
    val notForTests = ParameterExtractor.requiredBodyParameter(request, ParameterNames.NOT_FOR_TESTS).toBoolean
    val inExports = (kind == "SIMPLE") && ParameterExtractor.requiredBodyParameter(request, ParameterNames.IN_EXPORTS).toBoolean
    var hidden = ParameterExtractor.requiredBodyParameter(request, ParameterNames.HIDDEN).toBoolean
    val allowedValues:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.ALLOWED_VALUES)
    var dependsOn:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DEPENDS_ON)
    var dependsOnValue:Option[String] = None
    if (dependsOn.isDefined && dependsOn.get.trim.isEmpty) {
      dependsOn = None
    }
    if (dependsOn.isDefined) {
      dependsOnValue = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DEPENDS_ON_VALUE)
    }
    if (dependsOnValue.isDefined && dependsOnValue.get.trim.isEmpty) {
      dependsOnValue = None
    }
    if (!adminOnly) {
      hidden = false
    }
    val defaultValue = determineDefaultParameterValue(ParameterExtractor.optionalBodyParameter(request, ParameterNames.DEFAULT_VALUE), kind, allowedValues)
    models.SystemParameters(id, name, testKey, desc, use, kind, adminOnly, notForTests, inExports, hidden, allowedValues, 0, dependsOn, dependsOnValue, defaultValue, communityId)
  }

  def extractLandingPageInfo(request:Request[AnyContent]):LandingPages = {
    val name = requiredBodyParameter(request, ParameterNames.NAME)
    val desc = optionalBodyParameter(request, ParameterNames.DESCRIPTION)
    val content = HtmlUtil.sanitizeEditorContent(requiredBodyParameter(request, ParameterNames.CONTENT))
    val default = requiredBodyParameter(request, ParameterNames.DEFAULT).toBoolean
    val communityId = requiredBodyParameter(request, ParameterNames.COMMUNITY_ID).toLong
    LandingPages(0L, name, desc, content, default, communityId)
  }

  def extractTriggerDataItems(request:Request[AnyContent], parameter: String, triggerId: Option[Long]): Option[List[TriggerData]] = {
    val triggerDataStr = optionalBodyParameter(request, parameter)
    var triggerData: Option[List[TriggerData]] = None
    if (triggerDataStr.isDefined) {
      triggerData = Some(JsonUtil.parseJsTriggerDataItems(triggerDataStr.get, triggerId))
    }
    triggerData
  }

  def extractTriggerFireExpressions(request:Request[AnyContent], parameter: String, triggerId: Option[Long]): Option[List[TriggerFireExpression]] = {
    val triggerExpressionsStr = optionalBodyParameter(request, parameter)
    var triggerExpressions: Option[List[TriggerFireExpression]] = None
    if (triggerExpressionsStr.isDefined) {
      triggerExpressions = Some(JsonUtil.parseJsTriggerFireExpressions(triggerExpressionsStr.get, triggerId))
    }
    triggerExpressions
  }

  def extractCommunityResource(paramMap:Option[Map[String, Seq[String]]], communityId: Long): CommunityResources = {
    val name = requiredBodyParameter(paramMap, ParameterNames.NAME)
    val description = optionalBodyParameter(paramMap, ParameterNames.DESCRIPTION)
    CommunityResources(0L, name, description, communityId)
  }

  def extractTriggerInfo(request:Request[AnyContent], triggerId: Option[Long]): Trigger = {
    // Trigger.
    val name = requiredBodyParameter(request, ParameterNames.NAME)
    val description = optionalBodyParameter(request, ParameterNames.DESCRIPTION)
    val url = requiredBodyParameter(request, ParameterNames.URL)
    val operation = optionalBodyParameter(request, ParameterNames.OPERATION)
    val active = requiredBodyParameter(request, ParameterNames.ACTIVE).toBoolean
    val eventType = requiredBodyParameter(request, ParameterNames.EVENT).toShort
    val serviceType = requiredBodyParameter(request, ParameterNames.TYPE).toShort
    // Check that this is a valid value (otherwise throw exception)
    TriggerEventType.apply(eventType)
    val communityId = requiredBodyParameter(request, ParameterNames.COMMUNITY_ID).toLong
    new Trigger(
      Triggers(triggerId.getOrElse(0L), name, description, url, eventType, serviceType, operation, active, None, None, communityId),
      extractTriggerDataItems(request, ParameterNames.DATA, triggerId),
      extractTriggerFireExpressions(request, ParameterNames.EXPRESSIONS, triggerId)
    )
  }

  def extractLegalNoticeInfo(request:Request[AnyContent]):LegalNotices = {
    val name = requiredBodyParameter(request, ParameterNames.NAME)
    val desc = optionalBodyParameter(request, ParameterNames.DESCRIPTION)
    val content = HtmlUtil.sanitizeEditorContent(requiredBodyParameter(request, ParameterNames.CONTENT))
    val default = requiredBodyParameter(request, ParameterNames.DEFAULT).toBoolean
    val communityId = requiredBodyParameter(request, ParameterNames.COMMUNITY_ID).toLong
    LegalNotices(0L, name, desc, content, default, communityId)
  }

  def extractErrorTemplateInfo(request:Request[AnyContent]):ErrorTemplates = {
    val name = requiredBodyParameter(request, ParameterNames.NAME)
    val desc = optionalBodyParameter(request, ParameterNames.DESCRIPTION)
    val content = HtmlUtil.sanitizeEditorContent(requiredBodyParameter(request, ParameterNames.CONTENT))
    val default = requiredBodyParameter(request, ParameterNames.DEFAULT).toBoolean
    val communityId = requiredBodyParameter(request, ParameterNames.COMMUNITY_ID).toLong
    ErrorTemplates(0L, name, desc, content, default, communityId)
  }

	def extractIdsQueryParameter(request:Request[AnyContent]): Option[List[String]] = {
		val idsStr = ParameterExtractor.optionalQueryParameter(request, ParameterNames.IDS)
		val ids = idsStr match {
			case Some(str) => Some(str.split(",").toList)
			case None => None
		}
		ids
	}

  def extractIdsBodyParameter(request: Request[AnyContent]): Set[Long] = {
    val idsStr = ParameterExtractor.optionalBodyParameter(request, ParameterNames.IDS)
    val ids = idsStr match {
      case Some(str) => str.split(",").map(x => x.toLong).toSet
      case None => Set.empty[Long]
    }
    ids
  }

  def extractLongIdsQueryParameter(request:Request[AnyContent]): Option[List[Long]] = {
		val idsStr = ParameterExtractor.optionalQueryParameter(request, ParameterNames.IDS)
		val ids = idsStr match {
			case Some(str) => Some(str.split(",").map(_.toLong).toList)
			case None => None
		}
		ids
	}

  def extractLongIdsBodyParameter(request:Request[AnyContent]): Option[List[Long]] = {
    extractLongIdsBodyParameter(request, ParameterNames.IDS)
  }

  def extractLongIdsBodyParameter(request:Request[AnyContent], parameterName: String): Option[List[Long]] = {
    val idsStr = ParameterExtractor.optionalBodyParameter(request, parameterName)
    val ids = idsStr match {
      case Some(str) => Some(str.split(",").map(_.toLong).toList)
      case None => None
    }
    ids
  }

  def optionalListQueryParameter(request:Request[AnyContent],parameter:String): Option[List[String]] = {
    val listStr = ParameterExtractor.optionalQueryParameter(request, parameter)
    val list = listStr match {
      case Some(str) => Some(str.split(",").toList)
      case None => None
    }
    list
  }

  def optionalListBodyParameter(request:Request[AnyContent],parameter:String): Option[List[String]] = {
    val listStr = ParameterExtractor.optionalBodyParameter(request, parameter)
    val list = listStr match {
      case Some(str) => Some(str.split(",").toList)
      case None => None
    }
    list
  }

  def optionalLongListQueryParameter(request:Request[AnyContent],parameter:String): Option[List[Long]] = {
    val listStr = ParameterExtractor.optionalQueryParameter(request, parameter)
    val list = listStr match {
      case Some(str) => Some(str.split(",").filter(_.nonEmpty).map(_.toLong).toList)
      case None => None
    }
    list
  }

  def optionalLongListBodyParameter(request:Request[AnyContent],parameter:String): Option[List[Long]] = {
    val listStr = ParameterExtractor.optionalBodyParameter(request, parameter)
    val list = listStr match {
      case Some(str) => Some(str.split(",").filter(_.nonEmpty).map(_.toLong).toList)
      case None => None
    }
    list
  }

  def requiredLongListBodyParameter(request: Request[AnyContent], parameter: String): List[Long] = {
    val listStr = ParameterExtractor.requiredBodyParameter(request, parameter)
    listStr.split(",").map(_.toLong).toList
  }

  private def darkenColor(original: String): String = {
    val color = Color.decode(original).darker()
    "#%02x%02x%02x".formatted(color.getRed, color.getGreen, color.getBlue)
  }

  def extractTheme(request: Request[AnyContent], paramMap: Option[Map[String, Seq[String]]], themeIdToUse: Option[Long] = None): (Option[Theme], Option[ThemeFiles], Option[Result]) = {
    val files = ParameterExtractor.extractFiles(request)
    var resultToReturn: Option[Result] = None
    var theme: Option[Theme] = None
    var themeFiles: Option[ThemeFiles] = None
    var headerLogoFile: Option[NamedFile] = None
    var footerLogoFile: Option[NamedFile] = None
    var faviconFile: Option[NamedFile] = None
    val filesToScan = new ListBuffer[NamedFile]
    if (files.contains(ParameterNames.HEADER_LOGO_FILE)) {
      headerLogoFile = Some(NamedFile(files(ParameterNames.HEADER_LOGO_FILE).file, files(ParameterNames.HEADER_LOGO_FILE).name, Some(ParameterNames.HEADER_LOGO_FILE)))
      filesToScan += headerLogoFile.get
    }
    if (files.contains(ParameterNames.FOOTER_LOGO_FILE)) {
      footerLogoFile = Some(NamedFile(files(ParameterNames.FOOTER_LOGO_FILE).file, files(ParameterNames.FOOTER_LOGO_FILE).name, Some(ParameterNames.FOOTER_LOGO_FILE)))
      filesToScan += footerLogoFile.get
    }
    if (files.contains(ParameterNames.FAVICON_FILE)) {
      faviconFile = Some(NamedFile(files(ParameterNames.FAVICON_FILE).file, files(ParameterNames.FAVICON_FILE).name, Some(ParameterNames.FAVICON_FILE)))
      filesToScan += faviconFile.get
    }
    if (filesToScan.nonEmpty) {
      resultToReturn = ParameterExtractor.virusPresentInNamedFiles(filesToScan.toList).map { _ =>
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Files failed virus scan.")
      }
      if (resultToReturn.isEmpty) {
        val filesWithWrongType = filesToScan.filter(p => !imageMimeTypes.contains(MimeUtil.getMimeType(p.file.toPath)))
        if (filesWithWrongType.nonEmpty) {
          resultToReturn = Some(ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_REQUEST, "Only image files are allowed.", Some(filesWithWrongType.flatMap(_.identifier).mkString(","))))
        }
      }
    }
    if (resultToReturn.isEmpty) {
      themeFiles = Some(ThemeFiles(headerLogoFile, footerLogoFile, faviconFile))
      // Define calculated colours.
      val primaryButtonColor = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.PRIMARY_BUTTON_COLOR)
      var primaryButtonHoverColor = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.PRIMARY_BUTTON_HOVER_COLOR)
      var primaryButtonActiveColor = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.PRIMARY_BUTTON_ACTIVE_COLOR)
      val secondaryButtonColor = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.SECONDARY_BUTTON_COLOR)
      var secondaryButtonHoverColor = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.SECONDARY_BUTTON_HOVER_COLOR)
      var secondaryButtonActiveColor = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.SECONDARY_BUTTON_ACTIVE_COLOR)
      if (primaryButtonColor == primaryButtonHoverColor || primaryButtonColor == primaryButtonActiveColor) {
        primaryButtonHoverColor = darkenColor(primaryButtonColor)
        primaryButtonActiveColor = primaryButtonHoverColor
      }
      if (secondaryButtonColor == secondaryButtonHoverColor || secondaryButtonColor == secondaryButtonActiveColor) {
        secondaryButtonHoverColor = darkenColor(secondaryButtonColor)
        secondaryButtonActiveColor = secondaryButtonHoverColor
      }
      theme = Some(Theme(
        themeIdToUse.getOrElse(0L),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.KEY),
        ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.DESCRIPTION),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.ACTIVE).toBoolean,
        custom = true,
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.SEPARATOR_TITLE_COLOR),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.MODAL_TITLE_COLOR),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.TABLE_TITLE_COLOR),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.CARD_TILE_COLOR),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.PAGE_TITLE_COLOR),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.HEADING_COLOR),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.TAB_LINK_COLOR),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.FOOTER_TEXT_COLOR),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.HEADER_BACKGROUND_COLOR),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.HEADER_BORDER_COLOR),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.HEADER_SEPARATOR_COLOR),
        ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.HEADER_LOGO_PATH)
          .getOrElse(themeFiles.flatMap(_.headerLogo).map(_.name).getOrElse(throw new IllegalStateException("Missing header logo"))),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.FOOTER_BACKGROUND_COLOR),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.FOOTER_BORDER_COLOR),
        ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.FOOTER_LOGO_PATH)
          .getOrElse(themeFiles.flatMap(_.footerLogo).map(_.name).getOrElse(throw new IllegalStateException("Missing footer logo"))),
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.FOOTER_LOGO_DISPLAY),
        ParameterExtractor.optionalBodyParameter(paramMap, ParameterNames.FAVICON_PATH)
          .getOrElse(themeFiles.flatMap(_.faviconFile).map(_.name).getOrElse(throw new IllegalStateException("Missing favicon"))),
        primaryButtonColor,
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.PRIMARY_BUTTON_LABEL_COLOR),
        primaryButtonHoverColor,
        primaryButtonActiveColor,
        secondaryButtonColor,
        ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.SECONDARY_BUTTON_LABEL_COLOR),
        secondaryButtonHoverColor,
        secondaryButtonActiveColor,
      ))
      if (!theme.get.footerLogoDisplay.equals("inherit") && !theme.get.footerLogoDisplay.equals("none")) {
        resultToReturn = Some(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_PARAM, "Unexpected value for footer logo display."))
      }
    }
    (theme, themeFiles, resultToReturn)
  }

  def extractBadges(request: Request[AnyContent], paramMap: Option[Map[String, Seq[String]]], forReport: Boolean): (Option[Badges], Option[Result]) = {
    val successBadgeEnabledParam = if (forReport) ParameterNames.SUCCESS_BADGE_REPORT_ENABLED else ParameterNames.SUCCESS_BADGE_ENABLED
    val failureBadgeEnabledParam = if (forReport) ParameterNames.FAILURE_BADGE_REPORT_ENABLED else ParameterNames.FAILURE_BADGE_ENABLED
    val otherBadgeEnabledParam = if (forReport) ParameterNames.OTHER_BADGE_REPORT_ENABLED else ParameterNames.OTHER_BADGE_ENABLED
    val successBadgeParam = if (forReport) ParameterNames.SUCCESS_BADGE_REPORT else ParameterNames.SUCCESS_BADGE
    val failureBadgeParam = if (forReport) ParameterNames.FAILURE_BADGE_REPORT else ParameterNames.FAILURE_BADGE
    val otherBadgeParam = if (forReport) ParameterNames.OTHER_BADGE_REPORT else ParameterNames.OTHER_BADGE

    val files = ParameterExtractor.extractFiles(request)
    var resultToReturn: Option[Result] = None
    val hasSuccess = ParameterExtractor.requiredBodyParameter(paramMap, successBadgeEnabledParam).toBoolean
    val hasFailure = ParameterExtractor.requiredBodyParameter(paramMap, failureBadgeEnabledParam).toBoolean
    val hasOther = ParameterExtractor.requiredBodyParameter(paramMap, otherBadgeEnabledParam).toBoolean
    var successBadgeToStore: Option[NamedFile] = None
    var failureBadgeToStore: Option[NamedFile] = None
    var otherBadgeToStore: Option[NamedFile] = None
    val filesToScan = new ListBuffer[NamedFile]
    if (hasSuccess && files.contains(successBadgeParam)) {
      successBadgeToStore = Some(NamedFile(files(successBadgeParam).file, files(successBadgeParam).name))
      filesToScan += successBadgeToStore.get
    }
    if (files.contains(failureBadgeParam)) {
      failureBadgeToStore = Some(NamedFile(files(failureBadgeParam).file, files(failureBadgeParam).name))
      filesToScan += failureBadgeToStore.get
    }
    if (files.contains(otherBadgeParam)) {
      otherBadgeToStore = Some(NamedFile(files(otherBadgeParam).file, files(otherBadgeParam).name))
      filesToScan += otherBadgeToStore.get
    }
    if (filesToScan.nonEmpty) {
      if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(filesToScan.toList.map(_.file))) {
        resultToReturn = Some(ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Files failed virus scan."))
      }
      if (resultToReturn.isEmpty && filesToScan.exists(p => !imageMimeTypes.contains(MimeUtil.getMimeType(p.file.toPath)))) {
        resultToReturn = Some(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Only image files are allowed."))
      }
    }
    (Some(Badges(hasSuccess, hasFailure, hasOther, successBadgeToStore, failureBadgeToStore, otherBadgeToStore)), resultToReturn)
  }

  def extractReportStylesheet(request: Request[AnyContent]): (Option[File], Option[Result]) = {
    var stylesheetFile: Option[File] = None
    var response: Option[Result] = None
    val files = ParameterExtractor.extractFiles(request)
    if (files.contains(ParameterNames.FILE)) {
      stylesheetFile = Some(files(ParameterNames.FILE).file)
      // Check for virus
      if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
        val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
        val scanResult = virusScanner.scan(stylesheetFile.get)
        if (!ClamAVClient.isCleanReply(scanResult)) {
          response = Some(ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Stylesheet file failed virus scan."))
        }
      }
      if (response.isEmpty) {
        // Check for expected mime type
        val mimeType = MimeUtil.getMimeType(stylesheetFile.get.toPath)
        if (!xslMimeTypes.contains(mimeType)) {
          response = Some(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Only XSLT stylesheets are allowed."))
        }
      }
    }
    (stylesheetFile, response)
  }

  def extractTestServiceWithParameter(request: Request[AnyContent], domainId: Long, serviceId: Option[Long]): TestServiceWithParameter = {
    val parameter = DomainParameter(
      id = optionalLongBodyParameter(request, ParameterNames.PARAMETER).getOrElse(0L),
      name = requiredBodyParameter(request, ParameterNames.NAME),
      desc = optionalBodyParameter(request, ParameterNames.DESC),
      kind = "SIMPLE",
      value = Some(requiredBodyParameter(request, ParameterNames.VALUE)),
      inTests = true,
      contentType = None,
      isTestService = true,
      domain = domainId
    )
    var authBasicUsername = optionalBodyParameter(request, ParameterNames.AUTH_BASIC_USERNAME)
    var authBasicPassword = optionalBodyParameter(request, ParameterNames.AUTH_BASIC_PASSWORD)
    var authTokenPassword = optionalBodyParameter(request, ParameterNames.AUTH_TOKEN_PASSWORD)
    var authTokenUsername = optionalBodyParameter(request, ParameterNames.AUTH_TOKEN_USERNAME)
    var authTokenPasswordType = optionalShortBodyParameter(request, ParameterNames.AUTH_TOKEN_PASSWORD_TYPE).map(TestServiceAuthTokenPasswordType.apply(_).id.toShort)
    if (serviceId.isEmpty) {
      // This is a new test service - Ensure consistency of auth fields
      if ((authBasicUsername.nonEmpty || authBasicPassword.nonEmpty) && (authBasicUsername.isEmpty || authBasicPassword.isEmpty)) {
        authBasicUsername = None
        authBasicPassword = None
      }
      if ((authTokenUsername.nonEmpty || authTokenPassword.nonEmpty || authTokenPasswordType.nonEmpty) && (authTokenUsername.isEmpty || authTokenPassword.isEmpty || authTokenPasswordType.isEmpty)) {
        authTokenPassword = None
        authTokenUsername = None
        authTokenPasswordType = None
      }
    }
    val service = TestService(
      id = serviceId.getOrElse(optionalLongBodyParameter(request, ParameterNames.ID).getOrElse(0L)),
      serviceType = TestServiceType.apply(requiredBodyParameter(request, ParameterNames.SERVICE_TYPE).toInt).id.toShort,
      apiType = TestServiceApiType.apply(requiredBodyParameter(request, ParameterNames.API_TYPE).toInt).id.toShort,
      identifier = optionalBodyParameter(request, ParameterNames.IDENTIFIER),
      version = optionalBodyParameter(request, ParameterNames.VERSION),
      authBasicUsername = authBasicUsername,
      authBasicPassword = authBasicPassword,
      authTokenUsername = authTokenUsername,
      authTokenPassword = authTokenPassword,
      authTokenPasswordType = authTokenPasswordType,
      parameter = parameter.id
    )
    TestServiceWithParameter(service, parameter)
  }

  def extractApiKeyHeader(request: RequestHeader): Option[String] = {
    var header = request.headers.get(Configurations.HEADER_NAME_ITB_API_KEY)
    if (header.isEmpty) {
      // Backwards compatibility.
      header = request.headers.get("ITB_API_KEY")
    }
    header
  }

  def extractTestServiceSearchCriteria(request: Request[AnyContent]): TestServiceSearchCriteria = {
    TestServiceSearchCriteria(
      ParameterExtractor.optionalQueryParameter(request, ParameterNames.DOMAIN),
      ParameterExtractor.optionalQueryParameter(request, ParameterNames.KEY),
      ParameterExtractor.optionalQueryParameter(request, ParameterNames.IDENTIFIER),
      ParameterExtractor.optionalQueryParameter(request, ParameterNames.VERSION),
      ParameterExtractor.optionalQueryParameter(request, ParameterNames.SERVICE_TYPE).map(Enums.parseTestServiceTypeForApi),
      ParameterExtractor.optionalQueryParameter(request, ParameterNames.API_TYPE).map(Enums.parseTestServiceApiTypeForApi)
    )
  }

  def extractAvailableConformanceStatementSearchCriteria(request: Request[AnyContent]): AvailableStatementsSearchCriteria = {
    AvailableStatementsSearchCriteria(
      filterText = ParameterExtractor.optionalBodyParameter(request, ParameterNames.FILTER),
      selected = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SELECTED).forall(_.toBoolean),
      unselected = ParameterExtractor.optionalBodyParameter(request, ParameterNames.UNSELECTED).forall(_.toBoolean),
      selectedIds = ParameterExtractor.extractIdsBodyParameter(request)
    )
  }

  def validHttpAbsoluteUrl(value: String): Boolean = {
    var result = false
    if (!value.isBlank && !value.matches(".*\\s+.*")) {
      try {
        val url = URI.create(value).toURL
        val protocol = url.getProtocol
        result = "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)
      } catch {
        case _: Exception =>
          // Ignore error (result is false)
      }
    }
    result
  }

  def validTestVariableName(value: String): Boolean = {
    value.matches("^[a-zA-Z][a-zA-Z\\-_.0-9]*$")
  }

}