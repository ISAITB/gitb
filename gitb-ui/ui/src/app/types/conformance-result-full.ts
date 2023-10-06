import { Counters } from "../components/test-status-icons/counters";
import { ConformanceTestSuite } from "../pages/organisation/conformance-statement/conformance-test-suite";
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
    failedOptional: number
    completedOptional: number
    undefinedOptional: number
    result?: string
    updateTime?: string
    outputMessage?: string

    testSuitesLoaded?: boolean
    hasBadge?: boolean
    testSuites?: ConformanceTestSuite[]
    counters?: Counters
    copyBadgePending?: boolean

    [key: string]: any

}
