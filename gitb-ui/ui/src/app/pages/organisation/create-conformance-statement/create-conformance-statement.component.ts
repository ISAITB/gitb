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

import {AfterViewInit, Component, ElementRef, NgZone, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ConformanceService} from 'src/app/services/conformance.service';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';
import {Constants} from 'src/app/common/constants';
import {RoutingService} from 'src/app/services/routing.service';
import {ConformanceStatementItem} from 'src/app/types/conformance-statement-item';
import {mergeMap, Observable, of} from 'rxjs';
import {SystemService} from 'src/app/services/system.service';
import {PagingEvent} from '../../../components/paging-controls/paging-event';
import {PagingControlsApi} from '../../../components/paging-controls/paging-controls-api';
import {PagingPlacement} from '../../../components/paging-controls/paging-placement';
import {CheckboxOption} from '../../../components/checkbox-option-panel/checkbox-option';
import {CreateStatementSearchCriteria} from './create-statement-search-criteria';
import {CheckboxOptionState} from '../../../components/checkbox-option-panel/checkbox-option-state';

@Component({
    selector: 'app-create-conformance-statement',
    templateUrl: './create-conformance-statement.component.html',
    styleUrls: ['./create-conformance-statement.component.less'],
    standalone: false
})
export class CreateConformanceStatementComponent implements OnInit, AfterViewInit, OnDestroy {

  systemId!: number
  organisationId!: number
  communityId?: number
  domainId?: number
  dataStatus = {status: Constants.STATUS.PENDING}
  items: ConformanceStatementItem[] = []
  createDisabled = true
  createPending = false
  updatePending = false
  hasOtherStatements = false
  hasStatementsBeforeFiltering = false
  selectedActorIds = new Set<number>()
  animated = true
  hasVisibleItems = false
  protected readonly PagingPlacement = PagingPlacement;
  Constants = Constants
  searchCriteria: CreateStatementSearchCriteria = {
    filterText: undefined,
    selected: true,
    unselected: true
  }
  statusOptions: CheckboxOption[][] = [
    [
      {key: "selected", label: 'Selected statements', default: this.searchCriteria.selected},
      {key: "unselected", label: 'Unselected statements', default: this.searchCriteria.unselected}
    ]
  ]
  controlsWrapped = false
  @ViewChild("pagingControls") pagingControls?: PagingControlsApi
  @ViewChild("controlContainer") controlContainer?: ElementRef
  @ViewChild("statusContainer") statusContainer?: ElementRef
  @ViewChild("conformanceItemPage") conformanceItemPage?: ElementRef
  resizeObserver!: ResizeObserver

  constructor(
    public readonly dataService: DataService,
    private readonly popupService: PopupService,
    private readonly route: ActivatedRoute,
    private readonly conformanceService: ConformanceService,
    private readonly systemService: SystemService,
    private readonly routingService: RoutingService,
    private readonly zone: NgZone
  ) { }

  ngOnInit(): void {
    this.systemId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID))
    this.organisationId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID))
    if (this.route.snapshot.paramMap.has(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID)) {
      this.communityId = Number(this.route.snapshot.paramMap.get(Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID))
    }
    let domainIdObservable: Observable<number|undefined>
    if (this.communityId == undefined) {
      // Use own community domain.
      domainIdObservable = of(this.dataService.community?.domainId)
    } else {
      // Lookup from navigation community.
      domainIdObservable = this.conformanceService.getCommunityDomain(this.communityId)
      .pipe(
        mergeMap((data) => {
          return of(data?.id)
        })
      )
    }
    domainIdObservable.subscribe((domainId) => {
      this.domainId = domainId
      this.loadStatements()
    })
  }

  ngAfterViewInit(): void {
    this.resizeObserver = new ResizeObserver(() => {
      this.zone.run(() => {
        this.calculateWrapping()
      })
    })
    if (this.conformanceItemPage) {
      this.resizeObserver.observe(this.conformanceItemPage.nativeElement)
    }
  }

  filterByStatus(choices: CheckboxOptionState) {
    this.searchCriteria.selected = choices["selected"]
    this.searchCriteria.unselected = choices["unselected"]
    this.loadStatements()
  }

  loadStatements() {
    return this.loadStatementsInternal({ targetPage: 1, targetPageSize: Constants.TABLE_PAGE_SIZE })
  }

  doPagingNavigation(pagingInfo: PagingEvent) {
    this.loadStatementsInternal(pagingInfo)
  }

  private loadStatementsInternal(pagingInfo: PagingEvent) {
    if (this.dataStatus.status == Constants.STATUS.FINISHED) {
      this.updatePending = true
    } else {
      this.dataStatus.status = Constants.STATUS.PENDING
    }
    if ((!this.searchCriteria.selected || !this.searchCriteria.unselected) && this.selectedActorIds.size > 0) {
      this.searchCriteria.selectedIds = [...this.selectedActorIds]
    } else {
      this.searchCriteria.selectedIds = undefined
    }
    this.conformanceService.getAvailableConformanceStatements(this.domainId, this.systemId, this.searchCriteria, pagingInfo.targetPage, pagingInfo.targetPageSize)
      .subscribe(data => {
        this.hasOtherStatements = data.hasOtherStatements
        this.items = this.dataService.prepareConformanceStatementItemsForDisplay(data.data)
        this.hasVisibleItems = data.data.length > 0
        if (this.dataStatus.status == Constants.STATUS.PENDING) {
          // This is the initial, unfiltered load.
          this.hasStatementsBeforeFiltering = this.hasVisibleItems
          // If there is only one possible option, preselect it.
          if (this.hasVisibleItems && this.items.length ==  1) {
            const uniqueActor = this.findUniqueActor(this.items[0])
            if (uniqueActor) {
              this.selectedActorIds.add(uniqueActor.id)
            }
          }
        }
        this.updateChecks()
        this.toggleAnimated(true)
        this.applyPagingResult(pagingInfo, data.count)
      }).add(() => {
        this.updatePending = false
        this.dataStatus.status = Constants.STATUS.FINISHED
    })
  }

  private applyPagingResult(pagingInfo: PagingEvent, count: number) {
    this.pagingControls?.updateStatus(pagingInfo.targetPage, count)
  }

  private findUniqueActor(item: ConformanceStatementItem): ConformanceStatementItem|undefined {
    if (item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.ACTOR) {
      return item
    } else {
      if (item.items?.length == 1) {
        return this.findUniqueActor(item.items[0])
      } else {
        return undefined
      }
    }
  }

  private toggleAnimated(animatedValue: boolean) {
    setTimeout(() => {
      this.animated = animatedValue
    }, 1)
  }

  selectAllStatements() {
    this.updatePending = true
    this.conformanceService.getAvailableConformanceStatementIds(this.domainId, this.systemId, this.searchCriteria).subscribe(data => {
      data.forEach((actorId) => {
        this.selectedActorIds.add(actorId)
      })
      this.updateChecks()
    }).add(() => {
      this.updatePending = false
    })
  }

  deselectAllStatements() {
    this.updatePending = true
    this.conformanceService.getAvailableConformanceStatementIds(this.domainId, this.systemId, this.searchCriteria).subscribe(data => {
      data.forEach((actorId) => {
        this.selectedActorIds.delete(actorId)
      })
      this.updateChecks()
    }).add(() => {
      this.updatePending = false
    })
  }

  private updateCheckForItem(item: ConformanceStatementItem) {
    if (item.items != undefined && item.items.length > 0) {
      if (item.items.length == 1) {
        if (item.items[0].hidden) {
          item.checked = this.selectedActorIds.has(item.items[0].id)
        } else {
          this.updateCheckForItem(item.items[0])
        }
      } else {
        item.items.forEach((item) => {
          this.updateCheckForItem(item)
        })
      }
    } else {
      item.checked = this.selectedActorIds.has(item.id)
    }
  }


  private updateChecks() {
    this.items.forEach((item) => {
      this.updateCheckForItem(item)
    })
    this.createDisabled = this.selectedActorIds.size == 0
  }

  toggleCollapse(collapse: boolean) {
    this.animated = false
    setTimeout(() => {
      this.dataService.visitConformanceItems(this.items, (item) => {
        item.collapsed = collapse
      })
      this.toggleAnimated(true)
    })
  }

  selectionChanged(selectedItem: ConformanceStatementItem) {
    if (this.selectedActorIds.has(selectedItem.id)) {
      this.selectedActorIds.delete(selectedItem.id)
    } else {
      this.selectedActorIds.add(selectedItem.id)
    }
    this.updateChecks()
  }

  create() {
    if (!this.createDisabled) {
      this.createPending = true
      this.systemService.defineConformanceStatements(this.systemId, this.selectedActorIds)
      .subscribe(() => {
        this.cancel()
        if (this.selectedActorIds.size > 1) {
          this.popupService.success("Conformance statements created.")
        } else {
          this.popupService.success("Conformance statement created.")
        }
      }).add(() => {
        this.createPending = false
      })
    }
  }

  cancel() {
    if (this.communityId == undefined) {
      this.routingService.toOwnConformanceStatements(this.organisationId, this.systemId)
    } else {
      this.routingService.toConformanceStatements(this.communityId, this.organisationId, this.systemId)
    }
  }

  ngOnDestroy() {
    if (this.resizeObserver) {
      if (this.conformanceItemPage) {
        this.resizeObserver.unobserve(this.conformanceItemPage.nativeElement)
      }
    }
  }

  private calculateWrapping() {
    if (this.controlContainer && this.statusContainer) {
      this.controlsWrapped = this.hasStatementsBeforeFiltering && this.controlContainer.nativeElement.getBoundingClientRect().top != this.statusContainer.nativeElement.getBoundingClientRect().top
    }
  }

}
