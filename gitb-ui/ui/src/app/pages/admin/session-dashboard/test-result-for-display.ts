import { SessionData } from "src/app/components/diagram/test-session-presentation/session-data";

export interface TestResultForDisplay extends SessionData {

    // session: string,
    domain?: string,
    specification?: string,
    actor?: string,
    organization?: string,
    system?: string,
    startTime: string,
    obsolete: boolean,
    testCaseId?: number,
    deletePending?: boolean,
    exportPending?: boolean,
    checked?: boolean,
}
