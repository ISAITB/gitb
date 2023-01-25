import { TestSuiteUploadTestCase } from "./test-suite-upload-test-case"
import { TestSuiteUploadTestCaseChoice } from "./test-suite-upload-test-case-choice"

export interface SpecificationChoice {

    specification: number
    updateActors: boolean
    updateTestSuite: boolean
    skipUpdate: boolean
    dataExists: boolean
    testSuiteExists: boolean
    testCasesInArchiveAndDB: TestSuiteUploadTestCaseChoice[]
    testCasesInArchive: TestSuiteUploadTestCase[]
    testCasesInDB: TestSuiteUploadTestCase[]

    showTestCasesToUpdate: boolean
    showTestCasesToDelete: boolean
    showTestCasesToAdd: boolean
}
