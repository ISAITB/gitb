import { ConformanceTestCase } from "./conformance-test-case";

export interface ConformanceTestSuite {

    id: number
    sname: string
    description?: string
    result: string
    hasDocumentation: boolean
    testCases: ConformanceTestCase[]
    expanded: boolean
    hasOptionalTestCases?: boolean
    hasDisabledTestCases?: boolean

}
