import { Component, Input, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';

@Component({
  selector: 'app-test-result-status-display',
  templateUrl: './test-result-status-display.component.html',
  styles: [
  ]
})
export class TestResultStatusDisplayComponent implements OnInit {

  @Input() message?: string
  @Input() result?: string
  iconToShow!: string
  tooltipText!: string
  popoverClass!: string
  Constants = Constants

  constructor(
    private dataService: DataService
  ) { }

  ngOnInit(): void {
    this.iconToShow = this.dataService.iconForTestResult(this.result)
    this.tooltipText = this.dataService.tooltipForTestResult(this.result)
    this.popoverClass = 'result-message-popover failure'
    if (this.result == Constants.TEST_CASE_RESULT.SUCCESS) {
      this.popoverClass = 'result-message-popover success'
    }
  }

}
