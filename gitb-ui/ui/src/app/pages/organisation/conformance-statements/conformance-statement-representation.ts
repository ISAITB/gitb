import { Counters } from "src/app/components/test-status-icons/counters";

export interface ConformanceStatementRepresentation {

    domainId: number
    domain: string
    domainFull: string
    actorId: number
    actor: string
    actorFull: string
    specificationId: number
    specification: string
    specificationFull: string
    status: string
    updateTime?: string
    counters?: Counters
    
}