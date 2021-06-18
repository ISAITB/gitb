import { AnyContent } from "../any-content";
import { AssertionReport } from "../assertion-report";

export interface StepReport {

    context?: AnyContent
    counters?: {
        nrOfAssertions: number
        nrOfErrors: number
        nrOfWarnings: number
    }
    date?: string
    id?: string
    name?: string
    overview?: {
        profileID?: string
        customizationID?: string
        transactionID?: string
        validationServiceName?: string
        validationServiceVersion?: string
        note?: string
    }
    reports?: {
        assertionReports: AssertionReport[]
    }
    result?: string
    type?: 'TAR'|'DR'|'SR'
    decision?: string
    tcInstanceId?: string
    path?: string

}
