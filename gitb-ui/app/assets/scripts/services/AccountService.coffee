class AccountService

    @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
    @defaultConfig = { headers: @headers }

    constructor: (@$log, @$http, @$q, @RestService) ->
        @$log.debug "Constructing AccountService..."

    registerVendor: (vendorSname, vendorFname, adminName, adminEmail, adminPassword) ->
        @$log.debug "Registering Vendor..."

        @RestService.post({
            path: jsRoutes.controllers.AccountService.registerVendor().url,
            data: {
                vendor_sname : vendorSname,
                vendor_fname : vendorFname,
                user_name    : adminName
                user_email   : adminEmail
                password     : adminPassword
            }
        })

    updateVendorProfile: (vendorFname, vendorSname) ->
        @$log.debug "Updating Vendor profile..."

        data = {}
        if vendorFname?
            data.vendor_fname = vendorFname
        if vendorSname?
            data.vendor_sname = vendorSname

        @RestService.post({
            path: jsRoutes.controllers.AccountService.updateVendorProfile().url,
            data: data
            authenticate: true
        })

    getVendorProfile: () ->
        @$log.debug "Getting Vendor profile..."

        @RestService.get({
            path: jsRoutes.controllers.AccountService.getVendorProfile().url,
            authenticate: true
        })

    getVendorUsers: () ->
        @$log.debug "Getting Vendor users..."

        @RestService.get({
            path: jsRoutes.controllers.AccountService.getVendorUsers().url,
            authenticate: true
        })

    registerUser: (userName, userEmail, userPassword) ->
        @$log.debug "Registering User..."

        @RestService.post({
            path: jsRoutes.controllers.AccountService.registerUser().url,
            data: {
                user_name  : userName,
                user_email : userEmail,
                password   : userPassword
            }
            authenticate: true
        })

    updateUserProfile: (name, password, oldPassword) ->
        @$log.debug "Updating User profile..."
        @$log.debug "Name: #{name}, Password: #{password}, Old Password: #{oldPassword}"

        data = {}
        if name? then data.user_name = name
        if password? then data.password = password
        if oldPassword? then data.old_password = oldPassword

        @RestService.post({
            path: jsRoutes.controllers.AccountService.updateUserProfile().url,
            data: data
            authenticate: true
        })

    getUserProfile: () ->
        @$log.debug "Getting User profile..."

        @RestService.get({
            path: jsRoutes.controllers.AccountService.getUserProfile().url,
            authenticate: true
        })

services.service('AccountService', AccountService)
