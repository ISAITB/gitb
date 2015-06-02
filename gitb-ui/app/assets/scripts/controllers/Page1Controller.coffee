class Page1Controller

    constructor: (@$log, @$scope, @$location, @SystemService) ->
        @$log.debug "Constructing Page1Controller..."

        @$scope.suites = []
        @$scope.testcases = []
        @$scope.parties = [{name: 'NHIS Client'}]

        @$scope.suiteGrid = {
            data: 'suites',
            selectedItems: [],
            multiSelect: false,
            enableColumnResize : true,
            columnDefs: [{field:'name', displayName:'Name'}]
        }

        @caseGrid = {
            data: 'testcases',
            selectedItems: [],
            multiSelect  : false,
            enableColumnResize : true,
            columnDefs: [
                {field:'name', displayName:'Name'},
                {field:'description', displayName:'Description'}
            ]
        }

        @partyGrid = {
            data: 'parties',
            selectedItems: [],
            multiSelect  : false,
            enableColumnResize : true,
            columnDefs: [
                {field:'name', displayName:'Name'}
            ]
        }

        @SystemService.getTestSuites()
            .then(
                (data) =>
                    @$scope.suites = data
                ,
                (error) =>
                    @$log.debug error
            )

    testSuiteSelected: () ->
        if @$scope.suiteGrid.selectedItems.length > 0
            @$scope.testcases = @$scope.suiteGrid.selectedItems[0].testcases

    test: () ->
        @$log.debug "senan"

    redirect: (address) ->
        @$location.path(address)

controllers.controller('Page1Controller', Page1Controller)