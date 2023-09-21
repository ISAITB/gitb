import { ConformanceStatementItem } from "./conformance-statement-item";
import { ConformanceStatus } from "./conformance-status";
import { Organisation } from "./organisation.type";
import { System } from "./system";

export interface ConformanceStatementWithResults {

    statement: ConformanceStatementItem
    results: ConformanceStatus
    system: System
    organisation: Organisation

}
