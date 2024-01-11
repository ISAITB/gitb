import { SessionPresentationData } from "./session-presentation-data";

export interface SessionData {

    session: string
    endTime?: string
    result: 'SUCCESS'|'FAILURE'|'UNDEFINED'
    diagramLoaded?: boolean
    hideLoadingIcon?: boolean
    diagramExpanded?: boolean
    expanded?: boolean
    testSuite: string
    testCase: string
    diagramState?: SessionPresentationData

    hasUnreadErrorLogs?: boolean
    hasUnreadWarningLogs?: boolean
    hasUnreadMessageLogs?: boolean
    reviewedLogLines?: number

}
