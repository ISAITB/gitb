import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {CloseEvent} from './close-event';

@Component({
  selector: 'app-test-result-status-display',
  templateUrl: './test-result-status-display.component.html',
  styleUrls: [ './test-result-status-display.component.less' ]
})
export class TestResultStatusDisplayComponent implements OnInit {

  @Input() popupId!: number
  @Input() message?: string
  @Input() result?: string
  @Input() ignored = false
  @Input() refresh?: EventEmitter<void>
  @Input() close?: EventEmitter<CloseEvent>
  @Output() open = new EventEmitter<number>()
  isOpen = false

  iconToShow!: string
  tooltipText!: string
  popoverClass!: string
  Constants = Constants

  constructor(
    private dataService: DataService
  ) { }

  ngOnInit(): void {
    this.initialise()
    if (this.close) {
      this.close.subscribe((event) => {
        if (event.idToSkip != this.popupId && this.isOpen) {
          this.isOpen = false
        }
      })
    }
    if (this.refresh) {
      this.refresh.subscribe(() => {
        this.initialise()
      })
    }
  }

  private initialise() {
    this.iconToShow = this.dataService.iconForTestResult(this.result)
    this.tooltipText = this.dataService.tooltipForTestResult(this.result)
    if (this.result == Constants.TEST_CASE_RESULT.SUCCESS) {
      this.popoverClass = 'result-message-popover success'
    } else if (this.result == Constants.TEST_CASE_RESULT.FAILURE) {
      this.popoverClass = 'result-message-popover failure'
    } else {
      this.popoverClass = 'result-message-popover undefined'
    }
    this.isOpen = false
  }

  clicked(event: Event) {
    if (this.message != undefined) {
      event.stopPropagation()
      if (!this.isOpen) {
        this.isOpen = true
        this.open.emit(this.popupId)
      } else {
        this.isOpen = false
      }
    }
  }

  closed() {
    this.isOpen = false
  }
}
