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

import { Component, Input, OnInit } from '@angular/core';
import { Constants } from 'src/app/common/constants';
import { DiagramEvents } from '../diagram-events';
import { StepData } from '../step-data';

@Component({
    selector: 'app-sequence-diagram-message-status',
    templateUrl: './sequence-diagram-message-status.component.html',
    styles: [],
    standalone: false
})
export class SequenceDiagramMessageStatusComponent implements OnInit {

  @Input() message!: StepData
  @Input() events!: DiagramEvents
  classValue!: string

  TEST_STATUS = Constants.TEST_STATUS

  constructor() { }

  ngOnInit(): void {
    this.classValue = this.calculateClass()
  }

  private calculateClass() {
    let classValue = 'status-wrapper '
    if (this.message.fromIndex! > this.message.toIndex!) {
      classValue += 'backwards-message '
    } else {
      if (this.message.fromIndex == this.message.toIndex) {
        classValue += 'self-message '
      }
    }
    classValue += this.message.type+'-type'
    return classValue
  }

}
