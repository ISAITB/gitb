import { ParameterPresetValue } from "./parameter-preset-value";

export interface Parameter {

    id: number
    name: string
    testKey: string
    desc?: string
    use: 'R'|'O'
    kind: 'SIMPLE'|'SECRET'|'BINARY'
    kindLabel?: string
    adminOnly: boolean
    notForTests: boolean
    hidden: boolean
    allowedValues?: string
    dependsOn?: string
    dependsOnValue?: string
    defaultValue?: string
    hasPresetValues: boolean
    presetValues?: ParameterPresetValue[]

    inExports?: boolean
    inSelfRegistration?: boolean
    configured?: boolean
    value?: string
    valueToShow?: string
    checkedPrerequisites?: boolean
    prerequisiteOk?: boolean
    mimeType?: string
    fileName?: string
    endpoint?: number

}
