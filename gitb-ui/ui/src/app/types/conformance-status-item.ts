export interface ConformanceStatusItem {

    id?: number
    exportPending?: boolean
    
    testSuiteId: number
    testSuiteName: string
    testSuiteDescription?: string
    testSuiteHasDocumentation: boolean
    testCaseId: number
    testCaseName: string
    testCaseDescription?: string
    testCaseHasDocumentation: boolean
    result: string
    outputMessage?: string
    sessionId?: string

}
