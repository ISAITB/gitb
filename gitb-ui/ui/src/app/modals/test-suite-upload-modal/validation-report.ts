import { ValidationReportItem } from "./validation-report-item"

export interface ValidationReport {

    counters: {
        infos: number
        errors: number
        warnings: number
    }
    result: string
    reports: ValidationReportItem[]

}
