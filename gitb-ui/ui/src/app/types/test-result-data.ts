import { TestResult } from "./test-result";
import { TestResultReport } from "./test-result-report";

export interface TestResultData {

    data: TestResultReport[],
    count?: number,
    orgParameters?: string[]
    sysParameters?: string[]

}
