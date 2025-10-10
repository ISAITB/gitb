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

import {Component, ElementRef, HostListener, Input, OnInit} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {TestStatusBase} from '../test-status-base/test-status-base';

@Component({
    selector: 'app-test-status-icons',
    templateUrl: './test-status-icons.component.html',
    styleUrls: ['./test-status-icons.component.less'],
    standalone: false
})
export class TestStatusIconsComponent extends TestStatusBase implements OnInit {

  @Input() centerAligned = true
  @Input() asLine? = false
  @Input() tooltipOnLeft? = false

  successIcon!: string
  failedIcon!: string
  otherIcon!: string
  expanded = false

  constructor(
    private readonly dataService: DataService,
    private readonly eRef: ElementRef
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
