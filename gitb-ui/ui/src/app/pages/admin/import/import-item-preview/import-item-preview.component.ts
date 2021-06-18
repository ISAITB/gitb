import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { IdLabel } from 'src/app/types/id-label';
import { ImportItemState } from '../import-item-state';

@Component({
  selector: '[app-import-item-preview]',
  templateUrl: './import-item-preview.component.html',
  styles: [
  ]
})
export class ImportItemPreviewComponent implements OnInit {

  @Input() tbImportItem!: ImportItemState
  @Output() processStatusChange = new EventEmitter<void>()
  Constants = Constants
  iconTooltip!: string
  processOptions: IdLabel[] = []

  constructor() { }

  ngOnInit(): void {
    if (this.tbImportItem.match == Constants.IMPORT_ITEM_MATCH.ARCHIVE_ONLY) {
      this.iconTooltip = "Data defined in the provided archive for which no match with existing data was found."
    } else if (this.tbImportItem.match == Constants.IMPORT_ITEM_MATCH.BOTH) {
      this.iconTooltip = "Existing data that was matched by relevant data from the provided archive."
    } else {
      this.iconTooltip = "Existing data for which no match could be found in the provided archive."
    }
    if (this.tbImportItem.match == Constants.IMPORT_ITEM_MATCH.ARCHIVE_ONLY) {
      this.processOptions.push({id: Constants.IMPORT_ITEM_CHOICE.PROCEED, label: "Create"})
      this.processOptions.push({id: Constants.IMPORT_ITEM_CHOICE.SKIP, label: "Skip"})
    } else if (this.tbImportItem.match == Constants.IMPORT_ITEM_MATCH.BOTH) {
      this.processOptions.push({id: Constants.IMPORT_ITEM_CHOICE.PROCEED, label: "Update"})
      if (this.tbImportItem.hasGroups) {
        this.processOptions.push({id: Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN, label: "Skip but process children"})
      }
      this.processOptions.push({id: Constants.IMPORT_ITEM_CHOICE.SKIP, label: "Skip"})
    } else {
      this.processOptions.push({id: Constants.IMPORT_ITEM_CHOICE.PROCEED, label: "Delete"})
      this.processOptions.push({id: Constants.IMPORT_ITEM_CHOICE.SKIP, label: "Skip"})
    }
    this.applyProcessOption(this.tbImportItem, this.tbImportItem.selectedProcessOption, true)
    this.tbImportItem.selectedProcessOptionUpdated = new EventEmitter<void>()
    this.tbImportItem.selectedProcessOptionUpdated.subscribe(() => {
      this.applyProcessOption(this.tbImportItem, this.tbImportItem.selectedProcessOption!, false)
      this.processStatusChange.emit()
    })
  }

  processOptionChanged() {
    this.tbImportItem.selectedProcessOptionUpdated?.emit()
  }

  itemName(): string {
    if (this.tbImportItem.type == Constants.IMPORT_ITEM_TYPE.CUSTOM_LABEL) {
      return Constants.LABEL_TYPE_LABEL[Number(this.tbImportItem.name!)]
    } else {
      return this.tbImportItem.name!
    }
  }

  toggleItem() {
    if (this.tbImportItem.hasGroups) {
      if (this.tbImportItem.open) {
        this.closeItem(this.tbImportItem)
      } else {
        this.tbImportItem.open = true
      }
    }
  }

  closeItem(item: ImportItemState) {
    if (item.groups != undefined) {
      item.open = false
      for (let group of item.groups) {
        group.open = false
        for (let groupItem of group.items) {
          this.closeItem(groupItem)
        }
      }
    }
  }

  applyProcessOption(item: ImportItemState, newOption: number, force: boolean) {
    if (force || item.process != newOption) {
      let optionForSelf:number = newOption
      let processTypeForChildren: undefined|number = undefined

      if (newOption == Constants.IMPORT_ITEM_CHOICE.PROCEED && item.process == Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT) {
        optionForSelf = item.previousOption!
      }
        
      if (optionForSelf == Constants.IMPORT_ITEM_CHOICE.SKIP) {
        processTypeForChildren = Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT
      } else if (optionForSelf == Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT) {
        processTypeForChildren = Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT
      } else if ((optionForSelf == Constants.IMPORT_ITEM_CHOICE.PROCEED || optionForSelf == Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN) && !(item.process == Constants.IMPORT_ITEM_CHOICE.PROCEED || item.process == Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN)) {
        processTypeForChildren = Constants.IMPORT_ITEM_CHOICE.PROCEED
      }

      if (item.process != Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT) {
        // Always keep reference to previous state before being disabled due to parent.
        item.previousOption = item.process
      }
      item.process = optionForSelf
      item.selectedProcessOption = item.process
      if (!force) item.selectedProcessOptionUpdated?.emit()
      if (item.groups != undefined && processTypeForChildren != undefined) {
        for (let group of item.groups) {
          for (let groupItem of group.items) {
            groupItem.selectedProcessOption = processTypeForChildren
            if (!force) groupItem.selectedProcessOptionUpdated?.emit()
          }
        }
      }
    }
  }

  isSkipped() {
    return this.isSkipOption(this.tbImportItem.process)
  }

  isSkipOption(processOption: number) {
    return processOption == Constants.IMPORT_ITEM_CHOICE.SKIP || processOption == Constants.IMPORT_ITEM_CHOICE.SKIP_PROCESS_CHILDREN || processOption == Constants.IMPORT_ITEM_CHOICE.SKIP_DUE_TO_PARENT
  }

  showExpandAll() {
    return this.tbImportItem.hasGroups && this.hasClosedChild(this.tbImportItem)
  }

  hasClosedChild(item: ImportItemState) {
    if (!item.open) {
      return true
    } else if (item.groups != undefined) {
      for (let group of item.groups) {
        if (!group.open) {
          return true
        } else {
          for (let groupItem of group.items) {
            if (this.hasClosedChild(groupItem)) {
              return true
            }
          }
        }
      }
    }
    return false
  }

  expandItem(item: ImportItemState) {
    item.open = true
    if (item.groups != undefined) {
      for (let group of item.groups) {
        group.open = true
        for (let groupItem of group.items) {
          this.expandItem(groupItem)
        }
      }
    }
  }

  childProcessStatusUpdated() {
    this.processStatusChange.emit()
  }

}
