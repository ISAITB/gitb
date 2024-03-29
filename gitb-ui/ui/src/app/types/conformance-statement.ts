export interface ConformanceStatement {

    domainId: number
    domain: string
    domainFull: string
    actorId: number
    actor: string
    actorFull: string
    specificationId: number
    specification: string
    specificationFull: string
    updateTime?: string
    results: {
        undefined: number,
        failed: number,
        completed: number,
        undefinedOptional: number,
        failedOptional: number,
        completedOptional: number
    }

}
