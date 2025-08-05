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

import {IdLabel} from '../types/id-label';
import {LabelConfig} from '../types/label-config.type';

export class Constants {

	public static readonly THEME_CSS_LINK_ID = "themeCssLink"
	public static readonly THEME_FAVICON_LINK_ID = "themeFaviconLink"

  public static readonly TABLE_PAGE_SIZE = 10
  public static readonly DEFAULT_LOGO = '/assets/images/gitb.png'
	public static readonly DEFAULT_COMMUNITY_ID = 0
	public static readonly TOOLTIP_DELAY = 500

	public static readonly TOKEN_COOKIE_EXPIRE = 2592000000 // 1 month in millis
	public static readonly ACCESS_TOKEN_COOKIE_KEY = 'tat'

	public static readonly TEST_ENGINE_ACTOR_ID = 'com.gitb.TestEngine'
	public static readonly TESTER_ACTOR_ID = 'com.gitb.Operator'
	public static readonly ADMINISTRATOR_ACTOR_ID = 'com.gitb.Administrator'
	public static readonly TEST_ENGINE_ACTOR_NAME = 'Test Engine'
	public static readonly TESTER_ACTOR_NAME = 'Operator'
	public static readonly ADMINISTRATOR_ACTOR_NAME = 'Administrator'

	public static readonly LATEST_CONFORMANCE_STATUS_LABEL = 'Latest conformance status'

	public static readonly EMBEDDING_METHOD = {
		BASE64: 'BASE64',
		STRING: 'STRING',
		URI: 'URI'
	}

	public static readonly FILTER_TYPE = {
		DOMAIN: 'domain',
		SPECIFICATION: 'specification',
		ACTOR: 'actor',
		TEST_SUITE: 'test_suite',
		TEST_CASE: 'test_case',
		COMMUNITY: 'community',
		ORGANISATION: 'organisation',
		SYSTEM: 'system',
		RESULT: 'result',
		START_TIME: 'start_time',
		END_TIME: 'end_time',
		SESSION: 'session',
		ORGANISATION_PROPERTY: 'org_property',
		SYSTEM_PROPERTY: 'sys_property',
		SPECIFICATION_GROUP: 'specification_group',
	}

	public static readonly ORDER = {
		ASC: 'asc',
		DESC: 'desc'
	}

    public static readonly USER_ROLE = {
		VENDOR_ADMIN: 1,
		VENDOR_USER : 2,
		DOMAIN_USER : 3,
		SYSTEM_ADMIN: 4,
		COMMUNITY_ADMIN: 5
    }

	public static readonly USER_ROLE_LABEL: {[key: number]: string} = {
		1 : "Administrator",
		2 : "User",
		3 : "Domain user",
		4 : "Test Bed administrator",
		5 : "Community administrator"
	}

	public static readonly VENDOR_USER_ROLES: IdLabel[] = [
		{
			id: Constants.USER_ROLE.VENDOR_ADMIN,
			label: Constants.USER_ROLE_LABEL[Constants.USER_ROLE.VENDOR_ADMIN]
		},
		{
			id: Constants.USER_ROLE.VENDOR_USER,
			label: Constants.USER_ROLE_LABEL[Constants.USER_ROLE.VENDOR_USER]
		}
	]

	public static readonly LABEL_TYPE = {
		DOMAIN: 1,
		SPECIFICATION: 2,
		ACTOR: 3,
		ENDPOINT: 4,
		ORGANISATION: 5,
		SYSTEM: 6,
		SPECIFICATION_IN_GROUP: 7,
		SPECIFICATION_GROUP: 8
    }

	public static readonly LABEL_TYPE_LABEL: {[key: number]:string} = {
		1: "Domain",
		2: "Specification",
		3: "Actor",
		4: "Endpoint",
		5: "Organisation",
		6: "System",
		7: "Specification in group",
		8: "Specification group"
  }

	public static readonly LABEL_DEFAULT: {[key: number]: LabelConfig} = {
		1: {singularForm: "Domain", pluralForm: "Domains", fixedCase: false},
		2: {singularForm: "Specification", pluralForm: "Specifications", fixedCase: false},
		3: {singularForm: "Actor", pluralForm: "Actors", fixedCase: false},
		4: {singularForm: "Endpoint", pluralForm: "Endpoints", fixedCase: false},
		5: {singularForm: "Organisation", pluralForm: "Organisations", fixedCase: false},
		6: {singularForm: "System", pluralForm: "Systems", fixedCase: false},
		7: {singularForm: "Option", pluralForm: "Options", fixedCase: false},
		8: {singularForm: "Specification group", pluralForm: "Specification groups", fixedCase: false}
	}

	public static readonly SELF_REGISTRATION_TYPE = {
		NOT_SUPPORTED: 1,
		PUBLIC_LISTING: 2,
		PUBLIC_LISTING_WITH_TOKEN: 3,
		TOKEN: 4
	}

	public static readonly SELF_REGISTRATION_RESTRICTION = {
		NO_RESTRICTION: 1,
		USER_EMAIL: 2,
		USER_EMAIL_DOMAIN: 3
	}

	public static readonly LOGIN_OPTION = {
		NONE: "none",
		REGISTER: "register",
		DEMO: "demo",
		MIGRATE: "migrate",
		LINK_ACCOUNT: "link",
		FORCE_CHOICE: "force"
	}

	public static readonly PLACEHOLDER__ERROR_DESCRIPTION = "$ERROR_DESCRIPTION"
	public static readonly PLACEHOLDER__ERROR_ID = "$ERROR_ID"
	public static readonly PLACEHOLDER__ORGANISATION = "$ORGANISATION"
	public static readonly PLACEHOLDER__SYSTEM = "$SYSTEM"
	public static readonly PLACEHOLDER__SPECIFICATION = "$SPECIFICATION"
	public static readonly PLACEHOLDER__SPECIFICATION_GROUP = "$SPECIFICATION_GROUP"
	public static readonly PLACEHOLDER__SPECIFICATION_GROUP_OPTION = "$SPECIFICATION_GROUP_OPTION"
	public static readonly PLACEHOLDER__ACTOR = "$ACTOR"
	public static readonly PLACEHOLDER__DOMAIN = "$DOMAIN"
	public static readonly PLACEHOLDER__BADGE = "$BADGE"
	public static readonly PLACEHOLDER__BADGES = "$BADGES"
  public static readonly PLACEHOLDER__LAST_UPDATE_DATE = "$LAST_UPDATE_DATE"
  public static readonly PLACEHOLDER__REPORT_DATE = "$REPORT_DATE"
  public static readonly PLACEHOLDER__SNAPSHOT = "$SNAPSHOT"

	public static readonly TEST_STATUS = {
		UNKNOWN: null,
		PROCESSING : 0,
		SKIPPED : 1,
		WAITING : 2,
		ERROR : 3,
		WARNING: 4,
		COMPLETED: 5
	}

	public static readonly TEST_CASE_STATUS = {
		READY: 0,
		PROCESSING: 1,
		PENDING : 2,
		ERROR : 3,
		COMPLETED: 4,
		STOPPED: 5,
		CONFIGURING: 6
	}

	public static readonly TEST_CASE_RESULT = {
		SUCCESS : "SUCCESS",
		FAILURE : "FAILURE",
		UNDEFINED : "UNDEFINED",
		WARNING : "WARNING"
	}

	public static readonly STATUS = {
		NONE: 0,
		PENDING: 1,
		FINISHED: 2
	}

	public static readonly REPORT_OPTION_CHOICE = {
		CERTIFICATE: 1,
		REPORT: 2,
		DETAILED_REPORT: 3
	}

	public static readonly CREATE_ACCOUNT_OPTION = {
		LINK: 1,
		SELF_REGISTER: 2,
		MIGRATE: 3
	}

	public static readonly DISCONNECT_ROLE_OPTION = {
		CURRENT_PARTIAL: 1,
		CURRENT_FULL: 2,
		ALL: 3
	}

	public static readonly ERROR_CODES = {
		INVALID_CREDENTIALS: 104
	}

	public static readonly TRIGGER_SERVICE_TYPE = {
		GITB: 1,
		JSON: 2
	}

	public static readonly TRIGGER_EVENT_TYPE = {
		ORGANISATION_CREATED: 1,
		SYSTEM_CREATED: 2,
		CONFORMANCE_STATEMENT_CREATED: 3,
		ORGANISATION_UPDATED: 4,
		SYSTEM_UPDATED: 5,
		CONFORMANCE_STATEMENT_UPDATED: 6,
		TEST_SESSION_SUCCEEDED: 7,
		TEST_SESSION_FAILED: 8,
		CONFORMANCE_STATEMENT_SUCCEEDED: 9,
		TEST_SESSION_STARTED: 10,
	}

	public static readonly TRIGGER_DATA_TYPE = {
		COMMUNITY: 1,
		ORGANISATION: 2,
		SYSTEM: 3,
		SPECIFICATION: 4,
		ACTOR: 5,
		ORGANISATION_PARAMETER: 6,
		SYSTEM_PARAMETER: 7,
		DOMAIN_PARAMETER: 8,
		TEST_SESSION: 9,
		STATEMENT_PARAMETER: 10,
		TEST_REPORT: 11
	}

	public static readonly IMPORT_ITEM_TYPE = {
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
		TRIGGER: 23,
		RESOURCE: 24,
		SPECIFICATION_GROUP: 25,
		SYSTEM_SETTINGS: 26,
		THEME: 27,
		DEFAULT_LANDING_PAGE: 28,
		DEFAULT_LEGAL_NOTICE: 29,
		DEFAULT_ERROR_TEMPLATE: 30,
		SYSTEM_ADMINISTRATOR: 31,
		SYSTEM_CONFIGURATION: 32,
    SYSTEM_RESOURCE: 33
	}

	public static readonly IMPORT_ITEM_MATCH = {
		ARCHIVE_ONLY: 1,
		BOTH: 2,
		DB_ONLY: 3
	}

	public static readonly IMPORT_ITEM_CHOICE = {
		SKIP: 1,
		SKIP_PROCESS_CHILDREN: 2,
		SKIP_DUE_TO_PARENT: 3,
		PROCEED: 4
	}

	public static readonly OPERATION = {
		UPDATE: 1,
		ADD: 2,
		DELETE: 3
	}

	public static readonly WEB_SOCKET_COMMAND = {
		REGISTER: "register",
		NOTIFY: "notify",
		PING: "ping"
	}

	public static readonly TAB = {
		DOMAIN: {
			SPECIFICATIONS: 0,
			TEST_SUITES: 1,
			PARAMETERS: 2
		},
		SPECIFICATION: {
			TEST_SUITES: 0,
			ACTORS: 1
		}
	}

	public static readonly TEST_CASE_UPLOAD_MATCH = {
		IN_ARCHIVE_ONLY: 1,
		IN_DB_ONLY: 2,
		IN_ARCHIVE_AND_DB : 3
	}

	public static readonly CONFORMANCE_STATEMENT_ITEM_TYPE = {
		DOMAIN: 1,
		SPECIFICATION_GROUP: 2,
		SPECIFICATION: 3,
		ACTOR: 4
	}

	public static readonly NAVIGATION_QUERY_PARAM = {
		TEST_SESSION_ID: 'session',
		SPECIFICATION_GROUP_ID: 'group',
		VIEW_PROPERTIES: 'viewProperties',
		SYSTEM_ID: 'system',
		TEST_CASE_ID: 'tc',
		TEST_SUITE_ID: 'ts',
		COPY: 'copy',
		COPY_DEFAULT: 'copyDefault',
		SNAPSHOT_ID: 'snapshot'
	}

	public static readonly NAVIGATION_PATH_PARAM = {
		DOMAIN_ID: 'domain_id',
		SPECIFICATION_GROUP_ID: 'group_id',
		SPECIFICATION_ID: 'spec_id',
		ACTOR_ID: 'actor_id',
		ENDPOINT_ID: 'endpoint_id',
		TEST_SUITE_ID: 'testsuite_id',
		TEST_CASE_ID: 'testcase_id',
		COMMUNITY_ID: 'community_id',
		ORGANISATION_ID: 'org_id',
		SYSTEM_ID: 'sys_id',
		USER_ID: 'user_id',
		LANDING_PAGE_ID: 'page_id',
		LEGAL_NOTICE_ID: 'notice_id',
		ERROR_TEMPLATE_ID: 'template_id',
		TRIGGER_ID: 'trigger_id',
		THEME_ID: 'theme_id',
		SNAPSHOT_ID: 'snapshot_id',
		SNAPSHOT_LABEL: 'snapshot_label',
		TAB: 'snapshot_id'
	}

  public static readonly NAVIGATION_DATA = {
    IMPLICIT_COMMUNITY_ID: 'implicitCommunityId'
  }

	public static readonly SYSTEM_CONFIG = {
		SESSION_ALIVE_TIME: 'session_alive_time',
		REST_API_ENABLED: 'rest_api_enabled',
		REST_API_ADMIN_KEY: 'rest_api_admin_key',
		SELF_REGISTRATION_ENABLED: 'self_registration_enabled',
		DEMO_ACCOUNT: 'demo_account',
		WELCOME_MESSAGE: 'welcome',
		ACCOUNT_RETENTION_PERIOD: 'account_retention_period',
		EMAIL_SETTINGS: 'email_settings',
	}

	public static readonly USER_SSO_STATUS = {
		NOT_MIGRATED: 1,
		NOT_LINKED: 2,
		LINKED: 3
	}

	public static readonly REPORT_TYPE = {
		CONFORMANCE_STATEMENT_REPORT: 1,
		CONFORMANCE_OVERVIEW_REPORT: 2,
		TEST_CASE_REPORT: 3,
		TEST_STEP_REPORT: 4,
		CONFORMANCE_STATEMENT_CERTIFICATE: 5,
		CONFORMANCE_OVERVIEW_CERTIFICATE: 6
	}

  public static readonly TRIGGER_FIRE_EXPRESSION_TYPE = {
    TEST_CASE_IDENTIFIER: 1,
    TEST_SUITE_IDENTIFIER: 2,
    ACTOR_IDENTIFIER: 3,
    SPECIFICATION_NAME: 4,
    SYSTEM_NAME: 5,
    ORGANISATION_NAME: 6
  }

	public static readonly FILTER_COMMAND = {
		TOGGLE: 1,
		CLEAR: 2,
		REFRESH: 3,
    HIDE_PENDING_INDICATOR: 4,
    CLEAR_WITHOUT_RELOAD: 5,
	}

  public static readonly TEST_FILTER = {
    SUCCEEDED: '0',
    FAILED: '1',
    INCOMPLETE: '2',
    OPTIONAL: '3',
    DISABLED: '4'
  }

	public static readonly PASSWORD_REGEX = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[\!\@\#\&\(\)\[\{\}\]\:\;\'\"\,\?\/\\\*\~\$\^\+\=\<\>\_\-]).{8,}$/
	public static readonly END_OF_TEST_STEP = "-1"
	public static readonly END_OF_TEST_STEP_EXTERNAL = "-2"
	public static readonly LOG_EVENT_TEST_STEP = "-999"
	public static readonly EMAIL_REGEX = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/
	public static readonly DATA_URL_REGEX = /^data:.+\/(.+);base64,(.*)$/
	public static readonly VARIABLE_NAME_REGEX = /^[a-zA-Z][a-zA-Z\-_\.0-9]*$/
	public static readonly LOG_LEVEL_REGEX = /^\[\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\] (DEBUG|ERROR|WARN|INFO) /
	public static readonly BADGE_PLACEHOLDER_REGEX = /(\$com\.gitb\.placeholder\.BadgeUrl\{[A-Z]+\|\-?\d+\|\-?\d+\|\-?\d+\|\-?\d+\})/g


}
