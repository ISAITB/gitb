import { ParameterPresetValue } from "./parameter-preset-value";

export interface ParameterReference {

    id: number
    name: string
    key: string
    kind: 'SIMPLE'|'SECRET'|'BINARY'
    hasPresetValues: boolean
    presetValues?: ParameterPresetValue[]

}
