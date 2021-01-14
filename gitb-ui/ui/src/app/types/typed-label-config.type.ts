import { LabelConfig } from './label-config.type'

export interface TypedLabelConfig extends LabelConfig {
    labelType: number, 
    custom?: boolean
}
