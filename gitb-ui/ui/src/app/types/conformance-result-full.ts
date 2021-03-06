import { ConformanceStatusItem } from "./conformance-status-item";

export interface ConformanceResultFull {

    communityId: number
    communityName: string
    organizationId: number
    organizationName: string
    systemId: number
    systemName: string
    domainId: number
    domainName: string
    specId: number
    specName: string
    actorId: number
    actorName: string
    testSuiteName: string
    testCaseName: string
    testCaseDescription?: string
    failed: number
    completed: number
    undefined: number
    result?: string
    outputMessage?: string

    testCasesLoaded?: boolean
    testCases?: Partial<ConformanceStatusItem>[]
    [key: string]: any

}
