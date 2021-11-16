export interface FilterState {

    filters: string[],
    names?: {[key: string]: string}
    updatePending: boolean,
    filterData?: () => {[key: string]: any} 

}
