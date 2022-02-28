import { Component, Input, OnInit } from '@angular/core';
import { AssertionReport } from '../../assertion-report';

@Component({
  selector: 'app-test-assertion-report',
  templateUrl: './test-assertion-report.component.html',
  styleUrls: ['./test-assertion-report.component.less']
})
export class TestAssertionReportComponent implements OnInit {

  @Input() assertionReport!: AssertionReport

  description?: string
  types = {
    INFO: 'info',
    WARNING: 'warning',
    ERROR: 'error'
  }

  constructor() { }

  ngOnInit(): void {
    this.description = this.assertionReport?.value?.description
    if (this.assertionReport.value?.location) {
      this.assertionReport.extractedLocation = this.extractLocationInfo(this.assertionReport.value.location)
    }
  }

  extractLocationInfo(locationStr?: string) {
    let location: {type: string, name: string, line: number, column: number}|undefined = undefined
    if (locationStr) {
      const LINE_NUMBER_REGEX = /^([\w\.]+):([\-0-9]+):([\-0-9]+)$/
      if (LINE_NUMBER_REGEX.test(locationStr)) {
        const matches = LINE_NUMBER_REGEX.exec(locationStr)
        if (matches) {
          location = {
            type: "line-column-number",
            name: matches[1],
            line: Number(matches[2]),
            column: Number(matches[3],)
          }
        }
      }
    }
    return location
  }
}
