import { TestSuiteUploadItemResult } from "./test-suite-upload-item-result"
import { TestSuiteUploadResultSpec } from "./test-suite-upload-result-spec";
import { TestSuiteUploadSpecTestCases } from "./test-suite-upload-spec-test-cases";
import { TestSuiteUploadTestCase } from "./test-suite-upload-test-case";
import { ValidationReport } from "./validation-report";

export interface TestSuiteUploadResult {

    success: boolean
    errorInformation?: string
    pendingFolderId?: string
    existsForSpecs: TestSuiteUploadResultSpec[]
    matchingDataExists: number[]
    items: TestSuiteUploadItemResult[]
    validationReport?: ValidationReport
    needsConfirmation: boolean
    testCases?: TestSuiteUploadSpecTestCases[]
    sharedTestSuiteId?: number
    sharedTestCases?: TestSuiteUploadTestCase[]
    updateMetadata: boolean
    updateSpecification: boolean
}
