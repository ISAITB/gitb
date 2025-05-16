import {Component, ElementRef, EventEmitter, forwardRef, HostListener, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {DataService} from 'src/app/services/data.service';
import {ItemMap} from './item-map';
import {MultiSelectConfig} from './multi-select-config';
import {EntityWithId} from '../../types/entity-with-id';
import {filter, find} from 'lodash';
import {FilterValues} from '../test-filter/filter-values';
import {FilterUpdate} from '../test-filter/filter-update';
import {map, Observable, of, share, Subscription} from 'rxjs';
import {Constants} from '../../common/constants';
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from '@angular/forms';

@Component({
  selector: 'app-multi-select-filter',
  templateUrl: './multi-select-filter.component.html',
  styleUrls: ['./multi-select-filter.component.less'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MultiSelectFilterComponent),
      multi: true
    }
  ],
    standalone: false
})
export class MultiSelectFilterComponent<T extends EntityWithId> implements OnInit, OnDestroy, ControlValueAccessor {

  @Input() config!: MultiSelectConfig<T>
  @Input() typeahead = true
  @Input() pending = false
  @Input() disable = false
  @Output() apply = new EventEmitter<FilterUpdate<any>>()
  @Output() ready = new EventEmitter<string>()
  @ViewChild('filterText') filterTextElement?: ElementRef
  @ViewChild('filterControl') filterControlElement?: ElementRef
  @ViewChild('filterForm') filterFormElement?: ElementRef

  selectedItems: T[] = []
  availableItems: T[] = []
  visibleAvailableItems: T[] = []
  selectedAvailableItems: ItemMap<T> = {}
  selectedSelectedItems: ItemMap<T> = {}
  formVisible = false
  hasCheckedSelectedItem = false
  showClearIcon = false
  defaultFilterLabel = 'All'
  filterLabel = 'All'
  openToLeft = false
  loadPending = false
  textValue = ''
  noItemsMessage!: string
  searchPlaceholder!: string
  id!: string
  formWidth!: number
  formTop!: number
  availableItemsHeight!: number

  replaceItemsSubscription?: Subscription
  replaceSelectedItemsSubscription?: Subscription
  clearItemsSubscription?: Subscription

  focusedSelectedItemIndex?: number
  focusedAvailableItemIndex?: number
  onChange = (_: any) => {}
  onTouched = () => {}
  _value: T|T[]|undefined

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
    if (this.config.filterLabel != undefined) {
      this.filterLabel = this.config.filterLabel
      this.defaultFilterLabel = this.config.filterLabel
    }
    if (this.config.noItemsMessage != undefined) {
      this.noItemsMessage = this.config.noItemsMessage
    } else {
      this.noItemsMessage = "No items found"
    }
    if (this.config.searchPlaceholder != undefined) {
      this.searchPlaceholder = this.config.searchPlaceholder
    } else {
      this.searchPlaceholder = "Search items..."
    }
    if (this.config.id != undefined) {
      this.id = this.config.id
    } else {
      this.id = this.config.name
    }
    if (this.config.clearItems) {
      this.clearItemsSubscription = this.config.clearItems.subscribe(() => {
        this.selectedSelectedItems = {}
        this.selectedItems = []
        this.updateCheckFlag(false)
        this.updateLabel()
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
        this.replaceSelectedItems(newItems)
      })
    }
    if (this.config.initialValues != undefined) {
      this.replaceSelectedItems(this.config.initialValues)
    }
    this.ready.emit(this.config.name)
  }

  writeValue(v: T|T[]|undefined): void {
    if (this.config.singleSelection) {
      if (v == undefined) {
      } else if (Array.isArray(v)) {
        if (v.length == 0) {
          this.clearSingleSelection()
        } else {
          this.selectAvailableItem(v[0])
        }
      } else {
        this.selectAvailableItem(v)
      }
    } else {
      if (v == undefined) {
        this.replaceSelectedItems([])
      } else if (Array.isArray(v)) {
        this.replaceSelectedItems(v)
      } else {
        this.replaceSelectedItems([v])
      }
    }
  }

  registerOnChange(fn: any): void {
    this.onChange = fn
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn
  }

  emitChanges() {
    this.onChange(this._value)
    this.onTouched()
  }

  private replaceSelectedItems(newItems: T[]) {
    if (this.config.singleSelection == true) {
      for (let item of newItems) {
        this.selectedSelectedItems[item.id] = { selected: true, item: item }
      }
    } else {
      const allowedIdSet = this.dataService.asIdSet(newItems)
      const previousIdSet = this.selectedSelectedItems
      let hasPrevious = false
      for (let id in previousIdSet) {
        hasPrevious = true
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
      if (!hasPrevious) {
        for (let item of newItems) {
          this.selectedSelectedItems[item.id] = { selected: true, item: item }
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
  }

  ngOnDestroy(): void {
    if (this.replaceItemsSubscription) this.replaceItemsSubscription.unsubscribe()
    if (this.replaceSelectedItemsSubscription) this.replaceSelectedItemsSubscription.unsubscribe()
    if (this.clearItemsSubscription) this.clearItemsSubscription.unsubscribe()
  }

  @HostListener('window:resize', ['$event'])
  onWindowResize() {
    if (this.formVisible) {
      this.calculateSizeAndPosition()
    }
  }

  @HostListener('window:scroll', ['$event'])
  onWindowScroll() {
    if (this.formVisible) {
      this.calculateSizeAndPosition()
    }
  }

  @HostListener('document:click', ['$event'])
  clickRegistered(event: any) {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.close()
    }
  }

  filterTextFocused() {
    this.focusedSelectedItemIndex = undefined
    this.focusedAvailableItemIndex = undefined
  }

  private textFieldVisible(): boolean {
    return this.typeahead && this.availableItems.length > 0 && this.filterTextElement != undefined
  }

  @HostListener('document:keydown', ['$event'])
  keyUpRegistered(event: KeyboardEvent) {
    if (this.formVisible) {
      switch (event.key) {
        case 'Escape': {
          if (this.focusedSelectedItemIndex != undefined || this.focusedAvailableItemIndex != undefined) {
            this.focusedSelectedItemIndex = undefined
            this.focusedAvailableItemIndex = undefined
            if (this.textFieldVisible()) {
              this.filterTextElement!.nativeElement.focus()
            }
          } else {
            if (this.typeahead) {
              if (this.textValue == '') {
                this.close()
              } else {
                this.textValue = ''
              }
              this.searchApplied()
            } else {
              this.close()
            }
          }
          break;
        }
        case 'Enter': {
          if (this.config.singleSelection) {
            if (this.focusedAvailableItemIndex != undefined) {
              const item = this.visibleAvailableItems[this.focusedAvailableItemIndex]
              this.selectAvailableItem(item)
            } else if (this.focusedSelectedItemIndex != undefined) {
              const item = this.selectedItems[this.focusedSelectedItemIndex]
              this.selectSelectedItem(item)
            } else if (this.visibleAvailableItems.length == 1) {
              this.selectAvailableItem(this.visibleAvailableItems[0])
            }
          } else {
            if (this.visibleAvailableItems.length == 1 && this.selectedItems.length == 0) {
              this.selectAvailableItem(this.visibleAvailableItems[0])
            }
            this.applyItems()
            setTimeout(() => {
              this.close()
            })
          }
          break;
        }
        case 'ArrowDown': {
          event.preventDefault()
          if (this.selectedItems.length > 0 || this.visibleAvailableItems.length > 0) {
            if (this.textFieldVisible()) {
              this.filterTextElement!.nativeElement.blur()
            }
            if (this.focusedSelectedItemIndex != undefined) {
              if (this.focusedSelectedItemIndex + 1 < this.selectedItems.length) {
                this.focusedSelectedItemIndex += 1
              } else if (this.visibleAvailableItems.length > 0) {
                this.focusedSelectedItemIndex = undefined
                this.focusedAvailableItemIndex = 0
              }
            } else if (this.focusedAvailableItemIndex != undefined) {
              if (this.focusedAvailableItemIndex + 1 < this.visibleAvailableItems.length) {
                this.focusedAvailableItemIndex += 1
              }
            } else if (!this.config.singleSelection && this.selectedItems.length > 0) {
              this.focusedSelectedItemIndex = 0
            } else if (this.visibleAvailableItems.length > 0) {
              this.focusedAvailableItemIndex = 0
            }
            this.ensureFocusedItemIsVisible()
          }
          break;
        }
        case 'ArrowUp': {
          event.preventDefault()
          if (this.focusedSelectedItemIndex != undefined) {
            if (this.focusedSelectedItemIndex > 0) {
              this.focusedSelectedItemIndex -= 1
            } else {
              if (this.textFieldVisible()) {
                this.focusedSelectedItemIndex = undefined
                this.filterTextElement!.nativeElement.focus()
              }
            }
          } else if (this.focusedAvailableItemIndex != undefined) {
            if (this.focusedAvailableItemIndex > 0) {
              this.focusedAvailableItemIndex -= 1
            } else if (!this.config.singleSelection && this.selectedItems.length > 0) {
              this.focusedSelectedItemIndex = this.selectedItems.length - 1
              this.focusedAvailableItemIndex = undefined
            } else {
              this.focusedAvailableItemIndex = undefined
              if (this.textFieldVisible()) {
                this.filterTextElement!.nativeElement.focus()
              }
            }
          }
          this.ensureFocusedItemIsVisible()
        }
        break;
        case ' ': {
          if (this.focusedSelectedItemIndex != undefined) {
            event.preventDefault()
            this.selectSelectedItem(this.selectedItems[this.focusedSelectedItemIndex])
          } else if (this.focusedAvailableItemIndex != undefined) {
            event.preventDefault()
            this.selectAvailableItem(this.visibleAvailableItems[this.focusedAvailableItemIndex])
          }
          break;
        }
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

  controlMouseDown(event: MouseEvent) {
    if (!this.formVisible) {
      // Prevent the focus when clicking the mouse
      event.preventDefault()
    }
  }

  controlClicked() {
    this.focusedSelectedItemIndex = undefined
    this.focusedAvailableItemIndex = undefined
    if (this.formVisible) {
      this.close()
    } else {
      this.formVisible = true
      this.calculateSizeAndPosition()
      this.updateCheckFlag()
      this.updateSingleSelectionClearFlag()
      this.loadData().subscribe(() => {
        this.calculateSizeAndPosition()
        if (this.typeahead) {
          setTimeout(() => {
            if (this.filterTextElement) {
              this.filterTextElement.nativeElement.focus()
            }
          })
        }
      })
    }
  }

  private calculateSizeAndPosition() {
    this.openToLeft = this.shouldOpenToLeft()
    const buffer = 20;
    const minWidth = 400;
    if (this.filterControlElement) {
      // Width calculation
      const triggerRect = this.filterControlElement.nativeElement.getBoundingClientRect();
      const viewportWidth = window.innerWidth;
      let availableWidth: number;
      if (this.openToLeft) {
        availableWidth = triggerRect.right - buffer;
      } else {
        availableWidth = viewportWidth - triggerRect.left - buffer;
      }
      // Start with element's natural width
      let desiredWidth = this.filterControlElement.nativeElement.offsetWidth;
      // Clamp to min width
      if (desiredWidth < minWidth) {
        desiredWidth = minWidth;
      }
      // Clamp to available space
      this.formWidth = Math.min(desiredWidth, availableWidth);
      // Height calculation
    } else {
      this.formWidth = minWidth;
    }
    this.adjustHeight(300)
  }

  private adjustHeight(minHeight: number) {
    this.availableItemsHeight = minHeight;
    setTimeout(() => {
      if (this.filterFormElement && this.filterControlElement) {
        let fitsBottom: boolean
        let fitsTop: boolean
        let formHeight = this.filterFormElement.nativeElement.offsetHeight
        const coords = this.filterControlElement.nativeElement.getBoundingClientRect()
        const controlHeight = this.filterControlElement.nativeElement.innerHeight
        let controlBottom: number|undefined
        if (coords.bottom != undefined) {
          controlBottom = coords.bottom
        } else if (coords.y != undefined) {
          controlBottom = coords.y
        } else {
          controlBottom = 0
        }
        let controlTop: number
        if (coords.top != undefined) {
          controlTop = coords.top
        } else if (coords.y != undefined) {
          controlTop = coords.y - controlHeight
        } else {
          controlTop = 0
        }
        const maxHeight = window.innerHeight
        const formBottom = controlBottom + formHeight
        const formTop = controlTop - formHeight
        fitsBottom = formBottom <= maxHeight
        fitsTop = formTop > 0
        if (fitsBottom) {
          this.formTop = controlHeight
        } else if (fitsTop) {
          this.formTop = formHeight * -1
        } else {
          if (minHeight > 80) {
            this.availableItemsHeight = minHeight - 20
            this.adjustHeight(minHeight - 20)
          }
        }
      }
    })
  }

  private updateSingleSelectionClearFlag() {
    this.showClearIcon = this.config.singleSelection === true && this.config.singleSelectionClearable === true && this.selectedItems.length > 0
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

  private loadData(): Observable<any> {
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
    return loadObservable.pipe(
      map((items) => {
        this.loadPending = false
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
      }),
      share()
    )
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
    this.focusedSelectedItemIndex = undefined
    this.focusedAvailableItemIndex = undefined
    this.selectAvailableItem(item)
  }

  selectAvailableItem(item: T) {
    if (this.config.singleSelection) {
      this.selectedItems = [item]
      const itemToReport: FilterValues<T> = { active: [], other: [] }
      itemToReport.active = this.getItemsToSignalForItem(item)
      this._value = item
      this.emitChanges()
      if (this.config.eventsDisabled != true) {
        this.apply.emit({ values: itemToReport, applyFilters: false })
      }
      this.updateLabel()
      this.close()
    } else {
      this.itemClicked(this.selectedAvailableItems, item)
    }
  }

  selectedItemClicked(item: T) {
    this.focusedSelectedItemIndex = undefined
    this.focusedAvailableItemIndex = undefined
    this.selectSelectedItem(item)
  }

  selectSelectedItem(item: T) {
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
    if (this.config.singleSelection) {
      if (selectedItemsToReport.active.length == 0) {
        this._value = undefined
      } else {
        this._value = selectedItemsToReport.active[0]
      }
    } else {
      this._value = selectedItemsToReport.active
    }
    this.emitChanges()
    if (this.config.eventsDisabled != true) {
      this.apply.emit({ values: selectedItemsToReport, applyFilters: skipRefresh == undefined || !skipRefresh })
    }
  }

  clearSingleSelection() {
    this.clearSelectedItems()
    this.applyItems()
  }

  clearSelectedItems() {
    this.focusedSelectedItemIndex = undefined
    this.focusedAvailableItemIndex = undefined
    this.clearSelectedItemMap(this.selectedSelectedItems)
    this.clearSelectedItemMap(this.selectedAvailableItems)
    this.updateCheckFlag()
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
    this.focusedSelectedItemIndex = undefined
    this.focusedAvailableItemIndex = undefined
    this.textValue = ''
    // Make sure that previously selected items that have been unchecked but not applied are undone.
    for (let itemId in this.selectedSelectedItems) {
      this.selectedSelectedItems[itemId].selected = true
    }
    for (let itemId in this.selectedAvailableItems) {
      this.selectedAvailableItems[itemId].selected = false
    }
  }

  selectedItemCheckFocused(event: FocusEvent) {
    (event.target as HTMLElement).blur();
  }

  availableItemCheckFocused(event: FocusEvent) {
    (event.target as HTMLElement).blur();
  }

  private ensureFocusedItemIsVisible() {
    let element: HTMLElement|null = null
    if (this.focusedSelectedItemIndex != undefined) {
      element = document.querySelector('.itemContainer.selected-item-'+this.focusedSelectedItemIndex)
    } else if (this.focusedAvailableItemIndex != undefined) {
      element = document.querySelector('.itemContainer.available-item-'+this.focusedAvailableItemIndex)
    }
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }

  protected readonly Constants = Constants;
}
