import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { Observable } from 'rxjs';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { ErrorService } from 'src/app/services/error.service';
import { PopupService } from 'src/app/services/popup.service';
import { ErrorData } from 'src/app/types/error-data.type';
import { Specification } from 'src/app/types/specification';
import { PendingTestSuiteUploadChoice } from './pending-test-suite-upload-choice';
import { SpecificationChoice } from './specification-choice';
import { SpecificationResult } from './specification-result';
import { TestSuiteUploadResult } from './test-suite-upload-result';
import { ValidationReport } from './validation-report';

@Component({
  selector: 'app-test-suite-upload-modal',
  templateUrl: './test-suite-upload-modal.component.html',
  styles: [
  ]
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
  file?: File
  uploadResult?: TestSuiteUploadResult
  report?: ValidationReport
  results?: SpecificationResult[]
  specificationChoices?: SpecificationChoice[]
  specificationChoiceMap: {[key: number]: SpecificationChoice} = {}

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
        const specData: SpecificationChoice = {
          specification: spec.id,
          history: 'keep',
          metadata: 'skip',
          skipUpdate: false,
          dataExists: existingData,
          testSuiteExists: existingTestSuite
        }
        this.specificationChoices.push(specData)
        this.specificationChoiceMap[spec.id] = specData
      }
    }
    this.hasMultipleChoices = this.specificationChoices.length > 1
  }

  specificationIds() {
    return this.specifications.map((s) => s.id)
  }

  applyChoiceToAll(specification: number) {
    const reference = this.specificationChoiceMap[specification]
    for (let choice of this.specificationChoices!) {
      if (choice.specification != specification) {
        choice.metadata = reference.metadata
        if (choice.testSuiteExists) {
          choice.history = reference.history
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
    this.conformanceService.deployTestSuite(this.specificationIds(), this.file!)
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
          if (choice.history == 'drop') {
            hasDropHistory = true
          }
          action.action = 'proceed'
          action.pending_action_history = choice.history
          action.pending_action_metadata = choice.metadata
        }
      } else {
        action.action = 'proceed'
        action.pending_action_history = 'keep'
        action.pending_action_metadata = 'update'
      }
      actions.push(action as PendingTestSuiteUploadChoice)
    }
    const resolve$ = new Observable<boolean>((subscriber) => {
      if (hasDropHistory) {
        this.confirmationDialogService.confirm("Confirm test history deletion", "Dropping existing results will render this test suite's prior tests obsolete. Are you sure you want to proceed?", "Yes", "No")
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

  selectArchive(file: File) {
    this.file = file
  }

}
