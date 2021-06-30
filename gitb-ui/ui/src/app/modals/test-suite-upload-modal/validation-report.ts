export interface ValidationReport {

    counters: {
        infos: number
        errors: number
        warnings: number
    }
    result: string
    reports: {
        level: string
        assertionId?: string
        description?: string
        location?: string
    }[]

}
