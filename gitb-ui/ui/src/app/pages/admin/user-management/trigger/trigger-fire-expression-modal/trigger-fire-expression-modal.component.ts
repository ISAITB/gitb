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

import {AfterViewInit, Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {TriggerFireExpression} from '../../../../../types/trigger-fire-expression';
import {BsModalRef} from 'ngx-bootstrap/modal';
import {DataService} from '../../../../../services/data.service';
import {BaseComponent} from '../../../../base-component.component';
import {ValidationState} from '../../../../../types/validation-state';

@Component({
    selector: 'app-trigger-fire-expression-modal',
    templateUrl: './trigger-fire-expression-modal.component.html',
    styleUrl: './trigger-fire-expression-modal.component.less',
    standalone: false
})
export class TriggerFireExpressionModalComponent extends BaseComponent implements OnInit, AfterViewInit {

  @Input() fireExpression!: TriggerFireExpression;
  @Input() expressionTypes!: number[]
  @Output() savedFireExpression = new EventEmitter<TriggerFireExpression>()
  @ViewChild("expressionContent") expressionField?: ElementRef

  title!: string
  fireExpressionToEdit!: TriggerFireExpression;
  testExpression = false
  testedExpression = false
  testedExpressionMatches = false
  testValue: string = ''
  validation = new ValidationState()

  constructor(
    private readonly modalInstance: BsModalRef,
    public readonly dataService: DataService
  ) { super() }

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (this.expressionField) {
        this.expressionField.nativeElement.focus()
      }
    }, 1)
  }

  ngOnInit(): void {
    this.fireExpressionToEdit = {
      id: this.fireExpression.id,
      expression: this.fireExpression.expression,
      expressionType: this.fireExpression.expressionType,
      notMatch: this.fireExpression.notMatch
    }
  }

  cancel() {
    this.modalInstance.hide()
  }

  save() {
    if (this.validateExpression()) {
      this.savedFireExpression.emit(this.fireExpressionToEdit)
      this.modalInstance.hide()
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

}
