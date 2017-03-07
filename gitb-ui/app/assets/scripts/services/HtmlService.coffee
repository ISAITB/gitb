class HtmlService

  @$inject = ['$q', '$modal']
  constructor: (@$q, @$modal) ->

  showHtml: (headerText, html) =>
    modalOptions =
      templateUrl: 'assets/views/components/html-modal.html'
      controller: 'HtmlController'
      controllerAs: 'controller'
      size: 'lg'
      resolve:
        headerText: () => headerText
        html: () => html

    @$modal.open(modalOptions)

services.service('HtmlService', HtmlService)
