import { IdLabel } from '../types/id-label'
import { LabelConfig } from '../types/label-config.type'

export class Constants {

    public static TABLE_PAGE_SIZE = 10
    public static DEFAULT_LOGO = '/assets/images/gitb.png'
	public static DEFAULT_COMMUNITY_ID = 0

	public static TOKEN_COOKIE_EXPIRE = 2592000000 // 1 month in millis
	public static ACCESS_TOKEN_COOKIE_KEY = 'tat'
	public static LOGIN_OPTION_COOKIE_KEY = 'LOGIN_OPTION'

	public static TEST_ENGINE_ACTOR = 'Test Engine'
	public static TESTER_ACTOR = 'Operator'

	public static LOCAL_DATA = {
		ORGANISATION: 'organisation',
		COMMUNITY: 'community'
	}
	
	public static EMBEDDING_METHOD = {
		BASE64: 'BASE64',
		STRING: 'STRING',
		URI: 'URI'
	}

	public static FILTER_TYPE = {
		DOMAIN: 'domain',
		SPECIFICATION: 'specification',
		ACTOR: 'actor',
		TEST_SUITE: 'test_suite',
		TEST_CASE: 'test_case',
		COMMUNITY: 'community',
		ORGANISATION: 'organisation',
		SYSTEM: 'system',
		RESULT: 'result',
		TIME: 'time',
		SESSION: 'session',
		ORGANISATION_PROPERTY: 'org_property',
		SYSTEM_PROPERTY: 'sys_property'
	}

    public static USER_ROLE = {
		VENDOR_ADMIN: 1,
		VENDOR_USER : 2,
		DOMAIN_USER : 3,
		SYSTEM_ADMIN: 4,
		COMMUNITY_ADMIN: 5
    }

	public static USER_ROLE_LABEL: {[key: number]: string} = {
		1 : "Administrator",
		2 : "User",
		3 : "Domain user",
		4 : "Test bed administrator",
		5 : "Community administrator"
	}

	public static VENDOR_USER_ROLES: IdLabel[] = [
		{ 	
			id: Constants.USER_ROLE.VENDOR_ADMIN,
			label: Constants.USER_ROLE_LABEL[Constants.USER_ROLE.VENDOR_ADMIN]
		},
		{
			id: Constants.USER_ROLE.VENDOR_USER,
			label: Constants.USER_ROLE_LABEL[Constants.USER_ROLE.VENDOR_USER]
		}
	]

	public static LABEL_TYPE = {
		DOMAIN: 1,
		SPECIFICATION: 2,
		ACTOR: 3,
		ENDPOINT: 4,
		ORGANISATION: 5,
		SYSTEM: 6
    }

	public static LABEL_TYPE_LABEL: {[key: number]:string} = {
		1: "Domain",
		2: "Specification",
		3: "Actor",
		4: "Endpoint",
		5: "Organisation",
        6: "System"
    }

	public static LABEL_DEFAULT: {[key: number]: LabelConfig} = {
		1: {singularForm: "Domain", pluralForm: "Domains", fixedCase: false},
		2: {singularForm: "Specification", pluralForm: "Specifications", fixedCase: false},
		3: {singularForm: "Actor", pluralForm: "Actors", fixedCase: false},
		4: {singularForm: "Endpoint", pluralForm: "Endpoints", fixedCase: false},
		5: {singularForm: "Organisation", pluralForm: "Organisations", fixedCase: false},
		6: {singularForm: "System", pluralForm: "Systems", fixedCase: false},
	}

	public static SELF_REGISTRATION_TYPE = {
		NOT_SUPPORTED: 1,
		PUBLIC_LISTING: 2,
		PUBLIC_LISTING_WITH_TOKEN: 3,
		TOKEN: 4
	}

	public static SELF_REGISTRATION_RESTRICTION = {
		NO_RESTRICTION: 1,
		USER_EMAIL: 2,
		USER_EMAIL_DOMAIN: 3
	}

	public static LOGIN_OPTION = {
		NONE: "none",
		REGISTER: "register",
		DEMO: "demo",
		MIGRATE: "migrate",
		LINK_ACCOUNT: "link",
		FORCE_CHOICE: "force"
	}

	public static PLACEHOLDER__ERROR_DESCRIPTION = "$ERROR_DESCRIPTION"
	public static PLACEHOLDER__ERROR_ID = "$ERROR_ID"
	public static PLACEHOLDER__ORGANISATION = "$ORGANISATION"
	public static PLACEHOLDER__SYSTEM = "$SYSTEM"
	public static PLACEHOLDER__SPECIFICATION = "$SPECIFICATION"
	public static PLACEHOLDER__ACTOR = "$ACTOR"
	public static PLACEHOLDER__DOMAIN = "$DOMAIN"

	public static TEST_STATUS = {
		UNKNOWN: null,
		PROCESSING : 0,
		SKIPPED : 1,
		WAITING : 2,
		ERROR : 3,
		WARNING: 4,
		COMPLETED: 5
	}

	public static TEST_CASE_STATUS = {
		READY: 0,
		PROCESSING: 1,
		PENDING : 2,
		ERROR : 3,
		COMPLETED: 4,
		STOPPED: 5
	}

	public static TEST_CASE_RESULT = {
		SUCCESS : "SUCCESS",
		FAILURE : "FAILURE",
		UNDEFINED : "UNDEFINED"
	}

	public static STATUS = {
		PENDING: 0,
		FINISHED: 1
	}

	public static REPORT_OPTION_CHOICE = {
		CERTIFICATE: 1,
		REPORT: 2,
		DETAILED_REPORT: 3
	}

	public static CREATE_ACCOUNT_OPTION = {
		LINK: 1,
		SELF_REGISTER: 2,
		MIGRATE: 3
	}

	public static DISCONNECT_ROLE_OPTION = {
		CURRENT_PARTIAL: 1,
		CURRENT_FULL: 2,
		ALL: 3
	}

	public static ERROR_CODES = {
		INVALID_CREDENTIALS: 104
	}

	public static TRIGGER_EVENT_TYPE = {
		ORGANISATION_CREATED: 1,
		SYSTEM_CREATED: 2,
		CONFORMANCE_STATEMENT_CREATED: 3,
		ORGANISATION_UPDATED: 4,
		SYSTEM_UPDATED: 5,
		CONFORMANCE_STATEMENT_UPDATED: 6,
		TEST_SESSION_SUCCEEDED: 7,
		TEST_SESSION_FAILED: 8,
		CONFORMANCE_STATEMENT_SUCCEEDED: 9
	}

	public static TRIGGER_DATA_TYPE = {
		COMMUNITY: 1,
		ORGANISATION: 2,
		SYSTEM: 3,
		SPECIFICATION: 4,
		ACTOR: 5,
		ORGANISATION_PARAMETER: 6,
		SYSTEM_PARAMETER: 7,
		DOMAIN_PARAMETER: 8
	}

	public static IMPORT_ITEM_TYPE = {
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
		STATEMENT_CONFIGURATION: 22,
		TRIGGER: 23
	}

	public static IMPORT_ITEM_MATCH = {
		ARCHIVE_ONLY: 1,
		BOTH: 2,
		DB_ONLY: 3
	}

	public static IMPORT_ITEM_CHOICE = {
		SKIP: 1,
		SKIP_PROCESS_CHILDREN: 2,
		SKIP_DUE_TO_PARENT: 3,
		PROCEED: 4
	}

	public static OPERATION = {
		UPDATE: 1,
		ADD: 2,
		DELETE: 3
	}

	public static WEB_SOCKET_COMMAND = {
		REGISTER: "register",
		NOTIFY: "notify",
		PING: "ping"
	}

	public static PASSWORD_REGEX = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()_–\[{}\]:;'",?/\\*~$^+=<>]).{8,}$/
	public static END_OF_TEST_STEP = "-1"
	public static LOG_EVENT_TEST_STEP = "-999"	
	public static EMAIL_REGEX = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/
	public static DATA_URL_REGEX = /^data:.+\/(.+);base64,(.*)$/
	public static VARIABLE_NAME_REGEX = /^[a-zA-Z][a-zA-Z\-_\.0-9]*$/

	public static VERSION = "v1.13.0"

}
