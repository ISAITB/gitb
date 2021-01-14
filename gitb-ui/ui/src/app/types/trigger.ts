export interface Trigger {

    id: number
    name: string
    description?: string
    operation?: string
    url: string
    eventType: number
    eventTypeLabel?: string
    latestResultOk?: boolean
    active: boolean
    status?: number
    statusText?: string
    latestResultOutput?: string
}
