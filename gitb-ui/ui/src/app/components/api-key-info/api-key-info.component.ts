/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import {AfterViewInit, Component, Input, OnInit} from '@angular/core';
import {forkJoin, Observable, of} from 'rxjs';
import {Constants} from 'src/app/common/constants';
import {ConfirmationDialogService} from 'src/app/services/confirmation-dialog.service';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {OrganisationService} from 'src/app/services/organisation.service';
import {PopupService} from 'src/app/services/popup.service';
import {SystemService} from 'src/app/services/system.service';
import {ApiKeyActorInfo} from 'src/app/types/api-key-actor-info';

import {ApiKeyInfo} from 'src/app/types/api-key-info';
import {ApiKeySystemInfo} from 'src/app/types/api-key-system-info';
import {ApiKeyTestCaseInfo} from 'src/app/types/api-key-test-case-info';
import {ApiKeyTestSuiteInfo} from 'src/app/types/api-key-test-suite-info';
import {ConformanceSnapshot} from 'src/app/types/conformance-snapshot';
import {ConformanceSnapshotList} from 'src/app/types/conformance-snapshot-list';
import {MultiSelectConfig} from '../multi-select-filter/multi-select-config';
import {ApiKeyInfoState} from './api-key-info-state';

@Component({
    selector: 'app-api-key-info',
    templateUrl: './api-key-info.component.html',
    styleUrls: ['./api-key-info.component.less'],
    standalone: false
})
export class ApiKeyInfoComponent implements OnInit, AfterViewInit {

  @Input() state!: ApiKeyInfoState

  latestSnapshotLabel = Constants.LATEST_CONFORMANCE_STATUS_LABEL
  organisationUpdatePending = false
  organisationDeletePending = false
  systemUpdatePending: {[key: number]: boolean} = {}

  snapshotKeysLoading = false
  canUpdate = false
  Constants = Constants

  snapshotSelectionConfig!: MultiSelectConfig<ConformanceSnapshot>
  systemSelectionConfig!: MultiSelectConfig<ApiKeySystemInfo>
  specificationSelectionConfig!: MultiSelectConfig<ApiKeySystemInfo>
  actorSelectionConfig!: MultiSelectConfig<ApiKeyActorInfo>
  testSuiteSelectionConfig!: MultiSelectConfig<ApiKeyTestSuiteInfo>
  testCaseSelectionConfig!: MultiSelectConfig<ApiKeyTestCaseInfo>

  constructor(
    private readonly organisationService: OrganisationService,
    private readonly systemService: SystemService,
    private readonly popupService: PopupService,
    private readonly confirmationDialogService: ConfirmationDialogService,
    private readonly conformanceService: ConformanceService,
    public readonly dataService: DataService
  ) { }

  ngOnInit(): void {
    this.snapshotSelectionConfig = {
      name: "selectedSnapshot",
      singleSelection: true,
      singleSelectionPersistent: true,
      singleSelectionClearable: true,
      showAsFormControl: true,
      filterLabel: this.latestSnapshotLabel,
      textField: "label",
      loader: () => of(this.state.conformanceSnapshots!)
    }
    this.systemSelectionConfig = {
      name: "selectedSystem",
      singleSelection: true,
      singleSelectionPersistent: true,
      showAsFormControl: true,
      filterLabel: `Select ${this.dataService.labelSystemLower()}...`,
      textField: "name",
      loader: () => of(this.state.apiInfo?.systems!)
    }
    this.specificationSelectionConfig = {
      name: "selectedSpecification",
      singleSelection: true,
      singleSelectionPersistent: true,
      showAsFormControl: true,
      filterLabel: `Select ${this.dataService.labelSpecificationLower()}...`,
      textField: "name",
      loader: () => of(this.state.apiInfo?.specifications!)
    }
    this.actorSelectionConfig = {
      name: "selectedActor",
      singleSelection: true,
      singleSelectionPersistent: true,
      showAsFormControl: true,
      filterLabel: `Select ${this.dataService.labelActorLower()}...`,
      textField: "name",
      loader: () => of(this.state.selectedSpecification!.actors)
    }
    this.testSuiteSelectionConfig = {
      name: "selectedTestSuite",
      singleSelection: true,
      singleSelectionPersistent: true,
      showAsFormControl: true,
      filterLabel: `Select test suite...`,
      textField: "name",
      loader: () => of(this.state.selectedSpecification!.testSuites)
    }
    this.testCaseSelectionConfig = {
      name: "selectedTestCase",
      singleSelection: true,
      singleSelectionPersistent: true,
      showAsFormControl: true,
      filterLabel: `Select test case...`,
      textField: "name",
      loader: () => of(this.state.selectedTestSuite!.testCases)
    }
    this.canUpdate = this.dataService.isSystemAdmin || this.dataService.isCommunityAdmin || this.dataService.isVendorAdmin
  }

  ngAfterViewInit(): void {
    if (this.state.apiInfo == undefined) {
      this.loadApiInfo()
    } else {
      this.snapshotSelectionConfig.eventsDisabled = true
      setTimeout(() => {
        // Events should fire after view rendering to avoid reloading when changing tabs.
        this.snapshotSelectionConfig.eventsDisabled = false
      })
    }
  }

  snapshotChanged(): void {
    this.snapshotKeysLoading = true
    this.organisationService.getAutomationKeysForOrganisation(this.state.organisationId, this.state.selectedSnapshot?.id)
    .subscribe((data) => {
      this.apiInfoLoaded(data)
    }).add(() => {
      this.snapshotKeysLoading = false
    })
  }

  specificationChanged():void {
    this.state.selectedActor = undefined
    this.state.selectedTestSuite = undefined
    if (this.state.selectedSpecification) {
      if (this.state.selectedSpecification.actors.length > 0) {
        this.state.selectedActor = this.state.selectedSpecification.actors[0]
      }
      if (this.state.selectedSpecification.testSuites.length > 0) {
        this.state.selectedTestSuite = this.state.selectedSpecification.testSuites[0]
        this.testSuiteChanged()
      }
    }
  }

  testSuiteChanged():void {
    this.state.selectedTestCase = undefined
    if (this.state.selectedTestSuite) {
      if (this.state.selectedTestSuite.testCases.length > 0) {
        this.state.selectedTestCase = this.state.selectedTestSuite.testCases[0]
      }
    }
  }

  newOrganisationKey(): void {
    this.organisationUpdatePending = true
    this.organisationService.updateOrganisationApiKey(this.state.organisationId)
    .subscribe((newApiKey) => {
      this.state.apiInfo!.organisation = newApiKey
      this.popupService.success(this.dataService.labelOrganisation()+" API key updated.")
    }).add(() => {
      this.organisationUpdatePending = false
    })
  }

  updateOrganisationKey(): void {
    this.confirmationDialogService.confirmed("Confirm update", "Are you sure you want to update the value for this API key?", "Update", "Cancel", Constants.BUTTON_ICON.RESET)
    .subscribe(() => {
      this.newOrganisationKey()
    })
  }

  newSystemKey(systemId: number): void {
    this.systemUpdatePending[systemId] = true
    this.systemService.updateSystemApiKey(systemId)
    .subscribe((newApiKey) => {
      this.state.selectedSystem!.key = newApiKey
      this.popupService.success(this.dataService.labelSystem()+" API key updated.")
    }).add(() => {
      this.systemUpdatePending[systemId] = false
    })
  }

  updateSystemKey(systemId: number): void {
    this.confirmationDialogService.confirmed("Confirm update", "Are you sure you want to update the value for this API key?", "Update", "Cancel", Constants.BUTTON_ICON.RESET)
    .subscribe(() => {
      this.newSystemKey(systemId)
    })
  }

  deleteOrganisationKey(): void {
    this.confirmationDialogService.confirmedDangerous("Confirm delete", "Are you sure you want to delete this API key?", "Delete", "Cancel", Constants.BUTTON_ICON.DELETE)
    .subscribe(() => {
      this.organisationDeletePending = true
      this.organisationService.deleteOrganisationApiKey(this.state.organisationId)
      .subscribe(() => {
        this.state.apiInfo!.organisation = undefined
        this.popupService.success(this.dataService.labelOrganisation()+" API key deleted.")
      }).add(() => {
        this.organisationDeletePending = false
      })
    })
  }

  loadApiInfo() {
    if (this.state.status == Constants.STATUS.NONE) {
      this.state.status = Constants.STATUS.PENDING
      let snapshotsLoaded: Observable<ConformanceSnapshotList>
      if (this.state.adminOrganisation) {
        // Administrator organisations don't get included in snapshots
        snapshotsLoaded = of({snapshots: []})
      } else {
        snapshotsLoaded = this.conformanceService.getConformanceSnapshots(this.state.communityId, true)
      }
      const apiKeysLoaded = this.organisationService.getAutomationKeysForOrganisation(this.state.organisationId)
      forkJoin([snapshotsLoaded, apiKeysLoaded]).subscribe((results) => {
        // Snapshots
        if (results[0].snapshots == undefined) {
          this.state.conformanceSnapshots = []
        } else {
          this.state.conformanceSnapshots = results[0].snapshots
        }
        if (results[0].latest) {
          this.latestSnapshotLabel = results[0].latest
        }
        // API keys for latest status
        this.apiInfoLoaded(results[1])
      }).add(() => {
        this.state.status = Constants.STATUS.FINISHED
      })
    }
  }

  private apiInfoLoaded(apiKeyInfo: ApiKeyInfo) {
    this.state.apiInfo = apiKeyInfo
    this.state.selectedSystem = undefined
    this.state.selectedSpecification = undefined
    this.state.selectedActor = undefined
    this.state.selectedTestSuite = undefined
    this.state.selectedTestCase = undefined
    if (this.state.apiInfo.specifications.length > 0) {
      this.state.selectedSpecification = this.state.apiInfo.specifications[0]
      this.specificationChanged()
    }
    if (this.state.apiInfo.systems.length > 0) {
      this.state.selectedSystem = this.state.apiInfo.systems[0]
    }
  }

}
