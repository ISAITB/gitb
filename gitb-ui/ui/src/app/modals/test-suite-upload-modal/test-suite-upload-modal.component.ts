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
import { TestSuiteUploadTestCaseChoice } from './test-suite-upload-test-case-choice';
import { ValidationReport } from './validation-report';
import { find } from 'lodash';
import { PendingTestSuiteUploadChoiceTestCase } from './pending-test-suite-upload-choice-test-case';
import { ValidationReportItem } from './validation-report-item';
import { AssertionReport } from 'src/app/components/diagram/assertion-report';

@Component({
  selector: 'app-test-suite-upload-modal',
  templateUrl: './test-suite-upload-modal.component.html',
  styleUrls: ['./test-suite-upload-modal.component.less']
})
export class TestSuiteUploadModalComponent implements OnInit {

  @Input() testSuitesVisible = false
  @Input() sharedTestSuite = false
  @Input() domainId!: number
  @Input() availableSpecifications: Specification[] = []
  @Output() completed = new EventEmitter<boolean>()

  specifications: Specification[] = []
  actionPending = false
  actionProceedPending = false
  validationReportItems?: AssertionReport[]
  hasValidationWarnings = false
  hasValidationErrors = false
  hasValidationMessages = false
  hasMatchingTestSuite = false
  step = 'initial'
  file?: FileData
  uploadResult?: TestSuiteUploadResult
  report?: ValidationReport
  results?: SpecificationResult[]
  specificationChoices?: SpecificationChoice[]
  specificationChoiceMap: {[key: number]: SpecificationChoice} = {}
  reportItemsCollapsed = false
  specificationsCollapsed = true
  hasChoicesToComplete = false
  hasMultipleChoices = false

  // Shared test suite test cases
  updateTestSuiteMetadata = false
  testCasesInArchiveAndDB: TestSuiteUploadTestCaseChoice[] = []
  testCasesInArchive: TestSuiteUploadTestCaseChoice[] = []
  testCasesInDB: TestSuiteUploadTestCaseChoice[] = []

  constructor(
    public dataService: DataService,
    private modalInstance: BsModalRef,
    private confirmationDialogService: ConfirmationDialogService,
    private conformanceService: ConformanceService,
    private popupService: PopupService,
    private errorService: ErrorService
  ) { }

  ngOnInit(): void {
    if (!this.sharedTestSuite) {
      if (this.availableSpecifications.length == 1) {
        this.specifications.push(this.availableSpecifications[0])
      }
    }
  }

  proceedDisabled() {
    return this.hasValidationErrors || this.actionPending || this.actionProceedPending || !(this.file != undefined && (this.specifications.length > 0 || this.sharedTestSuite)) || (this.step == 'replace' && (this.specificationChoices != undefined && this.specificationChoices.length > 0 && !this.hasChoicesToComplete))
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
    if (this.uploadResult?.validationReport?.reports) {
      this.validationReportItems = this.toAssertionReports(this.uploadResult.validationReport.reports)
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

  showUploadResults() {
    this.popupService.success('Test suite uploaded.')
    this.close(true, this.refreshNeeded())
  }

  showSpecificationChoices() {
    this.step = 'replace'
    this.hasMatchingTestSuite = this.sharedTestSuite && this.uploadResult?.sharedTestSuiteId != undefined
    const existsMap: {[key: number]: {id: number, shared: boolean}} = {}
    this.specificationChoices = []
    this.specificationChoiceMap = {}
    if (this.uploadResult?.existsForSpecs) {
      for (let existSpec of this.uploadResult.existsForSpecs) {
        existsMap[existSpec.id] = existSpec
        this.hasMatchingTestSuite = true
      }    
    }
    // Shared test suite choices.
    if (this.sharedTestSuite) {
      if (this.uploadResult?.sharedTestCases) {
        const testCasesInArchiveAndDB: TestSuiteUploadTestCaseChoice[] = []
        const testCasesInArchive: TestSuiteUploadTestCaseChoice[] = []
        const testCasesInDB: TestSuiteUploadTestCaseChoice[] = []
        for (let testCase of this.uploadResult.sharedTestCases) {
          const testCaseInfo = {
            identifier: testCase.identifier,
            name: testCase.name,
            updateDefinition: testCase.updateMetadata,
            resetTestHistory: testCase.resetTestHistory
          }
          if (testCase.status == Constants.TEST_CASE_UPLOAD_MATCH.IN_ARCHIVE_AND_DB) {
            testCasesInArchiveAndDB.push(testCaseInfo)
          } else if (testCase.status == Constants.TEST_CASE_UPLOAD_MATCH.IN_ARCHIVE_ONLY) {
            testCasesInArchive.push(testCaseInfo)
          } else if (testCase.status == Constants.TEST_CASE_UPLOAD_MATCH.IN_DB_ONLY) {
            testCasesInDB.push(testCaseInfo)
          }
        }
        this.testCasesInArchiveAndDB = testCasesInArchiveAndDB
        this.testCasesInArchive = testCasesInArchive
        this.testCasesInDB = testCasesInDB
      }
      for (let spec of this.availableSpecifications) {
        const existingTestSuite = existsMap[spec.id] != undefined
        if (existingTestSuite) {
          const specData: SpecificationChoice = {
            specification: spec.id,
            name: spec.fname,
            updateActors: this.uploadResult!.updateSpecification,
            sharedTestSuite: true,
            updateTestSuite: this.uploadResult!.updateMetadata,
            skipUpdate: false,
            dataExists: true,
            testSuiteExists: existingTestSuite,
            testCasesInArchiveAndDB: [],
            testCasesInArchive: [],
            testCasesInDB: []
          }
          this.specificationChoices.push(specData)
          this.specificationChoiceMap[spec.id] = specData        
        }
      }
      this.updateTestSuiteMetadata = this.uploadResult!.updateMetadata
    } else {
      // Specification related choices
      const matchingDataMap: {[key: number]: number} = {}
      for (let matchingSpec of this.uploadResult!.matchingDataExists) {
        matchingDataMap[matchingSpec] = matchingSpec
      }
      for (let spec of this.specifications) {
        const existingData = matchingDataMap[spec.id] !=  undefined
        const existingTestSuite = existsMap[spec.id] != undefined
        const existingTestSuiteIsShared = existingTestSuite && existsMap[spec.id].shared
        if (existingData || existingTestSuite) {
          const testCasesInArchiveAndDB: TestSuiteUploadTestCaseChoice[] = []
          const testCasesInArchive: TestSuiteUploadTestCaseChoice[] = []
          const testCasesInDB: TestSuiteUploadTestCaseChoice[] = []
          const matchingSpecification = find(this.uploadResult!.testCases, (resultingSpec) => {
            return resultingSpec.specification == spec.id
          })
          if (matchingSpecification) {
            for (let testCase of matchingSpecification.testCases) {
              const testCaseInfo = {
                identifier: testCase.identifier,
                name: testCase.name,
                updateDefinition: testCase.updateMetadata,
                resetTestHistory: testCase.resetTestHistory
              }
              if (testCase.status == Constants.TEST_CASE_UPLOAD_MATCH.IN_ARCHIVE_AND_DB) {
                testCasesInArchiveAndDB.push(testCaseInfo)
              } else if (testCase.status == Constants.TEST_CASE_UPLOAD_MATCH.IN_ARCHIVE_ONLY) {
                testCasesInArchive.push(testCaseInfo)
              } else if (testCase.status == Constants.TEST_CASE_UPLOAD_MATCH.IN_DB_ONLY) {
                testCasesInDB.push(testCaseInfo)
              }
            }
          }
          const specData: SpecificationChoice = {
            specification: spec.id,
            name: spec.fname,
            updateActors: this.uploadResult!.updateSpecification,
            sharedTestSuite: existingTestSuiteIsShared,
            updateTestSuite: this.uploadResult!.updateMetadata,
            skipUpdate: false,
            dataExists: existingData,
            testSuiteExists: existingTestSuite,
            testCasesInArchiveAndDB: testCasesInArchiveAndDB,
            testCasesInArchive: testCasesInArchive,
            testCasesInDB: testCasesInDB
          }
          this.specificationChoices.push(specData)
          this.specificationChoiceMap[spec.id] = specData
        }
      }
    }
    this.hasMultipleChoices = this.specificationChoices.length > 1
  }

  specificationIds() {
    return this.specifications.map((s) => s.id)
  }

  uploadTestSuite() {
    this.actionPending = true
    this.actionProceedPending = true
    this.conformanceService.deployTestSuite(this.domainId, this.specificationIds(), this.sharedTestSuite, this.file!.file!)
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
    if (this.sharedTestSuite) {
      // Overall choices for the test suite
      const testCaseActions: PendingTestSuiteUploadChoiceTestCase[] = []
      for (let testCase of this.testCasesInArchiveAndDB) {
        if (testCase.resetTestHistory) {
          hasDropHistory = true
        }
        testCaseActions.push({
          identifier: testCase.identifier,
          resetTestHistory: testCase.resetTestHistory,
          updateDefinition: testCase.updateDefinition
        })
      }
      actions.push({
        action: 'proceed',
        updateActors: false,
        updateTestSuite: this.updateTestSuiteMetadata,
        sharedTestSuite: true,
        testCaseUpdates: testCaseActions
      })
      // Choices for already linked specifications
      if (this.specificationChoices) {
        for (let choice of this.specificationChoices) {
          let action: PendingTestSuiteUploadChoice = {
            specification: choice.specification,
            action: 'proceed',
            updateActors: choice.updateActors,
            updateTestSuite: false,
            sharedTestSuite: true,
            testCaseUpdates: []          
          }
          actions.push(action as PendingTestSuiteUploadChoice)
        }
      }
    } else {
      // Specification-level choices
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
            action.sharedTestSuite = this.sharedTestSuite
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
    }
    const resolve$ = new Observable<boolean>((subscriber) => {
      if (hasDropHistory) {
        this.confirmationDialogService.confirmDangerous("Confirm test history reset", "Resetting the testing history will render the selected test cases' existing tests obsolete. Are you sure you want to proceed?", "Reset history", "Cancel")
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
    this.conformanceService.resolvePendingTestSuite(this.uploadResult!.pendingFolderId!, 'proceed', this.domainId, this.specificationIds(), actions)
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
    return this.testSuitesVisible || this.sharedTestSuite
  }

  close(finalStep?: boolean, refreshNeeded?: boolean) {
    if (!finalStep && this.resolutionNeeded() && this.uploadResult?.pendingFolderId != undefined) {
      this.conformanceService.resolvePendingTestSuite(this.uploadResult.pendingFolderId, 'cancel', this.domainId, this.specificationIds()).subscribe(() => {})
    }
    this.completed.emit(refreshNeeded)
    this.modalInstance.hide()
  }

  selectArchive(file: FileData) {
    this.file = file
  }

  choiceUpdate(hasPendingChoices: boolean) {
    this.hasChoicesToComplete = hasPendingChoices
  }

  toAssertionReports(items: ValidationReportItem[]): AssertionReport[] {
    return items.map((item) => {
      return {
        type: item.level,
        value: {
          description: item.description,
          location: item.location
        }
      }
    })
  }
}
