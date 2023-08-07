import { Component, ElementRef, HostListener, Input, OnInit } from '@angular/core';
import { Counters } from '../test-status-icons/counters';

@Component({
  selector: 'app-test-result-ratio',
  templateUrl: './test-result-ratio.component.html',
  styleUrls: [ './test-result-ratio.component.less' ]
})
export class TestResultRatioComponent implements OnInit {

  constructor(private eRef: ElementRef) { }
  
  @Input() counters!: Counters
  @Input() alignRight = true

  completedPercentage = ''
  failedPercentage = ''
  otherPercentage = ''
  completedOptionalPercentage = ''
  failedOptionalPercentage = ''
  otherOptionalPercentage = ''
  hasRequired = false
  hasOptional = false
  expanded = false

  @HostListener('document:click', ['$event'])
  clickRegistered(event: any) {
    if (!this.eRef.nativeElement.contains(event.target) && this.expanded) {
      this.expanded = false
    }
  }

  @HostListener('document:keyup.escape', ['$event'])  
  escapeRegistered(event: KeyboardEvent) {
    if (this.expanded) {
      this.expanded = false
    }
  }

  ngOnInit(): void {
    if (this.counters) {
      let total = this.counters.completed + this.counters.failed + this.counters.other;
      if (total > 0) {
        this.hasRequired = true
        this.completedPercentage = ((this.counters.completed / total) * 100).toFixed(1);
        this.failedPercentage = ((this.counters.failed / total) * 100).toFixed(1);
        this.otherPercentage = ((this.counters.other / total) * 100).toFixed(1);
      }
      const totalOptional = this.counters.completedOptional + this.counters.failedOptional + this.counters.otherOptional;
      if (totalOptional > 0) {
        this.hasOptional = true
        this.completedOptionalPercentage = ((this.counters.completedOptional / totalOptional) * 100).toFixed(1);
        this.failedOptionalPercentage = ((this.counters.failedOptional / totalOptional) * 100).toFixed(1);
        this.otherOptionalPercentage = ((this.counters.otherOptional / totalOptional) * 100).toFixed(1);
      }
    }
  }

}
