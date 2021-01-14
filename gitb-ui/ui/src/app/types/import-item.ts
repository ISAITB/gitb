export interface ImportItem {

    name?: string
    type: number
    match: number
    target?: string
    source?: string
    children: ImportItem[]
    process: number

}
