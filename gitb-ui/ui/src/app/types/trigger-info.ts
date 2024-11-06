import { Trigger } from "./trigger";
import { TriggerDataItem } from "./trigger-data-item";
import {TriggerFireExpression} from './trigger-fire-expression';

export interface TriggerInfo {

    trigger: Trigger
    data?: TriggerDataItem[]
    fireExpressions?: TriggerFireExpression[]
}
