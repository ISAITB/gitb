import { ConformanceStatementResult } from "./conformance-statement-result"

export interface ConformanceStatementItem {

    id: number,
    name: string,
    description?: string,
    itemType: number,
    order: number
    items?: ConformanceStatementItem[]
    hidden?: boolean
    results?: ConformanceStatementResult

    matched?: boolean
    filtered?: boolean
    collapsed?: boolean
    checked?: boolean
}
