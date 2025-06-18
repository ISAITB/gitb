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
import controllers.util._
import exceptions.{ErrorCodes, InvalidRequestException}
import managers._
import models.Constants
import models.Enums.UserRole
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.tika.Tika
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils.{CryptoUtil, HtmlUtil, JsonUtil}

import java.nio.file.Files
import javax.inject.Inject
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}


class AccountService @Inject() (authorizedAction: AuthorizedAction,
                                cc: ControllerComponents,
                                accountManager: AccountManager,
                                legalNoticeManager: LegalNoticeManager,
                                organisationManager: OrganizationManager,
                                authorizationManager: AuthorizationManager)
                               (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[AccountService])
  private final val tika = new Tika()

  /**
   * Gets the company profile for the authenticated user
   */
  def getVendorProfile(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnOrganisation(request).flatMap { _ =>
      val userId = ParameterExtractor.extractUserId(request)
      accountManager.getVendorProfile(userId).map { organization =>
        val json:String = JsonUtil.serializeOrganization(organization)
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Updates the company profile
   */
  def updateVendorProfile(): Action[AnyContent] = authorizedAction.async { request =>
    (for {
      landingPageIdToUse <- {
        val landingPageId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.LANDING_PAGE_ID)
        val landingPageIdToUse = if (landingPageId.isDefined) {
          authorizationManager.canUpdateOwnOrganisationAndLandingPage(request, landingPageId).map { _ =>
            if (landingPageId.get == -1) {
              // Remove the currently set landing page.
              Some(None)
            } else {
              // Set the landing page.
              Some(landingPageId)
            }
          }
        } else {
          authorizationManager.canUpdateOwnOrganisation(request, ignoreExistingTests = false).map { _ =>
            // No update to the landing page.
            None
          }
        }
        landingPageIdToUse
      }
      result <- {
        val adminId = ParameterExtractor.extractUserId(request)
        val paramMap = ParameterExtractor.paramMap(request)

        val shortName = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.VENDOR_SNAME)
        val fullName = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.VENDOR_FNAME)
        val values = ParameterExtractor.extractOrganisationParameterValues(paramMap, Parameters.PROPERTIES, optional = true)
        val files = ParameterExtractor.extractFiles(request).map {
          case (key, value) => (key.substring(key.indexOf('_')+1).toLong, value)
        }
        if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(files.map(entry => entry._2.file))) {
          Future.successful {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "File failed virus scan.")
          }
        } else {
          organisationManager.updateOwnOrganization(adminId, shortName, fullName, values, Some(files), landingPageIdToUse).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        }
      }
    } yield result).andThen { _ =>
        if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  /**
   * Gets the all users for the vendor
   */
  def getVendorUsers(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnOrganisationUsers(request).flatMap { _ =>
    val userId = ParameterExtractor.extractUserId(request)
      accountManager.getVendorUsers(userId).map { list =>
        val json:String = JsonUtil.jsUsers(list).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * The authenticated admin registers new user for the organization
   */
  def registerUser: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canCreateUserInOwnOrganisation(request).flatMap { _ =>
      val adminId = ParameterExtractor.extractUserId(request)
      val roleId = ParameterExtractor.requiredBodyParameter(request, Parameters.ROLE_ID).toShort
      val user = UserRole(roleId) match {
        case UserRole.VendorUser => ParameterExtractor.extractUserInfo(request)
        case UserRole.VendorAdmin => ParameterExtractor.extractAdminInfo(request)
        case _ => throw new IllegalArgumentException("Cannot create user with role " + roleId)
      }
      accountManager.registerUser(adminId, user).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
   * Returns the user profile of the authenticated user
   */
  def getUserProfile: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOwnProfile(request).flatMap { _ =>
      val userId = ParameterExtractor.extractUserId(request)
      accountManager.getUserProfile(userId).map { user =>
        val json:String = JsonUtil.serializeUser(user)
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }
  
  /**
   * Updates the user profile of the authenticated user
   */
  def updateUserProfile(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateOwnProfile(request).flatMap { _ =>
      val userId = ParameterExtractor.extractUserId(request)
      val name:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.USER_NAME)
      val passwd:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.PASSWORD)
      val oldPasswd:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.OLD_PASSWORD)

      if (passwd.isDefined && !CryptoUtil.isAcceptedPassword(passwd.get)) {
        Future.successful {
          ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "The provided password does not match minimum complexity requirements.", Some("new"))
        }
      } else {
        accountManager.updateUserProfile(userId, name, passwd, oldPasswd).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }.recover {
          case _: InvalidRequestException => ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_CREDENTIALS, "Incorrect password.", Some("current"))
        }
      }
    }
  }

  def getConfiguration: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewConfiguration(request).flatMap { _ =>
      legalNoticeManager.getCommunityDefaultLegalNotice(Constants.DefaultCommunityId).map { legalNotice =>
        val configProperties = new java.util.HashMap[String, String]()
        configProperties.put("email.enabled", String.valueOf(Configurations.EMAIL_ENABLED))
        configProperties.put("email.contactFormEnabled", String.valueOf(Configurations.EMAIL_CONTACT_FORM_ENABLED.getOrElse(false)))
        configProperties.put("email.attachments.maxCount", String.valueOf(Configurations.EMAIL_ATTACHMENTS_MAX_COUNT))
        configProperties.put("email.attachments.maxSize", String.valueOf(Configurations.EMAIL_ATTACHMENTS_MAX_SIZE))
        configProperties.put("email.attachments.allowedTypes", String.valueOf(StringUtils.join(Configurations.EMAIL_ATTACHMENTS_ALLOWED_TYPES,",")))
        configProperties.put("survey.enabled", String.valueOf(Configurations.SURVEY_ENABLED))
        configProperties.put("survey.address", String.valueOf(Configurations.SURVEY_ADDRESS))
        configProperties.put("moreinfo.enabled", String.valueOf(Configurations.MORE_INFO_ENABLED))
        configProperties.put("moreinfo.address", String.valueOf(Configurations.MORE_INFO_ADDRESS))
        configProperties.put("releaseinfo.enabled", String.valueOf(Configurations.RELEASE_INFO_ENABLED))
        configProperties.put("releaseinfo.address", String.valueOf(Configurations.RELEASE_INFO_ADDRESS))
        configProperties.put("userguide.ou", String.valueOf(Configurations.USERGUIDE_OU))
        configProperties.put("userguide.oa", String.valueOf(Configurations.USERGUIDE_OA))
        configProperties.put("userguide.ta", String.valueOf(Configurations.USERGUIDE_TA))
        configProperties.put("userguide.ca", String.valueOf(Configurations.USERGUIDE_CA))
        configProperties.put("sso.enabled", String.valueOf(Configurations.AUTHENTICATION_SSO_ENABLED))
        configProperties.put("sso.inMigration", String.valueOf(Configurations.AUTHENTICATION_SSO_IN_MIGRATION_PERIOD))
        configProperties.put("demos.enabled", String.valueOf(Configurations.DEMOS_ENABLED))
        configProperties.put("demos.account", String.valueOf(Configurations.DEMOS_ACCOUNT))
        configProperties.put("registration.enabled", String.valueOf(Configurations.REGISTRATION_ENABLED))
        configProperties.put("savedFile.maxSize", String.valueOf(Configurations.SAVED_FILE_MAX_SIZE))
        configProperties.put("mode", String.valueOf(Configurations.TESTBED_MODE))
        configProperties.put("automationApi.enabled", String.valueOf(Configurations.AUTOMATION_API_ENABLED))
        configProperties.put("versionNumber", Configurations.versionInfo())
        configProperties.put("hasDefaultLegalNotice", legalNotice.exists(notice => StringUtils.isNotBlank(notice.content)).toString)
        configProperties.put("conformanceStatementReportMaxTestCases", String.valueOf(Configurations.CONFORMANCE_STATEMENT_REPORT_MAX_TEST_CASES))
        val json = JsonUtil.serializeConfigurationProperties(configProperties)
        ResponseConstructor.constructJsonResponse(json.toString())
      }
    }
  }

  def submitFeedback: Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canSubmitFeedback(request).flatMap { _ =>
      if (Configurations.EMAIL_CONTACT_FORM_ENABLED.getOrElse(true)) {
        val paramMap = ParameterExtractor.paramMap(request)
        val userId = ParameterExtractor.extractOptionalUserId(request)
        val userEmail: String = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.USER_EMAIL)
        val messageTypeId: String = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.MESSAGE_TYPE_ID)
        val messageTypeDescription: String = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.MESSAGE_TYPE_DESCRIPTION)
        val messageContent: String = HtmlUtil.sanitizeMinimalEditorContent(ParameterExtractor.requiredBodyParameter(paramMap, Parameters.MESSAGE_CONTENT))
        // Extract attachments
        val attachments = new mutable.LinkedHashMap[String, AttachmentType]()
        val files = ParameterExtractor.extractFiles(request)
        var response: Option[Result] = None
        if (files.nonEmpty) {
          var totalAttachmentSize = 0L
          for (file <- files) {
            attachments += (file._2.key -> new AttachmentType(file._2.name, file._2.file))
            totalAttachmentSize += Files.size(file._2.file.toPath)
          }
          // Validate attachments
          if (attachments.size > Configurations.EMAIL_ATTACHMENTS_MAX_COUNT) {
            // Count.
            response = Some(ResponseConstructor.constructErrorResponse(ErrorCodes.EMAIL_ATTACHMENT_COUNT_EXCEEDED, s"A maximum of ${Configurations.EMAIL_ATTACHMENTS_MAX_COUNT} attachments can be provided", Some("files")))
          } else if (totalAttachmentSize > (Configurations.EMAIL_ATTACHMENTS_MAX_SIZE * 1024 * 1024)) {
            // Size.
            response = Some(ResponseConstructor.constructErrorResponse(ErrorCodes.EMAIL_ATTACHMENT_COUNT_EXCEEDED, s"The total size of attachments cannot exceed ${Configurations.EMAIL_ATTACHMENTS_MAX_SIZE} MBs.", Some("files")))
          } else {
            if (response == null) {
              // Check for viruses.
              if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(attachments.map(_._2.getContent))) {
                response = Some(ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Attachments failed virus scan."))
              } else {
                // Check (and set) mime types.
                val invalidAttachmentKeys = ListBuffer[String]()
                attachments.foreach { attachment =>
                  val detectedMimeType = tika.detect(attachment._2.getContent)
                  if (!Configurations.EMAIL_ATTACHMENTS_ALLOWED_TYPES.contains(detectedMimeType)) {
                    logger.warn(s"Attachment type [$detectedMimeType] of file [${attachment._2.getName}] not allowed.")
                    invalidAttachmentKeys += attachment._1
                  } else {
                    attachment._2.setType(detectedMimeType);
                  }
                }
                if (invalidAttachmentKeys.nonEmpty) {
                  response = Some(ResponseConstructor.constructErrorResponse(ErrorCodes.EMAIL_ATTACHMENT_TYPE_NOT_ALLOWED, "Allowed attachment types are images, text files and PDFs.", Some(invalidAttachmentKeys.mkString(","))))
                }
              }
            }
          }
        }
        if (response.isEmpty) {
          accountManager.submitFeedback(userId, userEmail, messageTypeId, messageTypeDescription, messageContent, attachments.values.toArray).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful(response.get)
        }
      } else {
        Future.successful(ResponseConstructor.constructEmptyResponse)
      }
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }
}
