import { EventEmitter } from "@angular/core";
import { Observable } from "rxjs";
import { EntityWithId } from "../../types/entity-with-id";

export interface MultiSelectConfig<T extends EntityWithId> {

  id?: string
  name: string
  textField: string
  loader?: () => Observable<T[]>
  clearItems?: EventEmitter<void>
  replaceItems?: EventEmitter<T[]>
  replaceSelectedItems?: EventEmitter<T[]>
  singleSelection?: boolean
  singleSelectionPersistent?: boolean
  filterLabel?: string
  noItemsMessage?: string
  searchPlaceholder?: string
  eventsDisabled?: boolean
  initialValues?: T[]

}
