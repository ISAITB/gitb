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

import { Injectable } from '@angular/core';
import { NotificationsService, NotificationType } from 'angular2-notifications';

@Injectable({
  providedIn: 'root'
})
export class PopupService {

  constructor(
    private notificationService: NotificationsService
  ) { }

  success(message: string, persistent?: boolean) {
    if (message !== undefined) {
      if (!message.endsWith(".")) {
        message = message + "."
      }
      if (persistent) {
        this.notificationService.html(message, NotificationType.Success, { timeOut: 0, clickToClose: true }, 'success')
      } else {
        this.notificationService.html(message, NotificationType.Success, undefined, 'success')
      }
    }
  }

  warning(message: string, persistent?: boolean) {
    if (message !== undefined) {
      if (persistent) {
        this.notificationService.html(message, NotificationType.Warn, { timeOut: 0, clickToClose: true }, 'warn')
      } else {
        this.notificationService.html(message, NotificationType.Warn, undefined, 'warn')
      }
    }
  }

  error(message: string, persistent?: boolean) {
    if (message !== undefined) {
      if (persistent) {
        this.notificationService.html(message, NotificationType.Error, { timeOut: 0, clickToClose: true }, 'error')
      } else {
        this.notificationService.html(message, NotificationType.Error, undefined, 'error')
      }
    }
  }

  info(message: string, persistent?: boolean) {
    if (message !== undefined) {
      if (persistent) {
        this.notificationService.html(message, NotificationType.Info, { timeOut: 0, clickToClose: true }, 'info')
      } else {
        this.notificationService.html(message, NotificationType.Info, undefined, 'info')
      }
    }
  }

  closeAll() {
    this.notificationService.remove()
  }
}
