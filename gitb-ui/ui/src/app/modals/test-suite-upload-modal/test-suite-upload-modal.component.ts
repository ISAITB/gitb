import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { Observable } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { PopupService } from 'src/app/services/popup.service';
import { FileData } from 'src/app/types/file-data.type';
import { Specification } from 'src/app/types/specification';
import { PendingTestSuiteUploadChoice } from './pending-test-suite-upload-choice';
import { SpecificationChoice } from './specification-choice';
import { SpecificationResult } from './specification-result';
import { TestSuiteUploadResult } from './test-suite-upload-result';
import { TestSuiteUploadTestCase } from './test-suite-upload-test-case';
import { TestSuiteUploadTestCaseChoice } from './test-suite-upload-test-case-choice';
import { ValidationReport } from './validation-report';
import { find } from 'lodash';
import { PendingTestSuiteUploadChoiceTestCase } from './pending-test-suite-upload-choice-test-case';

@Component({
  selector: 'app-test-suite-upload-modal',
  templateUrl: './test-suite-upload-modal.component.html',
  styleUrls: ['./test-suite-upload-modal.component.less']
})
export class TestSuiteUploadModalComponent implements OnInit {

  @Input() testSuitesVisible = false
  @Input() availableSpecifications!: Specification[]
  @Output() completed = new EventEmitter<boolean>()

  specifications: Specification[] = []
  actionPending = false
  actionProceedPending = false
  hasValidationWarnings = false
  hasValidationErrors = false
  hasValidationMessages = false
  hasMatchingTestSuite = false
  hasMultipleChoices = false
  step = 'initial'
  hasChoicesToComplete = true
  specificationNames: {[key: number]: string} = {}
  skipCount = 0
  file?: FileData
  uploadResult?: TestSuiteUploadResult
  report?: ValidationReport
  results?: SpecificationResult[]
  specificationChoices?: SpecificationChoice[]
  specificationChoiceMap: {[key: number]: SpecificationChoice} = {}
  reportItemsCollapsed = false

  constructor(
    public dataService: DataService,
    private modalInstance: BsModalRef,
    private confirmationDialogService: ConfirmationDialogService,
    private conformanceService: ConformanceService,
    private popupService: PopupService,
    private errorService: ErrorService
  ) { }

  ngOnInit(): void {
    for (let specification of this.availableSpecifications) {
      this.specificationNames[specification.id] = specification.fname
    }
    if (this.availableSpecifications.length == 1) {
      this.specifications.push(this.availableSpecifications[0])
    }
  }

  proceedDisabled() {
    return this.hasValidationErrors || this.actionPending || this.actionProceedPending || !(this.file != undefined && this.specifications.length > 0) || (this.step == 'replace' && !this.hasChoicesToComplete)
  }

  parseValidationReport() {
    if (this.uploadResult?.validationReport?.counters?.errors != undefined && this.uploadResult.validationReport?.counters?.warnings != undefined && this.uploadResult.validationReport?.counters?.infos != undefined) {
      this.hasValidationErrors = Number(this.uploadResult.validationReport.counters.errors) > 0
      this.hasValidationWarnings = Number(this.uploadResult.validationReport.counters.warnings) > 0
      this.hasValidationMessages = Number(this.uploadResult.validationReport.counters.infos) > 0
    } else {
      this.hasValidationErrors = false
      this.hasValidationWarnings = false
      this.hasValidationMessages = false
    }
  }

  showValidationReport() {
    this.step = 'validation'
    this.report = this.uploadResult?.validationReport
  }

  actionLabel(itemAction: string) {
    if (itemAction == 'update') {
      return '(updated)'
    } else if (itemAction == 'add') {
      return '(added)'
    } else if (itemAction == 'unchanged') {
      return '(unchanged)'
    } else {
      return '(deleted)'
    }
  }

  specificationName(specificationId: number)  {
    return this.specificationNames[specificationId]
  }

  showUploadResults() {
    this.popupService.success('Test suite uploaded.')
    this.step = 'results'
    const specificationIdToIndexMap: {[key: number]: number} = {}
    const specificationResults: SpecificationResult[] = []
    let latestIndex = -1
    for (let item of this.uploadResult!.items) {
      let specIndex = specificationIdToIndexMap[item.specification]
      if (specIndex == undefined) {
        latestIndex += 1
        specificationResults.push({
          specification: item.specification,
          testSuites: [],
          testCases: [],
          actors: [],
          endpoints: [],
          parameters: []
        })
        specIndex = latestIndex
        specificationIdToIndexMap[item.specification] = specIndex
      }
      let itemEntry = item.name+' '+this.actionLabel(item.action)
      if (item.type == 'testSuite') {
        specificationResults[specIndex].testSuites.push(itemEntry)
      } else if (item.type == 'testCase') {
        specificationResults[specIndex].testCases.push(itemEntry)
      } else if (item.type == 'actor') {
        specificationResults[specIndex].actors.push(itemEntry)
      } else if (item.type == 'endpoint') {
        specificationResults[specIndex].endpoints.push(itemEntry)
      } else {
        specificationResults[specIndex].parameters.push(itemEntry)
      }
    }
    for (let result of specificationResults) {
      if (result.testSuites.length > 0) {
        result.testSuiteSummary = result.testSuites.join(', ')
      }
      if (result.testCases.length > 0) {
        result.testCaseSummary = result.testCases.join(', ')
      }
      if (result.actors.length > 0) {
        result.actorSummary = result.actors.join(', ')
      }
      if (result.endpoints.length > 0) {
        result.endpointSummary = result.endpoints.join(', ')
      }
      if (result.parameters.length > 0) {
        result.parameterSummary = result.parameters.join(', ')
      }
    }
    this.results = specificationResults
  }

  showSpecificationChoices() {
    this.step = 'replace'
    this.hasMatchingTestSuite = false
    const existsMap: {[key: number]: number} = {}
    for (let existSpec of this.uploadResult!.existsForSpecs) {
      existsMap[existSpec] = existSpec
      this.hasMatchingTestSuite = true
    }
    const matchingDataMap: {[key: number]: number} = {}
    for (let matchingSpec of this.uploadResult!.matchingDataExists) {
      matchingDataMap[matchingSpec] = matchingSpec
    }
    this.specificationChoices = []
    this.specificationChoiceMap = {}
    for (let spec of this.specifications) {
      const existingData = matchingDataMap[spec.id] !=  undefined
      const existingTestSuite = existsMap[spec.id] != undefined
      if (existingData || existingTestSuite) {
        const testCasesInArchiveAndDB: TestSuiteUploadTestCaseChoice[] = []
        const testCasesInArchive: TestSuiteUploadTestCase[] = []
        const testCasesInDB: TestSuiteUploadTestCase[] = []
        const matchingSpecification = find(this.uploadResult!.testCases, (resultingSpec) => {
          return resultingSpec.specification == spec.id
        })
        if (matchingSpecification) {
          for (let testCase of matchingSpecification.testCases) {
            if (testCase.status == Constants.TEST_CASE_UPLOAD_MATCH.IN_ARCHIVE_AND_DB) {
              testCasesInArchiveAndDB.push({
                identifier: testCase.identifier,
                name: testCase.name,
                updateDefinition: false,
                resetTestHistory: false
              })
            } else {
              const testCaseInfo = {
                identifier: testCase.identifier,
                name: testCase.name,
                status: testCase.status
              }
              if (testCase.status == Constants.TEST_CASE_UPLOAD_MATCH.IN_ARCHIVE_ONLY) {
                testCasesInArchive.push(testCaseInfo)
              } else if (testCase.status == Constants.TEST_CASE_UPLOAD_MATCH.IN_DB_ONLY) {
                testCasesInDB.push(testCaseInfo)
              }
            }
          }
        }
        const specData: SpecificationChoice = {
          specification: spec.id,
          updateActors: false,
          updateTestSuite: false,
          skipUpdate: false,
          dataExists: existingData,
          testSuiteExists: existingTestSuite,
          testCasesInArchiveAndDB: testCasesInArchiveAndDB,
          testCasesInArchive: testCasesInArchive,
          testCasesInDB: testCasesInDB,
          showTestCasesToUpdate: false,
          showTestCasesToAdd: false,
          showTestCasesToDelete: false
        }
        this.specificationChoices.push(specData)
        this.specificationChoiceMap[spec.id] = specData
      }
    }
    this.hasMultipleChoices = this.specificationChoices.length > 1
  }

  toTestCaseChoices(testCases: TestSuiteUploadTestCase[]): TestSuiteUploadTestCaseChoice[] {
    const choices: TestSuiteUploadTestCaseChoice[] = []
    for (let testCase of testCases) {
      choices.push({
        identifier: testCase.identifier,
        name: testCase.name,
        updateDefinition: false,
        resetTestHistory: false
      })
    }
    return choices
  }

  toggleTestCaseChoices(choice: SpecificationChoice, selected: boolean) {
    for (let testCase of choice.testCasesInArchiveAndDB) {
      testCase.updateDefinition = selected
      testCase.resetTestHistory = selected
    }
  }

  toggleTestCaseDataChoice(choice: SpecificationChoice, selected: boolean) {
    for (let testCase of choice.testCasesInArchiveAndDB) {
      testCase.updateDefinition = selected
    }
  }

  toggleTestCaseHistoryChoice(choice: SpecificationChoice, selected: boolean) {
    for (let testCase of choice.testCasesInArchiveAndDB) {
      testCase.resetTestHistory = selected
    }
  }

  hasAllTestCaseDataChoice(choice: SpecificationChoice, selected: boolean) {
    for (let testCase of choice.testCasesInArchiveAndDB) {
      if (testCase.updateDefinition != selected) {
        return false
      }
    }
    return true
  }

  hasAllTestCaseHistoryChoice(choice: SpecificationChoice, selected: boolean) {
    for (let testCase of choice.testCasesInArchiveAndDB) {
      if (testCase.resetTestHistory != selected) {
        return false
      }
    }
    return true
  }

  specificationIds() {
    return this.specifications.map((s) => s.id)
  }

  applyChoiceToAll(specification: number) {
    const reference = this.specificationChoiceMap[specification]
    for (let choice of this.specificationChoices!) {
      if (choice.specification != specification) {
        choice.updateActors = reference.updateActors
        if (choice.testSuiteExists) {
          choice.updateTestSuite = reference.updateTestSuite
          for (let referenceTestCase of reference.testCasesInArchiveAndDB) {
            const matchingTestCase = find(choice.testCasesInArchiveAndDB, (testCase) => {
              return testCase.identifier == referenceTestCase.identifier
            })
            if (matchingTestCase) {
              matchingTestCase.updateDefinition = referenceTestCase.updateDefinition
              matchingTestCase.resetTestHistory = referenceTestCase.resetTestHistory
            }
          }
        }
      }
    }
  }

  skipUpdate(specification: number) {
    this.specificationChoiceMap[specification].skipUpdate = true
    this.skipCount = this.skipCount + 1
    this.hasChoicesToComplete = this.specifications.length > this.skipCount
  }

  processUpdate(specification: number) {
    this.specificationChoiceMap[specification].skipUpdate = false
    this.skipCount = this.skipCount - 1
    this.hasChoicesToComplete = this.specifications.length > this.skipCount
  }

  uploadTestSuite() {
    this.actionPending = true
    this.actionProceedPending = true
    this.conformanceService.deployTestSuite(this.specificationIds(), this.file!.file!)
    .subscribe((result) => {
      this.uploadResult = result
      if (this.uploadResult.validationReport) {
        this.parseValidationReport()
        if (this.hasValidationErrors || this.hasValidationWarnings || this.hasValidationMessages) {
          this.showValidationReport()
        } else {
          if (this.uploadResult.needsConfirmation) {
            this.showSpecificationChoices()
          } else if (this.uploadResult.success) {
            this.showUploadResults()
          } else {
            this.showErrorMessage()
          }
        }
      } else if (!this.uploadResult.success) {
        this.showErrorMessage()
      }
    }).add(() => {
      this.actionPending = false
      this.actionProceedPending = false
    })
  }

  showErrorMessage() {
    let msg: string
    if (this.uploadResult != undefined) {
      if (this.uploadResult.errorInformation != undefined) {
        msg = 'An error occurred while processing the test suite: '+this.uploadResult.errorInformation
      } else {
        msg = 'An error occurred while processing the test suite'
      }
    } else {
      msg = 'An error occurred while processing the test suite: Response was empty'
    }
    this.errorService.showSimpleErrorMessage("Upload error", msg)
  }

  applyPendingUpload() {
    const actions: PendingTestSuiteUploadChoice[] = []
    let hasDropHistory = false
    for (let specification of this.specifications) {
      const choice = this.specificationChoiceMap[specification.id]
      let action: Partial<PendingTestSuiteUploadChoice> = {
          specification: specification.id
      }
      if (choice != undefined) {
        if (choice.skipUpdate) {
          action.action = 'cancel'
        } else {
          action.action = 'proceed'
          action.updateActors = choice.updateActors
          action.updateTestSuite = choice.updateTestSuite
          const testCaseActions: PendingTestSuiteUploadChoiceTestCase[] = []
          for (let testCase of choice.testCasesInArchiveAndDB) {
            if (testCase.resetTestHistory) {
              hasDropHistory = true
            }
            testCaseActions.push({
              identifier: testCase.identifier,
              resetTestHistory: testCase.resetTestHistory,
              updateDefinition: testCase.updateDefinition
            })
          }
          action.testCaseUpdates = testCaseActions
        }
      } else {
        action.action = 'proceed'
        action.updateActors = false
        action.updateTestSuite = false
      }
      actions.push(action as PendingTestSuiteUploadChoice)
    }
    const resolve$ = new Observable<boolean>((subscriber) => {
      if (hasDropHistory) {
        this.confirmationDialogService.confirm("Confirm test history reset", "Resetting the testing history will render the selected test cases' existing tests obsolete. Are you sure you want to proceed?", "Yes", "No")
        .subscribe((choice: boolean) => {
          subscriber.next(choice)
          subscriber.complete()
        })
      } else {
        subscriber.next(true)
        subscriber.complete()
      }
    })
    resolve$.subscribe((proceed) => {
      if (proceed) {
        this.proceedWithPendingUpload(actions)
      }
    })
  }

  ignoreValidationWarnings() {
    if (this.uploadResult!.existsForSpecs?.length > 0 || this.uploadResult!.matchingDataExists?.length > 0) {
      this.showSpecificationChoices()
    } else {
      this.proceedWithPendingUpload()
    }
  }

  private proceedWithPendingUpload(actions?: PendingTestSuiteUploadChoice[]) {
    this.actionPending = true
    this.actionProceedPending = true
    this.conformanceService.resolvePendingTestSuite(this.uploadResult!.pendingFolderId!, 'proceed', this.specificationIds(), actions)
    .subscribe((result) => {
      this.uploadResult = result
      if (this.uploadResult.success) {
        this.showUploadResults()
      } else {
        this.showErrorMessage()
      }
    }).add(() => {
      this.actionPending = false
      this.actionProceedPending = false
    })
  }

  proceedVisible() {
    return !(this.step == 'results' || this.step == 'validation' && this.hasValidationErrors)
  }

  proceed() {
    if (this.step == 'initial') {
      this.uploadTestSuite()
    } else if (this.step == 'validation') {
      this.ignoreValidationWarnings()
    } else if (this.step == 'replace') {
      this.applyPendingUpload()
    }
  }

  resolutionNeeded() {
    return (this.step == 'validation' && !this.hasValidationErrors) || (this.step == 'replace')
  }

  refreshNeeded() {
    return this.step == 'results' && this.testSuitesVisible
  }

  close() {
    if (this.resolutionNeeded()) {
      this.conformanceService.resolvePendingTestSuite(this.uploadResult!.pendingFolderId!, 'cancel', this.specificationIds()).subscribe(() => {})
    }
    this.completed.emit(this.refreshNeeded())
    this.modalInstance.hide()
  }

  selectArchive(file: FileData) {
    this.file = file
  }

}
