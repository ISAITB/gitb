import { ImportItemState } from "./import-item-state";

export interface ImportItemStateGroup {

    type: number
    typeLabel: string
    open: boolean
    items: ImportItemState[]

}
