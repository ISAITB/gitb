class MessageService

  @$inject = ['$q', '$modal']
  constructor: (@$q, @$modal) ->

  showMessage: (headerText, bodyText) =>
    modalOptions =
      templateUrl: 'assets/views/components/message-modal.html'
      controller: 'MessageController'
      controllerAs: 'controller'
      resolve:
        headerText: () => headerText
        bodyText: () => bodyText

    @$modal.open(modalOptions)

services.service('MessageService', MessageService)
