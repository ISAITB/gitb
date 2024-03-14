import { EventEmitter } from "@angular/core";
import { Observable } from "rxjs";
import { EntityWithId } from "../../types/entity-with-id";

export interface MultiSelectConfig<T extends EntityWithId> {

    name: string
    textField: string
    loader?: () => Observable<T[]>
    clearItems?: EventEmitter<void>
    replaceItems?: EventEmitter<T[]>
    replaceSelectedItems?: EventEmitter<T[]>
    singleSelection?: boolean
    singleSelectionPersistent?: boolean
    filterLabel?: string

}
