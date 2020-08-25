class EditorModalController

  @$inject = ['$scope', '$log', '$uibModalInstance', 'name', 'editorOptions', 'indicators', 'lineNumber']

  constructor: (@$scope, @$log, @$uibModalInstance, documentName, editorOptions, indicators, lineNumber) ->
    @$log.debug "Constructing EditorModalController"

    @$scope.isNameVisible = documentName?
    @$scope.documentName = documentName
    @$scope.editorOptions = editorOptions
    @$scope.indicators = indicators
    @$scope.lineNumber = lineNumber
    @$scope.isDownloadVisible = editorOptions.download? && editorOptions.value?
    if editorOptions.copy == undefined
      @$scope.isCopyVisible = true 
    else 
      @$scope.isCopyVisible = editorOptions.copy

    @$scope.close = () =>
      @$uibModalInstance.dismiss()

    @$scope.copyToClipboard = () =>
      cm = $('.CodeMirror')[0].CodeMirror
      cm.focus()
      cm.execCommand "selectAll"
      try
        success = document.execCommand 'copy'
      catch err

    @$scope.download = () =>
      bb = new Blob([@$scope.editorOptions.value], {type: @$scope.editorOptions.download.mimeType})
      saveAs(bb, @$scope.editorOptions.download.fileName)

@controllers.controller 'EditorModalController', EditorModalController
