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

  object SpecificationType extends Enumeration(1) {
    type SpecificationType = Value
    val IntegrationProfile, ContentSpecification = Value
  }

  object Protocol extends Enumeration(1) {
    type Protocol = Value
    val HTTP, UDP, DICOM = Value
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
    val KEEP_TEST_HISTORY = Value(0)
    val DROP_TEST_HISTORY = Value(1)
    val CANCEL = Value(2)
  }

}
