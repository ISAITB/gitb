package controllers.util

object Parameters {

  val USER_ID = "GITB-USER-ID"
  val ID      = "id"

  //Dashboard filter parameters
  val COMMUNITY_IDS = "community_ids"
  val DOMAIN_IDS = "domain_ids"
  val SPEC_IDS = "specification_ids"
  val TEST_SUITE_IDS = "test_suite_ids"
  val TEST_CASE_IDS = "test_case_ids"
  val ORG_IDS = "organization_ids"
  val SYSTEM_IDS = "system_ids"
  val RESULTS = "results"
  val START_TIME_BEGIN = "start_time_begin"
  val START_TIME_END = "start_time_end"
  val END_TIME_BEGIN = "end_time_begin"
  val END_TIME_END = "end_time_end"

  //Dashboard sorting
  val SORT_COLUMN = "sort_column"
  val SORT_ORDER = "sort_order"

  //Some common parameters
  val USERNAME = "username"
  val PASSWORD = "password"
  val NAME     = "name"
  val EMAIL    = "email"
	val IDS      = "ids"
  val TYPE     = "type"
  val DESCRIPTION = "description"
  val USE = "use"
  val KIND = "kind"
  val TEST_KEY = "test_key"

  // Landing page Service
  val DEFAULT = "default_flag"
  val CONTENT = "content"
  val LANDING_PAGE_ID = "landing_page_id"
  val LEGAL_NOTICE_ID = "legal_notice_id"
  val ERROR_TEMPLATE_ID = "error_template_id"

  //Authentication Service and OAuth 2.0 parameters
  val GRANT_TYPE = "grant_type"
  val GRANT_TYPE_PASSWORD = "password"
  val REFRESH_TOKEN = "refresh_token"

  //System configuration parameters
  val PARAMETER = "parameter"
  val PARAMETER_ID = "parameter_id"

  //Account Service parameters
  val VENDOR_SNAME = "vendor_sname"
  val VENDOR_FNAME = "vendor_fname"
  val USER_NAME    = "user_name"
  val USER_EMAIL   = "user_email"
  val OLD_PASSWORD = "old_password"
  val USER_ROLE    = "user_role"
  val ROLE_ID      = "role_id"
  val ORGANIZATION_ID = "organization_id"
  val OTHER_ORGANISATION = "other_organisation"
  val TEMPLATE = "template"
  val TEMPLATE_ID = "template_id"
  val TEMPLATE_NAME = "template_name"

  //System Service parameters
  val SYSTEM_ID    = "system_id"
  val SYSTEM_SNAME = "system_sname"
  val SYSTEM_FNAME = "system_fname"
  val SYSTEM_DESC  = "system_description"
  val SYSTEM_VERSION = "system_version"
  val OTHER_SYSTEM = "other_system"

  val DATA = "data"
  val IS_BASE64 = "is_base64"

  //Conformance Service parameters
  val ACTORS  = "actors"
  val ACTOR   = "actor"
  val SPEC    = "spec"
  val OPTION  = "option"
  val OPTIONS = "options"
	val SHORT_NAME = "sname"
	val FULL_NAME = "fname"
	val DESC  = "description"
	val URLS = "urls"
	val DIAGRAM = "diagram"
	val SPEC_TYPE = "spec_type"
	val DOMAIN_ID = "domain_id"
	val SPECIFICATION_ID = "spec_id"
  val PENDING_TEST_SUITE_ID = "pending_id"
  val PENDING_TEST_SUITE_ACTION = "pending_action"
  val FULL = "full"
  val TESTS = "tests"

  //Community Service parameters
  val COMMUNITY_ID = "community_id"
  val COMMUNITY_SNAME = "community_sname"
  val COMMUNITY_FNAME = "community_fname"
  val COMMUNITY_EMAIL = "community_email"
  val COMMUNITY_SELFREG_TYPE = "community_selfreg_type"
  val COMMUNITY_SELFREG_TOKEN = "community_selfreg_token"

  //TestService parameters
  val ACTOR_ID   = "actor_id"
  val ACTOR_DEFAULT   = "default"
  val DISPLAY_ORDER   = "displayOrder"
  val ACTOR_IDS   = "actor_ids"
  val ENDPOINT_ID   = "endpoint_id"
  val TEST_ID    = "test_id"
  val INCLUDE_CONTEXT = "include_context"
  val SESSION_ID = "session_id"
  val TEST_IDS   = "test_ids"
  val SESSION_IDS= "session_ids"
  val CONFIGS    = "configs"
  val CONFIG     = "config"
  val INPUTS     = "inputs"
  val TEST_STEP  = "teststep"
  val SETTINGS   = "settings"
  val UPDATE_PASSWORDS  = "updatePasswords"
  val REMOVE_KEYSTORE   = "removeKeystore"
  val INCLUDE_KEYSTORE_DATA  = "keystore"
  val ADMIN_ONLY  = "admin_only"
  val NOT_FOR_TESTS  = "not_for_tests"

  val FILE       = "file"
	val LIMIT      = "limit"
	val PAGE       = "page"

  val MESSAGE_TYPE_ID = "msg_type_id"
  val MESSAGE_TYPE_DESCRIPTION = "msg_type_description"
  val MESSAGE_CONTENT = "msg_content"
}
