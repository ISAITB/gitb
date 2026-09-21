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

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MessageChainItem } from '../../types/message-chain-item';
import {Constants} from '../../common/constants';

/**
 * The "chain of earlier messages" for a reply - reused both below the body editor when composing a reply
 * (ComposeMessageModalComponent) and above a reply's own content in the message detail panel
 * (MessageDetailComponent). The chain itself is hidden by default behind a "Show earlier messages"
 * toggle (so a long thread doesn't dominate the screen); once expanded, each entry is rendered via
 * app-message-item - the same component the main selected message uses - individually collapsed by
 * default, connected by an indented line.
 */
@Component({
  selector: 'app-message-chain',
  standalone: false,
  templateUrl: './message-chain.component.html',
  styleUrl: './message-chain.component.less'
})
export class MessageChainComponent {

  @Input() chain: MessageChainItem[] = []
  // When true, the indent line extends one item further than the chain itself, connecting it to
  // additional content shown alongside it (the reply's own body editor, or a viewed message's own
  // content). `connectorPosition` says whether that extra content sits before the chain (compose modal -
  // the chain is ordered newest-ancestor-first there) or after it (message detail panel - oldest-first).
  @Input() continuesToContent = false
  @Input() connectorPosition: 'before'|'after' = 'after'
  // False suppresses every entry's options menu - used by the compose modal's reply preview.
  @Input() interactive = true
  @Output() replyRequested = new EventEmitter<{ id: number, subject?: string }>()

  // Whether the collapsed-by-default chain list itself is currently shown - independent of each
  // individual entry's own collapsed/expanded body state (see app-message-item).
  chainExpanded = false

  toggleChain() {
    this.chainExpanded = !this.chainExpanded
  }

  protected readonly Constants = Constants;
}
