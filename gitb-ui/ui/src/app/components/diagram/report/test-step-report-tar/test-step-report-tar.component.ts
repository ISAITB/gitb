import { Component, Input, OnInit } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { mergeMap, Observable, of } from 'rxjs';
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
  styleUrls: ['./test-step-report-tar.component.less']
})
export class TestStepReportTARComponent extends ReportSupport implements OnInit {

  @Input() report!: StepReport
  @Input() sessionId!: string
  collapsed = false
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
        let valueObservable: Observable<string>
        if (this.isFileReference(relevantContextItem)) {
          valueObservable = this.downloadFileReference(this.sessionId, relevantContextItem).pipe(
            mergeMap((data) => {
              return of(new TextDecoder("utf-8").decode(data.data))
            })
          )
        } else {
          valueObservable = of(relevantContextItem.valueToUse!)
        }
        valueObservable.subscribe((valueToUse) => {
          this.openEditorWindow(
            relevantContextItem.name, 
            valueToUse,
            this.report.reports?.assertionReports,
            location?.line,
            relevantContextItem.mimeType
          )
        })
      }
    }
  }

}
