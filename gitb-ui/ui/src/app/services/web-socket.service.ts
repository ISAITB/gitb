import { Injectable } from '@angular/core';
import { NextObserver } from 'rxjs';
import { webSocket } from 'rxjs/webSocket';
import { ROUTES } from '../common/global';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  constructor() { }

  connect(openObserver: NextObserver<Event>, closeObserver: NextObserver<CloseEvent>) {
    return webSocket({
      url: ROUTES.controllers.WebSocketService.socket("session").webSocketURL(),
      openObserver: openObserver,
      closeObserver: closeObserver
    })
  }
}
