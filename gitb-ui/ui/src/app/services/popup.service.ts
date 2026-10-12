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
import {PopupNotificationAction} from '../types/popup-notification-action';

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

  info(message: string, persistent?: boolean, action?: PopupNotificationAction): string|null {
    return this.add(message, 'info', persistent, undefined, action)
  }

  /**
   * Info notification with a title and a (less prominent) subtitle line. Unlike plain notifications, both
   * texts are rendered as text (not HTML), so they can safely include user-provided content.
   */
  infoWithSubtitle(title: string, subtitle?: string, persistent?: boolean, action?: PopupNotificationAction): string|null {
    return this.add(title, 'info', persistent, subtitle, action)
  }

  /**
   * Replaces the displayed texts and action of an existing notification (its severity and persistence are
   * retained). Returns false if the notification is no longer displayed, in which case the caller should
   * raise a new one.
   */
  update(id: string, changes: { message: string, subtitle?: string, action?: PopupNotificationAction }): boolean {
    const list = this.notificationsSubject.value
    if (!list.some(n => n.id === id)) return false
    this.notificationsSubject.next(list.map(n => n.id === id ? {
      ...n,
      message: this.withTrailingDot(changes.message),
      subtitle: changes.subtitle,
      action: changes.action
    } : n))
    return true
  }

  private withTrailingDot(message: string): string {
    return message.endsWith(".") ? message : message + "."
  }

  private add(message: string, type: 'error'|'warning'|'success'|'info', persistent?: boolean, subtitle?: string, action?: PopupNotificationAction): string|null {
    if (message !== undefined) {
      const notification: PopupNotification = {
        id: crypto.randomUUID(),
        type: type,
        message: this.withTrailingDot(message),
        subtitle: subtitle,
        persistent: persistent === true,
        action: action
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
