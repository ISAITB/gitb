package controllers.util

import java.util.Random
import java.util.concurrent.ThreadLocalRandom

import config.Configurations
import controllers.util.ParameterExtractor.getUserInfoForSSO
import exceptions.{ErrorCodes, InvalidRequestException}
import models.Enums._
import models._
import org.mindrot.jbcrypt.BCrypt
import play.api.mvc._
import utils.HtmlUtil

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

  def optionalLongQueryParameter(request:Request[AnyContent], parameter:String):Option[Long] = {
    val param = request.getQueryString(parameter)
    if (param.isDefined) {
      Some(param.get.toLong)
    } else {
      None
    }
  }

  def requiredBodyParameter(request:Request[AnyContent], parameter:String):String = {
    try {
      val param = request.body.asFormUrlEncoded.get(parameter)(0)
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
      if(paramList.length > 0){
        Some(paramList(0).toLong)
      } else{
        None
      }
    } catch {
      case e:NoSuchElementException =>
        None
    }
  }

  def optionalIntBodyParameter(request:Request[AnyContent], parameter:String):Option[Int] = {
    try {
      val paramList = request.body.asFormUrlEncoded.get(parameter)
      if(paramList.length > 0){
        Some(paramList(0).toInt)
      } else{
        None
      }
    } catch {
      case e:NoSuchElementException =>
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

  def extractUserId(request:Request[AnyContent]):Long = {
    request.headers.get(Parameters.USER_ID).get.toLong
  }

  def extractOrganizationInfo(request:Request[AnyContent]):Organizations = {
    val vendorSname = requiredBodyParameter(request, Parameters.VENDOR_SNAME)
    val vendorFname = requiredBodyParameter(request, Parameters.VENDOR_FNAME)
    val communityId = requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    val landingPageId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.LANDING_PAGE_ID)
    val legalNoticeId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.LEGAL_NOTICE_ID)
    val errorTemplateId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.ERROR_TEMPLATE_ID)
    Organizations(0L, vendorSname, vendorFname, OrganizationType.Vendor.id.toShort, false, landingPageId, legalNoticeId, errorTemplateId, communityId)
  }

  def extractCommunityInfo(request:Request[AnyContent]):Communities = {
    val sname = requiredBodyParameter(request, Parameters.COMMUNITY_SNAME)
    val fname = requiredBodyParameter(request, Parameters.COMMUNITY_FNAME)
    val email = optionalBodyParameter(request, Parameters.COMMUNITY_EMAIL)
    val domainId:Option[Long] = ParameterExtractor.optionalLongBodyParameter(request, Parameters.DOMAIN_ID)
    Communities(0L, sname, fname, email, domainId)
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

  def extractAdminInfo(request:Request[AnyContent]):Users = {
    if (Configurations.AUTHENTICATION_SSO_ENABLED) {
      val ssoEmail = requiredBodyParameter(request, Parameters.USER_EMAIL)
      getUserInfoForSSO(ssoEmail, UserRole.VendorAdmin.id.toShort)
    } else {
      val name = requiredBodyParameter(request, Parameters.USER_NAME)
      val email = requiredBodyParameter(request, Parameters.USER_EMAIL)
      val password = requiredBodyParameter(request, Parameters.PASSWORD)
      Users(0L, name, email, BCrypt.hashpw(password, BCrypt.gensalt()), true, UserRole.VendorAdmin.id.toShort, 0L, None, None, UserSSOStatus.NotMigrated.id.toShort)
    }
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
		val urls = ParameterExtractor.optionalBodyParameter(request, Parameters.URLS)
		val diagram:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DIAGRAM)
		val descr:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
		val specificationType: Option[Short] = ParameterExtractor.optionalBodyParameter(request, Parameters.SPEC_TYPE) match {
			case Some(str) => Some(str.toShort)
			case _ => None
		}
		val domain = ParameterExtractor.optionalBodyParameter(request, Parameters.DOMAIN_ID) match {
			case Some(str) => Some(str.toLong)
			case _ => None
		}

		Specifications(0l, sname, fname, urls, diagram, descr, specificationType.getOrElse(0), domain.getOrElse(0l))
	}

	def extractActor(request:Request[AnyContent]):Actors = {
    val id:Long = ParameterExtractor.optionalBodyParameter(request, Parameters.ID) match {
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
    var displayOrder:Option[Short] = None
    val displayOrderStr = ParameterExtractor.optionalBodyParameter(request, Parameters.DISPLAY_ORDER)
    if (displayOrderStr.isDefined) {
      displayOrder = Some(displayOrderStr.get.toShort)
    }
		val domainId:Long = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
		Actors(id, actorId, name, description, default, displayOrder, domainId)
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
      case _ => 0l
    }
    val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val desc:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
    val use:String = ParameterExtractor.requiredBodyParameter(request, Parameters.USE)
    val kind:String = ParameterExtractor.requiredBodyParameter(request, Parameters.KIND)
    val endpointId:Long = ParameterExtractor.requiredBodyParameter(request, Parameters.ENDPOINT_ID).toLong
    models.Parameters(id, name, desc, use, kind, endpointId)
  }

  def extractLandingPageInfo(request:Request[AnyContent]):LandingPages = {
    val name = requiredBodyParameter(request, Parameters.NAME)
    val desc = optionalBodyParameter(request, Parameters.DESCRIPTION)
    val content = HtmlUtil.sanitizeEditorContent(requiredBodyParameter(request, Parameters.CONTENT))
    val default = requiredBodyParameter(request, Parameters.DEFAULT).toBoolean
    val communityId = requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
    LandingPages(0L, name, desc, content, default, communityId)
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

}