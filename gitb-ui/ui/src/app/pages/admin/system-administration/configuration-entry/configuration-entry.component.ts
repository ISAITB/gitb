import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { ConfigStatus } from '../config-status';

@Component({
  selector: 'app-configuration-entry',
  templateUrl: './configuration-entry.component.html',
  styleUrls: [ './configuration-entry.component.less' ]
})
export class ConfigurationEntryComponent implements OnInit {

  @Input() configTitle!: string
  @Input() configDescription?: string
  @Input() status!: ConfigStatus
  @Output() beforeExpand = new EventEmitter<ConfigStatus>()
  @Output() afterExpand = new EventEmitter<ConfigStatus>()
  @Output() statusChange = new EventEmitter<ConfigStatus>()

  Constants = Constants
  hovering = false
  pendingExpand = false
  loaded = false

  constructor() { }

  ngOnInit(): void {
    this.status.deferredExpand?.subscribe((proceed) => {
      this.pendingExpand = false
      if (proceed) {
        setTimeout(() => {
          this.status.collapsed = false
          this.loaded = true
          this.statusChange.emit(this.status)          
        }, 1)
      }
    })
  }

  headerClicked() {
    if (this.status.collapsed) {
      if (this.loaded) {
        this.status.collapsed = false
      } else {
        if (this.status.deferredExpand) {
          if (!this.pendingExpand) {
            this.pendingExpand = true
            this.beforeExpand.emit(this.status)
          }
        } else {
          this.status.collapsed = false
          this.statusChange.emit(this.status)
        }
      }
    } else {
      this.status.collapsed = true
    }
  }

  contentExpanded() {
    this.afterExpand.emit(this.status)
  }

  statusForeground() {
    if (this.status.enabled) {
      return "#428bca"
    } else {
      return "#c5c5c5"
    }
  }

  statusBackground() {
    if (this.status.enabled) {
      return "#FFFFFF"
    } else {
      return "#FFFFFF"
    }
  }

  statusIcon() {
    if (this.status.enabled) {
      return "fa-solid fa-circle-check"
    } else {
      return "fa-solid fa-ban"
    }
  }

  statusText() {
    if (this.status.enabled && this.status.fromEnv) {
      return "ENABLED (ENVIRONMENT SETTING)"
    } else if (this.status.enabled && this.status.fromDefault) {
      return "ENABLED (DEFAULT SETTING)"
    } else if (this.status.enabled) {
      return "ENABLED"
    } else if (this.status.fromEnv) {
      return "DISABLED (ENVIRONMENT SETTING)"
    } else if (this.status.fromDefault) {
      return "DISABLED (DEFAULT SETTING)"
    } else {
      return "DISABLED"
    }
  }
}
