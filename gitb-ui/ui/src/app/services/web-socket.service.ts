import {Injectable} from '@angular/core';
import {NextObserver} from 'rxjs';
import {webSocket} from 'rxjs/webSocket';
import {ROUTES} from '../common/global';
import {Utils} from '../common/utils';
import {DataService} from './data.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {

  constructor(private dataService: DataService) { }

  prepareWebSocket<T>(webSocketRoute: any, openObserver: NextObserver<Event>, closeObserver: NextObserver<CloseEvent>) {
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

  connect(openObserver: NextObserver<Event>, closeObserver: NextObserver<CloseEvent>) {
    return this.prepareWebSocket(ROUTES.controllers.WebSocketService.socket("session"), openObserver, closeObserver)
  }
}
