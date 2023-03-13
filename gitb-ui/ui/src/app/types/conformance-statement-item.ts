export interface ConformanceStatementItem {

    id: number,
    name: string,
    description?: string,
    itemType: number,
    items?: ConformanceStatementItem[]

    hidden?: boolean
    matched?: boolean
    filtered?: boolean
    collapsed?: boolean
    checked?: boolean
}
