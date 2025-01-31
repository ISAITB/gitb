import {Component, ElementRef, EventEmitter, HostListener, Input, OnInit} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {Counters} from './counters';
import {TestStatusBase} from '../test-status-base/test-status-base';

@Component({
  selector: 'app-test-status-icons',
  templateUrl: './test-status-icons.component.html',
  styleUrls: [ './test-status-icons.component.less' ]
})
export class TestStatusIconsComponent extends TestStatusBase implements OnInit {

  @Input() centerAligned = true
  @Input() asLine? = false
  @Input() tooltipOnLeft? = false
  @Input() refresh?: EventEmitter<Counters>

  successIcon!: string
  failedIcon!: string
  otherIcon!: string
  expanded = false

  constructor(
    private dataService: DataService,
    private eRef: ElementRef
  ) { super() }

  ngOnInit(): void {
    super.ngOnInit();
  }

  protected updateCounters() {
    super.updateCounters();
    this.successIcon = this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.SUCCESS)
    this.failedIcon = this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.FAILURE)
    this.otherIcon = this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.UNDEFINED)
  }

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

}
