import { ConformanceTestSuite } from "../pages/organisation/conformance-statement/conformance-test-suite";
import { ConformanceStatusSummary } from "./conformance-status-summary";

export interface ConformanceStatus {

    summary: ConformanceStatusSummary
    testSuites: ConformanceTestSuite[]

}
