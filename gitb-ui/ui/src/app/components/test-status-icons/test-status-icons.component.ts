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
  @Input() centerAligned = true

  successIcon!: string
  failedIcon!: string
  otherIcon!: string
  hasOptional!: boolean

  constructor(private dataService: DataService) { }

  ngOnInit(): void {
    this.successIcon = this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.SUCCESS)
    this.failedIcon = this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.FAILURE)
    this.otherIcon = this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.UNDEFINED)
    this.hasOptional = this.counters.completedOptional > 0 || this.counters.failedOptional > 0 || this.counters.otherOptional > 0
  }

}
