import { ConformanceIds } from "./conformance-ids"

export interface ConformanceStatusSummary extends ConformanceIds{

    failed: number
    completed: number
    undefined: number
    failedOptional: number
    completedOptional: number
    undefinedOptional: number
    result: string
    updateTime?: string
    hasBadge: boolean,

}
