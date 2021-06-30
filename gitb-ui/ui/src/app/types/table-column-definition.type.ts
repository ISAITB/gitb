export interface TableColumnDefinition {

    field: string,
    title: string,
    iconFn?: (columnData: any) => string,
    headerClass?: string,
    sortable?: boolean,
    order?: 'asc'|'desc'|null

}
