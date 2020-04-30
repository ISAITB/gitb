class PopupService

  @$inject = ['$uibModal', 'growl']
  constructor: (@$uibModal, @growl) ->

  show: (headerText, data, fnSuccessCallback) =>
    modalOptions =
      templateUrl: 'assets/views/components/popup-modal.html'
      controller: 'PopupController'
      controllerAs: 'controller'
      size: 'lg'
      resolve:
        headerText: () => headerText
        data: () => data
    @$uibModal.open(modalOptions).result.finally(() =>
      if fnSuccessCallback?
        fnSuccessCallback()
    ).then(angular.noop, angular.noop)

  success: (message) =>
    if message?
      if !message.endsWith(".")
        message = message + "."
      @growl.success(message)

services.service('PopupService', PopupService)
