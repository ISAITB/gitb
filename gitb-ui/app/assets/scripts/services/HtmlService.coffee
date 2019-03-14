class HtmlService

  @$inject = ['$q', '$uibModal']
  constructor: (@$q, @$uibModal) ->

  showHtml: (headerText, html) =>
    modalOptions =
      templateUrl: 'assets/views/components/html-modal.html'
      controller: 'HtmlController'
      controllerAs: 'controller'
      size: 'lg'
      resolve:
        headerText: () => headerText
        html: () => html

    @$uibModal.open(modalOptions).result.finally(angular.noop).then(angular.noop, angular.noop)

services.service('HtmlService', HtmlService)
