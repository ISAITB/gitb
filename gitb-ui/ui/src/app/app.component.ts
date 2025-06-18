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

import { Component, OnInit } from '@angular/core';
import { NotificationAnimationType, Options } from 'angular2-notifications';
import { setTheme } from 'ngx-bootstrap/utils';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    standalone: false
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
