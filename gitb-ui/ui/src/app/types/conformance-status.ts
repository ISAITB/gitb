import { ConformanceStatusItem } from "./conformance-status-item";

export interface ConformanceStatus {

    summary: {
        failed: number
        completed: number
        undefined: number
        result: string
        updateTime?: string
    }
    items: ConformanceStatusItem[]

}
