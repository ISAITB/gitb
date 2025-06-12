import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { Specification } from 'src/app/types/specification';
import { PendingTestSuiteUploadChoice } from '../test-suite-upload-modal/pending-test-suite-upload-choice';
import { SpecificationChoice } from '../test-suite-upload-modal/specification-choice';
import { TestSuiteUploadResult } from '../test-suite-upload-modal/test-suite-upload-result';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { of } from 'rxjs';
import { MultiSelectConfig } from 'src/app/components/multi-select-filter/multi-select-config';
import { FilterUpdate } from 'src/app/components/test-filter/filter-update';

@Component({
    selector: 'app-link-shared-test-suite-modal',
    templateUrl: './link-shared-test-suite-modal.component.html',
    styleUrls: ['./link-shared-test-suite-modal.component.less'],
    standalone: false
})
export class LinkSharedTestSuiteModalComponent extends BaseComponent implements OnInit {

  @Input() domainId!: number
  @Input() testSuiteId!: number
  @Input() availableSpecifications: Specification[] = []
  @Input() step: 'select'|'confirm'|'results' = 'select'
  @Input() uploadResult?: TestSuiteUploadResult
  @Output() completed = new EventEmitter<boolean>()

  actionPending = false
  specifications: Specification[] = []
  specificationChoices?: SpecificationChoice[]
  specificationChoiceMap: {[key: number]: SpecificationChoice} = {}
  hasMultipleChoices = false
  skipCount = 0
  hasChoicesToComplete = false
  selectConfig!: MultiSelectConfig<Specification>

  constructor(
    public dataService: DataService,
    private modalInstance: BsModalRef,
    private conformanceService: ConformanceService,
    private popupService: PopupService
  ) { super() }

  ngOnInit(): void {
    if (this.availableSpecifications.length == 1) {
      this.specifications.push(this.availableSpecifications[0])
    }
    this.selectConfig = {
      name: "specification",
      textField: "fname",
      clearItems: new EventEmitter<void>(),
      replaceItems: new EventEmitter<Specification[]>(),
      replaceSelectedItems: new EventEmitter<Specification[]>(),
      showAsFormControl: true,
      filterLabel: `Select ${this.dataService.labelSpecificationsLower()}...`,
      loader: () => of(this.availableSpecifications)
    }
    if (this.step == 'confirm') {
      // We are requested only to show the result of a link action.
      this.showSpecificationChoices()
    }
  }

  specificationsChanged(update: FilterUpdate<Specification>) {
    this.specifications = update.values.active
  }

  proceedDisabled() {
    return this.actionPending || this.specifications.length == 0 || (this.step == 'confirm' && !this.hasChoicesToComplete)
  }

  proceed() {
    if (this.step == 'select') {
      this.linkSpecifications()
    } else if (this.step == 'confirm') {
      this.confirmSpecificationLink()
    }
  }

  showSpecificationChoices() {
    this.step = 'confirm'
    const existsMap: {[key: number]: number} = {}
    for (let existSpec of this.uploadResult!.existsForSpecs) {
      existsMap[existSpec.id] = existSpec.id
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
          name: spec.fname,
          updateActors: false,
          sharedTestSuite: !existingTestSuite,
          updateTestSuite: false,
          skipUpdate: false,
          dataExists: existingData,
          testSuiteExists: existingTestSuite,
          testCasesInArchiveAndDB: [],
          testCasesInArchive: [],
          testCasesInDB: []
        }
        if (existingTestSuite) {
          this.skipCount += 1
        }
        this.specificationChoices.push(specData)
        this.specificationChoiceMap[spec.id] = specData
      }
    }
    this.hasMultipleChoices = this.specificationChoices.length > 1
    this.hasChoicesToComplete = this.specifications.length > this.skipCount
  }

  showUploadResults() {
    this.popupService.success('Test suite linked to '+this.dataService.labelSpecificationsLower()+'.')
    this.close(true)
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
    this.addAlertError(msg)
  }

  linkSpecifications() {
    this.actionPending = true
    this.clearAlerts()
    this.conformanceService.linkSharedTestSuite(this.testSuiteId, this.specifications.map((x => x.id)))
    .subscribe((result) => {
      this.uploadResult = result
      if (this.uploadResult.needsConfirmation) {
        this.showSpecificationChoices()
      } else if (this.uploadResult.success) {
        this.showUploadResults()
      } else {
        this.showErrorMessage()
      }
    }).add(() => {
      this.actionPending = false
    })
  }

  confirmSpecificationLink() {
    this.actionPending = true
    const actions: PendingTestSuiteUploadChoice[] = []
    for (let specification of this.specifications) {
      const choice = this.specificationChoiceMap[specification.id]
      let action: Partial<PendingTestSuiteUploadChoice> = {
        specification: specification.id
      }
      if (choice != undefined) {
        if (choice.skipUpdate || choice.testSuiteExists) {
          action.action = 'cancel'
        } else {
          action.action = 'proceed'
          action.updateActors = choice.updateActors
          action.updateTestSuite = false
          action.sharedTestSuite = false
          action.testCaseUpdates = []
        }
      } else {
        action.action = 'proceed'
        action.updateActors = false
        action.updateTestSuite = false
      }
      actions.push(action as PendingTestSuiteUploadChoice)
    }
    this.conformanceService.confirmLinkSharedTestSuite(this.testSuiteId, this.specifications.map((s) => s.id), actions)
    .subscribe((results) => {
      this.uploadResult = results
      this.showUploadResults()
    }).add(() => {
      this.actionPending = false
    })
  }

  close(refreshNeeded?: boolean) {
    this.completed.emit(refreshNeeded)
    this.modalInstance.hide()
  }

}
