export interface TableColumnDefinition {

    field: string,
    title: string,
    iconFn?: (columnData: any) => string,
    iconTooltipFn?: (columnData: any) => string,
    headerClass?: string,
    sortable?: boolean,
    order?: 'asc'|'desc'|null
    atEnd?: boolean
    isHiddenFlag?: boolean

}
