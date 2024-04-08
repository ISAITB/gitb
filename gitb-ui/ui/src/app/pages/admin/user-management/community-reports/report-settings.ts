import { EventEmitter } from "@angular/core"
import { ConfigStatus } from "../../system-administration/config-status"

export class ReportSettings {

    status: ConfigStatus = { pending: false, collapsed: true, deferredExpand: new EventEmitter<boolean>() }
    expanding = new EventEmitter<boolean>()
    expanded = new EventEmitter<void>()

    setExpanding() {
        this.expanding.emit(true)        
    }

    setLoaded() {
        this.status.deferredExpand!.emit(true)        
    }

    setExpanded() {
        this.expanded.emit()
    }

    setCollapsed() {
        this.status.collapsed = true
    }
}
