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

import {Injectable} from '@angular/core';
import {BehaviorSubject, Subject} from 'rxjs';
import {PopupNotification} from '../types/popup-notification';
import {CloseNotificationEvent} from '../types/close-notification-event';

@Injectable({
  providedIn: 'root'
})
export class PopupService {

  private notificationsSubject = new BehaviorSubject<PopupNotification[]>([]);
  private closeNotificationsSubject = new Subject<CloseNotificationEvent>();
  notifications$ = this.notificationsSubject.asObservable();
  closeNotifications$ = this.closeNotificationsSubject.asObservable();
  private closedNotificationsSubject = new Subject<string>();
  closedNotifications$ = this.closedNotificationsSubject.asObservable();

  constructor() { }

  success(message: string, persistent?: boolean): string|null {
    return this.add(message, 'success', persistent)
  }

  warning(message: string, persistent?: boolean): string|null {
    return this.add(message, 'warning', persistent)
  }

  error(message: string, persistent?: boolean): string|null {
    return this.add(message, 'error', persistent)
  }

  info(message: string, persistent?: boolean): string|null {
    return this.add(message, 'info', persistent)
  }

  private add(message: string, type: 'error'|'warning'|'success'|'info', persistent?: boolean): string|null {
    if (message !== undefined) {
      if (!message.endsWith(".")) {
        message = message + "."
      }
      const notification: PopupNotification = {
        id: crypto.randomUUID(),
        type: type,
        message: message,
        persistent: persistent === true
      }
      const list = this.notificationsSubject.value;
      this.notificationsSubject.next([...list, notification]);
      return notification.id
    } else {
      return null
    }
  }

  remove(id: string) {
    this.notificationsSubject.next(
      this.notificationsSubject.value.filter(n => n.id !== id)
    );
    this.closedNotificationsSubject.next(id)
  }

  close(notificationId: string): void {
    this.closeNotificationsSubject.next({
      id: notificationId
    })
  }

  closeAll(skipIfPersistent?: boolean) {
    this.closeNotificationsSubject.next({
      skipIfPersistent: skipIfPersistent
    })
  }
}
