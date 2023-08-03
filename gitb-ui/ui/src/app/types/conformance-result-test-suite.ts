import { ConformanceStatusItem } from "./conformance-status-item";

export interface ConformanceResultTestSuite {

    testSuiteId: number
    testSuiteName: string
    testCases: ConformanceStatusItem[]
    result: string
    expanded: boolean
    hasOptionalTestCases?: boolean
    hasDisabledTestCases?: boolean

}
