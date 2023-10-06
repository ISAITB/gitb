import { Component, ElementRef, HostListener, Input, OnInit } from '@angular/core';
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
  @Input() asLine? = false

  successIcon!: string
  failedIcon!: string
  otherIcon!: string
  hasRequired!: boolean
  hasOptional!: boolean
  expanded = false

  constructor(
    private dataService: DataService,
    private eRef: ElementRef
  ) { }

  ngOnInit(): void {
    this.successIcon = this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.SUCCESS)
    this.failedIcon = this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.FAILURE)
    this.otherIcon = this.dataService.iconForTestResult(Constants.TEST_CASE_RESULT.UNDEFINED)
    this.hasRequired = this.counters.completed > 0 || this.counters.failed > 0 || this.counters.other > 0
    this.hasOptional = this.counters.completedOptional > 0 || this.counters.failedOptional > 0 || this.counters.otherOptional > 0
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
