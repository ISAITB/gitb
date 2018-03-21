class SystemsTitleController

	@$inject = ['$log', '$window', 'DataService']
	constructor:(@$log, @$window, @DataService) ->

		@organization = JSON.parse(@$window.localStorage['organization'])

controllers.controller('SystemsTitleController', SystemsTitleController)