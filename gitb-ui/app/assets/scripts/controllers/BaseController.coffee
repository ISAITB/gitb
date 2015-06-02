class ControllerUtils
	register: (module, instance) ->
		module.controller instance.name, instance

@ControllerUtils = new ControllerUtils()
