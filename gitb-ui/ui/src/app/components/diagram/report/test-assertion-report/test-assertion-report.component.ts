import { Component, Input, OnInit } from '@angular/core';
import { AssertionReport } from '../../assertion-report';

@Component({
    selector: 'app-test-assertion-report',
    templateUrl: './test-assertion-report.component.html',
    styleUrls: ['./test-assertion-report.component.less'],
    standalone: false
})
export class TestAssertionReportComponent implements OnInit {

  @Input() assertionReport!: AssertionReport
  clickable = false
  locationToShow?: string

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
      const separatorIndex = this.assertionReport.value.location.indexOf("|")
      if (separatorIndex < 0) {
        this.assertionReport.extractedLocation = this.extractLocationInfo(this.assertionReport.value.location)
        if (this.assertionReport.extractedLocation == undefined) {
          this.locationToShow = this.assertionReport.value.location
        }
      } else {
        this.assertionReport.extractedLocation = this.extractLocationInfo(this.assertionReport.value.location.substring(0, separatorIndex))
        if (separatorIndex < this.assertionReport.value.location.length - 1) {
          this.locationToShow = this.assertionReport.value.location.substring(separatorIndex + 1)
        }
      }
      this.clickable = this.assertionReport.extractedLocation != undefined
    } else {
      this.locationToShow = this.assertionReport.value?.location
    }
  }

  private extractLocationInfo(locationStr?: string) {
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
