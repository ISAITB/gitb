import { SessionData } from "src/app/components/diagram/test-session-presentation/session-data";

export interface TestResultForDisplay extends SessionData {

    domain?: string,
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
    testCaseId?: number,
    deletePending?: boolean,
    exportPending?: boolean,
    checked?: boolean,
    communityId?: number
    
}
