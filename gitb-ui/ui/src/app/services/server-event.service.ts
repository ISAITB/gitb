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

import {Injectable, NgZone} from '@angular/core';
import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {filter, map, take, takeUntil, timeout} from 'rxjs/operators';
import {ROUTES} from '../common/global';
import {TestSessionUpdateMessage} from '../types/test-session-update-message';
import {DataService} from './data.service';

/**
 * Manages the server-sent events (SSE) channel between the application and the server. The channel is opened once
 * the user is authenticated (authentication is based on the user's session cookie) and remains open until logout.
 *
 * The browser automatically reconnects a dropped connection, resuming the same channel (without losing events)
 * if the server still holds it. If this is not possible, a new channel is created and a reset is signalled.
 *
 * Note that this service must not depend on services that require the user's authentication (e.g. RestService)
 * as it is used by the AuthProviderService.
 */
@Injectable({
  providedIn: 'root'
})
export class ServerEventService {

  private static readonly MIN_RECONNECT_DELAY = 2000
  private static readonly MAX_RECONNECT_DELAY = 30000
  // The server sends a heartbeat every 20 seconds. Without any event for this long the connection is considered dead.
  private static readonly SILENCE_TIMEOUT = 60000
  private static readonly WATCHDOG_INTERVAL = 15000
  // How long to wait for a channel to be established (e.g. right after login) before giving up.
  private static readonly CHANNEL_WAIT_TIMEOUT = 15000

  private eventSource?: EventSource
  private reconnectTimer?: ReturnType<typeof setTimeout>
  private reconnectAttempts = 0
  private lastActivity = 0
  private watchdog?: ReturnType<typeof setInterval>
  private currentChannelId?: string
  private readonly channelIdSubject = new BehaviorSubject<string|undefined>(undefined)
  private readonly channelResetSubject = new Subject<void>()
  private readonly sessionMessageSubject = new Subject<{ session: string, message: TestSessionUpdateMessage }>()

  /**
   * The ID of the currently established channel (undefined if there is no channel).
   */
  public readonly channelId$ = this.channelIdSubject.asObservable()

  constructor(
    private readonly dataService: DataService,
    private readonly zone: NgZone
  ) { }

  connect() {
    this.disconnect()
    this.open()
  }

  private open() {
    const eventSource = new EventSource(this.dataService.completePath(ROUTES.controllers.ServerEventService.connect().url))
    this.lastActivity = Date.now()
    eventSource.addEventListener('heartbeat', () => {
      this.lastActivity = Date.now()
    })
    eventSource.addEventListener('connected', (event: MessageEvent) => {
      this.lastActivity = Date.now()
      this.reconnectAttempts = 0
      const channelId = JSON.parse(event.data).channelId as string
      if (this.currentChannelId != undefined && this.currentChannelId != channelId) {
        // The previous channel could not be resumed. Anything relying on it is lost.
        this.channelIdSubject.next(undefined)
        this.channelResetSubject.next()
      }
      if (this.currentChannelId != channelId) {
        this.currentChannelId = channelId
        this.channelIdSubject.next(channelId)
      }
    })
    eventSource.addEventListener('session', (event: MessageEvent) => {
      this.lastActivity = Date.now()
      this.sessionMessageSubject.next(JSON.parse(event.data))
    })
    eventSource.addEventListener('closed', () => {
      // The server closed the channel (e.g. logout or expiry). Do not attempt to reconnect.
      this.disconnect()
    })
    eventSource.onerror = () => {
      if (eventSource.readyState == EventSource.CLOSED && this.eventSource == eventSource) {
        /*
         * The browser gave up reconnecting (e.g. it received a non-successful response from a proxy while the server was
         * restarting). Try again ourselves, using increasing delays. As we can no longer resume the previous channel,
         * a new one will be created.
         */
        console.debug('Server event channel closed unexpectedly')
        this.closeEventSource()
        const delay = Math.min(ServerEventService.MAX_RECONNECT_DELAY, ServerEventService.MIN_RECONNECT_DELAY * Math.pow(2, this.reconnectAttempts))
        this.reconnectAttempts += 1
        this.reconnectTimer = setTimeout(() => {
          this.reconnectTimer = undefined
          this.open()
        }, delay)
      }
    }
    this.eventSource = eventSource
    this.startWatchdog()
  }

  /**
   * Detect connections that were silently dropped (e.g. by a proxy or during a server restart) for which the browser
   * never signals an error. In such cases we start over (a new channel will be created).
   */
  private startWatchdog() {
    if (this.watchdog == undefined) {
      // Run outside Angular's zone to avoid triggering change detection for every check.
      this.watchdog = this.zone.runOutsideAngular(() => setInterval(() => {
        if (this.eventSource && Date.now() - this.lastActivity > ServerEventService.SILENCE_TIMEOUT) {
          this.zone.run(() => {
            console.debug('Server event channel silent for too long - reconnecting')
            this.closeEventSource()
            this.open()
          })
        }
      }, ServerEventService.WATCHDOG_INTERVAL))
    }
  }

  disconnect() {
    if (this.watchdog != undefined) {
      clearInterval(this.watchdog)
      this.watchdog = undefined
    }
    if (this.reconnectTimer != undefined) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = undefined
    }
    this.reconnectAttempts = 0
    this.closeEventSource()
  }

  private closeEventSource() {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = undefined
    }
    if (this.currentChannelId != undefined) {
      this.currentChannelId = undefined
      this.channelIdSubject.next(undefined)
      this.channelResetSubject.next()
    }
  }

  /**
   * Wait for a channel to be established (it may not yet be, e.g. right in the moments after login), returning its
   * ID once ready. Errors (with an RxJS TimeoutError) if no channel is established within the given delay.
   *
   * The channel ID must be recorded by the caller (e.g. to later subscribe/unsubscribe it to a test session's
   * updates via TestService, and to pass along when unsubscribing).
   */
  awaitChannel(timeoutMs: number = ServerEventService.CHANNEL_WAIT_TIMEOUT): Observable<string> {
    return this.channelId$.pipe(
      filter((channelId): channelId is string => channelId != undefined),
      take(1),
      timeout({first: timeoutMs})
    )
  }

  /**
   * The updates of the given test session. The observable completes if the channel is lost (or closed).
   * For updates to be received, the channel must also be subscribed to the test session (see TestService).
   */
  testSessionUpdates(session: string): Observable<TestSessionUpdateMessage> {
    return this.sessionMessageSubject.pipe(
      filter((event) => event.session == session),
      map((event) => event.message),
      takeUntil(this.channelResetSubject)
    )
  }

}
