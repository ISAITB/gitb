/*
 * Copyright (C) 2025 European Union
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

import {Component, EventEmitter, NgZone, OnInit} from '@angular/core';
import {BsModalService} from 'ngx-bootstrap/modal';
import {Observable, of} from 'rxjs';
import {Constants} from 'src/app/common/constants';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {RoutingService} from 'src/app/services/routing.service';
import {ConformanceCertificateSettings} from 'src/app/types/conformance-certificate-settings';
import {ConformanceSnapshotsModalComponent} from 'src/app/modals/conformance-snapshots-modal/conformance-snapshots-modal.component';
import {Community} from 'src/app/types/community';
import {Organisation} from 'src/app/types/organisation.type';
import {System} from 'src/app/types/system';
import {CommunityService} from 'src/app/services/community.service';
import {OrganisationService} from 'src/app/services/organisation.service';
import {SystemService} from 'src/app/services/system.service';
import {ConformanceStatementItem} from 'src/app/types/conformance-statement-item';
import {ExportReportEvent} from 'src/app/types/export-report-event';
import {ReportSupportService} from 'src/app/services/report-support.service';
import {find} from 'lodash';
import {MultiSelectConfig} from 'src/app/components/multi-select-filter/multi-select-config';
import {FilterUpdate} from 'src/app/components/test-filter/filter-update';
import {
  BaseConformanceItemDisplayComponent
} from 'src/app/components/base-conformance-item-display/base-conformance-item-display.component';
import {PagingEvent} from '../../../components/paging-controls/paging-event';
import {CheckboxOptionState} from '../../../components/checkbox-option-panel/checkbox-option-state';

@Component({
    selector: 'app-conformance-dashboard',
    templateUrl: './conformance-dashboard.component.html',
    styleUrls: ['./conformance-dashboard.component.less'],
    standalone: false
})
export class ConformanceDashboardComponent extends BaseConformanceItemDisplayComponent implements OnInit {

  selectedCommunityId?: number
  selectedOrganisationId?: number
  selectedSystemId?: number
  settings?: Partial<ConformanceCertificateSettings>
  availableCommunities?: Community[]
  availableOrganisations?: Organisation[]
  availableSystems?: System[]
  exportOverviewPending = false
  communitySelectConfig?: MultiSelectConfig<Community>
  organisationSelectConfig?: MultiSelectConfig<Organisation>
  systemSelectConfig?: MultiSelectConfig<System>

  constructor(
    dataService: DataService,
    zone: NgZone,
    conformanceService: ConformanceService,
    private readonly modalService: BsModalService,
    private readonly routingService: RoutingService,
    private readonly communityService: CommunityService,
    private readonly organisationService: OrganisationService,
    private readonly systemService: SystemService,
    private readonly reportSupportService: ReportSupportService,
  ) { super(dataService, zone, conformanceService) }

  ngOnInit(): void {
    this.latestSnapshotButtonLabel = 'Latest conformance status'
    this.snapshotButtonLabel = this.latestSnapshotButtonLabel
		if (!this.dataService.isSystemAdmin) {
			this.communityId = this.dataService.community!.id
      this.selectedCommunityId = this.communityId
    }
    // Tree view selection configs - start
    if (this.dataService.isSystemAdmin) {
      this.communitySelectConfig = {
        name: 'availableCommunities',
        textField: 'fname',
        filterLabel: 'Select community',
        singleSelection: true,
        singleSelectionPersistent: true,
        clearItems: new EventEmitter<void>(),
        replaceItems: new EventEmitter<Community[]>(),
        replaceSelectedItems: new EventEmitter<Community[]>()
      }
    }
    this.organisationSelectConfig = {
      name: 'availableOrganisations',
      textField: 'fname',
      filterLabel: `Select ${this.dataService.labelOrganisationLower()}`,
      singleSelection: true,
      singleSelectionPersistent: true,
      clearItems: new EventEmitter<void>(),
      replaceItems: new EventEmitter<Organisation[]>(),
      replaceSelectedItems: new EventEmitter<Organisation[]>()
    }
    this.systemSelectConfig = {
      name: 'availableSystems',
      textField: 'fname',
      filterLabel: `Select ${this.dataService.labelSystemLower()}`,
      singleSelection: true,
      singleSelectionPersistent: true,
      clearItems: new EventEmitter<void>(),
      replaceItems: new EventEmitter<System[]>(),
      replaceSelectedItems: new EventEmitter<System[]>()
    }
    // Tree view select configs - end
    this.routingService.conformanceDashboardBreadcrumbs()
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();
    this.viewTypeToggled()
  }

  private countTestCases(item: ConformanceStatementItem): number {
    if (item.results) {
      return item.results.completed + item.results.failed + item.results.undefined
    } else if (item.items) {
      let total = 0
      item.items.forEach(item => {
        total += this.countTestCases(item)
      })
      return total
    } else {
      return 0
    }
  }

  onExportConformanceItem(event: ExportReportEvent) {
    if (event.format == 'xml') {
      event.item.exportXmlPending = true
    } else {
      event.item.exportPdfPending = true
    }
    let reportObservable: Observable<any>
    if (event.statementReport) {
      const testCaseCount = this.countTestCases(event.item)
      reportObservable = this.reportSupportService.handleConformanceStatementReport(this.selectedCommunityId!, event.actorId!, this.selectedSystemId!, this.activeConformanceSnapshot?.id, event.format, true, testCaseCount)
    } else {
      const reportLevel = this.determineReportLevel(event)
      reportObservable = this.reportSupportService.handleConformanceOverviewReport(this.selectedCommunityId!, this.selectedSystemId!, event.item.id, reportLevel, this.activeConformanceSnapshot?.id, event.format, this.dataService.conformanceStatusForConformanceItem(event.item))
    }
    reportObservable.subscribe(() => {
      // Do nothing further
    }).add(() => {
      if (event.format == 'xml') {
        event.item.exportXmlPending = false
      } else {
        event.item.exportPdfPending = false
      }
    })
  }

  manageConformanceSnapshots() {
    if (this.selectedCommunityId != undefined) {
      const modalRef = this.modalService.show(ConformanceSnapshotsModalComponent, {
        class: 'modal-lg',
        initialState: {
          communityId: this.selectedCommunityId!,
          currentlySelectedSnapshot: this.activeConformanceSnapshot?.id
        }
      })
      modalRef.content!.select.subscribe((selectedSnapshot) => {
        if (selectedSnapshot) {
          this.snapshotButtonLabel = selectedSnapshot.label
          if (this.activeConformanceSnapshot == undefined || this.activeConformanceSnapshot.id != selectedSnapshot.id) {
            this.activeConformanceSnapshot = selectedSnapshot
            if (this.listView) {
              this.listViewTable?.reloadData()
            } else {
              this.updateTreeViewForSnapshotChange()
            }
          }
          this.activeConformanceSnapshot = selectedSnapshot
        } else {
          this.viewLatestConformanceSnapshot()
        }
      })
    }
  }

  viewLatestConformanceSnapshot(forceReload?: boolean) {
    let reload = forceReload == true
    if (this.activeConformanceSnapshot != undefined) {
      this.activeConformanceSnapshot = undefined
      reload = true
    }
    if (reload) {
      if (this.listView) {
        this.listViewTable?.reloadData()
      } else {
        this.updateTreeViewForSnapshotChange()
      }
    }
    this.snapshotButtonLabel = this.latestSnapshotButtonLabel
  }

  private updateTreeViewForSnapshotChange() {
    this.communityChanged(true)
  }

  viewTypeToggled() {
    if (this.listView) {
      this.selectedCommunityId = this.communityId
      this.selectedOrganisationId = undefined
      this.selectedSystemId = undefined
      this.availableOrganisations = undefined
      this.availableSystems = undefined
      this.statements = []
      if (this.dataService.isSystemAdmin) {
        // Force a switch back to the latest snapshot and refresh
        this.viewLatestConformanceSnapshot(true)
      }
    } else {
      this.resetStatementFilters()
      let observable: Observable<Community[]>
      if (this.availableCommunities == undefined) {
        if (this.dataService.isSystemAdmin) {
          observable = this.communityService.getCommunities(undefined, true)
        } else if (this.dataService.community) {
          observable = of([this.dataService.community])
        } else {
          observable = of([])
        }
      } else {
        observable = of(this.availableCommunities)
      }
      observable.subscribe((data) => {
        setTimeout(() => {
          this.availableCommunities = data
          let communityToApply: Community|undefined
          if (data.length == 1) {
            communityToApply = data[0]
            this.selectedCommunityId = communityToApply.id
          } else {
            this.selectedCommunityId = undefined
          }
          if (this.communitySelectConfig) {
            setTimeout(() => {
              this.communitySelectConfig!.replaceItems!.emit(this.availableCommunities)
              this.communitySelectConfig!.replaceSelectedItems!.emit((communityToApply == undefined)?[]:[communityToApply])
            }, 1)
          }
          if (this.dataService.isSystemAdmin) {
            // Force a switch back to the latest snapshot and refresh
            this.viewLatestConformanceSnapshot(true)
          } else {
            this.communityChanged(true)
          }
        })
      })
    }
  }

  communityChanged(fromSnapshotChange: boolean, community?: FilterUpdate<Community>) {
    this.dataStatus.status = Constants.STATUS.PENDING
    if (community && community.values.active.length > 0) {
      this.selectedCommunityId = community.values.active[0].id
    }
    if (!fromSnapshotChange) {
      this.activeConformanceSnapshot = undefined
    }
    let loadObservable: Observable<Organisation[]>
    if (this.selectedCommunityId == undefined) {
      loadObservable = of([])
    } else {
      loadObservable = this.organisationService.getOrganisationsByCommunity(this.selectedCommunityId, false, this.activeConformanceSnapshot?.id)
    }
    setTimeout(() => {
      this.availableOrganisations = undefined
      loadObservable.subscribe((data) => {
        this.availableOrganisations = data
        let organisationToApply: Organisation|undefined
        if (data.length == 1) {
          organisationToApply = data[0]
          this.selectedOrganisationId = organisationToApply.id
        } else if (data.length > 1 && this.selectedOrganisationId != undefined) {
          organisationToApply = find(data, (org) => org.id == this.selectedOrganisationId)
          if (organisationToApply == undefined) {
            this.selectedOrganisationId = undefined
          }
        } else {
          this.selectedOrganisationId = undefined
        }
        setTimeout(() => {
          this.organisationSelectConfig!.replaceItems!.emit(this.availableOrganisations)
          this.organisationSelectConfig!.replaceSelectedItems!.emit((organisationToApply == undefined)?[]:[organisationToApply])
          this.organisationChanged()
        })
      })
    })
  }

  organisationChanged(organisation?: FilterUpdate<Organisation>) {
    this.dataStatus.status = Constants.STATUS.PENDING
    if (organisation && organisation.values.active.length > 0) {
      this.selectedOrganisationId = organisation.values.active[0].id
    }
    let loadObservable: Observable<System[]>
    if (this.selectedOrganisationId == undefined) {
      loadObservable = of([])
    } else {
      loadObservable = this.systemService.getSystemsByOrganisation(this.selectedOrganisationId, this.activeConformanceSnapshot?.id)
    }
    setTimeout(() => {
      this.availableSystems = undefined
      loadObservable.subscribe((data) => {
        this.availableSystems = data
        let systemToApply: System|undefined
        if (data.length == 1) {
          systemToApply = data[0]
          this.selectedSystemId = systemToApply.id
        } else if (data.length > 1 && this.selectedSystemId != undefined) {
          systemToApply = find(data, (sys) => sys.id == this.selectedSystemId)
          if (systemToApply == undefined) {
            this.selectedSystemId = undefined
          }
        } else {
          this.selectedSystemId = undefined
        }
        setTimeout(() => {
          this.systemSelectConfig!.replaceItems!.emit(this.availableSystems)
          this.systemSelectConfig!.replaceSelectedItems!.emit((systemToApply == undefined)?[]:[systemToApply])
          this.systemChanged()
        }, 1)
      })
    })
  }

  systemChanged(system?: FilterUpdate<System>) {
    this.dataStatus.status = Constants.STATUS.PENDING
    if (system && system.values.active.length > 0) {
      this.selectedSystemId = system.values.active[0].id
    }
    setTimeout(() => {
      if (this.selectedSystemId == undefined) {
        this.dataStatus.status = Constants.STATUS.FINISHED
        this.statements = []
      } else {
        this.getConformanceStatementsForTreeView()
      }
    })
  }

  filterByStatus(choices: CheckboxOptionState) {
    super.filterByStatus(choices);
    this.getConformanceStatementsForTreeView()
  }

  doTreeViewPaging(event: PagingEvent) {
    this.getConformanceStatementsForTreeViewInternal(event)
  }

  getConformanceStatementsForTreeView() {
    this.getConformanceStatementsForTreeViewInternal({ targetPage: 1, targetPageSize: 10 })
  }

  getConformanceStatementsForTreeViewInternal(pagingInfo: PagingEvent) {
    if (this.selectedSystemId != undefined) {
      if (this.dataStatus.status == Constants.STATUS.FINISHED) {
        this.updatePending = true
      } else {
        this.dataStatus.status = Constants.STATUS.PENDING
      }
      this.conformanceService.getConformanceStatementsForSystem(this.selectedSystemId, this.activeConformanceSnapshot?.id, pagingInfo.targetPage, pagingInfo.targetPageSize, this.searchCriteria)
      .subscribe((data) => {
        this.applyTreeViewSearchResult(data, pagingInfo)
      }).add(() => {
        this.updatePending = false
        this.dataStatus.status = Constants.STATUS.FINISHED
      })
    }
  }

  exportOverview(format: 'xml'|'pdf') {
    this.exportOverviewPending = true
    const overallStatus = this.dataService.conformanceStatusForConformanceItems(this.statements)
    this.reportSupportService.handleConformanceOverviewReport(this.selectedCommunityId!, this.selectedSystemId!, undefined, "all", this.activeConformanceSnapshot?.id, format, overallStatus)
    .subscribe(() => {
      // Do nothing further
    }).add(() => {
      this.exportOverviewPending = false
    })
  }

  treeControlsExist() {
    return (this.communityId == undefined && (this.availableCommunities == undefined || this.availableCommunities.length > 0)) ||
      ((this.selectedCommunityId != undefined && this.availableCommunities) && (this.availableOrganisations == undefined || this.availableOrganisations.length > 0)) ||
      ((this.selectedOrganisationId != undefined && this.availableOrganisations) && (this.availableSystems == undefined || this.availableSystems.length > 0)) ||
      (this.statements.length > 0)
  }

  listViewCommunityChange(newCommunityId: number|undefined) {
    if (this.communityId == undefined) {
      if (newCommunityId != undefined) {
        if (newCommunityId != this.selectedCommunityId) {
          this.snapshotButtonLabel = this.latestSnapshotButtonLabel
          this.activeConformanceSnapshot = undefined
          this.selectedCommunityId = newCommunityId
        }
      } else {
        this.snapshotButtonLabel = this.latestSnapshotButtonLabel
        this.activeConformanceSnapshot = undefined
        this.selectedCommunityId = undefined
      }
    }
  }

  onStatementSelect(statement: ConformanceStatementItem) {
    this.routingService.toConformanceStatement(this.selectedOrganisationId!, this.selectedSystemId!, statement.id, this.selectedCommunityId!, this.activeConformanceSnapshot?.id, this.activeConformanceSnapshot?.label)
  }

}
