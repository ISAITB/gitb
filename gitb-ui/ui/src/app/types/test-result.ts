export interface TestResult {

    sessionId: string,
    systemId?: number,
    actorId?: number,
    testId?: number,
    specificationId?: number,
    result: "SUCCESS"|"FAILURE"|"UNDEFINED",
    startTime: string,
    endTime?: string,
    tpl?: string,
    outputMessage?: string,
    obsolete: boolean

}
