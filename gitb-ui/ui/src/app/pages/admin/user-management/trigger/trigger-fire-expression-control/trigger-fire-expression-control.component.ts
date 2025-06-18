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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {TriggerFireExpression} from '../../../../../types/trigger-fire-expression';
import {DataService} from '../../../../../services/data.service';
import {Constants} from '../../../../../common/constants';

@Component({
    selector: 'app-trigger-fire-expression-control',
    templateUrl: './trigger-fire-expression-control.component.html',
    styleUrl: './trigger-fire-expression-control.component.less',
    standalone: false
})
export class TriggerFireExpressionControlComponent implements OnInit {

  @Input() fireExpression!: TriggerFireExpression;
  @Input() updateEmitter!: EventEmitter<void>;
  @Input() expressionTypes!: number[]
  @Input() first: boolean = false
  @Output() edit = new EventEmitter<TriggerFireExpression>();
  @Output() delete = new EventEmitter<TriggerFireExpression>();
  expressionDescription!: string

  constructor(private dataService: DataService) {
  }

  ngOnInit(): void {
    this.updateDescription()
    this.updateEmitter.subscribe(() => {
      this.updateDescription()
    })
  }

  private updateDescription(): void {
    let matchType = 'matches'
    if (this.fireExpression.notMatch) {
      matchType = 'does not match'
    }
    this.expressionDescription = `${this.dataService.triggerExpressionTypeLabel(this.fireExpression.expressionType)} ${matchType}`
  }

  doEdit() {
    this.edit.emit(this.fireExpression)
  }

  doDelete() {
    this.delete.emit(this.fireExpression)
  }

  protected readonly Constants = Constants;
}
