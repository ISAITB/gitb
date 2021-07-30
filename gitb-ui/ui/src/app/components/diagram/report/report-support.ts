import { BsModalService } from "ngx-bootstrap/modal"
import { filter, map } from 'lodash'
import { CodeEditorModalComponent } from "../../code-editor-modal/code-editor-modal.component"
import { AssertionReport } from "../assertion-report"
import { Indicator } from "../../code-editor-modal/indicator"

export abstract class ReportSupport {

    constructor(
        private modalService: BsModalService
    ) {}

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
    
}
