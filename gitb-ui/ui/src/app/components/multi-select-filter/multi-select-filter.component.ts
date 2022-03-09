import { Component, ElementRef, EventEmitter, HostListener, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { ItemMap } from './item-map';
import { MultiSelectConfig } from './multi-select-config';
import { MultiSelectItem } from './multi-select-item';
import { filter } from 'lodash';

@Component({
  selector: 'app-multi-select-filter',
  templateUrl: './multi-select-filter.component.html',
  styleUrls: [ './multi-select-filter.component.less' ]
})
export class MultiSelectFilterComponent implements OnInit, OnDestroy {

  @Input() config!: MultiSelectConfig
  @Input() typeahead = true
  @Output() apply = new EventEmitter<MultiSelectItem[]>()
  @ViewChild('filterText') filterTextElement?: ElementRef

  selectedItems: MultiSelectItem[] = []
  availableItems: MultiSelectItem[] = []
  visibleAvailableItems: MultiSelectItem[] = []
  selectedAvailableItems: ItemMap = {}
  selectedSelectedItems: ItemMap = {}
  formVisible = false
  hasCheckedSelectedItem = false
  filterLabel = 'All'
  openToLeft = false
  loadPending = true
  textValue = ''
  focusInterval?: any

  constructor(
    private eRef: ElementRef,
    private dataService: DataService
  ) { }

  ngOnInit(): void {
    if (this.config.clearItems) {
      this.config.clearItems.subscribe(() => {
        this.selectedSelectedItems = {}
        this.updateCheckFlag(false)
        this.updateLabel()
        this.selectedItems = []
      })
    }
    if (this.config.replaceSelectedItems) {
      this.config.replaceSelectedItems.subscribe((newItems) => {
        const allowedIdSet = this.dataService.asIdSet(newItems)
        const previousIdSet = this.selectedSelectedItems
        for (let id in previousIdSet) {
          if (!allowedIdSet[id]) {
            delete this.selectedSelectedItems[id]
          }
        }
        this.selectedItems = this.sortItems(newItems)
        this.updateCheckFlag()
        this.updateLabel()
      })
    }
  }

  ngOnDestroy(): void {
    if (this.focusInterval) {
      clearInterval(this.focusInterval)
      this.focusInterval = undefined
    }
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
      this.textValue = ''
      this.searchApplied()
    }
  }

  private updateLabel() {
    if (this.selectedItems.length == 0) {
      this.filterLabel = 'All'
    } else {
      this.filterLabel = "("+this.selectedItems.length+")"
    }
  } 

  private hasCheckedItems(itemMap: ItemMap) {
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

  private loadData() {
    this.selectedAvailableItems = {}
    this.availableItems = []
    this.visibleAvailableItems = []
    this.loadPending = true
    if (this.config.loader) {
      this.config.loader().subscribe((items) => {
        const newSelectedAvailableItems: ItemMap = {}
        const newAvailableItems = []
        for (let item of items) {
          if (!this.selectedSelectedItems[item.id]) {
            newAvailableItems.push(item)
            newSelectedAvailableItems[item.id] = { selected: false, item: item }
          }
        }
        this.selectedAvailableItems = newSelectedAvailableItems
        this.availableItems = newAvailableItems
        this.visibleAvailableItems = this.availableItems
      }).add(() => {
        this.loadPending = false
      })
    }
  }

  searchApplied() {
    if (this.textValue.length == 0) {
      this.visibleAvailableItems = this.availableItems
    } else {
      const textForSearch = this.textValue.trim().toLowerCase()
      this.visibleAvailableItems = filter(this.availableItems, (item) => { return (<string>item[this.config.textField]).toLowerCase().indexOf(textForSearch) >= 0 })
    }
  }

  availableItemClicked(item: MultiSelectItem) {
    this.itemClicked(this.selectedAvailableItems, item)
  }

  selectedItemClicked(item: MultiSelectItem) {
    this.itemClicked(this.selectedSelectedItems, item)
  }

  private itemClicked(itemMap: ItemMap, item: MultiSelectItem) {
    itemMap[item.id].selected = !itemMap[item.id].selected
    if (itemMap[item.id].selected) {
      this.updateCheckFlag(true)
    } else {
      this.updateCheckFlag()
    }
  }

  private sortItems(items: MultiSelectItem[]) {
    return items.sort((a, b) => { return (<string>a[this.config.textField]).localeCompare(<string>b[this.config.textField])})
  }

  applyItems() {
    this.formVisible = false
    // Remove previously selected items that were unchecked.
    for (let itemId in this.selectedSelectedItems) {
      if (!this.selectedSelectedItems[itemId].selected) {
        delete this.selectedSelectedItems[itemId]
      }
    }
    const newSelectedItems = []
    for (let item of this.selectedItems) {
      if (this.selectedSelectedItems[item.id] != undefined) {
        newSelectedItems.push(this.selectedSelectedItems[item.id].item)
      }
    }
    // Add available items that have been checked.
    for (let itemId in this.selectedAvailableItems) {
      if (this.selectedAvailableItems[itemId].selected) {
        const item = this.selectedAvailableItems[itemId].item
        newSelectedItems.push(item)
        this.selectedSelectedItems[itemId] = { selected: true, item: item }
      }
    }
    this.selectedItems = this.sortItems(newSelectedItems)
    this.updateCheckFlag()
    this.updateLabel()
    this.apply.emit(this.selectedItems)
  }

  clearSelectedItems() {
    this.clearSelectedItemMap(this.selectedSelectedItems)
    this.clearSelectedItemMap(this.selectedAvailableItems)
  }

  private clearSelectedItemMap(itemMap: ItemMap) {
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
  }

}
