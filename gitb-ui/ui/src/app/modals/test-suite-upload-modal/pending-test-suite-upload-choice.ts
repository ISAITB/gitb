import { PendingTestSuiteUploadChoiceTestCase } from "./pending-test-suite-upload-choice-test-case"

export interface PendingTestSuiteUploadChoice {

    specification?: number
    action: 'cancel'|'drop'|'proceed'
    updateTestSuite: boolean
    updateActors: boolean
    sharedTestSuite: boolean
    testCaseUpdates: PendingTestSuiteUploadChoiceTestCase[]

}
