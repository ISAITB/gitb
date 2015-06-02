# Serves as a useful base class for all other classes to inherit from. This
# includes controllers, directives, services, etc. Allows us to specify the
# module name in one place and whatnot :)
#
# Also provides a minimal logging system, to be used in all other classes.
class window.Base

	# @property [String] name of the angular module to attach all objects to
	@appName: "App"

	# @property [Number] log level, between 0 and 4, where 4 is most verbose
	@logLevel: 4

	## Logging. It would have been broken into a seperate class, but it's
	## useful to have methods on each object and whatnot. The logLevel is global,
	## being static on AppBaseClass.
	##
	## Log messages with levels between 1 and 4 respect filtering, whereas log
	## messages of level 0 are always visible
	##
	## Log levels:
	##   0 - Unlabeled and unfiltered
	##   1 - Error
	##   2 - Warning
	##   3 - Debug
	##   4 - Info
	@logLabels: [
		"",
		"[Error]> ",
		"[Warning]> ",
		"[Debug]> ",
		"[Info]> "
	]

	# Main logging method. The level has to be between 1 and 4, and is capped
	# otherwise (following a warning).
	#
	# @param [String] message
	# @param [Number] level optional, between 1 and 4
	log: (message, level) ->
		if level > 4
		  console.warn "Log level greater than 4, capping [#{level}]"
		  level = 4
		else if level < 0
		  console.warn "Log level less than 0, capping [#{level}]"
		  level = 0

		labeledMessage = "#{Base.logLabels[level]}#{message}"

		if level == 0 then console.log message
		else if level == 1
			if console.error then console.error labeledMessage
			else console.log labeledMessage
		else if level == 2
		    if console.warn then console.warn labeledMessage
		    else console.log labeledMessage
		else if level == 3
			if console.debug then console.debug labeledMessage
			else console.log labeledMessage
		else if level == 4
			if console.info then console.info labeledMessage
			else console.log labeledMessage

		message

	# Log an error directly. Alias for log message, 1
	#
	# @param [String] message
	logError: (message) -> @log message, 1

	# Log a warning directly. Alias for log message, 2
	#
	# @param [String] message
	logWarning: (message) -> @log message, 2

	# Log a debug message directly. Alias for log message, 3
	#
	# @param [String] message
	logDebug: (message) -> @log message, 3

	# Log an info message directly. Alias for log message, 4
	#
	# @param [String] message
	logInfo: (message) -> @log message, 4