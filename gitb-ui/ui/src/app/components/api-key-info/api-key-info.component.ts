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
import {ApiKeySpecificationInfo} from '../../types/api-key-specification-info';

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
  assignedSpecificationSelectionConfig!: MultiSelectConfig<ApiKeySystemInfo>
  assignedActorSelectionConfig!: MultiSelectConfig<ApiKeyActorInfo>
  assignedTestSuiteSelectionConfig!: MultiSelectConfig<ApiKeyTestSuiteInfo>
  assignedTestCaseSelectionConfig!: MultiSelectConfig<ApiKeyTestCaseInfo>

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
    this.assignedSpecificationSelectionConfig = {
      name: "selectedAssignedSpecification",
      singleSelection: true,
      singleSelectionPersistent: true,
      showAsFormControl: true,
      filterLabel: `Select ${this.dataService.labelSpecificationLower()}...`,
      textField: "name",
      loader: () => of(this.state.assignedSpecifications!)
    }
    this.assignedActorSelectionConfig = {
      name: "selectedAssignedActor",
      singleSelection: true,
      singleSelectionPersistent: true,
      showAsFormControl: true,
      filterLabel: `Select ${this.dataService.labelActorLower()}...`,
      textField: "name",
      loader: () => of(this.state.selectedAssignedSpecification!.actors)
    }
    this.assignedTestSuiteSelectionConfig = {
      name: "selectedAssignedTestSuite",
      singleSelection: true,
      singleSelectionPersistent: true,
      showAsFormControl: true,
      filterLabel: `Select test suite...`,
      textField: "name",
      loader: () => of(this.state.selectedAssignedActor!.testSuites)
    }
    this.assignedTestCaseSelectionConfig = {
      name: "selectedAssignedTestCase",
      singleSelection: true,
      singleSelectionPersistent: true,
      showAsFormControl: true,
      filterLabel: `Select test case...`,
      textField: "name",
      loader: () => of(this.state.selectedAssignedTestSuite!.testCases)
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

  systemChanged():void {
    this.state.selectedAssignedSpecification = undefined
    this.state.assignedSpecifications = []
    const assignedActorKeys = new Set<string>()
    if (this.state.apiInfo?.statements) {
      this.state.apiInfo.statements
        .filter(statement => statement.system === this.state.selectedSystem?.id)
        .flatMap(statement => statement.actors)
        .forEach((key) => assignedActorKeys.add(key))
    }
    if (this.state.apiInfo?.specifications) {
      this.state.apiInfo.specifications.forEach(spec => {
        const assignedSpecEntry: ApiKeySpecificationInfo = { id: spec.id, name: spec.name, actors: [] }
        if (spec.actors) {
          spec.actors.forEach(actor => {
            if (assignedActorKeys.has(actor.key)) {
              assignedSpecEntry.actors.push(actor)
            }
          })
        }
        if (assignedSpecEntry.actors.length > 0) this.state.assignedSpecifications!.push(assignedSpecEntry)
      })
    }
    if (this.state.assignedSpecifications.length > 0) this.state.selectedAssignedSpecification = this.state.assignedSpecifications[0]
    this.assignedSpecificationChanged()
  }

  assignedSpecificationChanged():void {
    this.state.selectedAssignedActor = undefined
    if (this.state.selectedAssignedSpecification) {
      if (this.state.selectedAssignedSpecification.actors.length > 0) {
        this.state.selectedAssignedActor = this.state.selectedAssignedSpecification.actors[0]
      }
    }
    this.assignedActorChanged()
  }

  assignedActorChanged():void {
    this.state.selectedAssignedTestSuite = undefined
    if (this.state.selectedAssignedActor) {
      if (this.state.selectedAssignedActor.testSuites.length > 0) {
        this.state.selectedAssignedTestSuite = this.state.selectedAssignedActor.testSuites[0]
      }
    }
    this.assignedTestSuiteChanged()
  }

  assignedTestSuiteChanged():void {
    this.state.selectedAssignedTestCase = undefined
    if (this.state.selectedAssignedTestSuite) {
      if (this.state.selectedAssignedTestSuite.testCases.length > 0) {
        this.state.selectedAssignedTestCase = this.state.selectedAssignedTestSuite.testCases[0]
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
    this.state.selectedAssignedSpecification = undefined
    this.state.selectedAssignedActor = undefined
    this.state.selectedAssignedTestSuite = undefined
    this.state.selectedAssignedTestCase = undefined
    if (this.state.apiInfo.systems.length > 0) {
      this.state.selectedSystem = this.state.apiInfo.systems[0]
    }
    this.systemChanged()
  }

}
