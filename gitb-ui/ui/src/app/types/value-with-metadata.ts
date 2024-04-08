import { ValueType } from "./value-type"

export interface ValueWithMetadata {

    value: string
    valueType: ValueType
    mimeType: string|undefined

}
