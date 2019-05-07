# Provides a wrapper service for WebSockets
class ErrorService

  @$inject = ['$q', '$log', '$uibModal', 'Constants', 'ErrorTemplateService', 'AccountService']
  constructor: (@$q, @$log, @$uibModal, @Constants, @ErrorTemplateService, @AccountService) ->
    @$log.debug "Constructing ErrorService"

  showErrorMessage: (error, withRetry) =>
    errorDeferred = @$q.defer()
    if !error?
      errorObj = {}
    else
      if (typeof error == 'string' || error instanceof String)
        errorObj = {}
        errorObj.data = {}
        errorObj.data.error_description = error
      else
        errorObj = error
    if errorObj.data?.error_id?
      # An error ID is assigned only to unexpected errors
      if !errorObj.template?
        @AccountService.getVendorProfile()
        .then (vendor) =>
          if vendor.errorTemplates?
            errorObj.template = vendor.errorTemplates.content
            @modal = @openModal(errorObj, withRetry, errorDeferred)
          else
            communityId = vendor.community
            @ErrorTemplateService.getCommunityDefaultErrorTemplate(communityId)
            .then (data) =>
              if data.exists == true
                errorObj.template = data.content
              @modal = @openModal(errorObj, withRetry, errorDeferred)
      else
        @modal = @openModal(errorObj, withRetry, errorDeferred)
    else
      # Expected errors (e.g. validation errors) that have clear error messages
      @modal = @openModal(errorObj, withRetry, errorDeferred)
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
    modalInstance = @$uibModal.open(modalOptions)
    modalInstance.result
      .finally(angular.noop)
      .then(() => 
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