import { TestSuiteUploadItemResult } from "./test-suite-upload-item-result"
import { TestSuiteUploadSpecTestCases } from "./test-suite-upload-spec-test-cases";
import { ValidationReport } from "./validation-report";

export interface TestSuiteUploadResult {

    success: boolean
    errorInformation?: string
    pendingFolderId?: string
    existsForSpecs: number[]
    matchingDataExists: number[]
    items: TestSuiteUploadItemResult[]
    validationReport?: ValidationReport
    needsConfirmation: boolean
    testCases: TestSuiteUploadSpecTestCases[]
}
