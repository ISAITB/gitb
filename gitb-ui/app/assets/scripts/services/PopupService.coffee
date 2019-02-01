class PopupService

  @$inject = ['$q', '$uibModal']
  constructor: (@$q, @$uibModal) ->

  show: (headerText, data) =>
    modalOptions =
      templateUrl: 'assets/views/components/popup-modal.html'
      controller: 'PopupController'
      controllerAs: 'controller'
      size: 'lg'
      resolve:
        headerText: () => headerText
        data: () => data

    @$uibModal.open(modalOptions)

services.service('PopupService', PopupService)
