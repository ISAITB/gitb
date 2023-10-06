import { SessionData } from "src/app/components/diagram/test-session-presentation/session-data";

export interface TestResultForDisplay extends SessionData {

    domain?: string,
    domainId?: number
    specification?: string,
    specificationId?: number,
    actor?: string,
    actorId?: number,
    organization?: string,
    organizationId?: number,
    system?: string,
    systemId?: number,
    startTime: string,
    obsolete: boolean,
    testSuiteId?: number,
    testCaseId?: number,
    deletePending?: boolean,
    exportPending?: boolean,
    actionPending?: boolean,
    checked?: boolean,
    communityId?: number
    
}
