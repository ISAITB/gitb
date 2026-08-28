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

import { Component, Input } from '@angular/core';
import { Constants } from '../../common/constants';
import { MessageChainItem } from '../../types/message-chain-item';

/**
 * The "chain of earlier messages" for a reply - reused both below the body editor when composing a reply
 * (ComposeMessageModalComponent) and above a reply's own content in the message detail panel
 * (MessageDetailComponent). Presentation is modelled on the test session comment chain (see
 * TestResultCommentsModalComponent): rounded, collapsible entries connected by an indented line.
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

  protected readonly Constants = Constants

  toggle(item: MessageChainItem) {
    item.collapsed = !item.collapsed
  }

}
