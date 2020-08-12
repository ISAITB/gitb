class Constants

	@TABLE_PAGE_SIZE = 10
	@DEFAULT_LOGO = '/assets/images/gitb.png'

	@TOKEN_COOKIE_EXPIRE: 2592000000 # 1 month in millis
	@ACCESS_TOKEN_COOKIE_KEY : 'tat'
	@LOGIN_OPTION_COOKIE_KEY : 'LOGIN_OPTION'

	@SECONDS_IN_DAY: 86400

	@END_OF_TEST_STEP: "-1"
	@LOG_EVENT_TEST_STEP: "-999"

	@DEFAULT_COMMUNITY_ID = 0

	@LABEL_TYPE = 
		DOMAIN: 1
		SPECIFICATION: 2
		ACTOR: 3
		ENDPOINT: 4
		ORGANISATION: 5
		SYSTEM: 6

	@LABEL_TYPE_LABEL = 
		1: "Domain"
		2: "Specification"
		3: "Actor"
		4: "Endpoint"
		5: "Organisation"
		6: "System"

	@LABEL_DEFAULT = {
		1: {singularForm: "Domain", pluralForm: "Domains", fixedCase: false}
		2: {singularForm: "Specification", pluralForm: "Specifications", fixedCase: false}
		3: {singularForm: "Actor", pluralForm: "Actors", fixedCase: false}
		4: {singularForm: "Endpoint", pluralForm: "Endpoints", fixedCase: false}
		5: {singularForm: "Organisation", pluralForm: "Organisations", fixedCase: false}
		6: {singularForm: "System", pluralForm: "Systems", fixedCase: false}
	}

	@SELF_REGISTRATION_TYPE = 
		NOT_SUPPORTED: 1
		PUBLIC_LISTING: 2
		PUBLIC_LISTING_WITH_TOKEN: 3
		TOKEN: 4

	@SELF_REGISTRATION_RESTRICTION = 
		NO_RESTRICTION: 1
		USER_EMAIL: 2
		USER_EMAIL_DOMAIN: 3

	@TEST_ROLE =
		SUT: "SUT"
		SIMULATED: "SIMULATED"
		MONITOR: "MONITOR"

	@LOGIN_OPTION =
		NONE: "none" 
		REGISTER: "register" 
		DEMO: "demo" 
		MIGRATE: "migrate" 
		LINK_ACCOUNT: "link" 
		FORCE_CHOICE: "force"

	@WEB_SOCKET_COMMAND =
		REGISTER: "register"
		NOTIFY: "notify"
		PING: "ping"

	@SEARCH_STATE_ORIGIN =
		SYSTEM_TESTS: 0,
		DASHBOARD : 1

	@TEST_CASE_TYPE =
		CONFORMANCE: 0,
		INTEROPERABILITY : 1

	@USER_ROLE =
		VENDOR_ADMIN: 1,
		VENDOR_USER : 2,
		DOMAIN_USER : 3,
		SYSTEM_ADMIN: 4,
		COMMUNITY_ADMIN: 5,

	@USER_ROLE_LABEL =
		1 : "Administrator",
		2 : "User",
		3 : "Domain user",
		4 : "Test bed administrator",
		5 : "Community administrator"

	@VENDOR_USER_ROLES = [
		{
			id: @USER_ROLE.VENDOR_ADMIN,
			label: @USER_ROLE_LABEL[@USER_ROLE.VENDOR_ADMIN]
		}
		{
			id: @USER_ROLE.VENDOR_USER,
			label: @USER_ROLE_LABEL[@USER_ROLE.VENDOR_USER]
		}
	]

	@PLACEHOLDER__ERROR_DESCRIPTION = "$ERROR_DESCRIPTION"
	@PLACEHOLDER__ERROR_ID = "$ERROR_ID"
	@PLACEHOLDER__ORGANISATION = "$ORGANISATION"
	@PLACEHOLDER__SYSTEM = "$SYSTEM"
	@PLACEHOLDER__SPECIFICATION = "$SPECIFICATION"
	@PLACEHOLDER__ACTOR = "$ACTOR"
	@PLACEHOLDER__DOMAIN = "$DOMAIN"

	@TEST_STATUS =
		UNKNOWN: null,
		PROCESSING : 0,
		SKIPPED : 1,
		WAITING : 2,
		ERROR : 3,
		WARNING: 4,
		COMPLETED: 5

	@TEST_CASE_STATUS =
		READY : 0,
		PROCESSING : 1,
		PENDING : 2,
		ERROR : 3,
		COMPLETED: 4
		STOPPED: 5

	@TEST_RESULT =
		SUCCESS: 1,
		FAIL: 2,
		NOT_DONE: 3,
		INVALID: 4,
		UNDEFINED: 5

	@OPERATION =
		UPDATE: 1
		ADD: 2
		DELETE: 3

	@REPORT_OPTION_CHOICE = 
		CERTIFICATE: 1
		REPORT: 2
		DETAILED_REPORT: 3

	@CREATE_ACCOUNT_OPTION =
		LINK: 1
		SELF_REGISTER: 2
		MIGRATE: 3

	@DISCONNECT_ROLE_OPTION = 
		CURRENT_PARTIAL: 1
		CURRENT_FULL: 2
		ALL: 3

	@IMPORT_ITEM_TYPE =
		DOMAIN: 1,
		DOMAIN_PARAMETER: 2,
		SPECIFICATION: 3,
		ACTOR: 4,
		ENDPOINT: 5,
		ENDPOINT_PARAMETER: 6,
		TEST_SUITE: 7,
		COMMUNITY: 8,
		ADMINISTRATOR: 9,
		CUSTOM_LABEL: 10,
		ORGANISATION_PROPERTY: 11,
		SYSTEM_PROPERTY: 12,
		LANDING_PAGE: 13,
		LEGAL_NOTICE: 14,
		ERROR_TEMPLATE: 15,
		ORGANISATION: 16,
		ORGANISATION_USER: 17,
		ORGANISATION_PROPERTY_VALUE: 18,
		SYSTEM: 19,
		SYSTEM_PROPERTY_VALUE: 20,
		STATEMENT: 21,
		STATEMENT_CONFIGURATION: 22

	@IMPORT_ITEM_MATCH =
		ARCHIVE_ONLY: 1,
		BOTH: 2,
		DB_ONLY: 3

	@IMPORT_ITEM_CHOICE =
		SKIP: 1,
		SKIP_PROCESS_CHILDREN: 2,
		SKIP_DUE_TO_PARENT: 3,
		PROCEED: 4

	@TEST_CASE_RESULT =
		SUCCESS : "SUCCESS"
		FAILURE : "FAILURE"
		UNDEFINED : "UNDEFINED"

	@EMAIL_REGEX: /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/
	@DATA_URL_REGEX: /^data:.+\/(.+);base64,(.*)$/
	@VARIABLE_NAME_REGEX: /^[a-zA-Z][a-zA-Z\-_\.0-9]*$/

	@VERSION = "v1.10.0"

common.value('Constants', Constants)
