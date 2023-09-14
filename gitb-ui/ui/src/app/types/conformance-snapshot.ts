export interface ConformanceSnapshot {

    id: number
    label: string
    snapshotTime: string
    community: number

    actionPending?: boolean
    deletePending?: boolean
    visible?: boolean

}
