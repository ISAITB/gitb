export interface ValidationReportItem {

    level: "info"|"warning"|"error"
    assertionId?: string
    description?: string
    location?: string

}
