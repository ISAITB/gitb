export interface FilterState {

    filters: string[],
    updatePending: boolean,
    filterData?: () => {[key: string]: any} 

}
