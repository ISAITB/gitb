import {Component, ElementRef, HostListener, Input, OnInit} from '@angular/core';
import {TestStatusBase} from '../test-status-base/test-status-base';

@Component({
    selector: 'app-test-result-ratio',
    templateUrl: './test-result-ratio.component.html',
    styleUrls: ['./test-result-ratio.component.less'],
    standalone: false
})
export class TestResultRatioComponent extends TestStatusBase implements OnInit {

  constructor(private eRef: ElementRef) { super() }

  @Input() alignRight = true
  @Input() asLine? = false

  completedPercentage = ''
  failedPercentage = ''
  otherPercentage = ''
  completedIgnoredPercentage = ''
  failedIgnoredPercentage = ''
  otherIgnoredPercentage = ''
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
    super.ngOnInit();
  }

  protected updateCounters() {
    super.updateCounters();
    let total = this.completed + this.failed + this.other;
    if (total > 0) {
      this.completedPercentage = ((this.completed / total) * 100).toFixed(1);
      this.failedPercentage = ((this.failed / total) * 100).toFixed(1);
      this.otherPercentage = ((this.other / total) * 100).toFixed(1);
    }
    const totalIgnored = this.completedIgnored + this.failedIgnored + this.otherIgnored;
    if (totalIgnored > 0) {
      this.completedIgnoredPercentage = ((this.completedIgnored / totalIgnored) * 100).toFixed(1);
      this.failedIgnoredPercentage = ((this.failedIgnored / totalIgnored) * 100).toFixed(1);
      this.otherIgnoredPercentage = ((this.otherIgnored / totalIgnored) * 100).toFixed(1);
    }
  }

}
