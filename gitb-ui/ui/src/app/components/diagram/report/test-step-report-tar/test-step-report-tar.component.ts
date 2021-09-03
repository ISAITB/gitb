import { Component, Input, OnInit } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { AnyContent } from '../../any-content';
import { AssertionReport } from '../../assertion-report';
import { ReportSupport } from '../report-support';
import { StepReport } from '../step-report';

@Component({
  selector: 'app-test-step-report-tar',
  templateUrl: './test-step-report-tar.component.html',
  styles: [ 
    '.detailItems { padding: 10px 10px 0px 10px; border-top: 1px solid #e5e5e5; } ',
    '.detailsLabel { margin-bottom:8px; } ',
    '.countersDiv { padding-bottom:10px; }',
    '.itemsDiv { border: 1px solid #e5e5e5; border-radius: 3px; margin-top: 15px; } '
  ]
})
export class TestStepReportTARComponent extends ReportSupport implements OnInit {

  @Input() report!: StepReport
  collapsed = false

  constructor(
    modalService: BsModalService
  ) { super(modalService) }

  ngOnInit(): void {
    // Calculate the value of each item in the report context
    if (this.report.context) {
      this.setContextValues(this.report.context)
    }
  }

  private setContextValues(context: AnyContent) {
    if (context.value != undefined) {
      if (context.embeddingMethod == Constants.EMBEDDING_METHOD.BASE64) {
        context.valueToUse = this.base64ToString(context.value)
      } else {
        context.valueToUse = context.value
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
        this.openEditorWindow(
          relevantContextItem.name, 
          relevantContextItem.valueToUse, 
          this.report.reports?.assertionReports,
          location.line,
          relevantContextItem.mimeType
        )
      }
    }
  }

}
