export interface FilterState {

  filters: string[],
  names?: {[key: string]: string}
  updatePending: boolean,
  updateDisabled: boolean,
  filterData?: () => {[key: string]: any}

}
