/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
