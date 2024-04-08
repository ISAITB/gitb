import { Component, ElementRef, EventEmitter, HostListener, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { ItemMap } from './item-map';
import { MultiSelectConfig } from './multi-select-config';
import { EntityWithId } from '../../types/entity-with-id';
import { filter, find } from 'lodash';
import { FilterValues } from '../test-filter/filter-values';
import { FilterUpdate } from '../test-filter/filter-update';
import { Observable, Subscription, of } from 'rxjs';

@Component({
  selector: 'app-multi-select-filter',
  templateUrl: './multi-select-filter.component.html',
  styleUrls: [ './multi-select-filter.component.less' ]
})
export class MultiSelectFilterComponent<T extends EntityWithId> implements OnInit, OnDestroy {

  @Input() config!: MultiSelectConfig<T>
  @Input() typeahead = true
  @Output() apply = new EventEmitter<FilterUpdate<any>>()
  @ViewChild('filterText') filterTextElement?: ElementRef

  selectedItems: T[] = []
  availableItems: T[] = []
  visibleAvailableItems: T[] = []
  selectedAvailableItems: ItemMap<T> = {}
  selectedSelectedItems: ItemMap<T> = {}
  formVisible = false
  hasCheckedSelectedItem = false
  defaultFilterLabel = 'All'
  filterLabel = 'All'
  openToLeft = false
  loadPending = false
  textValue = ''
  focusInterval?: any

  replaceItemsSubscription?: Subscription
  replaceSelectedItemsSubscription?: Subscription
  clearItemsSubscription?: Subscription

  /*
   * ID of the displayed item to array of the hidden items.
   * The 'applicable' flag is used to cover the case of items with the same name that  
   * are hidden and should not be returned as part of the filter's values.
   * This is needed when we change the values of upstream selections (e.g. a specification of a test suite).
   */
  itemsWithSameValue: { [key: number]: {applicable: boolean, item: T}[] } = {}

  constructor(
    private eRef: ElementRef,
    private dataService: DataService
  ) { }

  ngOnInit(): void {
    if (this.config.singleSelection == undefined) {
      this.config.singleSelection = false
    }
    if (this.config.filterLabel) {
      this.filterLabel = this.config.filterLabel
      this.defaultFilterLabel = this.config.filterLabel
    }
    if (this.config.clearItems) {
      this.clearItemsSubscription = this.config.clearItems.subscribe(() => {
        this.selectedSelectedItems = {}
        this.updateCheckFlag(false)
        this.updateLabel()
        this.selectedItems = []
      })
    }
    if (this.config.replaceItems) {
      this.replaceItemsSubscription = this.config.replaceItems.subscribe((newItems) => {
        this.selectedSelectedItems = {}
        this.updateCheckFlag(false)
        this.updateLabel()
        this.selectedItems = []
        this.availableItems = newItems
        this.visibleAvailableItems = this.availableItems
      })
    }
    if (this.config.replaceSelectedItems) {
      this.replaceSelectedItemsSubscription = this.config.replaceSelectedItems.subscribe((newItems) => {
        if (this.config.singleSelection == true) {
          for (let item of newItems) {
            this.selectedSelectedItems[item.id] = { selected: true, item: item }
          }
        } else {
          const allowedIdSet = this.dataService.asIdSet(newItems)
          const previousIdSet = this.selectedSelectedItems
          for (let id in previousIdSet) {
            if (this.itemsWithSameValue[id]) {
              // We have other items with the same text value - mark as applicable only the ones we are supposed to keep.
              // The other values are maintained to allow switching upstream values (e.g. the specification of a test suite).
              for (let similarItem of this.itemsWithSameValue[id]) {
                similarItem.applicable = allowedIdSet[similarItem.item.id] != undefined
              }
            }
            if (!allowedIdSet[id] && this.selectedSelectedItems[id]) {
              // The currently selected item no longer applies.
              const previouslySelectedItem = this.selectedSelectedItems[id].item
              if (this.itemsWithSameValue[id]) {
                // Other similarly valued items exist.
                let otherApplicableItemWithSameTextValue = find(this.itemsWithSameValue[id], (entry) => { return entry.applicable })
                if (otherApplicableItemWithSameTextValue != undefined) {
                  const newSelectedItemId = otherApplicableItemWithSameTextValue.item.id
                  // A previously hidden similarly valued item still applies - replace the visible one with it.
                  const otherHiddenItems = filter(this.itemsWithSameValue[id], (entry) => { return entry.item.id != newSelectedItemId })
                  // Add the previously selected item as a hidden, non-applicable one and the remaining ones.
                  this.itemsWithSameValue[newSelectedItemId] = [{applicable: false, item: previouslySelectedItem}].concat(otherHiddenItems)
                  this.selectedSelectedItems[newSelectedItemId] = { selected: true, item: otherApplicableItemWithSameTextValue.item }
                }
                delete this.itemsWithSameValue[id]
              }
              delete this.selectedSelectedItems[id]
            }
          }
        }
        const newItemsToSet: T[] = []
        for (let key in this.selectedSelectedItems) {
          if (this.selectedSelectedItems[key].selected) {
            newItemsToSet.push(this.selectedSelectedItems[key].item)
          }
        }
        this.selectedItems = this.sortItems(newItemsToSet)
        this.updateCheckFlag()
        this.updateLabel()
        this.applyItems(true)
      })
    }
  }

  ngOnDestroy(): void {
    if (this.focusInterval) {
      clearInterval(this.focusInterval)
      this.focusInterval = undefined
    }
    if (this.replaceItemsSubscription) this.replaceItemsSubscription.unsubscribe()
    if (this.replaceSelectedItemsSubscription) this.replaceSelectedItemsSubscription.unsubscribe()
    if (this.clearItemsSubscription) this.clearItemsSubscription.unsubscribe()
  }

  @HostListener('document:click', ['$event'])
  clickRegistered(event: any) {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.close()
    }
  }

  @HostListener('document:keyup.escape', ['$event'])  
  escapeRegistered(event: KeyboardEvent) {
    if (this.formVisible && this.typeahead) {
      if (this.textValue == '') {
        this.close()
      } else {
        this.textValue = ''
      }
      this.searchApplied()
    }
  }

  @HostListener('document:keyup.enter', ['$event'])  
  enterRegistered(event: KeyboardEvent) {
    if (this.formVisible && this.typeahead) {
      if (this.visibleAvailableItems?.length == 1) {
        this.availableItemClicked(this.visibleAvailableItems[0])
      }
    }
  }

  private updateLabel() {
    if (this.selectedItems.length == 0) {
      this.filterLabel = this.defaultFilterLabel
    } else {
      if (this.config.singleSelection == true) {
        if (this.config.singleSelectionPersistent == true) {
          this.filterLabel = this.selectedItems[0][this.config.textField]
        }
      } else {
        this.filterLabel = "("+this.selectedItems.length+")"
      }
    }
  } 

  private hasCheckedItems(itemMap: ItemMap<T>) {
    for (let itemId in itemMap) {
      if (itemMap[itemId].selected) {
        return true
      }
    }
    return false
  }

  controlClicked() {
    if (this.formVisible) {
      this.close()
    } else {
      this.formVisible = true
      this.openToLeft = this.shouldOpenToLeft()
      this.loadData()
      if (this.typeahead) {
        setTimeout(() => {
          if (this.filterTextElement) {
            this.filterTextElement.nativeElement.focus()
          }
          if (this.focusInterval == undefined) {
            this.focusInterval = setInterval(() => {
              if (this.filterTextElement) {
                this.filterTextElement.nativeElement.focus()
              }
            }, 500)
          }
        })
      }
    }
  }

  private shouldOpenToLeft() {
    let result = false
    const coords = this.eRef.nativeElement.getBoundingClientRect()
    if (coords) {
      let x: number|undefined
      if (coords.left != undefined) {
        x = coords.left
      } else if (coords.x != undefined) {
        x = coords.x
      }
      if (x != undefined) {
        const maxWidth = window.innerWidth
        if (maxWidth != undefined) {
          result = x + 400 > maxWidth
        }
      }
    }
    return result
  }

  private isSelected(id: number): boolean {
    if (this.selectedSelectedItems[id]) {
      return true
    } else if (this.itemsWithSameValue[id]) {
      return find(this.itemsWithSameValue[id], (entry) => { 
        return entry.applicable && entry.item.id == id
      }) != undefined
    }
    return false
  }

  private findItemWithSameTextValue(items: T[], item: T) {
    let itemWithSameTextValue: T|undefined
    if (items != undefined) {
      itemWithSameTextValue = find(items, (existingItem) => {
        return existingItem[this.config.textField] == item[this.config.textField]
      })
    }
    return itemWithSameTextValue
  }

  private recordItemWithSameTextValue(visibleItem: T, itemToHide: T) {
    if (this.itemsWithSameValue[visibleItem.id] == undefined) {
      this.itemsWithSameValue[visibleItem.id] = []
    }
    this.itemsWithSameValue[visibleItem.id].push({ applicable: true, item: itemToHide })
  }

  private loadData() {
    this.selectedAvailableItems = {}
    this.itemsWithSameValue = {}
    let loadObservable: Observable<T[]>
    if (this.config.loader) {
      this.loadPending = true
      this.availableItems = []
      loadObservable = this.config.loader()
    } else {
      loadObservable = of(this.availableItems)
    }
    loadObservable.subscribe((items) => {
      const newSelectedAvailableItems: ItemMap<T> = {}
      const newAvailableItems: T[] = []
      for (let item of items) {
        if (this.config.singleSelection == true) {
          newAvailableItems.push(item)
          newSelectedAvailableItems[item.id] = { selected: false, item: item }
        } else {
          if (!this.isSelected(item.id)) {
            let matchingItem = this.findItemWithSameTextValue(newAvailableItems, item)
            if (matchingItem) {
              this.recordItemWithSameTextValue(matchingItem, item)
            } else {
              matchingItem = this.findItemWithSameTextValue(this.selectedItems, item)
              if (matchingItem) {
                this.recordItemWithSameTextValue(matchingItem, item)
              } else {
                newAvailableItems.push(item)
                newSelectedAvailableItems[item.id] = { selected: false, item: item }
              }
            }
          }
        }
      }
      this.selectedAvailableItems = newSelectedAvailableItems
      this.availableItems = newAvailableItems
      this.visibleAvailableItems = this.availableItems
    }).add(() => {
      this.loadPending = false
    })
  }

  searchApplied() {
    if (this.textValue.length == 0) {
      this.visibleAvailableItems = this.availableItems
    } else {
      const textForSearch = this.textValue.trim().toLowerCase()
      this.visibleAvailableItems = filter(this.availableItems, (item) => { return (<string>item[this.config.textField]).toLowerCase().indexOf(textForSearch) >= 0 })
    }
  }

  availableItemClicked(item: T) {
    if (this.config.singleSelection) {
      this.selectedItems = [item]
      const itemToReport: FilterValues<T> = { active: [], other: [] }
      itemToReport.active = this.getItemsToSignalForItem(item)
      this.apply.emit({ values: itemToReport, applyFilters: false })
      this.updateLabel()
      this.close()
    } else {
      this.itemClicked(this.selectedAvailableItems, item)
    }
  }

  selectedItemClicked(item: T) {
    this.itemClicked(this.selectedSelectedItems, item)
  }

  private itemClicked(itemMap: ItemMap<T>, item: T) {
    itemMap[item.id].selected = !itemMap[item.id].selected
    if (itemMap[item.id].selected) {
      this.updateCheckFlag(true)
    } else {
      this.updateCheckFlag()
    }
  }

  private sortItems(items: T[]) {
    return items.sort((a, b) => { return (<string>a[this.config.textField]).localeCompare(<string>b[this.config.textField])})
  }

  private getItemsToSignalForItem(item: T): T[] {
    return [item].concat(this.getHiddenItemsForItem(item, true))
  }

  private getHiddenItemsForItem(item: T, applicable: boolean): T[] {
    let items: T[] = []
    if (this.itemsWithSameValue[item.id]) {
      items = filter(this.itemsWithSameValue[item.id], (otherItem) => { return otherItem.applicable == applicable }).map((otherItem) => otherItem.item)
    }
    if (items == undefined) {
      items = []
    }
    return items
  }

  applyItems(skipRefresh?: boolean) {
    this.formVisible = false
    // Remove previously selected items that were unchecked.
    for (let itemId in this.selectedSelectedItems) {
      if (!this.selectedSelectedItems[itemId].selected) {
        delete this.selectedSelectedItems[itemId]
      }
    }
    let newSelectedItems: T[] = []
    let selectedItemsToReport: FilterValues<T> = { active: [], other: [] }
    for (let item of this.selectedItems) {
      if (this.selectedSelectedItems[item.id] != undefined) {
        newSelectedItems.push(item)
        selectedItemsToReport.active = selectedItemsToReport.active.concat(this.getItemsToSignalForItem(item))
      }
    }
    // Add available items that have been checked.
    for (let itemId in this.selectedAvailableItems) {
      if (this.selectedAvailableItems[itemId].selected) {
        const item = this.selectedAvailableItems[itemId].item
        newSelectedItems.push(item)
        selectedItemsToReport.active = selectedItemsToReport.active.concat(this.getItemsToSignalForItem(item))
        this.selectedSelectedItems[itemId] = { selected: true, item: item }
        delete this.selectedAvailableItems[itemId]
      }
    }
    for (let item of newSelectedItems) {
      selectedItemsToReport.other = selectedItemsToReport.other.concat(this.getHiddenItemsForItem(item, false))
    }
    this.selectedItems = this.sortItems(newSelectedItems)
    this.updateCheckFlag()
    this.updateLabel()
    this.apply.emit({ values: selectedItemsToReport, applyFilters: skipRefresh == undefined || !skipRefresh })
  }

  clearSelectedItems() {
    this.clearSelectedItemMap(this.selectedSelectedItems)
    this.clearSelectedItemMap(this.selectedAvailableItems)
  }

  private clearSelectedItemMap(itemMap: ItemMap<T>) {
    for (let itemId in itemMap) {
      itemMap[itemId].selected = false
    }
  }

  updateCheckFlag(forcedValue?: boolean) {
    if (forcedValue == undefined) {
      this.hasCheckedSelectedItem = this.hasCheckedItems(this.selectedSelectedItems) || this.hasCheckedItems(this.selectedAvailableItems)
    } else {
      this.hasCheckedSelectedItem = forcedValue
    }
  }

  close() {
    this.formVisible = false
    this.textValue = ''
    // Make sure that previously selected items that have been unchecked but not applied are undone.
    for (let itemId in this.selectedSelectedItems) {
      this.selectedSelectedItems[itemId].selected = true
    }
  }

}
