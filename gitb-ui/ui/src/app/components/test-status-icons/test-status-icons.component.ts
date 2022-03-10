import { Component, Input, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { DataService } from 'src/app/services/data.service';
import { Counters } from './counters';

@Component({
  selector: 'app-test-status-icons',
  templateUrl: './test-status-icons.component.html',
  styleUrls: [ './test-status-icons.component.less' ]
})
export class TestStatusIconsComponent implements OnInit {

  @Input() counters!: Counters

  successIcon!: string
  failedIcon!: string
  otherIcon!: string
  tooltipText!: string

  constructor(private dataService: DataService) { }

  ngOnInit(): void {
    this.successIcon = this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.SUCCESS)
    this.failedIcon = this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.FAILURE)
    this.otherIcon = this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.UNDEFINED)
    this.tooltipText = this.counters.completed + " passed, " + 
      this.counters.failed + " failed, " + 
      this.counters.other + " incomplete<br/>("+(this.counters.completed + this.counters.failed + this.counters.other)+" in total)"
  }

}
