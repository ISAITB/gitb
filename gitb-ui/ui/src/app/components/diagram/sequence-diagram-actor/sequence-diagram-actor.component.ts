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

import { Component, Input, OnInit } from '@angular/core';
import { ActorInfo } from '../actor-info';
import {ActorRole} from '../../../types/actor-role';
import {Constants} from '../../../common/constants';

@Component({
    selector: 'app-sequence-diagram-actor',
    templateUrl: './sequence-diagram-actor.component.html',
    styleUrl: './sequence-diagram-actor.component.less',
    standalone: false
})
export class SequenceDiagramActorComponent implements OnInit {

  @Input() actor!: ActorInfo
  @Input() index!: number

  actorForDisplay!: string

  constructor() { }

  ngOnInit(): void {
    if (this.actor.name != undefined) {
      this.actorForDisplay = this.actor.name
    } else {
      this.actorForDisplay = this.actor.id
    }
  }

  protected readonly ActorRole = ActorRole;
  protected readonly Constants = Constants;
}
