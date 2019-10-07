class ConfirmationDialogService

  @$inject = ['$q', '$uibModal']
  constructor: (@$q, @$uibModal) ->
    @sessionNotificationOpen = false

  invalidSessionNotification: () =>
    notify = @$q.defer()
    if !@sessionNotificationOpen
      @sessionNotificationOpen = true
      @notify("Invalid session", "Your current session is invalid. You will now return to the login screen to reconnect.", "Close").then(
        () =>
          @sessionNotificationOpen = false
          notify.resolve()
        , 
        () =>
          @sessionNotificationOpen = false
          notify.resolve()
      )
    else
      notify.reject()
    notify.promise

  notify: (headerText, bodyText, buttonText) =>
    modalOptions =
      templateUrl: 'assets/views/components/confirmation-modal.html'
      controller: 'ConfirmationDialogController'
      controllerAs: 'controller'
      backdrop: 'static'
      keyboard: false
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
      keyboard: false
      resolve:
        headerText: () => headerText
        bodyText: () => bodyText
        actionButtonText: () => actionButtonText
        closeButtonText: () => closeButtonText
        sameStyles: () => sameStyles
        oneButton: () => false

    @$uibModal.open(modalOptions).result

services.service('ConfirmationDialogService', ConfirmationDialogService)
