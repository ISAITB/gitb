import { EventEmitter } from "@angular/core";
import { Observable } from "rxjs";
import { MultiSelectItem } from "./multi-select-item";

export interface MultiSelectConfig {

    textField: string
    loader?: () => Observable<MultiSelectItem[]>
    clearItems?: EventEmitter<void>
    replaceSelectedItems?: EventEmitter<MultiSelectItem[]>

}
