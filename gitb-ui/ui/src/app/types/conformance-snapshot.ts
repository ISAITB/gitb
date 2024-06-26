export interface ConformanceSnapshot {

    id: number
    label: string
    publicLabel?: string
    snapshotTime: string
    hidden?: boolean
    apiKey?: string

    actionPending?: boolean
    deletePending?: boolean
    visible?: boolean
    sameLabel?: boolean
    latest?: boolean
    labelToDisplay?: string

}
