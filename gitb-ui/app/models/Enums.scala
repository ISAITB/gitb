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
    DefaultLandingPage, DefaultLegalNotice, DefaultErrorTemplate, SystemAdministrator, SystemConfiguration = Value
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

}
