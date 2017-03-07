class PopupService

  @$inject = ['$q', '$modal']
  constructor: (@$q, @$modal) ->

  show: (headerText, data) =>
    modalOptions =
      templateUrl: 'assets/views/components/popup-modal.html'
      controller: 'PopupController'
      controllerAs: 'controller'
      size: 'lg'
      resolve:
        headerText: () => headerText
        data: () => data

    @$modal.open(modalOptions)

services.service('PopupService', PopupService)
