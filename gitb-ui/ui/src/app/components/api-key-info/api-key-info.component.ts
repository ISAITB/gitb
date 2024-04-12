import { Component, EventEmitter, Input, OnInit } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { Constants } from 'src/app/common/constants';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { OrganisationService } from 'src/app/services/organisation.service';
import { PopupService } from 'src/app/services/popup.service';
import { SystemService } from 'src/app/services/system.service';
import { ApiKeyActorInfo } from 'src/app/types/api-key-actor-info';

import { ApiKeyInfo } from 'src/app/types/api-key-info';
import { ApiKeySpecificationInfo } from 'src/app/types/api-key-specification-info';
import { ApiKeySystemInfo } from 'src/app/types/api-key-system-info';
import { ApiKeyTestCaseInfo } from 'src/app/types/api-key-test-case-info';
import { ApiKeyTestSuiteInfo } from 'src/app/types/api-key-test-suite-info';
import { ConformanceSnapshot } from 'src/app/types/conformance-snapshot';
import { ConformanceSnapshotList } from 'src/app/types/conformance-snapshot-list';

@Component({
  selector: 'app-api-key-info',
  templateUrl: './api-key-info.component.html',
  styleUrls: ['./api-key-info.component.less']
})
export class ApiKeyInfoComponent implements OnInit {

  @Input() organisationName?: string
  @Input() organisationId!: number
  @Input() communityId!: number
  @Input() adminOrganisation!: boolean
  @Input() loadData?: EventEmitter<void>

  apiInfo?: ApiKeyInfo
  conformanceSnapshots?: ConformanceSnapshot[]
  latestSnapshotLabel = Constants.LATEST_CONFORMANCE_STATUS_LABEL
  selectedSnapshot?: ConformanceSnapshot
  selectedSpecification?: ApiKeySpecificationInfo
  selectedActor?: ApiKeyActorInfo
  selectedTestSuite?: ApiKeyTestSuiteInfo
  selectedTestCase?: ApiKeyTestCaseInfo
  selectedSystem?: ApiKeySystemInfo
  organisationUpdatePending = false
  organisationDeletePending = false
  systemUpdatePending: {[key: number]: boolean} = {}
  systemDeletePending: {[key: number]: boolean} = {}
  dataStatus = {status: Constants.STATUS.NONE}

  snapshotKeysLoading = false
  canUpdate = false
  Constants = Constants

  constructor(
    private organisationService: OrganisationService,
    private systemService: SystemService,
    private popupService: PopupService,
    private confirmationDialogService: ConfirmationDialogService,
    private conformanceService: ConformanceService,
    public dataService: DataService
  ) { }

  ngOnInit(): void {
    if (this.loadData == undefined) {
      this.loadApiInfo()
    } else {
      this.loadData.subscribe(() => {
        if (this.apiInfo == undefined) {
          this.loadApiInfo()
        }
      })
    }
    this.canUpdate = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || this.dataService.isVendorAdmin
  }

  snapshotChanged(): void {
    this.snapshotKeysLoading = true
    this.organisationService.getAutomationKeysForOrganisation(this.organisationId, this.selectedSnapshot?.id)
    .subscribe((data) => {
      this.apiInfoLoaded(data)
    }).add(() => {
      this.snapshotKeysLoading = false
    })
  }

  specificationChanged():void {
    this.selectedActor = undefined
    this.selectedTestSuite = undefined
    if (this.selectedSpecification) {
      if (this.selectedSpecification.actors.length > 0) {
        this.selectedActor = this.selectedSpecification.actors[0]
      }
      if (this.selectedSpecification.testSuites.length > 0) {
        this.selectedTestSuite = this.selectedSpecification.testSuites[0]
        this.testSuiteChanged()
      }
    }
  }

  actorChanged():void {}

  testSuiteChanged():void {
    this.selectedTestCase = undefined
    if (this.selectedTestSuite) {
      if (this.selectedTestSuite.testCases.length > 0) {
        this.selectedTestCase = this.selectedTestSuite.testCases[0]
      } 
    }
  }

  testCaseChanged():void {}
  systemChanged():void {}

  newOrganisationKey(): void {
    this.organisationUpdatePending = true
    this.organisationService.updateOrganisationApiKey(this.organisationId)
    .subscribe((newApiKey) => {
      this.apiInfo!.organisation = newApiKey
      this.popupService.success(this.dataService.labelOrganisation()+" API key updated.")
    }).add(() => {
      this.organisationUpdatePending = false
    })
  }

  updateOrganisationKey(): void {
    this.confirmationDialogService.confirmed("Confirm update", "Are you sure you want to update the value for this API key?", "Update", "Cancel")
    .subscribe(() => {
      this.newOrganisationKey()
    })
  }

  newSystemKey(systemId: number): void {
    this.systemUpdatePending[systemId] = true
    this.systemService.updateSystemApiKey(systemId)
    .subscribe((newApiKey) => {
      this.selectedSystem!.key = newApiKey
      this.popupService.success(this.dataService.labelSystem()+" API key updated.")
    }).add(() => {
      this.systemUpdatePending[systemId] = false
    })
  }

  updateSystemKey(systemId: number): void {
    this.confirmationDialogService.confirmed("Confirm update", "Are you sure you want to update the value for this API key?", "Update", "Cancel")
    .subscribe(() => {
      this.newSystemKey(systemId)
    })
  }

  deleteOrganisationKey(): void {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this API key?", "Delete", "Cancel")
    .subscribe(() => {
      this.organisationDeletePending = true
      this.organisationService.deleteOrganisationApiKey(this.organisationId)
      .subscribe(() => {
        this.apiInfo!.organisation = undefined
        this.popupService.success(this.dataService.labelOrganisation()+" API key deleted.")
      }).add(() => {
        this.organisationDeletePending = false
      })
    })
  }

  deleteSystemKey(systemId: number): void {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this API key?", "Delete", "Cancel")
    .subscribe(() => {
      this.systemDeletePending[systemId] = true
      this.systemService.deleteSystemApiKey(systemId)
      .subscribe(() => {
        this.selectedSystem!.key = undefined
        this.popupService.success(this.dataService.labelSystem()+" API key deleted.")
      }).add(() => {
        this.systemDeletePending[systemId] = false
      })
    })
  }

  loadApiInfo() {
    if (this.dataStatus.status == Constants.STATUS.NONE) {
      this.dataStatus.status = Constants.STATUS.PENDING
      let snapshotsLoaded: Observable<ConformanceSnapshotList>
      if (this.adminOrganisation) {
        // Administrator organisations don't get included in snapshots
        snapshotsLoaded = of({snapshots: []})
      } else {
        snapshotsLoaded = this.conformanceService.getConformanceSnapshots(this.communityId, true)
      }
      const apiKeysLoaded = this.organisationService.getAutomationKeysForOrganisation(this.organisationId)
      forkJoin([snapshotsLoaded, apiKeysLoaded]).subscribe((results) => {
        // Snapshots
        this.conformanceSnapshots = results[0].snapshots
        if (results[0].latest) {
          this.latestSnapshotLabel = results[0].latest
        }
        // API keys for latest status
        this.apiInfoLoaded(results[1])
      }).add(() => {
        this.dataStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  private apiInfoLoaded(apiKeyInfo: ApiKeyInfo) {
    this.apiInfo = apiKeyInfo
    this.selectedSystem = undefined
    this.selectedSpecification = undefined
    this.selectedActor = undefined
    this.selectedTestSuite = undefined
    this.selectedTestCase = undefined
    if (this.apiInfo.specifications.length > 0) {
      this.selectedSpecification = this.apiInfo.specifications[0]
      this.specificationChanged()
    }
    if (this.apiInfo.systems.length > 0) {
      this.selectedSystem = this.apiInfo.systems[0]
    }
  }

}
