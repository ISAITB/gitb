import { ConformanceTestSuite } from "../pages/organisation/conformance-statement/conformance-test-suite";
import { ConformanceResultFull } from "./conformance-result-full";

export interface ConformanceResultFullWithTestSuites extends ConformanceResultFull {

    testSuites?: ConformanceTestSuite[]

}
