/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { ConfigStatus } from '../config-status';

@Component({
    selector: 'app-configuration-entry',
    templateUrl: './configuration-entry.component.html',
    styleUrls: ['./configuration-entry.component.less'],
    standalone: false
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
    return "#FFFFFF"
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
