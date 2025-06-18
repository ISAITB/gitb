/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import { BsModalService } from "ngx-bootstrap/modal"
import { filter, map } from 'lodash'
import { CodeEditorModalComponent } from "../../code-editor-modal/code-editor-modal.component"
import { AssertionReport } from "../assertion-report"
import { Indicator } from "../../code-editor-modal/indicator"
import { ReportService } from "src/app/services/report.service"
import { AnyContent } from "../any-content"
import { mergeMap, Observable, of, map as rxMap, share } from "rxjs"
import { HtmlService } from "src/app/services/html.service"
import { DataService } from "src/app/services/data.service"
import { FileReference } from "src/app/types/file-reference"
import { ValueType } from "src/app/types/value-type"
import { ValueWithMetadata } from "src/app/types/value-with-metadata"
import { BaseComponent } from "src/app/pages/base-component.component"

export abstract class ReportSupport extends BaseComponent{

    constructor(
        private modalService: BsModalService,
        private reportService: ReportService,
        private htmlService: HtmlService,
        protected dataService: DataService
    ) { super() }

    private loadedDataUrlReferences: {[key: string]: FileReference} = {}
    private loadedArrayBufferReferences: {[key: string]: FileReference} = {}

    commonOpen(content: AnyContent, sessionId: string, assertionReports?: AssertionReport[], lineNumber?: number) {
        let valueObservable: Observable<ValueWithMetadata>
        if (this.isFileReference(content)) {
          valueObservable = this.downloadFileReference(sessionId, content, true).pipe(
            mergeMap((data) => {
              if (data.dataAsDataURL) {
                return of({ value: data.dataAsDataURL, valueType: ValueType.dataURL, mimeType: data.mimeType})
              } else { // Should normally never happen.
                return of({ value: new TextDecoder("utf-8").decode(data.dataAsBytes), valueType: ValueType.string, mimeType: data.mimeType})
              }
            })
          )
        } else {
          let valueToShow = content.valueToUse!
          let valueType = ValueType.string
          let mimeType = undefined
          if (content.embeddingMethod == 'BASE64') {
            if (this.dataService.isDataURL(valueToShow)) {
              mimeType = this.dataService.mimeTypeFromDataURL(valueToShow)
              valueType = ValueType.dataURL
            } else {
              valueType = ValueType.base64
            }
          } else if (content.embeddingMethod == 'STRING') {
            if (this.dataService.isImageType(content.mimeType)) {
                mimeType = content.mimeType
                if (this.dataService.isDataURL(valueToShow)) {
                    valueType = ValueType.dataURL
                } else {
                    valueType = ValueType.base64
                }
            }
          }
          valueObservable = of({value: valueToShow, valueType: valueType, mimeType: mimeType})
        }
        return valueObservable.pipe(
            rxMap((valueInfo) => {
                if (content.mimeType != undefined) {
                    valueInfo.mimeType = content.mimeType
                }
                this.openEditorWindow(
                    content.name == undefined?"":content.name,
                    valueInfo,
                    assertionReports,
                    lineNumber
                )
            }),
            share()
        )
    }

    private openEditorWindow(name: string, value: ValueWithMetadata, assertionReports?: AssertionReport[], lineNumber?: number) {
        if (value.value != undefined && value.valueType != ValueType.string && value.mimeType != undefined && this.dataService.isImageType(value.mimeType)) {
            let valueToUse = value.value // Keep as-is if data URL
            if (value.valueType == ValueType.base64) {
                valueToUse = this.dataService.dataUrlFromBase64(value.value, value.mimeType)
            }
            this.htmlService.showHtml(
                name == undefined ? "Image preview" : name,
                "<div class='report-image-container'><img src='"+valueToUse+"'></div>",
                "modal-lg"
            )
        } else {
            let valueToUse = value.value // Keep as-is if string
            if (value != undefined) {
                if (value.valueType == ValueType.base64) {
                    valueToUse = atob(value.value)
                } else if (value.valueType == ValueType.dataURL) {
                    valueToUse = atob(this.dataService.base64FromDataURL(value.value))
                }
            } else {
                valueToUse = ""
            }
            const indicators = this.extractRelatedIndicators(name, assertionReports)
            this.modalService.show(CodeEditorModalComponent, {
                class: 'modal-lg',
                initialState: {
                    documentName: name,
                    indicators: indicators,
                    lineNumber: lineNumber,
                    editorOptions: {
                        value: valueToUse,
                        readOnly: true,
                        lineNumbers: true,
                        smartIndent: false,
                        electricChars: false,
                        mode: (value.mimeType == undefined)?'application/xml':value.mimeType
                    }
                }
            })
        }
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

    downloadFileReference(sessionId: string, anyContent: AnyContent, asDataURL: boolean): Observable<FileReference> {
        const dataId = this.getFileReference(anyContent)
        if (asDataURL) {
            if (this.loadedDataUrlReferences[dataId] == undefined) {
                return this.reportService.getTestStepReportDataAsDataUrl(sessionId, dataId, anyContent.mimeType).pipe(
                    mergeMap((response) => {
                        this.loadedDataUrlReferences[dataId] = response
                        return of(response)
                    })
                )
            } else {
                return of(this.loadedDataUrlReferences[dataId])
            }
        } else {
            if (this.loadedArrayBufferReferences[dataId] == undefined) {
                return this.reportService.getTestStepReportData(sessionId, dataId, anyContent.mimeType).pipe(
                    mergeMap((response) => {
                        let mimeType: string|undefined
                        if (response.headers.has('Content-Type')) {
                            mimeType = response.headers.get('Content-Type') as string
                        }
                        const result: FileReference = {
                            dataAsBytes: response.body as ArrayBuffer,
                            mimeType: mimeType
                        }
                        this.loadedArrayBufferReferences[dataId] = result
                        return of(result)
                    })
                )
            } else {
                return of(this.loadedArrayBufferReferences[dataId])
            }
        }
    }

}
