# Provides a wrapper service for WebSockets
class ErrorService

  @$inject = ['$log', '$modal', 'Constants', 'ErrorTemplateService', 'AccountService']
  constructor: (@$log, @$modal, @Constants, @ErrorTemplateService, @AccountService) ->
    @$log.debug "Constructing ErrorService"

  showErrorMessage: (error, withRetry) =>
    if !error?
      error = {}
    if error.data?.error_id?
      # An error ID is assigned only to unexpected errors
      if !error.template?
        @AccountService.getVendorProfile()
        .then (vendor) =>
          if vendor.errorTemplates?
            error.template = vendor.errorTemplates.content
            @openModal(error, withRetry)
          else
            communityId = vendor.community
            @ErrorTemplateService.getCommunityDefaultErrorTemplate(communityId)
            .then (data) =>
              if data.exists == true
                error.template = data.content
                @openModal(error, withRetry)
              else if communityId != @Constants.DEFAULT_COMMUNITY_ID
                @ErrorTemplateService.getCommunityDefaultErrorTemplate(@Constants.DEFAULT_COMMUNITY_ID)
                .then (data) =>
                  error.template = data.content  if data.exists == true
                  @openModal(error, withRetry)
              else
                @openModal(error, withRetry)
      else
        @openModal(error, withRetry)
    else
      # Expected errors (e.g. validation errors) that have clear error messages
      @openModal(error, withRetry)

  openModal: (error, withRetry) =>
    if !error.template? || error.template == ''
      if error.data?.error_id?
        error.template = '<p><b>Error message: </b>'+@Constants.PLACEHOLDER__ERROR_DESCRIPTION+'</p>' +
        '<p><b>Error reference: </b>'+@Constants.PLACEHOLDER__ERROR_ID+'</p>'
      else
        error.template = '<p><b>Error message: </b>'+@Constants.PLACEHOLDER__ERROR_DESCRIPTION+'</p>'
    modalOptions =
      templateUrl: 'assets/views/components/error-modal.html'
      controller: 'ErrorController as errorCtrl'
      resolve:
        error: () => error
        withRetry: () => withRetry
    @$modal.open modalOptions

services.service('ErrorService', ErrorService)