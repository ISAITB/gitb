class MessageService

  @$inject = ['$q', '$uibModal']
  constructor: (@$q, @$uibModal) ->

  showMessage: (headerText, bodyText) =>
    modalOptions =
      templateUrl: 'assets/views/components/message-modal.html'
      controller: 'MessageController'
      controllerAs: 'controller'
      resolve:
        headerText: () => headerText
        bodyText: () => bodyText

    @$uibModal.open(modalOptions).result.finally(angular.noop).then(angular.noop, angular.noop)

services.service('MessageService', MessageService)
