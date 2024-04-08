export interface ConformanceOverviewMessage {

    id?: number
    level: 'all'|'domain'|'group'|'specification'
    identifier?: number
    message?: string

}
