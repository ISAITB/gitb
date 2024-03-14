import { ConformanceStatementItem } from "./conformance-statement-item"

export interface ExportReportEvent {

    statementReport: boolean
    item: ConformanceStatementItem
    actorId?: number
    format: 'xml'|'pdf'

}
