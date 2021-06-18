import { Injectable } from '@angular/core';
import { NotificationsService, NotificationType } from 'angular2-notifications';

@Injectable({
  providedIn: 'root'
})
export class PopupService {

  constructor(
    private notificationService: NotificationsService
  ) { }

  success(message: string) {
    if (message !== undefined) {
      if (!message.endsWith(".")) {
        message = message + "."
      }
      this.notificationService.html(message, NotificationType.Success, undefined, 'success')
    }
  }

  error(message: string) {
    if (message !== undefined) {
      this.notificationService.html(message, NotificationType.Error, {
        timeOut: 0,
        clickToClose: true
      }, 'error')
    }
  }
}
