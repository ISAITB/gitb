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

import {Component, Input} from '@angular/core';
import {BsModalRef} from 'ngx-bootstrap/modal';
import {Constants} from '../../common/constants';
import {RoutingService} from '../../services/routing.service';
import {finalize, mergeMap, Observable, of} from 'rxjs';
import {PopupService} from '../../services/popup.service';
import {UsageTipService} from '../../services/usage-tip.service';

@Component({
  selector: 'app-usage-tip-modal',
  standalone: false,
  templateUrl: './usage-tip-modal.component.html',
  styleUrl: './usage-tip-modal.component.less'
})
export class UsageTipModalComponent {

  @Input() tip!: number
  disableUsageTip = false
  disableUsageTips = false
  closePending = false
  goToCommunityManagementPending = false
  goToSystemAdministrationPending = false
  protected readonly Constants = Constants;

  constructor(
    private readonly modal: BsModalRef,
    private readonly routingService: RoutingService,
    private readonly popupService: PopupService,
    private readonly usageTipService: UsageTipService
  ) {}

  private closeModal(pendingFn: (pending: boolean) => void): Observable<any> {
    let close$: Observable<any>
    if (this.disableUsageTips || this.disableUsageTip) {
      pendingFn(true)
      if (this.disableUsageTips) {
        close$ = this.usageTipService.disableUsageTips()
      } else {
        close$ = this.usageTipService.disableUsageTip(this.tip)
      }
      close$ = close$.pipe(
        mergeMap(() => {
          this.modal.hide()
          if (this.disableUsageTips) {
            this.popupService.success("Usage tips disabled.")
          }
          return of(true)
        }),
        finalize(() => {
          pendingFn(false)
        })
      )
    } else {
      this.modal.hide()
      close$ = of(true)
    }
    return close$
  }

  toCommunityManagement() {
    this.closeModal((pending) => this.goToCommunityManagementPending = pending).subscribe(() => {
      this.routingService.toUserManagement()
    })
  }

  toSystemAdministration() {
    this.closeModal((pending) => this.goToSystemAdministrationPending = pending).subscribe(() => {
      this.routingService.toSystemAdministration(Constants.TAB.SYSTEM_ADMINISTRATION.LANDING_PAGES)
    })
  }

  close() {
    this.closeModal((pending) => this.closePending = pending).subscribe(() => {
    })
  }

}
