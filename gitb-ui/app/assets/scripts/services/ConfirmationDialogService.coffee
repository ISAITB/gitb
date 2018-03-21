class ConfirmationDialogService

  @$inject = ['$q', '$modal']
  constructor: (@$q, @$modal) ->

  confirm: (headerText, bodyText, actionButtonText, closeButtonText) =>
    modalOptions =
      templateUrl: 'assets/views/components/confirmation-modal.html'
      controller: 'ConfirmationDialogController'
      controllerAs: 'controller'
      backdrop: 'static'
      resolve:
        headerText: () => headerText
        bodyText: () => bodyText
        actionButtonText: () => actionButtonText
        closeButtonText: () => closeButtonText

    @$modal.open(modalOptions).result

services.service('ConfirmationDialogService', ConfirmationDialogService)
