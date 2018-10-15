@directives.directive 'testStepReport', ['$log', 'RecursionHelper',
  ($log, RecursionHelper) ->
    $log.debug 'Constructing test-step-report with the report'
    STEP_TYPES =
      MESSAGING: "msg"
      DECISION: "decision"
      LOOP: "loop"
      FLOW: "flow"
      VERIFY: "verify"
      EXIT: "exit"
      INTERACT: "interact"

    directive =
      scope:
        step: '='
        report: '='
      template: ''+
        '<div class="row test-step-report {{step.type}}-type">'+
          '<div class="col-md-6">'+
            #'<span class="report-item"><strong>Type: </strong>{{report.type}}</span>'+
            '<span class="report-item"><strong>Result: </strong>{{report.result}}</span>'+
          '</div>'+
          '<div class="col-md-6">'+
            '<span class="report-item"><strong>Time: </strong>{{report.date}}</span>'+
          '</div>'+
          '<div ng-if="report.type == types.SR"'+
            'sr-step-report report="report">'+
          '</div>'+
          '<div ng-if="report.type == types.DR" '+
            'dr-step-report report="report">'+
          '</div>'+
          '<div ng-if="report.type == types.TAR" '+
            'tar-step-report report="report">'+
          '</div>'+
        '</div>'
      restrict: 'A'
      compile: (element) =>
        RecursionHelper.compile element, (scope, element, attrs) ->
          scope.types =
            SR: 'SR'
            DR: 'DR'
            TAR: 'TAR'
    directive
]

@directives.directive 'srStepReport', ['$log',
  ($log) ->
    $log.debug 'Constructing sr-step-report with the report'

    directive =
      scope:
        report: '='
      template: ''+
        '<div class="step-report simple-step-report"></div>'
      restrict: 'A'
      replace: true
      link: (scope, element, attrs) =>
    directive
]

@directives.directive 'drStepReport', ['$log',
  ($log) ->
    $log.debug 'Constructing dr-step-report with the report'

    directive =
      scope:
        report: '='
      template: ''+
        '<div class="step-report decision-step-report">'+
          '<div class="col-md-12">'+
            '<span><strong>Decision: </strong>{{report.decision}}</span>'+
          '</div>'+
        '</div>'
      restrict: 'A'
      replace: true
      link: (scope, element, attrs) =>
    directive
]

extractLocationInfo = (locationStr) ->
  location = null

  LINE_NUMBER_REGEX = /^([\w\.]+):([\-0-9]+):([\-0-9]+)$/
  if LINE_NUMBER_REGEX.test locationStr
    matches = LINE_NUMBER_REGEX.exec locationStr

    location =
      type: "line-column-number"
      name: matches[1]
      line: Number(matches[2])
      column: Number(matches[3])
  else
    # check for xpath expression

  location

extractRelatedIndicators = (name, report) ->
  if !name? || !report? || !report.reports? || !report.reports.assertionReports?
    return []
  relatedAssertionReports = _.filter report.reports.assertionReports, (assertionReport) =>

    location = extractLocationInfo assertionReport.value.location

    return name? && location? && location.name.toLowerCase() == name.toLowerCase()

  indicators = _.map relatedAssertionReports, (assertionReport) ->
    location = extractLocationInfo assertionReport.value.location
    indicator =
      location: location
      type: assertionReport.type
      description: assertionReport.value.description
  return indicators

base64ToString = (base64) ->
  atob base64

openEditorWindow = ($modal, name, value, report, lineNumber) ->
  indicators = extractRelatedIndicators name, report

  modalOptions =
    templateUrl: 'assets/views/components/editor-modal.html'
    controller: 'EditorModalController as editorModalCtrl'
    resolve:
      name: () => name
      editorOptions: () =>
        value: value
        readOnly: true
        lineNumbers: true
        smartIndent: false
        electricChars: false
      indicators: () => indicators
      lineNumber: () => lineNumber
    size: 'lg'

  $modal.open modalOptions

@directives.directive 'tarStepReport', ['$log', 'RecursionHelper',
  ($log, RecursionHelper) ->

    $log.debug 'Constructing tar-step-report with the report'

    directive =
      scope:
        report: '='
      restrict: 'A'
      replace: true
      template: ''+
        '<div class="step-report test-assertion-step-report">'+
          '<div class="col-md-12" ng-if="report.context != null">'+
            #'<span class="title" ng-if="report.id != null"><strong>{{report.id}}:</strong></span>'+
            '<div any-content-view context="report.context" report="report"></div>'+
          '</div>'+
          '<div class="col-md-12 test-assertion-group-report" ng-if="report.reports != null">'+
            '<div class="assertion-reports" ng-if="report.reports.assertionReports != null && report.reports.assertionReports.length > 0">'+
              '<span class="title"><strong>Reports: </strong></span>'+
              '<div>'+
                '<div ng-repeat="assertionReport in report.reports.assertionReports" ng-click="openAssertionReport(assertionReport)" test-assertion-report assertion-report="assertionReport"></div>'+
              '</div>'+
            '</div>'+
            '<div class="sub-test-assertion-step-reports" ng-if="report.reports.reports != null && report.reports.reports.length > 0">'+
              '<span class="title"><strong>Sub Reports: </strong></span>'+
              '<div ng-repeat="subReport in report.reports.reports" ng-click="open(subReport)" tar-step-report report="subReport"></div>'+
            '</div>'+
          '</div>'+
        '</div>'
      compile: (element) =>
        RecursionHelper.compile element, (scope, element, attrs) ->
          scope.openAssertionReport = (assertionReport) =>
            scope.$broadcast 'assertion-report.open', assertionReport # open asssertion report event to be handled by the corresponding content view

    directive
]

@directives.directive 'testAssertionReport', ['$log',
  ($log) ->
    $log.debug 'Constructing test-assertion-report with the report'

    directive =
      scope:
        assertionReport: '='
      restrict: 'A'
      replace: true
      template: ''+
        '<span class="assertion-report" ng-class="{'+
          '\'error-assertion-report\': assertionReport.type == types.ERROR'+
          ', \'warning-assertion-report\': assertionReport.type == types.WARNING'+
          ', \'info-assertion-report\': assertionReport.type == types.INFO}">'+
          '<span class="report-item assertion-report-item" ng-if="assertionReport.value.assertionID != null"><strong>Assertion ID:</strong> {{assertionReport.value.assertionID}}</span>'+
          '<span class="report-item assertion-report-item">'+
            '<i class="fa fa-info-circle report-item-icon info-icon" ng-if="assertionReport.type == types.INFO"></i>'+
            '<i class="fa fa-warning report-item-icon warning-icon" ng-if="assertionReport.type == types.WARNING"></i>'+
            '<i class="fa fa-times-circle report-item-icon error-icon" ng-if="assertionReport.type == types.ERROR"></i>'+
            '<span class="description" ng-bind="description"></span>'+
          '</span>'+
          '<span class="report-item assertion-report-item" ng-if="assertionReport.value.value != null"><strong>Value:</strong> {{assertionReport.value.value}}</span>'+
          '<span class="report-item assertion-report-item" ng-if="assertionReport.value.test != null"><strong>Test:</strong> {{assertionReport.value.test}}</span>'+
        '</span>'
      link: (scope, element, attrs) =>
        htmlDecode = (value) ->
         if !value? then '' else $('<div/>').html(value).text();

        scope.types =
          INFO: 'info'
          WARNING: 'warning'
          ERROR: 'error'

        scope.description = htmlDecode scope.assertionReport.value.description

    directive
]

@directives.directive 'anyContentView', ['$log', '$modal', 'RecursionHelper', 'DataService', 'TestService', 'ErrorService'
($log, $modal, RecursionHelper, DataService, TestService, ErrorService) ->
    $log.debug 'Constructing any-content-view with the context'

    directive =
      scope:
        context: '='
        report: '='
      template: ''+
        '<div class="any-content-view" ng-if="context != null">'+
          '<span ng-if="context.name != null"><strong>{{context.name}}</strong></span>'+
          '<div class="value" ng-if="value != null">'+
            '<span ng-if="!isValueTooLong">{{value}}</span>'+
            '<span ng-if="isValueTooLong">{{value.length}} bytes</span>'+
            '<a href="" class="pull-right clearfix open" ng-click="open()" ng-if="isValueTooLong">'+
              'Open in editor' +
            '</a>'+
            '<a href="" class="pull-right clearfix open" style="padding-right: 20px;" ng-click="download()" ng-if="isValueTooLong">'+
              'Download as file' +
            '</a>'+
          '</div>'+
          '<div class="items">'+
            '<div ng-repeat="item in context.item" any-content-view context="item" report="report"></div>'+
          '</div>'+
        '</div>'
      restrict: 'A'
      compile: (element) ->
        RecursionHelper.compile element, (scope, element, attrs) ->
          scope.contentTypes =
            BASE64: 'BASE64'
            STRING: 'STRING'
            URI: 'URI'

          if !scope.context.value?
            scope.value = null
          else

            switch scope.context.embeddingMethod
              when scope.contentTypes.BASE64
                scope.value = base64ToString scope.context.value
              when scope.contentTypes.STRING
                scope.value = scope.context.value

            scope.isValueTooLong = scope.value.length > 100

            scope.$on 'assertion-report.open', (event, assertionReport) =>
              location = extractLocationInfo assertionReport.value.location
              if location? && location.name? && scope.context.name? && location.name.toLowerCase() == scope.context.name.toLowerCase()
                scope.open location.line

            scope.open = (lineNumber) =>
              openEditorWindow $modal, scope.context.name, scope.value, scope.report, lineNumber
            
            scope.download = () =>
              TestService.getBinaryMetadata(scope.context.value, (scope.context.embeddingMethod == 'BASE64'))
                .then (info) =>
                  if scope.context.embeddingMethod == 'BASE64'
                    bb = DataService.b64toBlob(scope.context.value, info.mimeType)
                  else
                    bb = new Blob([scope.context.value], {type: info.mimeType})
                  saveAs(bb, 'file'+info.extension)
                .catch (error) =>	
                  ErrorService.showErrorMessage(error)
          return

    directive
]

@directives.directive 'simpleStringContentView', ['$modal', '$log',
  ($modal, $log) =>
    $log.debug 'Constructing simple-string-content-view with the content'
    directive =
      scope:
        value: '='
      template: ''+
        '<span class="simple-content-view simple-string-content-view">'+
          '<span class="value" ng-if="!isValueTooLong">{{content.value}}</span>'+
          '<span class="value-length" ng-if="isValueTooLong">{{content.value.length}} bytes</span>'+
          '<a href="" class="pull-right clearfix open" ng-click="open()" ng-if="isValueTooLong">Open in editor</a>'+
        '</span>'
      restrict: 'A'
      replace: true
      link: (scope, element, attrs) =>

        if !scope.value?
          scope.value = '-'
        scope.isValueTooLong = scope.value.length > 100

        scope.$on 'assertion-report.open', (event, assertionReport) =>
          location = extractLocationInfo assertionReport.value.location
          if location? && location.name? && location.name == scope.content.name
            scope.open location.line

        scope.open = (lineNumber) =>
          openEditorWindow $modal, scope.content.name, scope.value, scope.report, lineNumber
]

@directives.directive 'uriContentView', ['$log',
  ($log) ->
    $log.debug 'Constructing uri-content-view with the content'

    directive =
      scope:
        content: '='
        report: '='
      template: ''+
        '<div class="content-view uri-content-view">'+
          '<div ng-repeat="item in content.item" simple-uri-content-view content="item" report="report"></div>'+
        '</div>'
      restrict: 'A'
      replace: true
      link: (scope, element, attrs) =>

    directive
]

@directives.directive 'simpleUriContentView', ['$modal','$log',
  ($modal, $log) =>
    $log.debug 'Constructing simple-uri-content-view with the content'

    directive =
      scope:
        content: '='
        report: '='
      template: ''+
        '<span class="simple-content-view simple-uri-content-view">'+
          '<strong class="name" ng-if="isNameVisible">{{content.name}}: </strong>'+
          '<span class="value">{{scope.content.value}}</span>'+
          '<a href="" class="open" ng-click="openURI()">Open</a>'+
        '</span>'
      link: (scope, element, attrs) =>

        scope.isNameVisible = scope.content.name?

        if !scope.content.value?
          scope.content.value = '-'

        scope.open = () =>

    directive
]

@directives.directive 'base64ContentView', ['$log',
  ($log) ->
    $log.debug 'Constructing base64-content-view with the content'

    directive =
      scope:
        content: '='
        report: '='
      template: ''+
        '<div class="content-view base64-content-view">'+
          '<div ng-repeat="item in content.item" simple-base64-content-view content="item" report="report"></div>'+
        '</div>'
      restrict: 'A'
      replace: true
      link: (scope, element, attrs) =>

    directive
]

@directives.directive 'simpleBase64ContentView', ['$modal', '$window', '$log',
  ($modal, $window, $log) =>
    $log.debug 'Constructing simple-base64-content-view with the content'

    base64toString = (base64) ->
      atob base64

    directive =
      scope:
        content: '='
        report: '='
      template: ''+
        '<span class="simple-content-view simple-base64-content-view">'+
          '<strong class="name" ng-if="isNameVisible">{{content.name}}: </strong>'+
          '<span class="value-length">{{content.value.length}} bytes</span>'+
          '<a href="" class="pull-right clearfix open" ng-click="open()">Open in editor</a>'+
        '</span>'
      link: (scope, element, attrs) =>

        scope.isNameVisible = scope.content.name?

        if !scope.content.value?
          scope.content.value = '-'

        scope.$on 'assertion-report.open', (event, assertionReport) =>
          location = extractLocationInfo assertionReport.value.location
          if location? && location.name? && location.name == scope.content.name
            scope.open location.line

        scope.open = (lineNumber) =>
          value = base64toString scope.content.value

          openEditorWindow $modal, scope.content.name, value, scope.report, lineNumber

    directive
]
