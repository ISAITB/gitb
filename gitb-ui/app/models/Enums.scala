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

object Enums {
  object OrganizationType extends Enumeration(1) {
    type OrganizationType = Value
    val Vendor, SDO = Value
  }

  object UserRole extends Enumeration(1) {
    type UserRole = Value
    val VendorAdmin, VendorUser, DomainUser, SystemAdmin, CommunityAdmin = Value
  }

  object SelfRegistrationType extends Enumeration(1) {
    type SelfRegistrationType = Value
    val NotSupported, PublicListing, PublicListingWithToken, Token = Value
  }

  object SelfRegistrationRestriction extends Enumeration(1) {
    type SelfRegistrationType = Value
    val NoRestriction, UserEmail, UserEmailDomain = Value
  }

  object ParameterType extends Enumeration(1) {
    type ParameterType = Value
    val Simple, SimpleList, Binary, BinaryList = Value
  }

  object Result extends Enumeration(1) {
    type Result = Value
    val Success, Fail, NotDone, Invalid, Undefined = Value
  }

  object Status extends Enumeration(1) {
    type Status = Value
    val Processing, Waiting, Error, Completed, Skipped = Value
  }

  object UserSSOStatus extends Enumeration(1) {
    type Status = Value
    val NotMigrated, NotLinked, Linked = Value
  }

  object TestResultStatus extends Enumeration {
    val UNDEFINED = Value("UNDEFINED")
    val SUCCESS = Value("SUCCESS")
    val FAILURE = Value("FAILURE")
  }

  object TestSuiteReplacementChoice extends Enumeration {
    type TestSuiteReplacementChoice = Value
    val PROCEED = Value(0)
    val CANCEL = Value(1)
  }

  object LabelType extends Enumeration(1) {
    type LabelType = Value
    val Domain, Specification, Actor, Endpoint, Organisation, System, SpecificationInGroup, SpecificationGroup = Value
  }

  object ImportItemType extends Enumeration(1) {
    type ImportItemType = Value
    val Domain, DomainParameter, Specification, Actor, Endpoint, EndpointParameter, TestSuite,
    Community, Administrator, CustomLabel, OrganisationProperty, SystemProperty, LandingPage, LegalNotice, ErrorTemplate,
    Organisation, OrganisationUser, OrganisationPropertyValue, System, SystemPropertyValue,
    Statement, StatementConfiguration, Trigger, CommunityResource, SpecificationGroup, Settings, Theme,
    DefaultLandingPage, DefaultLegalNotice, DefaultErrorTemplate, SystemAdministrator, SystemConfiguration, SystemResource = Value
  }

  object ImportItemMatch extends Enumeration(1) {
    type ImportItemMatch = Value
    val ArchiveOnly, Both, DBOnly = Value
  }

  object ImportItemChoice extends Enumeration(1) {
    type ImportItemChoice = Value
    val Skip, SkipProcessChildren, SkipDueToParent, Proceed = Value
  }

  object TriggerServiceType extends Enumeration(1) {
    type TriggerServiceType = Value
    val GITB, JSON = Value
  }

  object TriggerEventType extends Enumeration(1) {
    type TriggerEventType = Value
    val OrganisationCreated,
    SystemCreated,
    ConformanceStatementCreated,
    OrganisationUpdated,
    SystemUpdated,
    ConformanceStatementUpdated,
    TestSessionSucceeded,
    TestSessionFailed,
    ConformanceStatementSucceeded,
    TestSessionStarted
    = Value
  }

  object TriggerDataType extends Enumeration(1) {
    type TriggerDataType = Value
    val Community,
    Organisation,
    System,
    Specification,
    Actor,
    OrganisationParameter,
    SystemParameter,
    DomainParameter,
    TestSession,
    StatementParameter,
    TestReport = Value
  }

  object InputMappingMatchType extends Enumeration(1) {
    type InputMappingMatchType = Value
    val DEFAULT, TEST_SUITE, TEST_CASE, TEST_SUITE_AND_TEST_CASE = Value
  }

  object TestCaseUploadMatchType extends Enumeration(1) {
    type TestCaseUploadMatchType = Value
    val IN_ARCHIVE_ONLY, IN_DB_ONLY, IN_ARCHIVE_AND_DB = Value
  }

  object ConformanceStatementItemType extends Enumeration(1) {
    type ConformanceStatementItemType = Value
    val DOMAIN, SPECIFICATION_GROUP, SPECIFICATION, ACTOR = Value
  }

  object OverviewLevelType extends Enumeration {
    type OverviewLevelType = Value
    val OrganisationLevel:OverviewLevelType = Value(1, "all")
    val DomainLevel:OverviewLevelType = Value(2, "domain")
    val SpecificationGroupLevel:OverviewLevelType = Value(3, "group")
    val SpecificationLevel:OverviewLevelType = Value(4, "specification")
  }

  object ReportType extends Enumeration(1) {
    type ReportType = Value
    val ConformanceStatementReport, ConformanceOverviewReport, TestCaseReport, TestStepReport, ConformanceStatementCertificate, ConformanceOverviewCertificate = Value
  }

  object TriggerFireExpressionType extends Enumeration(1) {
    type TriggerFireExpressionType = Value
    val TestCaseIdentifier, TestSuiteIdentifier, ActorIdentifier, SpecificationName, SystemName, OrganisationName = Value
  }

  object ServiceHealthStatusType extends Enumeration(1) {
    type ServiceHealthStatusType = Value
    val Ok, Warning, Error, Info, Unknown = Value
  }

}
