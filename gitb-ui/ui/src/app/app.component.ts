import { Component } from '@angular/core';
import { NotificationAnimationType, Options } from 'angular2-notifications';
import { setTheme } from 'ngx-bootstrap/utils';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html'
})
export class AppComponent {

  public notificationOptions: Options = {
    position: ['top', 'right'],
    timeOut: 2000,
    showProgressBar: false,
    clickToClose: true,
    theClass: 'notification-item',
    animate: NotificationAnimationType.Fade
  }

  constructor() {
    setTheme('bs3')
  }

}
