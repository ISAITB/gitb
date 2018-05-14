# Provides references to and utilities for common objects that can be used accross different controllers.
# The intention is to use those common objects to bind them to different views. So,
# when a controller changes an attribute of an object (not the object itself!), the
# change will be reflected to a different view that is controlled by a different controller.
class DataService
	
	@$inject = ['Constants']
	constructor: (@Constants) ->
		@destroy() #we call destroy inside the constructor to create objects :)

	#should be called after logout, since no user data should be kept any more
	destroy: () ->
		@user = undefined
		@vendor = undefined
		@community = undefined
		@configuration = undefined
		@isSystemAdmin = false
		@isVendorUser = false
		@isCommunityAdmin = false
		@isDomainUser = false

	setUser: (user) ->
		@user = user

		@isVendorAdmin = (@user.role == @Constants.USER_ROLE.VENDOR_ADMIN)
		@isVendorUser  = (@user.role == @Constants.USER_ROLE.VENDOR_USER)
		@isDomainUser  = (@user.role == @Constants.USER_DOMAIN_USER)
		@isSystemAdmin = (@user.role == @Constants.USER_ROLE.SYSTEM_ADMIN)
		@isCommunityAdmin = (@user.role == @Constants.USER_ROLE.COMMUNITY_ADMIN)

	setConfiguration: (config) ->
		@configuration = config

	setVendor: (vendor) ->
		@vendor = vendor

	setCommunity: (community) ->
		@community = community

	setTestsToExecute: (tests) ->
		@tests = tests

services.service('DataService', DataService)
