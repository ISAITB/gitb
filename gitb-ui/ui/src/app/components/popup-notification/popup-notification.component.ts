/*
 * Copyright (C) 2026 European Union
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

import {Component, EventEmitter, Input, OnDestroy, OnInit, Output, ChangeDetectionStrategy} from '@angular/core';
import {PopupNotification} from '../../types/popup-notification';
import {PopupNotificationApi} from './popup-notification-api';
import {NgClass, NgTemplateOutlet} from '@angular/common';
import {Constants} from '../../common/constants';
import {NavTargetDirective} from '../../directives/nav-target.directive';
import {Utils} from '../../common/utils';

@Component({
  selector: 'app-popup-notification',
  standalone: true,
  imports: [NgClass, NgTemplateOutlet, NavTargetDirective],
  templateUrl: './popup-notification.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './popup-notification.component.less',
})
export class PopupNotificationComponent implements OnInit, OnDestroy, PopupNotificationApi {

  private static readonly SWAP_DURATION_MS = 150

  private currentNotification!: PopupNotification
  private swapTimeout?: ReturnType<typeof setTimeout>

  // What is actually rendered. This lags behind the notification when its content is replaced, so that
  // the old content can fade out before the new one fades in.
  displayed!: PopupNotification
  swapping = false

  @Input() set notification(value: PopupNotification) {
    const previous = this.currentNotification
    this.currentNotification = value
    if (previous != undefined && previous.id === value.id && this.contentChanged(previous, value)) {
      this.swapping = true
      if (this.swapTimeout) clearTimeout(this.swapTimeout)
      this.swapTimeout = setTimeout(() => {
        this.displayed = this.currentNotification
        this.swapping = false
        this.swapTimeout = undefined
      }, PopupNotificationComponent.SWAP_DURATION_MS)
    } else if (this.swapTimeout == undefined) {
      this.displayed = value
    }
  }
  get notification(): PopupNotification {
    return this.currentNotification
  }
  @Output() close = new EventEmitter<PopupNotification>();

  removing = false
  hovering = false
  expired = false

  private contentChanged(a: PopupNotification, b: PopupNotification): boolean {
    return a.message !== b.message || a.subtitle !== b.subtitle || a.action !== b.action
  }

  ngOnDestroy(): void {
    if (this.swapTimeout) clearTimeout(this.swapTimeout)
  }

  actionClicked(event: MouseEvent) {
    // Prevent the dismissal handler of the notification as a whole from running a second time.
    event.stopPropagation()
    if (Utils.isPlainNavigationClick(event)) {
      this.displayed.action?.handler?.()
    }
    this.closeNotification()
  }

  actionAuxClicked(event: MouseEvent) {
    if (event.button === 1) {
      // Middle click (e.g. opens the link in a new tab) - still considered as handled.
      event.stopPropagation()
      this.closeNotification()
    }
  }

  ngOnInit(): void {
    if (!this.notification.persistent) {
      setTimeout(() => {
        this.expired = true
        if (!this.hovering || this.removing) {
          this.closeNotification()
        }
      }, 2000)
    }
  }

  getId(): string {
    return this.notification.id;
  }

  closeNotification() {
    this.removing = true
    setTimeout(() => {
      this.close.emit(this.notification)
    }, 300)
  }

  isPersistent(): boolean {
    return this.notification.persistent
  }

  mouseLeaving() {
    this.hovering = false
  }

  mouseEntering() {
    this.hovering = true
  }

  protected readonly Constants = Constants;
}
