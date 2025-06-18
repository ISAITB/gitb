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

package models

import config.Configurations
import org.slf4j.{Logger, LoggerFactory}
import utils.MimeUtil

import java.util.Properties

object EmailSettings {

  def fromEnvironment(): EmailSettings = {
    EmailSettings(
      Configurations.EMAIL_ENABLED,
      Configurations.EMAIL_FROM,
      Configurations.EMAIL_TO,
      Configurations.EMAIL_SMTP_HOST,
      Configurations.EMAIL_SMTP_PORT,
      Configurations.EMAIL_SMTP_AUTH_ENABLED,
      Configurations.EMAIL_SMTP_AUTH_USERNAME,
      Configurations.EMAIL_SMTP_AUTH_PASSWORD,
      Configurations.EMAIL_SMTP_SSL_ENABLED,
      Configurations.EMAIL_SMTP_STARTTLS_ENABLED,
      Configurations.EMAIL_SMTP_SSL_PROTOCOLS,
      Some(Configurations.EMAIL_ATTACHMENTS_MAX_COUNT),
      Some(Configurations.EMAIL_ATTACHMENTS_MAX_SIZE),
      Some(Configurations.EMAIL_ATTACHMENTS_ALLOWED_TYPES),
      Some(Configurations.EMAIL_NOTIFICATION_TEST_INTERACTION_REMINDER),
      Configurations.EMAIL_CONTACT_FORM_ENABLED,
      Configurations.EMAIL_CONTACT_FORM_COPY_DEFAULT_MAILBOX
    )
  }

}

case class EmailSettings(
  enabled: Boolean,
  from: Option[String],
  to: Option[Array[String]],
  smtpHost: Option[String],
  smtpPort: Option[Int],
  authEnabled: Option[Boolean],
  authUsername: Option[String],
  authPassword: Option[String],
  sslEnabled: Option[Boolean],
  startTlsEnabled: Option[Boolean],
  sslProtocols: Option[Array[String]],
  maximumAttachments: Option[Int],
  maximumAttachmentSize: Option[Int],
  allowedAttachmentTypes: Option[Set[String]],
  testInteractionReminder: Option[Int],
  contactFormEnabled: Option[Boolean],
  contactFormCopyDefaultMailbox: Option[Boolean]
) {

  private final val logger: Logger = LoggerFactory.getLogger(classOf[EmailSettings])

  def withPassword(password: String): EmailSettings = {
    EmailSettings(
      enabled, from, to, smtpHost, smtpPort, authEnabled, authUsername, Some(password), sslEnabled, startTlsEnabled, sslProtocols,
      maximumAttachments, maximumAttachmentSize, allowedAttachmentTypes, testInteractionReminder, contactFormEnabled, contactFormCopyDefaultMailbox
    )
  }

  def toEnvironment(defaultSettings: Option[EmailSettings] = None): Unit = {
    Configurations.EMAIL_ENABLED = enabled
    if (enabled) {
      Configurations.EMAIL_FROM = from
      Configurations.EMAIL_TO = to
      Configurations.EMAIL_SMTP_HOST = smtpHost
      Configurations.EMAIL_SMTP_PORT = smtpPort
      Configurations.EMAIL_SMTP_AUTH_ENABLED = authEnabled
      if (authEnabled.getOrElse(false)) {
        Configurations.EMAIL_SMTP_AUTH_USERNAME = authUsername
        if (authPassword.isDefined) {
          Configurations.EMAIL_SMTP_AUTH_PASSWORD = Some(MimeUtil.decryptString(authPassword.get))
        }
      } else {
        Configurations.EMAIL_SMTP_AUTH_USERNAME = None
        Configurations.EMAIL_SMTP_AUTH_PASSWORD = None
      }
      Configurations.EMAIL_SMTP_SSL_ENABLED = sslEnabled
      Configurations.EMAIL_SMTP_SSL_PROTOCOLS = sslProtocols.orElse(defaultSettings.flatMap(_.sslProtocols))
      Configurations.EMAIL_SMTP_STARTTLS_ENABLED = startTlsEnabled
      Configurations.EMAIL_ATTACHMENTS_MAX_COUNT = maximumAttachments.getOrElse(defaultSettings.flatMap(_.maximumAttachments).getOrElse(5))
      Configurations.EMAIL_ATTACHMENTS_MAX_SIZE = maximumAttachmentSize.getOrElse(defaultSettings.flatMap(_.maximumAttachmentSize).getOrElse(5))
      Configurations.EMAIL_ATTACHMENTS_ALLOWED_TYPES = allowedAttachmentTypes.getOrElse(defaultSettings.flatMap(_.allowedAttachmentTypes).getOrElse(Set.empty))
      Configurations.EMAIL_NOTIFICATION_TEST_INTERACTION_REMINDER = testInteractionReminder.getOrElse(defaultSettings.flatMap(_.testInteractionReminder).getOrElse(30))
      Configurations.EMAIL_CONTACT_FORM_ENABLED = contactFormEnabled
      Configurations.EMAIL_CONTACT_FORM_COPY_DEFAULT_MAILBOX = contactFormCopyDefaultMailbox
    } else {
      Configurations.EMAIL_FROM = None
      Configurations.EMAIL_TO = None
      Configurations.EMAIL_SMTP_HOST = None
      Configurations.EMAIL_SMTP_PORT = None
      Configurations.EMAIL_SMTP_AUTH_ENABLED = None
      Configurations.EMAIL_SMTP_AUTH_USERNAME = None
      Configurations.EMAIL_SMTP_AUTH_PASSWORD = None
      Configurations.EMAIL_SMTP_SSL_ENABLED = None
      Configurations.EMAIL_SMTP_SSL_PROTOCOLS = defaultSettings.flatMap(_.sslProtocols)
      Configurations.EMAIL_SMTP_STARTTLS_ENABLED = None
      Configurations.EMAIL_ATTACHMENTS_MAX_COUNT = defaultSettings.flatMap(_.maximumAttachments).getOrElse(5)
      Configurations.EMAIL_ATTACHMENTS_MAX_SIZE = defaultSettings.flatMap(_.maximumAttachmentSize).getOrElse(5)
      Configurations.EMAIL_ATTACHMENTS_ALLOWED_TYPES = defaultSettings.flatMap(_.allowedAttachmentTypes).getOrElse(Set.empty)
      Configurations.EMAIL_NOTIFICATION_TEST_INTERACTION_REMINDER = defaultSettings.flatMap(_.testInteractionReminder).getOrElse(30)
      Configurations.EMAIL_CONTACT_FORM_ENABLED = defaultSettings.flatMap(_.contactFormEnabled)
      Configurations.EMAIL_CONTACT_FORM_COPY_DEFAULT_MAILBOX = defaultSettings.flatMap(_.contactFormCopyDefaultMailbox)
    }
  }

  def toSmtpProperties(): Properties = {
    val props = new Properties()
    if (smtpHost.isDefined) {
      props.setProperty("mail.smtp.host", smtpHost.get)
    }
    if (smtpPort.isDefined) {
      props.setProperty("mail.smtp.port", smtpPort.get.toString)
    }
    if (authEnabled.isDefined && authEnabled.get) {
      props.setProperty("mail.smtp.auth", "true")
    }
    if (sslEnabled.isDefined && sslEnabled.get) {
      props.setProperty("mail.smtp.ssl.enable", "true")
      if (sslProtocols.isDefined && sslProtocols.get.nonEmpty) {
        props.setProperty("mail.smtp.ssl.protocols", sslProtocols.get.mkString(" "))
      }
    }
    if (startTlsEnabled.isDefined && startTlsEnabled.get) {
      props.setProperty("mail.smtp.starttls.enable", "true")
    }
    props
  }

  def areValid(): Boolean = {
    var valid = true
    if (enabled) {
      if (smtpHost.isEmpty) {
        logger.warn("Invalid email settings: No SMTP server host is defined.")
        valid = false
      }
      if (smtpPort.isEmpty) {
        logger.warn("Invalid email settings: No SMTP server port is defined.")
        valid = false
      }
      if (authEnabled.getOrElse(false)) {
        if (authUsername.isEmpty) {
          logger.warn("Invalid email settings: SMTP server authentication is enabled but no username is defined.")
          valid = false
        }
        if (authPassword.isEmpty) {
          logger.warn("Invalid email settings: SMTP server authentication is enabled but no password is defined.")
          valid = false
        }
      }
      if (from.isEmpty) {
        logger.warn("Invalid email settings: The Test Bed's email address is not defined.")
        valid = false
      }
      if (contactFormEnabled.getOrElse(false)) {
        if (to.isEmpty || to.get.isEmpty) {
          logger.warn("Invalid email settings: The contact form is enabled but the Test Bed's default support mailbox is not defined.")
          valid = false
        }
      }
    }
    valid
  }

}