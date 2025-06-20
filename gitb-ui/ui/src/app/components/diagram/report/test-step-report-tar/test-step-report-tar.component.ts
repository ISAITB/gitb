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

import { Component, Input, OnInit } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { ReportService } from 'src/app/services/report.service';
import { AnyContent } from '../../any-content';
import { AssertionReport } from '../../assertion-report';
import { ReportSupport } from '../report-support';
import { StepReport } from '../step-report';
import { HtmlService } from 'src/app/services/html.service';
import { DataService } from 'src/app/services/data.service';

@Component({
    selector: '[app-test-step-report-tar]',
    templateUrl: './test-step-report-tar.component.html',
    styleUrls: ['./test-step-report-tar.component.less'],
    standalone: false
})
export class TestStepReportTARComponent extends ReportSupport implements OnInit {

  @Input() report!: StepReport
  @Input() sessionId!: string
  hasContextItems = false

  constructor(
    modalService: BsModalService,
    reportService: ReportService,
    htmlService: HtmlService,
    dataService: DataService
  ) { super(modalService, reportService, htmlService, dataService) }

  ngOnInit(): void {
    // Calculate the value of each item in the report context
    if (this.report.context) {
      this.setContextValues(this.report.context)
      if (this.report.context.value != undefined || (this.report.context.item != undefined && this.report.context.item.length > 0)) {
        this.hasContextItems = true
      }
    }
  }

  private setContextValues(context: AnyContent) {
    if (context.value != undefined) {
      context.valueToUse = context.value
      if (!this.isFileReference(context) && context.embeddingMethod == Constants.EMBEDDING_METHOD.BASE64) {
        context.valueToUse = this.base64ToString(context.valueToUse)
        context.embeddingMethod = 'STRING'
      }
    }
    if (context.item != undefined) {
      for (let childContext of context.item) {
        this.setContextValues(childContext)
      }
    }
  }

  private base64ToString(base64: string) {
    return atob(base64)
  }

  private findContextEntryByName(context: AnyContent, nameToFind: string): AnyContent|undefined {
    if (context.name != undefined && context.name.toLocaleLowerCase() == nameToFind) {
      return context
    } else if (context.item != undefined) {
      for (let childContext of context.item) {
        const found = this.findContextEntryByName(childContext, nameToFind)
        if (found != undefined) {
          return found
        }
      }
    }
    return undefined
  }

  openAssertionReport(assertionReport: AssertionReport) {
    let location = assertionReport.extractedLocation
    if (location?.name != undefined && this.report.context) {
      // Find the relevant value to display
      const relevantContextItem = this.findContextEntryByName(this.report.context, location.name.toLocaleLowerCase())
      if (relevantContextItem != undefined) {
        this.commonOpen(relevantContextItem, this.sessionId, this.report.reports?.assertionReports, location?.line).subscribe()
      }
    }
  }

}
