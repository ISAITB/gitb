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

import {Component, Input, OnInit} from '@angular/core';
import {TriggerFireExpression} from '../../../../../types/trigger-fire-expression';
import {DataService} from '../../../../../services/data.service';
import {BaseComponent} from '../../../../base-component.component';
import {ValidationState} from '../../../../../types/validation-state';
import {Constants} from '../../../../../common/constants';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'app-trigger-fire-expression-modal',
    templateUrl: './trigger-fire-expression-modal.component.html',
    styleUrl: './trigger-fire-expression-modal.component.less',
    standalone: false
})
export class TriggerFireExpressionModalComponent extends BaseComponent implements OnInit {

  @Input() fireExpression!: TriggerFireExpression;
  @Input() expressionTypes!: number[]

  title!: string
  fireExpressionToEdit!: TriggerFireExpression;
  testExpression = false
  testedExpression = false
  testedExpressionMatches = false
  testValue: string = ''
  validation = new ValidationState()

  constructor(
    private readonly modalInstance: NgbActiveModal,
    public readonly dataService: DataService
  ) { super() }

  ngOnInit(): void {
    this.fireExpressionToEdit = {
      id: this.fireExpression.id,
      expression: this.fireExpression.expression,
      expressionType: this.fireExpression.expressionType,
      notMatch: this.fireExpression.notMatch
    }
  }

  cancel() {
    this.modalInstance.dismiss()
  }

  save() {
    if (this.validateExpression()) {
      this.modalInstance.close(this.fireExpressionToEdit)
    }
  }

  saveDisabled() {
    return !this.textProvided(this.fireExpressionToEdit.expression)
  }

  private validateExpression(): boolean {
    this.validation.clearErrors()
    const valid = this.isValidRegularExpression(this.fireExpressionToEdit.expression)
    if (!valid) {
      this.validation.invalid('expressionContent', 'The provided value is not a valid regular expression.')
    }
    return valid
  }

  testSampleValue() {
    if (this.validateExpression()) {
      const expressionMatches = new RegExp(this.fireExpressionToEdit.expression).test(this.testValue);
      this.testedExpressionMatches = expressionMatches && !this.fireExpressionToEdit.notMatch || !expressionMatches && this.fireExpressionToEdit.notMatch
      this.testedExpression = true
    }
  }

  protected readonly Constants = Constants;
}
