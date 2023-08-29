import { ConformanceTestSuite } from "../pages/organisation/conformance-statement/conformance-test-suite";

export interface ConformanceStatus {

    summary: {
        failed: number
        completed: number
        undefined: number
        failedOptional: number
        completedOptional: number
        undefinedOptional: number
        result: string
        updateTime?: string
    }
    testSuites: ConformanceTestSuite[]

}
