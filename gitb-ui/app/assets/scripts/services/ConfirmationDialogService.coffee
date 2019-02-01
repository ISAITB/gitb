class ConfirmationDialogService

  @$inject = ['$q', '$uibModal']
  constructor: (@$q, @$uibModal) ->

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

    @$uibModal.open(modalOptions).result

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

    @$uibModal.open(modalOptions).result

services.service('ConfirmationDialogService', ConfirmationDialogService)
