import { Injectable } from '@angular/core';
import { NextObserver } from 'rxjs';
import { webSocket } from 'rxjs/webSocket';
import { ROUTES } from '../common/global';
import { Utils } from '../common/utils';
import {DataService} from './data.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  constructor(private dataService: DataService) { }

  connect(openObserver: NextObserver<Event>, closeObserver: NextObserver<CloseEvent>) {
    const webSocketRoute = ROUTES.controllers.WebSocketService.socket("session")
    const internalAddressWithServerPart = webSocketRoute.webSocketURL()
    const pathPart = webSocketRoute.url
    const serverPart = internalAddressWithServerPart.substring(0, internalAddressWithServerPart.length - pathPart.length)
    const urlToUse = Utils.webSocketURL(serverPart + this.dataService.completePath(pathPart))
    return webSocket({
      url: urlToUse,
      openObserver: openObserver,
      closeObserver: closeObserver
    })
  }
}
