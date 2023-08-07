import { ConformanceStatusItem } from "./conformance-status-item";

export interface ConformanceStatus {

    summary: {
        failed: number
        completed: number
        undefined: number
        failedOptional: number
        completedOptional: number
        undefinedOptional: number
        result: string
        updateTime?: string
    }
    items: ConformanceStatusItem[]

}
