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
