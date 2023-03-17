import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ConformanceService } from 'src/app/services/conformance.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { Constants } from 'src/app/common/constants';
import { RoutingService } from 'src/app/services/routing.service';
import { Organisation } from 'src/app/types/organisation.type';
import { ConformanceStatementItem } from 'src/app/types/conformance-statement-item';
import { mergeMap, Observable, of } from 'rxjs';
import { SystemService } from 'src/app/services/system.service';
import { filter, find, remove } from 'lodash';

@Component({
  selector: 'app-create-conformance-statement',
  templateUrl: './create-conformance-statement.component.html',
  styleUrls: [ './create-conformance-statement.component.less' ]
})
export class CreateConformanceStatementComponent implements OnInit {

  systemId!: number
  organisationId!: number
  domainId?: number
  dataStatus = {status: Constants.STATUS.PENDING}
  items: ConformanceStatementItem[] = []
  createDisabled = true
  createPending = false
  statementFilter?: string
  hasOtherStatements = false
  selectedActorIds: number[] = []
  itemsByType?: { groups: ConformanceStatementItem[], specs: ConformanceStatementItem[], actors: ConformanceStatementItem[] }
  animated = false
  visibleItemCount = 0

  Constants = Constants

  constructor(
    public dataService: DataService,
    private popupService: PopupService,
    private route: ActivatedRoute,
    private conformanceService: ConformanceService,
    private systemService: SystemService,
    private routingService: RoutingService
  ) { }

  ngOnInit(): void {
    this.systemId = Number(this.route.snapshot.paramMap.get('id'))
    this.organisationId = Number(this.route.snapshot.paramMap.get('org_id'))
    let domainIdObservable: Observable<number|undefined>
    if (this.dataService.vendor?.id == this.organisationId || this.dataService.isCommunityAdmin) {
      // Use own community domain.
      domainIdObservable = of(this.dataService.community?.domainId)
    } else if (this.dataService.isSystemAdmin) {
      // Lookup from organisation.
      const organisation: Organisation = JSON.parse(localStorage.getItem(Constants.LOCAL_DATA.ORGANISATION)!)
      domainIdObservable = this.conformanceService.getCommunityDomain(organisation.community)
      .pipe(
        mergeMap((data) => {
          return of(data?.id)
        })
      )
    } else {
      domainIdObservable = of(undefined)
    }
    const statementObservable = domainIdObservable.pipe(
      mergeMap((domainId) => {
        this.domainId = domainId
        return this.conformanceService.getAvailableConformanceStatements(this.domainId, this.systemId)
      })
    )
    statementObservable.subscribe((itemInfo) => {
      this.hasOtherStatements = itemInfo.exists
      if (itemInfo.items.length == 1) {
        if (this.domainId != undefined) {
          // We only have one domain.
          itemInfo.items[0].hidden = true
        }
        // Display single option as expanded.
        itemInfo.items[0].collapsed = false
        // If there is only one possible option, pre-select it.
        const uniqueActor = this.findUniqueActor(itemInfo.items[0])
        if (uniqueActor) {
          uniqueActor.checked = true
          this.selectionChanged(uniqueActor)
        }
      }
      let specs: ConformanceStatementItem[] = []
      for (let domain of itemInfo.items) {
        const specsInDomain = filter(domain.items, (item) => item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION)
        specsInDomain.forEach((item) => specs.push(item))
        const groupsInDomain = filter(domain.items, (item) => item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION_GROUP)
        for (let group of groupsInDomain) {
          const specsInGroup = filter(group.items, (item) => item.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION)
          specsInGroup.forEach((item) => specs.push(item))
        }
      }
      this.checkToHideActors(specs)
      this.itemsByType = this.getItemsByType(itemInfo.items)
      // Initialise item state.
      this.visit(itemInfo.items, (item) => {
        if (item.collapsed == undefined) {
          item.collapsed = true
        }
        if (item.items && item.items.length == 1) {
          item.items[0].collapsed = false
        }
        if (item.checked == undefined) {
          item.checked = false
        }
        if (item.hidden == undefined) {
          item.hidden = false
        }
        item.filtered = true
      })
      this.items = itemInfo.items
      this.countVisibleItems()
      this.toggleAnimated(true)
    }).add(() => {
      this.dataStatus.status = Constants.STATUS.FINISHED
    })
  }

  private countVisibleItems() {
    let count = 0
    this.visit(this.items, (item) => {
      if (item.filtered == true) {
        count += 1
      }
    })
    this.visibleItemCount = count
  }

  private checkToHideActors(specifications?: ConformanceStatementItem[]) {
    if (specifications) {
      for (let specification of specifications) {
        // If we have a specification with a single actor don't show the actor.
        if (specification.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION
          && specification.items != undefined && specification.items.length == 1 && specification.items[0].itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.ACTOR) {
            // The specification will be an alias for the actor.
            specification.id = specification.items[0].id
            specification.checked = specification.items[0].checked
            specification.items = []
        }
      }
    }
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

  private filterItems(items: ConformanceStatementItem[]|undefined, filterValue: string|undefined) {
    if (filterValue && items) {
      for (let item of items) {
        this.filterItem(item, filterValue)
      }
    }
  }

  private filterItem(item: ConformanceStatementItem, filterValue: string|undefined) {
    if (filterValue) {
      const filterToApply = filterValue.trim().toLowerCase()
      if (item.checked || ((item.name.toLowerCase().indexOf(filterToApply)) >= 0) || (item.description != undefined && item.description.toLowerCase().indexOf(filterToApply) >= 0)) {
        // Match.
        item.matched = true
        item.filtered = true
      }
    }
  }

  private getItemsByType(items: ConformanceStatementItem[]): { groups: ConformanceStatementItem[], specs: ConformanceStatementItem[], actors: ConformanceStatementItem[] } {
    let groups: ConformanceStatementItem[] = []
    let specs: ConformanceStatementItem[] = []
    let actors: ConformanceStatementItem[] = []
    for (let domain of items) {
      if (domain.items) {
        for (let specOrGroup of domain.items) {
          if (specOrGroup.itemType == Constants.CONFORMANCE_STATEMENT_ITEM_TYPE.SPECIFICATION_GROUP) {
            groups.push(specOrGroup)
            // Specifications in group
            if (specOrGroup.items) {
              specOrGroup.items.forEach((item) => specs.push(item))
            }
          } else {
            // Specification in domain
            specs.push(specOrGroup)
          }
        }
      }
    }
    for (let spec of specs) {
      if (spec.items) {
        spec.items.forEach((item) => actors.push(item))
      }
    }
    return {
      groups: groups, 
      specs: specs, 
      actors: actors
    }
  }

  searchStatements() {
    this.animated = false
    setTimeout(() => {
      if (this.itemsByType) {
        const defaultFilteredValue = this.statementFilter == undefined
        // Flag all items not filtered.
        this.items.forEach((item) => { item.filtered = defaultFilteredValue; item.matched = false; })
        this.itemsByType.groups.forEach((item) => { item.filtered = defaultFilteredValue; item.matched = false; })
        this.itemsByType.specs.forEach((item) => { item.filtered = defaultFilteredValue; item.matched = false; })
        this.itemsByType.actors.forEach((item) => { item.filtered = defaultFilteredValue; item.matched = false; })
        if (this.statementFilter != undefined) {
          // Apply filters per item type.
          this.filterItems(this.items, this.statementFilter)
          this.filterItems(this.itemsByType.groups, this.statementFilter)
          this.filterItems(this.itemsByType.specs, this.statementFilter)
          this.filterItems(this.itemsByType.actors, this.statementFilter)
          // Apply match semantics to hierarchy.
          this.filterParentsWithFilteredChildren(this.itemsByType.specs)
          this.filterParentsWithFilteredChildren(this.itemsByType.groups)
          this.filterParentsWithFilteredChildren(this.items)
          // Adapt collapsing
          this.adaptParentCollapse(this.itemsByType!.specs)
          this.adaptParentCollapse(this.itemsByType!.groups)
          this.adaptParentCollapse(this.items)
          const filteredDomains = filter(this.items, (item) => item.filtered != undefined && item.filtered)
          if (filteredDomains.length == 1) {
            filteredDomains[0].collapsed = false
          }
          this.toggleAnimated(true)
        }
        this.countVisibleItems()
      }
      this.toggleAnimated(true)
    }, 1)
  }

  private toggleAnimated(animatedValue: boolean) {
    setTimeout(() => {
      this.animated = animatedValue
    }, 1)
  }

  private adaptParentCollapse(items: ConformanceStatementItem[]) {
    for (let item of items) {
      const hasChild = item.items != undefined && item.items.length > 0
      const hasMatchedChild = hasChild && (find(item.items, (child) => {
        return child.matched == true
      }) != undefined)
      if (hasMatchedChild) {
        item.collapsed = false
      }
    }
  }

  private filterParentsWithFilteredChildren(items: ConformanceStatementItem[]) {
    for (let item of items) {
      const hasChild = item.items != undefined && item.items.length > 0
      const hasVisibleChild = hasChild && (find(item.items, (child) => {
        return child.filtered == true
      }) != undefined)
      if (hasVisibleChild) {
        item.collapsed = false
      }
      if (!item.filtered) {
        item.filtered = hasVisibleChild
      } else {
        // Apply filtering logic to children.
        this.visit(item.items, (item) => {
          item.filtered = true
        })
      }
    }
  }

  toggleCheck(check: boolean) { 
    this.visit(this.items, (item) => {
      if (item.filtered) {
        // Only toggle items that are visible.
        item.checked = check
        this.selectionChanged(item)
      }
    })
  }

  toggleCollapse(collapse: boolean) {
    this.animated = false
    setTimeout(() => {
      this.visit(this.items, (item) => {
        item.collapsed = collapse
      })
      this.toggleAnimated(true)
    })
  }

  private visit(items: ConformanceStatementItem[]|undefined, visitor: (item: ConformanceStatementItem) => any) {
    if (items) {
      for (let item of items) {
        visitor(item)
        this.visit(item.items, visitor)
      }
    }
  }

  selectionChanged(selectedItem: ConformanceStatementItem) {
    if (selectedItem.checked) {
      this.selectedActorIds.push(selectedItem.id)
    } else {
      remove(this.selectedActorIds, (id) => id == selectedItem.id)
    }
    this.createDisabled = this.selectedActorIds.length == 0
  }

  create() {
    if (!this.createDisabled) {
      this.createPending = true
      this.systemService.defineConformanceStatements(this.systemId, this.selectedActorIds)
      .subscribe(() => {
        this.routingService.toConformanceStatements(this.organisationId, this.systemId)        
        if (this.selectedActorIds.length > 1) {
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
    this.routingService.toConformanceStatements(this.organisationId, this.systemId)
  }

}
