class AccountService

    @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
    @defaultConfig = { headers: @headers }

    @$inject = ['$log', '$http', '$q', 'RestService']
    constructor: (@$log, @$http, @$q, @RestService) ->
        @$log.debug "Constructing AccountService..."

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

    getConfiguration: () ->
        @RestService.get({
            path: jsRoutes.controllers.AccountService.getConfiguration().url,
            authenticate: false
        })

    submitFeedback: (userEmail, messageTypeId, messageTypeDescription, messageContent, messageAttachments) ->
        data = {}
        data["user_email"] = userEmail
        data["msg_type_id"] = messageTypeId
        data["msg_type_description"] = messageTypeDescription
        data["msg_content"] = messageContent
        if (messageAttachments?)
            data["msg_attachments_count"] = messageAttachments.length
            data["msg_attachments"] = messageAttachments
        @RestService.post({
            path: jsRoutes.controllers.AccountService.submitFeedback().url,
            data: data
            authenticate: true
        })

services.service('AccountService', AccountService)
