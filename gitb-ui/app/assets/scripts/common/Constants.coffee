class Constants

	@TABLE_PAGE_SIZE = 10
	@DEFAULT_LOGO = '/assets/images/gitb.png'

	@TOKEN_COOKIE_EXPIRE: 180 # 6 months
	@ACCESS_TOKEN_COOKIE_KEY : 'tat'
	@REFRESH_TOKEN_COOKIE_KEY: 'trt'

	@SECONDS_IN_DAY: 86400

	@END_OF_TEST_STEP: "-1"

	@TEST_ROLE =
	    SUT: "SUT"
	    SIMULATED: "SIMULATED"
	    MONITOR: "MONITOR"

	@WEB_SOCKET_COMMAND =
	    REGISTER: "register"
	    NOTIFY: "notify"

	@TEST_CASE_TYPE =
		CONFORMANCE: 0,
		INTEROPERABILITY : 1

	@USER_ROLE =
		VENDOR_ADMIN: 1,
		VENDOR_USER : 2,
		DOMAIN_USER : 3,
		SYSTEM_ADMIN: 4

	@USER_ROLE_LABEL =
		1 : "Administrator",
		2 : "User",
		3 : "Domain user",
		4 : "Test bed administrator"

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

	@TEST_STATUS =
		UNKNOWN: null,
		PROCESSING : 0,
		SKIPPED : 1,
		WAITING : 2,
		ERROR : 3,
		COMPLETED: 4

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

	@TEST_CASE_RESULT =
		SUCCESS : "SUCCESS"
		FAILURE : "FAILURE"
		UNDEFINED : "UNDEFINED"

	@EMAIL_REGEX: /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/
	@DATA_URL_REGEX: /^data:.+\/(.+);base64,(.*)$/

common.value('Constants', Constants)
