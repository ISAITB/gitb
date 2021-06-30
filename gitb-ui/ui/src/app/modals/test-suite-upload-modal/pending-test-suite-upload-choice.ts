export interface PendingTestSuiteUploadChoice {

    specification: number
    action: 'cancel'|'drop'|'proceed'
    pending_action_history: 'keep'|'drop'
    pending_action_metadata: 'skip'|'update'

}
