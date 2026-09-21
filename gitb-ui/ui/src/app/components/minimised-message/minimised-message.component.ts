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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { NgbTooltipModule } from '@ng-bootstrap/ng-bootstrap';
import { CommonModule } from '@angular/common';
import { Constants } from '../../common/constants';
import { MessageComposeService } from '../../services/message-compose.service';

/** The minimised "Draft message" bar, shown at the bottom of the page (to the left of the scroll-to-top
 * control) while a message is being composed but the compose modal has been minimised. Mounted once at
 * the application root - see app.component.html - so it stays visible across navigation. */
@Component({
  selector: 'app-minimised-message',
  standalone: true,
  imports: [CommonModule, NgbTooltipModule],
  templateUrl: './minimised-message.component.html',
  styleUrl: './minimised-message.component.less'
})
export class MinimisedMessageComponent implements OnInit, OnDestroy {

  visible = false
  private subscription?: Subscription

  protected readonly Constants = Constants

  constructor(
    private readonly messageComposeService: MessageComposeService
  ) { }

  ngOnInit(): void {
    this.updateVisibility()
    this.subscription = this.messageComposeService.onStateChange.subscribe(() => this.updateVisibility())
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe()
  }

  private updateVisibility() {
    this.visible = this.messageComposeService.minimised && this.messageComposeService.draft != undefined
  }

  maximise() {
    this.messageComposeService.openDraft()
  }

  discard(event: Event) {
    event.stopPropagation()
    this.messageComposeService.discardDraft()
  }

}
