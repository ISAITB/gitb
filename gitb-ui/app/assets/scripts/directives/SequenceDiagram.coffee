TEST_ENGINE_ACTOR = 'Test Engine'
TESTER_ACTOR = 'Operator'

getTesterNameForActor = (actor, actorInfo) ->
  if !actor?
    sutActor = _.find(actorInfo, (a) =>
      a.role == 'SUT'
    )
    actor = sutActor.id
  actorName = actor + ' - ' + TESTER_ACTOR
  for info in actorInfo
    if actor == info.id
      if info.name?
        actorName = info.name + ' - ' + TESTER_ACTOR
      else
        actorName = info.id + ' - ' + TESTER_ACTOR
      break
  actorName

extractActors = (messages, actorInfo) =>
  collection = _ messages
  collection = collection.map (message) ->
    if message.from? && message.to?
      [message.from, message.to]
    else if message.type == 'loop'
      extractActors(message.steps, actorInfo)
    else if message.type == 'decision'
      _then = extractActors(message.then, actorInfo)
      _else = extractActors(message.else, actorInfo)

      _then.concat _else
    else if message.type == 'flow'
      for thread in message.threads
        extractActors(thread, actorInfo)
    else if message.type == 'exit'
      [TEST_ENGINE_ACTOR, TEST_ENGINE_ACTOR]
    else if message.type == 'interact'
      instructions = _.filter message.interactions, (interaction) -> interaction.type == 'instruction'
      requests = _.filter message.interactions, (interaction) -> interaction.type == 'request'

      instructionActors = _.map instructions, (instruction) -> [(getTesterNameForActor(instruction.with, actorInfo)), instruction.with]
      requestActors = _.map requests, (request) -> [(getTesterNameForActor(request.with, actorInfo)), TEST_ENGINE_ACTOR]

      instructionActors.concat requestActors
    else
      []
  flattened = collection.flatten().unique().value()
  hasOrdering = _.find(actorInfo, (actor) => 
    return actor.displayOrder?
  )
  if hasOrdering?
    sortActors(flattened, actorInfo)
  else
    flattened

sortActors = (actors, actorInfo) =>
  sortedArray = []
  for actor in actors
    actorDef = _.find(actorInfo, (actorDef) =>
      actor == actorDef.id
    )
    if actorDef?
      sortedArray.push(undefined)
    else
      sortedArray.push(actor)
  for actor in actorInfo
    for sortedActor, sortedIndex in sortedArray
      if !sortedActor?
        sortedArray[sortedIndex] = actor.id
        break
  sortedArray

extractSteps = (s, actorInfo) =>
  stepFilter = (step) ->
    step.type == 'msg' ||
      step.type == 'verify' ||
      step.type == 'interact' ||
      step.type == 'exit' ||
      (step.type == 'loop' && (_.filter step.steps, stepFilter).length > 0) ||
      (step.type == 'decision' &&
        (_.filter step.then, stepFilter).length + (_.filter step.else, stepFilter).length > 0) ||
      (step.type == 'flow' && (_.filter step.threads, ((thread) -> (_.filter thread, stepFilter).length > 0)).length > 0)
  steps = _.filter s, stepFilter
  processStep = (step, actorInfo) ->
    step.level = (step.id.split '.').length
    if step.type == 'verify' || step.type == 'exit'
      step.from = TEST_ENGINE_ACTOR
      step.to = TEST_ENGINE_ACTOR
    else if step.type == 'loop'
      step.steps = extractSteps(step.steps, actorInfo)
    else if step.type == 'decision'
      step.then = extractSteps(step.then, actorInfo)
      step.else = extractSteps(step.else, actorInfo)
    else if step.type == 'flow'
      for thread in  step.threads
        extractSteps(thread, actorInfo)
    else if step.type == 'interact'
      #step.from = TEST_ENGINE_ACTOR
      #step.to = TESTER_ACTOR
      for interaction in step.interactions
        if interaction.type == 'request'
          interaction.from = getTesterNameForActor(interaction.with, actorInfo)
          interaction.to = TEST_ENGINE_ACTOR
        else
          interaction.from = getTesterNameForActor(interaction.with, actorInfo)
          interaction.to = interaction.with
    step
  for step in steps
    processStep(step, actorInfo)

@directives.directive 'seqDiagram', () =>
  scope:
    stepsOfTests: '='
    test: '@'
    actorInfoOfTests: '='
  restrict: 'A'
  replace: true
  template: ''+
    '<div class="sequence-diagram actor-diagram-{{actors.length}}">'+
      '<div class="actor-container">'+
        '<div ng-repeat="actor in actors" seq-diagram-actor actor="actor" actor-info="actorInfo"></div>'+
      '</div>'+
      '<div class="lifeline-container">'+
        '<div class="lifeline" ng-repeat="actor in actors"></div>'+
      '</div>'+
      '<div class="message-container">'+
        '<div ng-repeat="message in messages" seq-diagram-message message="message"></div>'+
      '</div>'+
    '</div>'
  link: (scope, element, attrs) ->

    scope.$on 'sequence:testLoaded', (event, loadedTestId) =>
      if (loadedTestId+"" == scope.test+"")
        updateState()

    updateState = () =>
      steps = scope.stepsOfTests[scope.test]
      scope.actorInfo = scope.actorInfoOfTests[scope.test]
      if steps?
        scope.messages = extractSteps(steps, scope.actorInfo)
        scope.actors = extractActors(scope.messages, scope.actorInfo)

        setStepIndexes scope.messages

    setIndexes = (message) =>
      message.fromIndex = _.indexOf scope.actors, message.from
      message.toIndex = _.indexOf scope.actors, message.to
      message.span = Math.abs (message.fromIndex - message.toIndex)

    setLoopStepChildIndexes = (message) ->
      setStepIndexes message.steps

      firstChild = _.min message.steps, (childStep) -> childStep.fromIndex
      lastChild = _.max message.steps, (childStep) -> childStep.toIndex

      message.from = firstChild.from
      message.to = lastChild.to
      message.fromIndex = firstChild.fromIndex
      message.toIndex = lastChild.toIndex
      message.span = (Math.abs (message.fromIndex - message.toIndex))+1

    setDecisionStepChildIndexes = (message) ->

      childSteps = message.then.concat message.else

      setStepIndexes childSteps

      firstChild = _.min childSteps, (childStep) -> childStep.fromIndex
      lastChild = _.max childSteps, (childStep) -> childStep.toIndex

      message.from = firstChild.from
      message.to = lastChild.to
      message.fromIndex = firstChild.fromIndex
      message.toIndex = lastChild.toIndex
      message.span = (Math.abs (message.fromIndex - message.toIndex))+1

    setFlowStepChildIndexes = (message) ->
      _.forEach message.threads, setStepIndexes

      firstChild = _.min (_.flatten message.threads), (childStep) -> childStep.fromIndex
      lastChild = _.max (_.flatten message.threads), (childStep) -> childStep.fromIndex

      message.from = firstChild.from
      message.to = lastChild.to
      message.fromIndex = firstChild.fromIndex
      message.toIndex = firstChild.toIndex
      message.span = (Math.abs (message.fromIndex - message.toIndex))+1

    setInteractionStepChildIndexes = (message) =>
      _.forEach message.interactions, setIndexes

      firstChild = _.min message.interactions, (interaction) -> Math.min interaction.fromIndex, interaction.toIndex
      lastChild = _.max message.interactions, (interaction) -> Math.max interaction.fromIndex, interaction.toIndex

      message.fromIndex = Math.min firstChild.fromIndex, firstChild.toIndex
      message.toIndex = Math.max lastChild.fromIndex, lastChild.toIndex
      message.from = scope.actors[message.fromIndex]
      message.to = scope.actors[message.toIndex]
      message.span = (Math.abs (message.fromIndex - message.toIndex)) #+1

    setStepIndexes = (messages) ->
      _.forEach messages, (message, i) =>
        message.order = i

        if message.type == 'verify' || message.type == 'msg' || message.type == 'exit'
          setIndexes message
        else if message.type == 'loop'
          setLoopStepChildIndexes message
        else if message.type == 'decision'
          setDecisionStepChildIndexes message
        else if message.type == 'flow'
          setFlowStepChildIndexes message
        else if message.type == 'interact'
          setInteractionStepChildIndexes message

    scope.messages = []
    scope.actors = []

    updateState()


@directives.directive 'seqDiagramActor', ()->
  scope:
    actor: '='
    actorInfo: '='
    messages: '='
  restrict: 'A'
  replace: true
  template: ''+
    '<div class="actor">'+
      '<span class="name">{{actor}}</span>'+
    '</div>'
  link: (scope, element, attrs) ->
    for info in scope.actorInfo
      if scope.actor == info.id
        if info.name?
          scope.actor = info.name + " (" + info.role + ")"
        else
          scope.actor = info.id + " (" + info.role + ")"

@directives.directive 'seqDiagramMessage', ['RecursionHelper', 'ReportService', 'Constants', '$modal', '$timeout',
  (RecursionHelper, ReportService, Constants, $modal, $timeout) =>
    scope:
      message: '='
      actorInfo: '='
    restrict: 'A'
    replace: true
    template: ''+
      '<div class="message-wrapper offset-{{message.fromIndex}} {{message.type}}-type">'+
        '<div class="message span-{{message.span}} reverse-offset-{{message.span}} depth-{{depth}} level-{{message.level}}" '+
          'ng-class="{\'self-message\': message.fromIndex == message.toIndex, '+
          '\'backwards-message\': message.fromIndex > message.toIndex, '+
          '\'processing\': message.status == TEST_STATUS.PROCESSING, '+
          '\'skipped\': message.status == TEST_STATUS.SKIPPED, '+
          '\'waiting\': message.status == TEST_STATUS.WAITING, '+
          '\'error\': message.status == TEST_STATUS.ERROR, '+
          '\'completed\': message.status == TEST_STATUS.COMPLETED}">'+
          '<div class="message-type">'+
            '<span>{{message.type}}</span>'+
            '<span class="dropdown iterations" dropdown ng-if="message.type == \'loop\' && message.sequences != null && message.sequences.length > 0">'+
              '<span href="" class="dropdown-toggle" dropdown-toggle>Show Iterations</span>'+
              '<ul class="dropdown-menu">'+
                '<li class="dropdown-item"'+
                  'ng-repeat="iteration in message.sequences track by $index" ng-click="showLoopIteration($index)" '+
                  'ng-if="iteration != null" '+
                  'ng-class="{\'processing\': iteration.status == TEST_STATUS.PROCESSING, '+
                  '\'skipped\': iteration.status == TEST_STATUS.SKIPPED, '+
                  '\'waiting\': iteration.status == TEST_STATUS.WAITING, '+
                  '\'error\': iteration.status == TEST_STATUS.ERROR, '+
                  '\'completed\': iteration.status == TEST_STATUS.COMPLETED}"'+
                  '>Iteration {{$index+1}}</li>'+
              '</ul>'+
            '</span>'+
          '</div>'+
          '<div class="message-info">'+
            '<div class="step-icon" ng-if="message.type == \'verify\'">'+
              '<i class="fa fa-gear" ng-class="{\'fa-spin\': message.status == TEST_STATUS.PROCESSING}"></i>'+
            '</div>'+
            '<div class="step-icon" ng-if="message.type == \'exit\'">'+
              '<i class="fa fa-dot-circle-o""></i>'+
            '</div>'+
            '<span class="title">{{message.desc}}</span>'+
          '</div>'+
          '<div class="message-report" ng-if="message.report != null">'+
            '<a href="" class="report-link" ng-click="showReport()">'+
              '<span class="fa-stack">'+
                '<i class="fa fa-file-text-o report-icon"></i>'+
                '<i class="fa fa-circle status-background-icon"></i>'+
                '<i class="fa fa-gear processing-icon"></i>'+
                '<i class="fa fa-check completed-icon"></i>'+
                '<i class="fa fa-times error-icon"></i>'+
              '</span>'+
            '</a>'+
          '</div>'+
          '<div class="child-steps loop reverse-offset-{{message.fromIndex}}" ng-if="message.type == \'loop\'">'+
            '<div ng-repeat="subMessage in message.steps" seq-diagram-message message="subMessage"></div>'+
          '</div>'+
          '<div class="child-steps then reverse-offset-{{message.fromIndex}}" ng-if="message.type == \'decision\'">'+
            '<div ng-repeat="subMessage in message.then" seq-diagram-message message="subMessage"></div>'+
          '</div>'+
          '<div class="child-steps else reverse-offset-{{message.fromIndex}}" ng-if="message.type == \'decision\' && message.else.length > 0">'+
            '<div ng-repeat="subMessage in message.else" seq-diagram-message message="subMessage"></div>'+
          '</div>'+
          '<div class="child-steps thread thread-{{$index}} reverse-offset-{{message.fromIndex}}" ng-if="message.type == \'flow\'" ng-repeat="thread in message.threads">'+
            '<div ng-repeat="subMessage in thread" seq-diagram-message message="subMessage"></div>'+
          '</div>'+
          '<div class="child-steps interactions reverse-offset-{{message.fromIndex}}" ng-if="message.type == \'interact\'">'+
            '<div ng-repeat="subMessage in message.interactions" seq-diagram-message message="subMessage"></div>'+
          '</div>'+
          '<div class="arrow" ng-if="message.type == \'msg\' || message.type == \'instruction\' || message.type == \'request\'"></div>'+
          '<div seq-diagram-message-status message="message"></div>'+
        '</div>'+
      '</div>'
    compile: (element) =>
      RecursionHelper.compile element, (scope, element, attrs) =>
        calculateDepth = (message) ->
          if message.type == 'loop'
            childDepths = _.map message.steps, calculateDepth
            (_.max childDepths) + 1
          else if message.type == 'decision'
            childDepths = _.map (message.then.concat message.else), calculateDepth
            (_.max childDepths) + 1
          else if message.type == 'flow'
            childDepths = _.map (_.flatten message.threads), calculateDepth
            (_.max childDepths) + 1
          else if message.type == 'interact'
            childDepths = _.map message.interactions, calculateDepth
            (_.max childDepths) + 1
          else if message.type == 'instruction' || message.type == 'request'
            1
          else
            message.level
        scope.showReport = () =>
          showTestStepReportModal = (report) =>
            modalOptions =
              templateUrl: 'assets/views/components/test-step-report-modal.html'
              controller: 'TestStepReportModalController as testStepReportModalCtrl'
              resolve:
                step: () => scope.message
                report: () => report
                sessionId: () => scope.message.report.tcInstanceId
              size: 'lg'

            $modal.open modalOptions

          if scope.message.report?
            if scope.message.report.path? && !scope.message.report.result?
              ReportService.getTestStepReport escape(scope.message.report.path) #paths like 6[2].1.xml must be escaped
              .then (report) =>
                showTestStepReportModal report
              .catch (error) =>
                @$log.debug "An error occurred", error
            else
              showTestStepReportModal scope.message.report

        scope.showLoopIteration = (iterationIndex) =>
          setStatusesAndReports = (message, iteration) ->
            applyStatusesAndReportsToChildSteps = (childSteps, childStepIterations) =>

              zipped = _.zip childSteps, childStepIterations
              _.forEach zipped, (pair) ->
                setStatusesAndReports pair[0], pair[1]
            message.status = iteration.status
            message.report = iteration.report
            if message.type == 'loop'
              if iteration.sequences?
                message.sequences = iteration.sequences
              applyStatusesAndReportsToChildSteps message.steps, iteration.steps
            else if message.type == 'decision'
              applyStatusesAndReportsToChildSteps message.then, iteration.then
              applyStatusesAndReportsToChildSteps message.else, iteration.else
            else if message.type == 'flow'
              _.forEach (_.zip message.threads, iteration.threads), (threadPair) ->
                applyStatusesAndReportsToChildSteps threadPair[0], threadPair[1]

          scope.message.sequences[iterationIndex].steps = extractSteps(scope.message.sequences[iterationIndex].steps, scope.actorInfo)
          setStatusesAndReports scope.message, scope.message.sequences[iterationIndex]

          scope.message.status = scope.message.sequences[iterationIndex].status
          scope.message.report = scope.message.sequences[iterationIndex].report

        if scope.message.type == 'loop'
          onLoopIterationUpdated = (sequences) ->
            showLastStatus = () =>
              scope.showLoopIteration sequences.length - 1

            if sequences? && sequences.length > 0
              $timeout showLastStatus, 0
          scope.$watch 'message.sequences', onLoopIterationUpdated, true

        scope.depth = calculateDepth scope.message
        scope.TEST_STATUS = Constants.TEST_STATUS
]

@directives.directive 'seqDiagramMessageStatus', (RecursionHelper, Constants) ->
  scope:
    message: '='
  restrict: 'A'
  replace: true
  template: ''+
    '<div class="status-wrapper {{message.type}}-type">'+
      '<div class="status" '+
        'ng-class="{\'self-message\': message.fromIndex == message.toIndex, '+
        '\'backwards-message\': message.fromIndex > message.toIndex, '+
        '\'processing\': message.status == TEST_STATUS.PROCESSING, '+
        '\'skipped\': message.status == TEST_STATUS.SKIPPED, '+
        '\'waiting\': message.status == TEST_STATUS.WAITING, '+
        '\'error\': message.status == TEST_STATUS.ERROR, '+
        '\'completed\': message.status == TEST_STATUS.COMPLETED}">'+
      '</div>'+
    '</div>'
  compile: (element) ->
    RecursionHelper.compile element, (scope, element, attrs) ->
      scope.TEST_STATUS = Constants.TEST_STATUS
