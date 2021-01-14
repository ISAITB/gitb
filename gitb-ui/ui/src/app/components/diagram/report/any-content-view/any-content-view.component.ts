import { Component, Input, OnInit } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal';
import { DataService } from 'src/app/services/data.service';
import { TestService } from 'src/app/services/test.service';
import { AnyContent } from '../../any-content';
import { ReportSupport } from '../report-support';
import { StepReport } from '../step-report';

@Component({
  selector: 'app-any-content-view',
  templateUrl: './any-content-view.component.html'
})
export class AnyContentViewComponent extends ReportSupport implements OnInit {

  @Input() context!: AnyContent
  @Input() report!: StepReport

  value?: string
  isValueTooLong = false

  constructor(
    private testService: TestService,
    private dataService: DataService,
    modalService: BsModalService
  ) { super(modalService) }

  ngOnInit(): void {
    this.value = this.context.valueToUse
    if (this.value != undefined) {
      this.isValueTooLong = this.value?.length! > 100
    }
  }

  open(lineNumber?: number) {
    this.openEditorWindow(
      this.context.name,
      this.value,
      this.report?.reports?.assertionReports,
      lineNumber)
  }

  download() {
    this.testService.getBinaryMetadata(this.context.value!, (this.context.embeddingMethod == 'BASE64'))
    .subscribe((info) => {
      let bb: Blob
      if (this.context!.embeddingMethod == 'BASE64') {
        bb = this.dataService.b64toBlob(this.context.value!, info.mimeType)
      } else {
        bb = new Blob([this.context.value!], {type: info.mimeType})
      }
      saveAs(bb, 'file'+info.extension)
    })
  }

}
