import {EventEmitter} from '@angular/core';

export interface TriggerFireExpression {

  id?: number
  expression: string
  expressionType: number
  notMatch: boolean

}
