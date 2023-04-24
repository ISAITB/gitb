import { Component, Input, OnInit } from '@angular/core';
import { Counters } from '../test-status-icons/counters';

@Component({
  selector: 'app-test-result-ratio',
  templateUrl: './test-result-ratio.component.html',
  styleUrls: [ './test-result-ratio.component.less' ]
})
export class TestResultRatioComponent implements OnInit {

  constructor() { }

  @Input() counters!: Counters

  completedPercentage = ''
  failedPercentage = ''
  otherPercentage = ''

  ngOnInit(): void {
    if (this.counters) {
      const total = this.counters.completed + this.counters.failed + this.counters.other;
      if (total > 0) {
        this.completedPercentage = ((this.counters.completed / total) * 100).toFixed(2);
        this.failedPercentage = ((this.counters.failed / total) * 100).toFixed(2);
        this.otherPercentage = ((this.counters.other / total) * 100).toFixed(2);
      }
    }
  }

}
