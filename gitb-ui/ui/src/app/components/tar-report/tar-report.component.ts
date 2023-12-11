import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { AssertionReport } from '../diagram/assertion-report';

@Component({
  selector: 'app-tar-report',
  templateUrl: './tar-report.component.html',
  styleUrls: [ './tar-report.component.less' ]
})
export class TarReportComponent implements OnInit {

  @Input() reports: AssertionReport[] = []
  @Input() selectable = false
  @Output() selected = new EventEmitter<AssertionReport>();
  errors = 0
  warnings = 0
  messages = 0
  collapsed = false
  withSummaryBorder = true

  ngOnInit(): void {
    for (let report of this.reports) {
      if (report.type == 'info') {
        this.messages += 1
      } else if (report.type == 'warning') {
        this.warnings += 1
      } else {
        this.errors += 1
      }
    }
  }

  selectedItem(item: AssertionReport) {
    this.selected.emit(item)
  }

  expanding() {
    setTimeout(() => {
      this.withSummaryBorder = true
    }, 1)
  }

}
