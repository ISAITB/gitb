package controllers.util

object Parameters {

  val USER_ID = "GITB-USER-ID"
  val ID      = "id"

  //Some common parameters
  val USERNAME = "username"
  val PASSWORD = "password"
  val NAME     = "name"
  val EMAIL    = "email"
	val IDS      = "ids"
  val TYPE     = "type"

  //Authentication Service and OAuth 2.0 parameters
  val GRANT_TYPE = "grant_type"
  val GRANT_TYPE_PASSWORD = "password"
  val REFRESH_TOKEN = "refresh_token"

  //Account Service parameters
  val VENDOR_SNAME = "vendor_sname"
  val VENDOR_FNAME = "vendor_fname"
  val USER_NAME    = "user_name"
  val USER_EMAIL   = "user_email"
  val OLD_PASSWORD = "old_password"

  //System Service parameters
  val SYSTEM_ID    = "system_id"
  val SYSTEM_SNAME = "system_sname"
  val SYSTEM_FNAME = "system_fname"
  val SYSTEM_DESC  = "system_description"
  val SYSTEM_VERSION = "system_version"

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

  //TestService parameters
  val ACTOR_ID   = "actor_id"
  val TEST_ID    = "test_id"
  val SESSION_ID = "session_id"
  val CONFIGS    = "configs"
  val CONFIG     = "config"
  val INPUTS     = "inputs"
  val TEST_STEP  = "teststep"

  val FILE       = "file"
	val LIMIT      = "limit"
	val PAGE       = "page"
}
