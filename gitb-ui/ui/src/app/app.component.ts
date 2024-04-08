import { Component, OnInit } from '@angular/core';
import { NotificationAnimationType, Options } from 'angular2-notifications';
import { setTheme } from 'ngx-bootstrap/utils';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {

  public notificationOptions: Options = {
    position: ['top', 'right'],
    timeOut: 2000,
    showProgressBar: false,
    clickToClose: true,
    theClass: 'notification-item',
    animate: NotificationAnimationType.Fade
  }

  constructor() {
    setTheme('bs5')
  }

  ngOnInit(): void {
    if (navigator.userAgent.match(/firefox/i)) {
      // Firefox has an animation/transition bug that results in flashing when the notification closes.
      this.notificationOptions.animate = undefined
    }
  }

}
