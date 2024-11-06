import {AfterViewInit, Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {TriggerFireExpression} from '../../../../../types/trigger-fire-expression';
import {BsModalRef} from 'ngx-bootstrap/modal';
import {DataService} from '../../../../../services/data.service';
import {BaseComponent} from '../../../../base-component.component';
import {ValidationState} from '../../../../../types/validation-state';

@Component({
  selector: 'app-trigger-fire-expression-modal',
  templateUrl: './trigger-fire-expression-modal.component.html',
  styleUrl: './trigger-fire-expression-modal.component.less'
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
    private modalInstance: BsModalRef,
    public dataService: DataService
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
      this.testedExpressionMatches = expressionMatches && !this.fireExpressionToEdit.notMatch
      this.testedExpression = true
    }
  }

}
