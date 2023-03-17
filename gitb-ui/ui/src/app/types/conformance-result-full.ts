import { Counters } from "../components/test-status-icons/counters";
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
    specGroupName?: string
    specGroupOptionName: string
    actorId: number
    actorName: string
    testSuiteName: string
    testCaseName: string
    testCaseDescription?: string
    failed: number
    completed: number
    undefined: number
    result?: string
    updateTime?: string
    outputMessage?: string

    testCasesLoaded?: boolean
    testCases?: Partial<ConformanceStatusItem>[]
    counters?: Counters
    [key: string]: any

}
