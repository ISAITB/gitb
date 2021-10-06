import { BsModalService } from "ngx-bootstrap/modal"
import { filter, map } from 'lodash'
import { CodeEditorModalComponent } from "../../code-editor-modal/code-editor-modal.component"
import { AssertionReport } from "../assertion-report"
import { Indicator } from "../../code-editor-modal/indicator"
import { ReportService } from "src/app/services/report.service"
import { AnyContent } from "../any-content"
import { mergeMap, Observable, of } from "rxjs"

export abstract class ReportSupport {

    constructor(
        private modalService: BsModalService,
        private reportService: ReportService
    ) {}

    private loadedReferences: {[key: string]: {data: ArrayBuffer, mimeType: string|undefined}} = {}

    openEditorWindow(name?: string, value?: string, assertionReports?: AssertionReport[], lineNumber?: number, mimeType?: string) {
        const indicators = this.extractRelatedIndicators(name, assertionReports)
        this.modalService.show(CodeEditorModalComponent, {
            class: 'modal-lg',
            initialState: {
                documentName: name,
                indicators: indicators,
                lineNumber: lineNumber,
                editorOptions: {
                    value: value,
                    readOnly: true,
                    lineNumbers: true,
                    smartIndent: false,
                    electricChars: false,
                    mode: (mimeType == undefined)?'application/xml':mimeType
                }
            }
        })
    }

    private extractRelatedIndicators(name?: string, assertionReports?: AssertionReport[]) {
        if (name == undefined || assertionReports == undefined) {
            return []
        }
        let relatedAssertionReports = filter(assertionReports, (assertionReport) => {
            let location = assertionReport.extractedLocation
            return name != undefined && location != undefined && location.name.toLowerCase() == name.toLowerCase()
        })
        let indicators: Indicator[] = map(relatedAssertionReports, (assertionReport) => {
            let location = assertionReport.extractedLocation
            const indicator: Indicator = {
                location: location!,
                type: assertionReport.type,
                description: assertionReport.value.description
            }
            return indicator
        })
        return indicators
    }

    isFileReference(item: AnyContent) {
        return item?.valueToUse != undefined && item.valueToUse.startsWith('___[[') && item.valueToUse.endsWith(']]___')
    }
    
    getFileReference(item: AnyContent): string {
        return item.valueToUse!.slice(5, item.valueToUse!.length - 5)
    }
    
    downloadFileReference(sessionId: string, anyContent: AnyContent): Observable<{data: ArrayBuffer, mimeType: string|undefined}> {
        const dataId = this.getFileReference(anyContent)
        if (this.loadedReferences[dataId] == undefined) {
            return this.reportService.getTestStepReportData(sessionId, dataId, anyContent.mimeType).pipe(
                mergeMap((response) => {
                    let mimeType: string|undefined
                    if (response.headers.has('Content-Type')) {
                        mimeType = response.headers.get('Content-Type') as string
                    }
                    const result = {
                        data: response.body as ArrayBuffer,
                        mimeType: mimeType
                    }
                    this.loadedReferences[dataId] = result
                    return of(result)
                })
            )
        } else {
            return of(this.loadedReferences[dataId])
        }
    }

}
