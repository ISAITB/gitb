class WebEditorService

  @$inject = ['$q']
  constructor: (@$q) ->

  editor: (height, initialContent) =>
    tinymce.init
      mode: 'specific_textareas'
      editor_selector: 'mce-editor'
      height: height
      menubar: false
      plugins: [
        'advlist autolink lists link image charmap print preview anchor',
        'searchreplace visualblocks code fullscreen',
        'insertdatetime media table contextmenu paste code'
      ]
      toolbar: 'undo redo | insert | styleselect | bold italic | charmap | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image | table tabledelete | tableprops tablerowprops tablecellprops | tableinsertrowbefore tableinsertrowafter tabledeleterow | tableinsertcolbefore tableinsertcolafter tabledeletecol'
      init_instance_callback: () -> tinymce.activeEditor.setContent(initialContent)
      content_css: 'assets/stylesheets/css/tinymce/tinymce.css'
      branding: false

  editorMinimal: (height, initialContent, className) =>
    if !className?
      className = 'mce-editor'
    tinymce.init
      mode: 'specific_textareas'
      editor_selector: className
      height: height
      menubar: false
      plugins: [
        'advlist autolink lists link image charmap print preview anchor',
        'searchreplace visualblocks code fullscreen',
        'insertdatetime media table contextmenu paste code'
      ]
      toolbar: 'bold italic | charmap | bullist numlist outdent indent | link'
      init_instance_callback: () -> tinymce.get(className).setContent(initialContent)
      content_css: 'assets/stylesheets/css/tinymce/tinymce.css'
      branding: false

  editorForPdfInput: (height, initialContent, className) =>
    tinymce.init
      mode: 'specific_textareas'
      editor_selector: className
      height: height
      menubar: false
      plugins: [
        'autolink lists link image charmap anchor',
        'visualblocks code fullscreen',
        'contextmenu paste code'
      ]
      toolbar: 'bold italic | charmap | bullist numlist | link'
      init_instance_callback: () -> tinymce.activeEditor.setContent(initialContent)
      content_css: 'assets/stylesheets/css/tinymce/tinymce.css'
      branding: false

services.service('WebEditorService', WebEditorService)