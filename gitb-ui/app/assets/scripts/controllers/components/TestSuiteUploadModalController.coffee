class TestSuiteUploadModalController

    @$inject = ['$scope', '$q', '$uibModalInstance', 'ConfirmationDialogService', 'DataService', 'ErrorService', 'ConformanceService', '$state', 'availableSpecifications', 'testSuitesVisible', 'PopupService']
    constructor: (@$scope, @$q, @$uibModalInstance, @ConfirmationDialogService, @DataService, @ErrorService, @ConformanceService, @$state, @availableSpecifications, @testSuitesVisible, @PopupService) ->
      @specifications = []
      @actionPending = false
      @actionProceedPending = false
      @hasValidationWarnings = false
      @hasValidationErrors = false
      @step = 'initial'
      @hasChoicesToComplete = true
      @specificationNames = {}
      @skipCount = 0
      for specification in @availableSpecifications
        @specificationNames[specification.id] = specification.fname
      if @availableSpecifications.length == 1
        @specifications.push @availableSpecifications[0]

    proceedDisabled: () =>
      @hasValidationErrors || @actionPending || @actionProceedPending || !(@file? && @specifications.length > 0) || (@step == 'replace' && !@hasChoicesToComplete)

    parseValidationReport: () =>
      if @uploadResult.validationReport?.counters?.errors? && @uploadResult.validationReport?.counters?.warnings?
        @hasValidationErrors = parseInt(@uploadResult.validationReport.counters.errors) > 0
        @hasValidationWarnings = parseInt(@uploadResult.validationReport.counters.warnings) > 0
      else
        @hasValidationErrors = false
        @hasValidationWarnings = false

    showValidationReport: () =>
      @step = 'validation'
      @report = @uploadResult.validationReport

    actionLabel = (itemAction) -> 
      if (itemAction == 'update') 
        '(updated)'
      else if (itemAction == 'add') 
        "(added)"
      else if (itemAction == 'unchanged') 
        '(unchanged)'
      else 
        '(deleted)'

    specificationName: (specificationId) =>
      @specificationNames[specificationId]

    showUploadResults: () =>
      @PopupService.success('Test suite uploaded.')    
      @step = 'results'
      specificationIdToIndexMap = {}
      specificationResults = []
      latestIndex = -1
      for item in @uploadResult.items
        specIndex = specificationIdToIndexMap[item.specification]
        if !specIndex?
          latestIndex += 1
          specificationResults.push({
            specification: item.specification
            testSuites: []
            testCases: []
            actors: []
            endpoints: []
            parameters: []
          })
          specIndex = latestIndex
          specificationIdToIndexMap[item.specification] = specIndex
        itemEntry = item.name+' '+actionLabel(item.action)
        if item.type == 'testSuite'
          specificationResults[specIndex].testSuites.push(itemEntry)
        else if item.type == 'testCase'
          specificationResults[specIndex].testCases.push(itemEntry)
        else if item.type == 'actor'
          specificationResults[specIndex].actors.push(itemEntry)
        else if item.type == 'endpoint'
          specificationResults[specIndex].endpoints.push(itemEntry)
        else
          specificationResults[specIndex].parameters.push(itemEntry)
      for result in specificationResults
        if result.testSuites.length > 0
          result.testSuiteSummary = result.testSuites.join ', '
        if result.testCases.length > 0
          result.testCaseSummary = result.testCases.join ', '
        if result.actors.length > 0
          result.actorSummary = result.actors.join ', '
        if result.endpoints.length > 0
          result.endpointSummary = result.endpoints.join ', '
        if result.parameters.length > 0
          result.parameterSummary = result.parameters.join ', '
      @results = specificationResults

    showSpecificationChoices: () =>
      @step = 'replace'
      @hasMatchingTestSuite = false
      existsMap = {}
      for existSpec in @uploadResult.existsForSpecs
         existsMap[existSpec] = existSpec
         @hasMatchingTestSuite = true
      matchingDataMap = {}
      for matchingSpec in @uploadResult.matchingDataExists
         matchingDataMap[matchingSpec] = matchingSpec
      @specificationChoices = []
      @specificationChoiceMap = {}
      for spec in @specifications
        existingData = matchingDataMap[spec.id]?
        existingTestSuite = existsMap[spec.id]?
        if existingData || existingTestSuite
          specData = {
            specification: spec.id
            history: 'keep'
            metadata: 'skip'
            skipUpdate: false
            dataExists: existingData
            testSuiteExists: existingTestSuite
          }
          @specificationChoices.push(specData)
          @specificationChoiceMap[spec.id] = specData
      @hasMultipleChoices = @specificationChoices.length > 1

    specificationIds: () =>
      @specifications.map((s) => s.id)

    applyChoiceToAll: (specification) =>
      reference = @specificationChoiceMap[specification]
      for choice in @specificationChoices
        if choice.specification != specification
          choice.metadata = reference.metadata
          if choice.testSuiteExists
            choice.history = reference.history

    skipUpdate: (specification) =>
      @specificationChoiceMap[specification].skipUpdate = true
      @skipCount = @skipCount + 1
      @hasChoicesToComplete = @specifications.length > @skipCount

    processUpdate: (specification) =>
      @specificationChoiceMap[specification].skipUpdate = false
      @skipCount = @skipCount - 1
      @hasChoicesToComplete = @specifications.length > @skipCount

    uploadTestSuite: () =>
      @actionPending = true
      @actionProceedPending = true
      @ConformanceService.deployTestSuite(@specificationIds(), @file)
      .then((result) => 
        if result && result.data 
          @uploadResult = result.data
          if @uploadResult.validationReport
            @parseValidationReport()
            if @hasValidationErrors || @hasValidationWarnings
              @showValidationReport()
            else
              if @uploadResult.needsConfirmation
                @showSpecificationChoices()
              else if @uploadResult.success
                @showUploadResults()
              else
                @showErrorMessage()
        else
          @ErrorService.showErrorMessage('An error occurred while processing the test suite: Response was empty')
        @actionPending = false
        @actionProceedPending = false
      , (error) =>
        @ErrorService.showErrorMessage(error)
        @actionPending = false
        @actionProceedPending = false
      )

    showErrorMessage: () =>
      if @uploadResult?
        error = { 
          statusText: "Upload error",
        }
        if @uploadResult.errorInformation?
          error.data = {error_description: 'An error occurred while processing the test suite: '+@uploadResult.errorInformation}
        else
          error.data = {error_description: 'An error occurred while processing the test suite'}
        @ErrorService.showErrorMessage(error)
      else
        @ErrorService.showErrorMessage('An error occurred while processing the test suite: Response was empty')

    applyPendingUpload: () =>
      actions = []
      hasDropHistory = false
      for specification in @specifications
        choice = @specificationChoiceMap[specification.id]
        action = {
            specification: specification.id
        }
        if choice?
          if choice.skipUpdate
            action.action = 'cancel'
          else
            if choice.history == 'drop'
              hasDropHistory = true
            action.action = 'proceed'
            action.pending_action_history = choice.history
            action.pending_action_metadata = choice.metadata
        else
          action.action = 'proceed'
          action.pending_action_history = 'keep'
          action.pending_action_metadata = 'update'
        actions.push(action)
      deferred = @$q.defer()
      if hasDropHistory
        @ConfirmationDialogService.confirm("Confirm test history deletion", "Dropping existing results will render this test suite's prior tests obsolete. Are you sure you want to proceed?", "Yes", "No")
        .then(
          () =>
            deferred.resolve()
          , 
          () =>
            deferred.reject()
        )
      else
        deferred.resolve()
      deferred.promise.then(
        () =>
          @actionPending = true
          @actionProceedPending = true
          @ConformanceService.resolvePendingTestSuite(@uploadResult.pendingFolderId, 'proceed', @specificationIds(), actions)
          .then((result) =>
            if result
              @uploadResult = result
              if @uploadResult.success
                @showUploadResults()
              else
                @showErrorMessage()
            else
              @ErrorService.showErrorMessage('An error occurred while processing the test suite: Response was empty')
            @actionPending = false
            @actionProceedPending = false
          , (error) =>
            @ErrorService.showErrorMessage(error)
            @actionPending = false
            @actionProceedPending = false
          )
      )

    ignoreValidationWarnings: () =>
      if @uploadResult.existsForSpecs?.length > 0 || @uploadResult.matchingDataExists?.length > 0
        @showSpecificationChoices()
      else
        @actionPending = true
        @actionProceedPending = true
        @ConformanceService.resolvePendingTestSuite(@uploadResult.pendingFolderId, 'proceed', @specificationIds())
        .then((result) =>
          if result
            @uploadResult = result
            if @uploadResult.success
              @showUploadResults()
            else
              @showErrorMessage()
          else
            @ErrorService.showErrorMessage('An error occurred while processing the test suite: Response was empty')
          @actionPending = false
          @actionProceedPending = false
        , (error) =>
          @ErrorService.showErrorMessage(error)
          @actionPending = false
          @actionProceedPending = false
        )

    proceedVisible: () =>
      !(@step == 'results' || @step == 'validation' && @hasValidationErrors)

    proceed: () =>
      if @step == 'initial'
        @uploadTestSuite()
      else if @step == 'validation'
        @ignoreValidationWarnings()
      else if @step == 'replace'
        @applyPendingUpload()

    resolutionNeeded: () =>
      (@step == 'validation' && !@hasValidationErrors) || (@step == 'replace')

    refreshNeeded:() =>
      @step == 'results' && @testSuitesVisible

    close: () =>
      if @resolutionNeeded()
        @ConformanceService.resolvePendingTestSuite(@uploadResult.pendingFolderId, 'cancel', @specificationIds()).then(angular.noop, angular.noop)
      if @refreshNeeded()
        @$state.go(@$state.$current, null, { reload: true });
      @$uibModalInstance.dismiss()

    selectArchive: (files) =>
        @file = _.head files

@controllers.controller 'TestSuiteUploadModalController', TestSuiteUploadModalController

