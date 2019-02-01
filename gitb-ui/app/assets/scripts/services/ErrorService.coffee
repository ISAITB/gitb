# Provides a wrapper service for WebSockets
class ErrorService

  @$inject = ['$q', '$log', '$uibModal', 'Constants', 'ErrorTemplateService', 'AccountService']
  constructor: (@$q, @$log, @$uibModal, @Constants, @ErrorTemplateService, @AccountService) ->
    @$log.debug "Constructing ErrorService"

  showErrorMessage: (error, withRetry) =>
    errorDeferred = @$q.defer()
    if !error?
      error = {}
    if error.data?.error_id?
      # An error ID is assigned only to unexpected errors
      if !error.template?
        @AccountService.getVendorProfile()
        .then (vendor) =>
          if vendor.errorTemplates?
            error.template = vendor.errorTemplates.content
            @modal = @openModal(error, withRetry, errorDeferred)
          else
            communityId = vendor.community
            @ErrorTemplateService.getCommunityDefaultErrorTemplate(communityId)
            .then (data) =>
              if data.exists == true
                error.template = data.content
                @modal = @openModal(error, withRetry, errorDeferred)
              else if communityId != @Constants.DEFAULT_COMMUNITY_ID
                @ErrorTemplateService.getCommunityDefaultErrorTemplate(@Constants.DEFAULT_COMMUNITY_ID)
                .then (data) =>
                  error.template = data.content  if data.exists == true
                  @modal = @openModal(error, withRetry, errorDeferred)
              else
                @modal = @openModal(error, withRetry, errorDeferred)
      else
        @modal = @openModal(error, withRetry, errorDeferred)
    else
      # Expected errors (e.g. validation errors) that have clear error messages
      @modal = @openModal(error, withRetry)
    errorDeferred.promise

  openModal: (error, withRetry, errorDeferred) =>
    if !error.template? || error.template == ''
      if error.data?.error_id?
        error.template = '<p><b>Error message: </b>'+@Constants.PLACEHOLDER__ERROR_DESCRIPTION+'</p>' +
        '<p><b>Error reference: </b>'+@Constants.PLACEHOLDER__ERROR_ID+'</p>'
      else
        error.template = '<p>'+@Constants.PLACEHOLDER__ERROR_DESCRIPTION+'</p>'
    modalOptions =
      templateUrl: 'assets/views/components/error-modal.html'
      controller: 'ErrorController as errorCtrl'
      resolve:
        error: () => error
        withRetry: () => withRetry
    modalInstance = @$uibModal.open modalOptions
    modalInstance.result.then((result) => 
      # Closed
      if errorDeferred?
        errorDeferred.resolve()
    , () => 
      # Dismissed
      if errorDeferred?
        if withRetry? && withRetry
          errorDeferred.reject()
        else
          errorDeferred.resolve()
    )    

services.service('ErrorService', ErrorService)