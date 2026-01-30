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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {PopupNotification} from '../../types/popup-notification';
import {PopupNotificationApi} from './popup-notification-api';
import {NgClass} from '@angular/common';

@Component({
  selector: 'app-popup-notification',
  standalone: true,
  imports: [NgClass],
  templateUrl: './popup-notification.component.html',
  styleUrl: './popup-notification.component.less',
})
export class PopupNotificationComponent implements OnInit, PopupNotificationApi {

  @Input() notification!: PopupNotification
  @Output() close = new EventEmitter<PopupNotification>();

  removing = false
  hovering = false
  expired = false

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

  closeNotification() {
    this.removing = true
    setTimeout(() => {
      this.close.emit(this.notification)
    }, 300)
  }

  mouseLeaving() {
    this.hovering = false
  }

  mouseEntering() {
    this.hovering = true
  }
}
