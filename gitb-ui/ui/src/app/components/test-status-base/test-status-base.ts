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

import {Component, EventEmitter, Input, OnInit} from '@angular/core';
import {Counters} from '../test-status-icons/counters';

@Component({
    template: '',
    standalone: false
})
export abstract class TestStatusBase implements OnInit {

  @Input() counters!: Counters
  @Input() refresh?: EventEmitter<Counters>

  hasRequired = false
  hasIgnored = false
  completed = 0
  failed = 0
  other = 0
  completedIgnored = 0
  failedIgnored = 0
  otherIgnored = 0
  tooltipRequiredTestDescription = 'test cases'

  ngOnInit(): void {
    this.updateCounters()
    if (this.refresh) {
      this.refresh.subscribe((counters) => {
        this.counters = counters
        this.updateCounters()
      })
    }
  }

  protected updateCounters() {
    this.completed = this.counters.completedToConsider
    this.failed = this.counters.failedToConsider
    this.other = this.counters.otherToConsider
    this.completedIgnored = this.counters.completedOptional + (this.counters.completed - this.counters.completedToConsider)
    this.failedIgnored = this.counters.failedOptional + (this.counters.failed - this.counters.failedToConsider)
    this.otherIgnored = this.counters.otherOptional + (this.counters.other - this.counters.otherToConsider)
    this.hasRequired = this.completed > 0 || this.failed > 0 || this.other > 0
    this.hasIgnored = this.completedIgnored > 0 || this.failedIgnored > 0 || this.otherIgnored > 0
    // If we have more ignored test cases than we have optional test cases we know we have groups.
    if ((this.completedIgnored + this.failedIgnored + this.otherIgnored) != (this.counters.completedOptional + this.counters.failedOptional + this.counters.otherOptional)) {
      this.tooltipRequiredTestDescription = 'test cases or groups'
    }
  }

}
