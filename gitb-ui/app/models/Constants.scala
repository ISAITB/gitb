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

/**
 * Created by VWYNGAET on 07/11/2016.
 */
object Constants {
  // Organization constants
  val DefaultOrganizationId = 0L
  val DefaultCommunityId = 0L
  val AdminOrganizationName = "Admin Organization"

  // System configuration constants
  val SessionAliveTime = "session_alive_time"
  val RestApiEnabled = "rest_api_enabled"
  val RestApiAdminKey = "rest_api_admin_key"
  val SelfRegistrationEnabled = "self_registration_enabled"
  val MasterPassword = "master_password"
  val DemoAccount = "demo_account"
  val WelcomeMessage = "welcome"
  val AccountRetentionPeriod = "account_retention_period"
  val EmailSettings = "email_settings"

  // env variables
  val EnvironmentTheme = "THEME"
  val DefaultTheme = "gitb"

  val domainTestVariable = "DOMAIN"
  val organisationTestVariable = "ORGANISATION"
  val systemTestVariable = "SYSTEM"

  val domainConfigurationName: String = "com.gitb."+domainTestVariable
  val organisationConfigurationName: String = "com.gitb."+organisationTestVariable
  val systemConfigurationName: String = "com.gitb."+systemTestVariable

  val organisationConfiguration_shortName = "shortName"
  val organisationConfiguration_fullName = "fullName"
  val systemConfiguration_shortName = "shortName"
  val systemConfiguration_fullName = "fullName"
  val systemConfiguration_version = "version"
  val systemConfiguration_apiKey = "apiKey"

  val PlaceholderOrganisation = "$ORGANISATION"
  val PlaceholderSystem = "$SYSTEM"
  val PlaceholderSpecification = "$SPECIFICATION"
  val PlaceholderSpecificationGroup = "$SPECIFICATION_GROUP"
  val PlaceholderSpecificationGroupOption = "$SPECIFICATION_GROUP_OPTION"
  val PlaceholderActor = "$ACTOR"
  val PlaceholderDomain = "$DOMAIN"
  val PlaceholderBadge = "$BADGE"
  val PlaceholderBadges = "$BADGES"
  val PlaceholderReportDate = "$REPORT_DATE"
  val PlaceholderLastUpdateDate = "$LAST_UPDATE_DATE"
  val PlaceholderSnapshot = "$SNAPSHOT"

  val ProductionMode = "production"
  val DevelopmentMode = "development"

  val FilterDateFormat = "dd-MM-yyyy HH:mm:ss"
  val AutomationHeader = "ITB_API_KEY"
  val AcceptHeader = "Accept"

  val defaultPage = 1L
  val defaultLimit = 10L

  val MimeTypeAny = "*/*"
  val MimeTypePDF = "application/pdf"
  val MimeTypeXML = "application/xml"
  val MimeTypeTextXML = "text/xml"

  val UserAttributeEmail = "email"
  val UserAttributeFirstName = "firstName"
  val UserAttributeLastName = "lastName"
  val UserAttributeAuthenticationLevel = "authLevel"

  // When ending in "-snapshot", this is considered a non-published release.
  val VersionNumber = "1.26.0"
  val VersionNumberPostfixForResources = ""

}
