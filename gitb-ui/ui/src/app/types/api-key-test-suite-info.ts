import { ApiKeyTestCaseInfo } from "./api-key-test-case-info";

export interface ApiKeyTestSuiteInfo {

    id: number,
    name: string,
    key: string,
    testCases: ApiKeyTestCaseInfo[]

}
