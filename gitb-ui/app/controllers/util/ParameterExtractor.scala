package controllers.util

import java.util.concurrent.ThreadLocalRandom

import config.Configurations
import exceptions.{ErrorCodes, InvalidRequestException}
import models.Enums._
import models._
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.StringUtils
import org.mindrot.jbcrypt.BCrypt
import play.api.mvc._
import utils.{ClamAVClient, HtmlUtil, JsonUtil, MimeUtil}

object ParameterExtractor {

  def requiredQueryParameter(request:Request[AnyContent], parameter:String):String = {
    val param = request.getQueryString(parameter)
    if(!param.isDefined)
      throw new InvalidRequestException(ErrorCodes.MISSING_PARAMS, "Parameter '"+parameter+"' is missing in the request")
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

  def extractOrganisationParameterValues(request:Request[AnyContent], parameterName: String, optional: Boolean): Option[List[OrganisationParameterValues]] = {
    var values: Option[List[OrganisationParameterValues]] = None
    if (optional) {
      val valuesJson = optionalBodyParameter(request, parameterName)
      if (valuesJson.isDefined) {
        values = Some(JsonUtil.parseJsOrganisationParameterValues(valuesJson.get))
      }
    } else {
      val valuesJson = requiredBodyParameter(request, parameterName)
      values = Some(JsonUtil.parseJsOrganisationParameterValues(valuesJson))
    }
    values
  }

  def checkOrganisationParameterValues(values: Option[List[OrganisationParameterValues]]): Result = {
    var response: Result = null
    if (Configurations.ANTIVIRUS_SERVER_ENABLED && values.isDefined) {
      val dataUrls = values.get.collect({
        case x if MimeUtil.isDataURL(x.value) => x.value
      })
      if (virusPresent(dataUrls)) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
      }
    }
    response
  }

  def extractSystemParameterValues(request:Request[AnyContent], parameterName: String, optional: Boolean): Option[List[SystemParameterValues]] = {
    var values: Option[List[SystemParameterValues]] = None
    if (optional) {
      val valuesJson = optionalBodyParameter(request, parameterName)
      if (valuesJson.isDefined) {
        values = Some(JsonUtil.parseJsSystemParameterValues(valuesJson.get))
      }
    } else {
      val valuesJson = requiredBodyParameter(request, parameterName)
      values = Some(JsonUtil.parseJsSystemParameterValues(valuesJson))
    }
    values
  }

  def checkSystemParameterValues(values: Option[List[SystemParameterValues]]): Result = {
    var response: Result = null
    if (Configurations.ANTIVIRUS_SERVER_ENABLED && values.isDefined) {
      val dataUrls = values.get.collect({
        case x if MimeUtil.isDataURL(x.value) => x.value
      })
      if (virusPresent(dataUrls)) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
      }
    }
    response
  }

  def virusPresent(values: List[String]): Boolean = {
    val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
    values.foreach { value =>
      // Check for virus. Do this regardless of the type of parameter as this can be changed later on.
      val scanResult = virusScanner.scan(Base64.decodeBase64(MimeUtil.getBase64FromDataURL(value)))
      if (!ClamAVClient.isCleanReply(scanResult)) {
        return true
      }
    }
    false
  }

  def requiredBodyParameterMulti(request:Request[MultipartFormData[_]], parameter:String):String = {
      val param = request.body.dataParts.get(parameter)
      if (param.isDefined) {
        try {
          param.get.head
        } catch {
          case e:NoSuchElementException =>
            throw InvalidRequestException(ErrorCodes.MISSING_PARAMS, "Parameter '"+parameter+"' is missing in the request")
        }
      } else {
        throw InvalidRequestException(ErrorCodes.MISSING_PARAMS, "Parameter '"+parameter+"' is missing in the request")
      }
  }

  def requiredBinaryBodyParameter(request:Request[AnyContent], parameter:String):Array[Byte] = {
    try {
      var paramStr = request.body.asFormUrlEncoded.get(parameter).head
      if (MimeUtil.isDataURL(paramStr)) {
        paramStr = MimeUtil.getBase64FromDataURL(paramStr)
      }
      Base64.decodeBase64(paramStr)
    } catch {
      case e:NoSuchElementException =>
        throw new InvalidRequestException(ErrorCodes.MISSING_PARAMS, "Parameter '"+parameter+"' is missing in the request")
    }
  }

  def requiredBodyParameter(request:Request[AnyContent], parameter:String):String = {
    try {
      val param = request.body.asFormUrlEncoded.get(parameter).head
      param
    } catch {
      case e:NoSuchElementException =>
        throw new InvalidRequestException(ErrorCodes.MISSING_PARAMS, "Parameter '"+parameter+"' is missing in the request")
    }
  }

  def optionalBodyParameter(request:Request[AnyContent], parameter:String):Option[String] = {
    try {
      val paramList = request.body.asFormUrlEncoded.get(parameter)
      if(paramList.length > 0){
        Some(paramList(0))
      } else{
        None
      }
    } catch {
      case e:NoSuchElementException =>
      None
    }
  }

  def optionalLongBodyParameter(request:Request[AnyContent], parameter:String):Option[Long] = {
    try {
      val paramList = request.body.asFormUrlEncoded.get(parameter)
      if(paramList.nonEmpty){
        Some(paramList.head.toLong)
      } else{
        None
      }
    } catch {
      case _:NoSuchElementException =>
        None
    }
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

  def extractOptionalUserId(request:Request[_]):Option[Long] = {
    val userId = request.headers.get(Parameters.USER_ID)
    if (userId.isDefined) {
      Some(userId.get.toLong)
    } else {
      None
    }
  }

  def extractUserId(request:Request[_]):Long = {
    request.headers.get(Parameters.USER_ID).get.toLong
  }

  def extractOrganizationInfo(request:Request[AnyContent]):Organizations = {
    val vendorSname = requiredBodyParameter(request, Parameters.VENDOR_SNAME)
    val vendorFname = requiredBodyParameter(request, Parameters.VENDOR_FNAME)
    val communityId = requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    val landingPageId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.LANDING_PAGE_ID)
    val legalNoticeId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.LEGAL_NOTICE_ID)
    val errorTemplateId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.ERROR_TEMPLATE_ID)
    var template:Boolean = false
    var templateName: Option[String] = None
    if (Configurations.REGISTRATION_ENABLED) {
      template = optionalBodyParameter(request, Parameters.TEMPLATE).getOrElse("false").toBoolean
      if (template) {
        templateName = optionalBodyParameter(request, Parameters.TEMPLATE_NAME)
      }
    }
    Organizations(0L, vendorSname, vendorFname, OrganizationType.Vendor.id.toShort, false, landingPageId, legalNoticeId, errorTemplateId, template, templateName, communityId)
  }

  def validCommunitySelfRegType(selfRegType: Short) = {
    selfRegType == SelfRegistrationType.NotSupported.id.toShort || selfRegType == SelfRegistrationType.PublicListing.id.toShort || selfRegType == SelfRegistrationType.PublicListingWithToken.id.toShort || selfRegType == SelfRegistrationType.Token.id.toShort
  }

  def extractCommunityInfo(request:Request[AnyContent]):Communities = {
    val sname = requiredBodyParameter(request, Parameters.COMMUNITY_SNAME)
    val fname = requiredBodyParameter(request, Parameters.COMMUNITY_FNAME)
    val email = optionalBodyParameter(request, Parameters.COMMUNITY_EMAIL)
    val description = optionalBodyParameter(request, Parameters.DESCRIPTION)
    val allowCertificateDownload = requiredBodyParameter(request, Parameters.ALLOW_CERTIFICATE_DOWNLOAD).toBoolean
    val allowStatementManagement = requiredBodyParameter(request, Parameters.ALLOW_STATEMENT_MANAGEMENT).toBoolean
    val allowSystemManagement = requiredBodyParameter(request, Parameters.ALLOW_SYSTEM_MANAGEMENT).toBoolean
    val allowPostTestOrganisationUpdate = requiredBodyParameter(request, Parameters.ALLOW_POST_TEST_ORG_UPDATE).toBoolean
    val allowPostTestSystemUpdate = requiredBodyParameter(request, Parameters.ALLOW_POST_TEST_SYS_UPDATE).toBoolean
    val allowPostTestStatementUpdate = requiredBodyParameter(request, Parameters.ALLOW_POST_TEST_STM_UPDATE).toBoolean
    var selfRegType: Short = SelfRegistrationType.NotSupported.id.toShort
    var selfRegRestriction: Short = SelfRegistrationRestriction.NoRestriction.id.toShort
    var selfRegToken: Option[String] = None
    var selfRegTokenHelpText: Option[String] = None
    var selfRegNotification: Boolean = false
    var selfRegForceTemplateSelection: Boolean = false
    var selfRegForceRequiredProperties: Boolean = false
    if (Configurations.REGISTRATION_ENABLED) {
      selfRegType = requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_TYPE).toShort
      if (!validCommunitySelfRegType(selfRegType)) {
        throw new IllegalArgumentException("Unsupported value ["+selfRegType+"] for self-registration type")
      }
      selfRegToken = optionalBodyParameter(request, Parameters.COMMUNITY_SELFREG_TOKEN)
      selfRegTokenHelpText = optionalBodyParameter(request, Parameters.COMMUNITY_SELFREG_TOKEN_HELP_TEXT)
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
        selfRegForceTemplateSelection = requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_FORCE_TEMPLATE).toBoolean
        selfRegForceRequiredProperties = requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_FORCE_PROPERTIES).toBoolean
        if (Configurations.EMAIL_ENABLED) {
          selfRegNotification = requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_NOTIFICATION).toBoolean
        }
        if (Configurations.AUTHENTICATION_SSO_ENABLED) {
          selfRegRestriction = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_SELFREG_RESTRICTION).toShort
        }
      }
    } else {
      selfRegType = SelfRegistrationType.NotSupported.id.toShort
    }

    val domainId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
    Communities(
      0L, sname, fname, email, selfRegType, selfRegToken, selfRegTokenHelpText, selfRegNotification, description,
      selfRegRestriction, selfRegForceTemplateSelection, selfRegForceRequiredProperties,
      allowCertificateDownload, allowStatementManagement, allowSystemManagement,
      allowPostTestOrganisationUpdate, allowPostTestSystemUpdate, allowPostTestStatementUpdate,
      domainId
    )
  }

  def extractSystemAdminInfo(request:Request[AnyContent]):Users = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      val ssoEmail = requiredBodyParameter(request, Parameters.USER_EMAIL)
      getUserInfoForSSO(ssoEmail, UserRole.SystemAdmin.id.toShort)
    } else {
      val name = requiredBodyParameter(request, Parameters.USER_NAME)
      val email = requiredBodyParameter(request, Parameters.USER_EMAIL)
      val password = requiredBodyParameter(request, Parameters.PASSWORD)
      Users(0L, name, email, BCrypt.hashpw(password, BCrypt.gensalt()), true, UserRole.SystemAdmin.id.toShort, 0L, None, None, UserSSOStatus.NotMigrated.id.toShort)
    }
  }

  def extractCommunityAdminInfo(request:Request[AnyContent]):Users = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      val ssoEmail = requiredBodyParameter(request, Parameters.USER_EMAIL)
      getUserInfoForSSO(ssoEmail, UserRole.CommunityAdmin.id.toShort)
    } else {
      val name = requiredBodyParameter(request, Parameters.USER_NAME)
      val email = requiredBodyParameter(request, Parameters.USER_EMAIL)
      val password = requiredBodyParameter(request, Parameters.PASSWORD)
      Users(0L, name, email, BCrypt.hashpw(password, BCrypt.gensalt()), true, UserRole.CommunityAdmin.id.toShort, 0L, None, None, UserSSOStatus.NotMigrated.id.toShort)
    }
  }

  def extractAdminInfo(request:Request[AnyContent], ssoEmailToForce: Option[String], passwordIsOneTime: Option[Boolean]):Users = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      var ssoEmail: String = null
      if (ssoEmailToForce.isDefined) {
        ssoEmail = ssoEmailToForce.get
      } else {
        ssoEmail = requiredBodyParameter(request, Parameters.USER_EMAIL)
      }
      getUserInfoForSSO(ssoEmail, UserRole.VendorAdmin.id.toShort)
    } else {
      val name = requiredBodyParameter(request, Parameters.USER_NAME)
      val email = requiredBodyParameter(request, Parameters.USER_EMAIL)
      val password = requiredBodyParameter(request, Parameters.PASSWORD)
      Users(0L, name, email, BCrypt.hashpw(password, BCrypt.gensalt()), passwordIsOneTime.getOrElse(true), UserRole.VendorAdmin.id.toShort, 0L, None, None, UserSSOStatus.NotMigrated.id.toShort)
    }
  }

  def extractAdminInfo(request:Request[AnyContent]):Users = {
    extractAdminInfo(request, None, None)
  }

  private def getUserInfoForSSO(ssoEmail: String, role: Short): Users = {
    val randomPart = ThreadLocalRandom.current.nextInt(10000000, 99999999 + 1)
    val name = "User ["+randomPart+"]"
    val email = randomPart+"@itb.ec.europa.eu"
    val password = randomPart.toString
    Users(0L, name, email, BCrypt.hashpw(password, BCrypt.gensalt()), true, role, 0L, None, Some(ssoEmail), UserSSOStatus.NotLinked.id.toShort)
  }

  def extractUserInfo(request:Request[AnyContent]):Users = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      val ssoEmail = requiredBodyParameter(request, Parameters.USER_EMAIL)
      getUserInfoForSSO(ssoEmail, UserRole.VendorUser.id.toShort)
    } else {
      val name = requiredBodyParameter(request, Parameters.USER_NAME)
      val email = requiredBodyParameter(request, Parameters.USER_EMAIL)
      val password = requiredBodyParameter(request, Parameters.PASSWORD)
      Users(0L, name, email, BCrypt.hashpw(password, BCrypt.gensalt()), true, UserRole.VendorUser.id.toShort, 0L, None, None, UserSSOStatus.NotMigrated.id.toShort)
    }
  }

  def extractSystemInfo(request:Request[AnyContent]):Systems = {
    val sname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_SNAME)
    val fname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_FNAME)
    val descr:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_DESC)
    val version:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_VERSION)
    Systems(0L, sname, fname, descr, version, 0L)
  }

  def extractSystemWithOrganizationInfo(request:Request[AnyContent]):Systems = {
    val sname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_SNAME)
    val fname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_FNAME)
    val descr:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_DESC)
    val version:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_VERSION)
    val owner:Long = ParameterExtractor.requiredBodyParameter(request, Parameters.ORGANIZATION_ID).toLong
    Systems(0L, sname, fname, descr, version, owner)
  }

	def extractDomain(request:Request[AnyContent]):Domain = {
		val sname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
		val fname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
		val descr:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
		Domain(0l, sname, fname, descr)
	}

	def extractOption(request:Request[AnyContent]):Options = {
		val sname = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
		val fname = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
		val actor = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR).toLong
		val descr = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
		Options(0l, sname, fname, descr, actor)
	}

	def extractSpecification(request:Request[AnyContent]): Specifications = {
		val sname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
		val fname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
		val descr:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
    val hidden = ParameterExtractor.requiredBodyParameter(request, Parameters.HIDDEN).toBoolean
		val domain = ParameterExtractor.optionalBodyParameter(request, Parameters.DOMAIN_ID) match {
			case Some(str) => Some(str.toLong)
			case _ => None
		}

		Specifications(0l, sname, fname, descr, hidden, domain.getOrElse(0l))
	}

	def extractActor(request:Request[AnyContent]):Actors = {
    val id:Long = ParameterExtractor. optionalBodyParameter(request, Parameters.ID) match {
      case Some(i) => i.toLong
      case _ => 0l
    }
		val actorId:String = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID)
		val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
		val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
    var default:Option[Boolean] = None
    val defaultStr = ParameterExtractor.optionalBodyParameter(request, Parameters.ACTOR_DEFAULT)
    if (defaultStr.isDefined) {
      default = Some(defaultStr.get.toBoolean)
    } else {
      default = Some(false)
    }
    val hidden = ParameterExtractor.requiredBodyParameter(request, Parameters.HIDDEN).toBoolean
    var displayOrder:Option[Short] = None
    val displayOrderStr = ParameterExtractor.optionalBodyParameter(request, Parameters.DISPLAY_ORDER)
    if (displayOrderStr.isDefined) {
      displayOrder = Some(displayOrderStr.get.toShort)
    }
		val domainId:Long = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
		Actors(id, actorId, name, description, default, hidden, displayOrder, domainId)
	}

  def extractEndpoint(request:Request[AnyContent]):Endpoints = {
    val id:Long = ParameterExtractor.optionalBodyParameter(request, Parameters.ID) match {
      case Some(i) => i.toLong
      case _ => 0l
    }
    val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val desc:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
    val actorId:Long = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    Endpoints(id, name, desc, actorId)
  }

  def extractParameter(request:Request[AnyContent]):models.Parameters = {
      val id:Long = ParameterExtractor.optionalBodyParameter(request, Parameters.ID) match {
      case Some(i) => i.toLong
      case _ => 0L
    }
    val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val desc:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
    val use:String = ParameterExtractor.requiredBodyParameter(request, Parameters.USE)
    val kind:String = ParameterExtractor.requiredBodyParameter(request, Parameters.KIND)
    val endpointId:Long = ParameterExtractor.requiredBodyParameter(request, Parameters.ENDPOINT_ID).toLong
    val adminOnly = ParameterExtractor.requiredBodyParameter(request, Parameters.ADMIN_ONLY).toBoolean
    val notForTests = ParameterExtractor.requiredBodyParameter(request, Parameters.NOT_FOR_TESTS).toBoolean
    var hidden = ParameterExtractor.requiredBodyParameter(request, Parameters.HIDDEN).toBoolean
    val allowedValues:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.ALLOWED_VALUES)
    var dependsOn:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DEPENDS_ON)
    var dependsOnValue:Option[String] = None
    if (dependsOn.isDefined && dependsOn.get.trim.length == 0) {
      dependsOn = None
    }
    if (dependsOn.isDefined) {
      dependsOnValue = ParameterExtractor.optionalBodyParameter(request, Parameters.DEPENDS_ON_VALUE)
    }
    if (dependsOnValue.isDefined && dependsOnValue.get.trim.length == 0) {
      dependsOnValue = None
    }
    if (!adminOnly) {
      hidden = false
    }
    models.Parameters(id, name, desc, use, kind, adminOnly, notForTests, hidden, allowedValues, 0, dependsOn, dependsOnValue, endpointId)
  }

  def extractOrganisationParameter(request:Request[AnyContent]):models.OrganisationParameters = {
    val id:Long = ParameterExtractor.optionalBodyParameter(request, Parameters.ID) match {
      case Some(i) => i.toLong
      case _ => 0L
    }
    val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val testKey:String = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_KEY)
    val desc:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
    val use:String = ParameterExtractor.requiredBodyParameter(request, Parameters.USE)
    val kind:String = ParameterExtractor.requiredBodyParameter(request, Parameters.KIND)
    val communityId:Long = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    val adminOnly = ParameterExtractor.requiredBodyParameter(request, Parameters.ADMIN_ONLY).toBoolean
    val notForTests = ParameterExtractor.requiredBodyParameter(request, Parameters.NOT_FOR_TESTS).toBoolean
    val inExports:Boolean = (kind == "SIMPLE") && ParameterExtractor.requiredBodyParameter(request, Parameters.IN_EXPORTS).toBoolean
    val inSelfRegistration: Boolean = Configurations.REGISTRATION_ENABLED && (!adminOnly) && ParameterExtractor.requiredBodyParameter(request, Parameters.IN_SELFREG).toBoolean
    var hidden: Boolean = ParameterExtractor.requiredBodyParameter(request, Parameters.HIDDEN).toBoolean
    val allowedValues:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.ALLOWED_VALUES)
    var dependsOn:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DEPENDS_ON)
    var dependsOnValue:Option[String] = None
    if (dependsOn.isDefined && dependsOn.get.trim.length == 0) {
      dependsOn = None
    }
    if (dependsOn.isDefined) {
      dependsOnValue = ParameterExtractor.optionalBodyParameter(request, Parameters.DEPENDS_ON_VALUE)
    }
    if (dependsOnValue.isDefined && dependsOnValue.get.trim.length == 0) {
      dependsOnValue = None
    }
    if (!adminOnly) {
      hidden = false
    }
    models.OrganisationParameters(id, name, testKey, desc, use, kind, adminOnly, notForTests, inExports, inSelfRegistration, hidden, allowedValues, 0, dependsOn, dependsOnValue, communityId)
  }

  def extractSystemParameter(request:Request[AnyContent]):models.SystemParameters = {
    val id:Long = ParameterExtractor.optionalBodyParameter(request, Parameters.ID) match {
      case Some(i) => i.toLong
      case _ => 0L
    }
    val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val testKey:String = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_KEY)
    val desc:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
    val use:String = ParameterExtractor.requiredBodyParameter(request, Parameters.USE)
    val kind:String = ParameterExtractor.requiredBodyParameter(request, Parameters.KIND)
    val communityId:Long = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    val adminOnly = ParameterExtractor.requiredBodyParameter(request, Parameters.ADMIN_ONLY).toBoolean
    val notForTests = ParameterExtractor.requiredBodyParameter(request, Parameters.NOT_FOR_TESTS).toBoolean
    val inExports = (kind == "SIMPLE") && ParameterExtractor.requiredBodyParameter(request, Parameters.IN_EXPORTS).toBoolean
    var hidden = ParameterExtractor.requiredBodyParameter(request, Parameters.HIDDEN).toBoolean
    val allowedValues:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.ALLOWED_VALUES)
    var dependsOn:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DEPENDS_ON)
    var dependsOnValue:Option[String] = None
    if (dependsOn.isDefined && dependsOn.get.trim.length == 0) {
      dependsOn = None
    }
    if (dependsOn.isDefined) {
      dependsOnValue = ParameterExtractor.optionalBodyParameter(request, Parameters.DEPENDS_ON_VALUE)
    }
    if (dependsOnValue.isDefined && dependsOnValue.get.trim.length == 0) {
      dependsOnValue = None
    }
    if (!adminOnly) {
      hidden = false
    }
    models.SystemParameters(id, name, testKey, desc, use, kind, adminOnly, notForTests, inExports, hidden, allowedValues, 0, dependsOn, dependsOnValue, communityId)
  }

  def extractLandingPageInfo(request:Request[AnyContent]):LandingPages = {
    val name = requiredBodyParameter(request, Parameters.NAME)
    val desc = optionalBodyParameter(request, Parameters.DESCRIPTION)
    val content = HtmlUtil.sanitizeEditorContent(requiredBodyParameter(request, Parameters.CONTENT))
    val default = requiredBodyParameter(request, Parameters.DEFAULT).toBoolean
    val communityId = requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
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

  def extractTriggerInfo(request:Request[AnyContent], triggerId: Option[Long]): Trigger = {
    // Trigger.
    val name = requiredBodyParameter(request, Parameters.NAME)
    val description = optionalBodyParameter(request, Parameters.DESCRIPTION)
    val url = requiredBodyParameter(request, Parameters.URL)
    val operation = optionalBodyParameter(request, Parameters.OPERATION)
    val active = requiredBodyParameter(request, Parameters.ACTIVE).toBoolean
    val eventType = requiredBodyParameter(request, Parameters.EVENT).toShort
    // Check that this is a valid value (otherwise throw exception)
    TriggerEventType.apply(eventType)
    val communityId = requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    new Trigger(
      Triggers(triggerId.getOrElse(0L), name, description, url, eventType, operation, active, None, None, communityId),
      extractTriggerDataItems(request, Parameters.DATA, triggerId)
    )
  }

  def extractLegalNoticeInfo(request:Request[AnyContent]):LegalNotices = {
    val name = requiredBodyParameter(request, Parameters.NAME)
    val desc = optionalBodyParameter(request, Parameters.DESCRIPTION)
    val content = HtmlUtil.sanitizeEditorContent(requiredBodyParameter(request, Parameters.CONTENT))
    val default = requiredBodyParameter(request, Parameters.DEFAULT).toBoolean
    val communityId = requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    LegalNotices(0L, name, desc, content, default, communityId)
  }

  def extractErrorTemplateInfo(request:Request[AnyContent]):ErrorTemplates = {
    val name = requiredBodyParameter(request, Parameters.NAME)
    val desc = optionalBodyParameter(request, Parameters.DESCRIPTION)
    val content = HtmlUtil.sanitizeEditorContent(requiredBodyParameter(request, Parameters.CONTENT))
    val default = requiredBodyParameter(request, Parameters.DEFAULT).toBoolean
    val communityId = requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    ErrorTemplates(0L, name, desc, content, default, communityId)
  }

	def extractIdsQueryParameter(request:Request[AnyContent]): Option[List[String]] = {
		val idsStr = ParameterExtractor.optionalQueryParameter(request, Parameters.IDS)
		val ids = idsStr match {
			case Some(str) => Some(str.split(",").toList)
			case None => None
		}
		ids
	}

	def extractLongIdsQueryParameter(request:Request[AnyContent]): Option[List[Long]] = {
		val idsStr = ParameterExtractor.optionalQueryParameter(request, Parameters.IDS)
		val ids = idsStr match {
			case Some(str) => Some(str.split(",").map(_.toLong).toList)
			case None => None
		}
		ids
	}

  def extractLongIdsBodyParameter(request:Request[AnyContent]): Option[List[Long]] = {
    val idsStr = ParameterExtractor.optionalBodyParameter(request, Parameters.IDS)
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

  def optionalLongListQueryParameter(request:Request[AnyContent],parameter:String): Option[List[Long]] = {
    val listStr = ParameterExtractor.optionalQueryParameter(request, parameter)
    val list = listStr match {
      case Some(str) => Some(str.split(",").map(_.toLong).toList)
      case None => None
    }
    list
  }

  def optionalLongListBodyParameter(request:Request[AnyContent],parameter:String): Option[List[Long]] = {
    val listStr = ParameterExtractor.optionalBodyParameter(request, parameter)
    val list = listStr match {
      case Some(str) => Some(str.split(",").map(_.toLong).toList)
      case None => None
    }
    list
  }

}