class WebEditorService

  constructor: () ->

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
      toolbar: 'undo redo | insert | styleselect | bold italic | charmap | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image'
      init_instance_callback: () -> tinymce.activeEditor.setContent(initialContent)
      content_css: 'assets/stylesheets/css/tinymce/tinymce.css'

  editorMinimal: (height, initialContent) =>
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
      toolbar: 'bold italic | charmap | bullist numlist outdent indent | link'
      init_instance_callback: () -> tinymce.activeEditor.setContent(initialContent)
      content_css: 'assets/stylesheets/css/tinymce/tinymce.css'

services.service('WebEditorService', WebEditorService)