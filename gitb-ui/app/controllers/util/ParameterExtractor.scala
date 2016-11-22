package controllers.util

import models._
import models.Enums._
import play.api.mvc._
import exceptions.{ErrorCodes, InvalidRequestException}

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

  def extractUserId(request:Request[AnyContent]):Long = {
    request.headers.get(Parameters.USER_ID).get.toLong
  }

  def extractOrganizationInfo(request:Request[AnyContent]):Organizations = {
    val vendorSname = requiredBodyParameter(request, Parameters.VENDOR_SNAME)
    val vendorFname = requiredBodyParameter(request, Parameters.VENDOR_FNAME)
    Organizations(0L, vendorSname, vendorFname, OrganizationType.Vendor.id.toShort)
  }

  def extractSystemAdminInfo(request:Request[AnyContent]):Users = {
    val name = requiredBodyParameter(request, Parameters.USER_NAME)
    val email = requiredBodyParameter(request, Parameters.USER_EMAIL)
    val password = requiredBodyParameter(request, Parameters.PASSWORD)
    Users(0L, name, email, password, UserRole.SystemAdmin.id.toShort, 0L)
  }

  def extractAdminInfo(request:Request[AnyContent]):Users = {
    val name = requiredBodyParameter(request, Parameters.USER_NAME)
    val email = requiredBodyParameter(request, Parameters.USER_EMAIL)
    val password = requiredBodyParameter(request, Parameters.PASSWORD)
    Users(0L, name, email, password, UserRole.VendorAdmin.id.toShort, 0L)
  }

  def extractUserInfo(request:Request[AnyContent]):Users = {
    val name = requiredBodyParameter(request, Parameters.USER_NAME)
    val email = requiredBodyParameter(request, Parameters.USER_EMAIL)
    val password = requiredBodyParameter(request, Parameters.PASSWORD)
    Users(0L, name, email, password, UserRole.VendorUser.id.toShort, 0L)
  }

  def extractSystemInfo(request:Request[AnyContent]):Systems = {
    val sname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_SNAME)
    val fname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_FNAME)
    val descr:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_DESC)
    val version:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_VERSION)
    Systems(0L, sname, fname, descr, version, 0L)
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
		val sname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
		val fname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
		val descr:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
		val domainId:Long = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
		Actors(id, sname, fname, descr, domainId)
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
}
