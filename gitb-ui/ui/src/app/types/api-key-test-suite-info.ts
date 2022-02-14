import { ApiKeyTestCaseInfo } from "./api-key-test-case-info";

export interface ApiKeyTestSuiteInfo {

    name: string,
    key: string,
    testCases: ApiKeyTestCaseInfo[]

}
