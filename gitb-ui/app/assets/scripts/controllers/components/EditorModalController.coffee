class EditorModalController

  @$inject = ['$scope', '$log', '$modalInstance', 'name', 'editorOptions', 'indicators', 'lineNumber']

  constructor: (@$scope, @$log, @$modalInstance, documentName, editorOptions, indicators, lineNumber) ->
    @$log.debug "Constructing EditorModalController"

    @$scope.isNameVisible = documentName?
    @$scope.documentName = documentName
    @$scope.editorOptions = editorOptions
    @$scope.indicators = indicators
    @$scope.lineNumber = lineNumber

    @$scope.close = () =>
      @$modalInstance.dismiss()

    @$scope.copyToClipboard = () =>
      cm = $('.CodeMirror')[0].CodeMirror
      cm.focus()
      cm.execCommand "selectAll"
      try
        success = document.execCommand 'copy'
      catch err


@controllers.controller 'EditorModalController', EditorModalController
