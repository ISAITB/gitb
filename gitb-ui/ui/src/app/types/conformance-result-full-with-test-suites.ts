import { ConformanceResultFull } from "./conformance-result-full";
import { ConformanceResultTestSuite } from "./conformance-result-test-suite";

export interface ConformanceResultFullWithTestSuites extends ConformanceResultFull {

    testSuites?: ConformanceResultTestSuite[]

}
