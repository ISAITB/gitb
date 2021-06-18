import { EventEmitter } from "@angular/core";
import { ImportItem } from "src/app/types/import-item";
import { ImportItemStateGroup } from "./import-item-state-group";

export interface ImportItemState extends ImportItem {

    id: number
    children: ImportItemState[]
    open: boolean
    groups: ImportItemStateGroup[]
    hasGroups: boolean
    previousOption?: number
    disableProcessChoice: boolean
    selectedProcessOption: number
    selectedProcessOptionUpdated?: EventEmitter<void>

}
