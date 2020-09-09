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

  object TestSuiteReplacementChoiceHistory extends Enumeration {
    type TestSuiteReplacementChoiceHistory = Value
    val KEEP = Value(0)
    val DROP = Value(1)
  }

  object TestSuiteReplacementChoiceMetadata extends Enumeration {
    type TestSuiteReplacementChoiceMetadata = Value
    val SKIP = Value(0)
    val UPDATE = Value(1)
  }

  object TestSuiteReplacementChoice extends Enumeration {
    type TestSuiteReplacementChoice = Value
    val PROCEED = Value(0)
    val CANCEL = Value(1)
  }

  object LabelType extends Enumeration(1) {
    type LabelType = Value
    val Domain, Specification, Actor, Endpoint, Organisation, System = Value
  }

  object ImportItemType extends Enumeration(1) {
    type ImportItemType = Value
    val Domain, DomainParameter, Specification, Actor, Endpoint, EndpointParameter, TestSuite,
    Community, Administrator, CustomLabel, OrganisationProperty, SystemProperty, LandingPage, LegalNotice, ErrorTemplate,
    Organisation, OrganisationUser, OrganisationPropertyValue, System, SystemPropertyValue,
    Statement, StatementConfiguration, Trigger = Value
  }

  object ImportItemMatch extends Enumeration(1) {
    type ImportItemMatch = Value
    val ArchiveOnly, Both, DBOnly = Value
  }

  object ImportItemChoice extends Enumeration(1) {
    type ImportItemChoice = Value
    val Skip, SkipProcessChildren, SkipDueToParent, Proceed = Value
  }

  object TriggerEventType extends Enumeration(1) {
    type TriggerEventType = Value
    val OrganisationCreated,
        SystemCreated,
        ConformanceStatementCreated,
        OrganisationUpdated,
        SystemUpdated,
        ConformanceStatementUpdated
        = Value
  }

  object TriggerDataType extends Enumeration(1) {
    type TriggerDataType = Value
    val Community, Organisation, System, Specification, Actor, OrganisationParameter, SystemParameter = Value
  }

}
