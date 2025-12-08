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

import {Component, OnDestroy, OnInit, QueryList, ViewChildren} from '@angular/core';
import {PopupService} from '../../services/popup.service';
import {Observable, Subscription} from 'rxjs';
import {PopupNotification} from '../../types/popup-notification';
import {PopupNotificationApi} from '../popup-notification/popup-notification-api';

@Component({
  selector: 'app-popup-popup-notification-container',
  standalone: false,
  templateUrl: './popup-notification-container.component.html',
  styleUrl: './popup-notification-container.component.less',
})
export class PopupNotificationContainerComponent implements OnInit, OnDestroy {

  notifications$!: Observable<PopupNotification[]>
  closeNotifications$!: Subscription
  @ViewChildren("popupNotification") notifications?: QueryList<PopupNotificationApi>

  constructor(private popupService: PopupService) {
  }

  ngOnInit(): void {
    this.notifications$ = this.popupService.notifications$
    this.closeNotifications$ = this.popupService.closeNotifications$.subscribe(() => {
      if (this.notifications) {
        this.notifications.forEach((notification) => {
          // Do it via the component so that the closing animations are correctly triggered
          notification.closeNotification()
        })
      }
    })
  }

  ngOnDestroy(): void {
    if (this.closeNotifications$) this.closeNotifications$.unsubscribe();
  }

  close(notification: PopupNotification) {
    this.popupService.remove(notification.id);
  }

}
