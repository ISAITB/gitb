class ConfirmationDialogService

  @$inject = ['$q', '$modal']
  constructor: (@$q, @$modal) ->

  notify: (headerText, bodyText, buttonText) =>
    modalOptions =
      templateUrl: 'assets/views/components/confirmation-modal.html'
      controller: 'ConfirmationDialogController'
      controllerAs: 'controller'
      backdrop: 'static'
      resolve:
        headerText: () => headerText
        bodyText: () => bodyText
        actionButtonText: () => buttonText
        closeButtonText: () => ''
        sameStyles: () => true
        oneButton: () => true

    @$modal.open(modalOptions).result

  confirm: (headerText, bodyText, actionButtonText, closeButtonText, sameStyles) =>
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
        sameStyles: () => sameStyles
        oneButton: () => false

    @$modal.open(modalOptions).result

services.service('ConfirmationDialogService', ConfirmationDialogService)
