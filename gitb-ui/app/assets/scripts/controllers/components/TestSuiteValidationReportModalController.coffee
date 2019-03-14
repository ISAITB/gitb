class TestSuiteValidationReportModalController

  @$inject = ['$scope', '$uibModalInstance', 'report']

  constructor: (@$scope, @$uibModalInstance, report) ->

    @$scope.report = report

    @$scope.continue = () =>
      @$uibModalInstance.close()

    @$scope.close = () =>
      @$uibModalInstance.dismiss()

@controllers.controller 'TestSuiteValidationReportModalController', TestSuiteValidationReportModalController
