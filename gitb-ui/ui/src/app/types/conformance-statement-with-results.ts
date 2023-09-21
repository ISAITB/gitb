import { ConformanceStatementItem } from "./conformance-statement-item";
import { ConformanceStatus } from "./conformance-status";

export interface ConformanceStatementWithResults {

    statement: ConformanceStatementItem,
    results: ConformanceStatus

}
