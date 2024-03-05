import { EventEmitter } from "@angular/core"

export interface ConfigStatus {

    pending: boolean
    collapsed: boolean
    enabled?: boolean
    fromDefault?: boolean
    fromEnv?: boolean
    deferredExpand?: EventEmitter<boolean>

}
