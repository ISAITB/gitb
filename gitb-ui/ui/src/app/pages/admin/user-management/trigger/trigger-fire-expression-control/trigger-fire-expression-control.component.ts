import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {TriggerFireExpression} from '../../../../../types/trigger-fire-expression';
import {DataService} from '../../../../../services/data.service';
import {Constants} from '../../../../../common/constants';

@Component({
  selector: 'app-trigger-fire-expression-control',
  templateUrl: './trigger-fire-expression-control.component.html',
  styleUrl: './trigger-fire-expression-control.component.less'
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
