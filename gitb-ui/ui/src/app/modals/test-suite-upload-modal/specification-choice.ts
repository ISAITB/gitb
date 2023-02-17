import { TestSuiteUploadTestCaseChoice } from "./test-suite-upload-test-case-choice"

export interface SpecificationChoice {

    specification: number
    name: string
    updateActors: boolean
    sharedTestSuite: boolean
    updateTestSuite: boolean
    skipUpdate: boolean
    dataExists: boolean
    testSuiteExists: boolean
    testCasesInArchiveAndDB: TestSuiteUploadTestCaseChoice[]
    testCasesInArchive: TestSuiteUploadTestCaseChoice[]
    testCasesInDB: TestSuiteUploadTestCaseChoice[]
}
