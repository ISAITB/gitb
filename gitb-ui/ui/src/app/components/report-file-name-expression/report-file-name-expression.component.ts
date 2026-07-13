/*
 * Copyright (C) 2026 European Union
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

import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';
import {KeyValue} from 'src/app/types/key-value';

/**
 * The input control used to define the naming expression for a report's file name. Used both in
 * the system administration report settings and in the community report settings. Encapsulates
 * only the input group itself - the surrounding label and tooltip are left to the consumer.
 */
@Component({
    selector: 'app-report-file-name-expression',
    templateUrl: './report-file-name-expression.component.html',
    styleUrl: './report-file-name-expression.component.less',
    standalone: false
})
export class ReportFileNameExpressionComponent implements OnChanges {

  @Input() reportType!: number
  @Input() value?: string
  @Input() readonly = false
  @Output() valueChange = new EventEmitter<string>()

  placeholders: KeyValue[] = []
  extension = ''

  constructor(
    private readonly dataService: DataService,
    private readonly popupService: PopupService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['reportType'] != undefined) {
      this.placeholders = this.placeholdersFor(this.reportType)
      this.extension = this.extensionFor(this.reportType)
    }
  }

  onValueChange(newValue: string) {
    this.value = newValue
    this.valueChange.emit(newValue)
  }

  selectPlaceholder(placeholder: KeyValue) {
    this.dataService.copyToClipboard(placeholder.key).subscribe(() => {
      this.popupService.success('Placeholder copied to clipboard.')
    })
  }

  private placeholdersFor(reportType: number): KeyValue[] {
    const organisation = this.dataService.labelOrganisationLower()
    const system = this.dataService.labelSystemLower()
    const specification = this.dataService.labelSpecificationLower()
    const actor = this.dataService.labelActorLower()
    const organisationPlaceholder: KeyValue = { key: '${ORGANISATION}', value: `The ${organisation} for which the report is generated.` }
    const systemPlaceholder: KeyValue = { key: '${SYSTEM}', value: `The ${system} for which conformance is being tested.` }
    const datePlaceholder: KeyValue = { key: '${DATE}', value: 'The report date.' }
    const testCaseNamePlaceholder: KeyValue = { key: '${TEST_CASE_NAME}', value: 'The name of the related test case.' }
    const conformanceTarget = (targetOf: string): KeyValue => (
      { key: '${CONFORMANCE_TARGET}', value: `The target of the conformance ${targetOf} (${specification} or ${actor}).` }
    )
    switch (reportType) {
      case Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_REPORT:
        return [organisationPlaceholder, systemPlaceholder, conformanceTarget('statement'), datePlaceholder]
      case Constants.REPORT_TYPE.CONFORMANCE_OVERVIEW_REPORT:
        return [organisationPlaceholder, systemPlaceholder, conformanceTarget('statements'), datePlaceholder]
      case Constants.REPORT_TYPE.TEST_CASE_REPORT:
        return [organisationPlaceholder, systemPlaceholder, testCaseNamePlaceholder, datePlaceholder]
      case Constants.REPORT_TYPE.TEST_STEP_REPORT:
        return [datePlaceholder]
      case Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_CERTIFICATE:
      case Constants.REPORT_TYPE.CONFORMANCE_OVERVIEW_CERTIFICATE:
        return [organisationPlaceholder, systemPlaceholder, conformanceTarget('certificate'), datePlaceholder]
      case Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_DOCUMENTATION_REPORT:
        return [conformanceTarget('statement'), datePlaceholder]
      default:
        return []
    }
  }

  private extensionFor(reportType: number): string {
    switch (reportType) {
      case Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_REPORT:
      case Constants.REPORT_TYPE.CONFORMANCE_OVERVIEW_REPORT:
      case Constants.REPORT_TYPE.TEST_CASE_REPORT:
      case Constants.REPORT_TYPE.TEST_STEP_REPORT:
        return '.pdf | .xml'
      default:
        return '.pdf'
    }
  }

  protected readonly Constants = Constants;
}
