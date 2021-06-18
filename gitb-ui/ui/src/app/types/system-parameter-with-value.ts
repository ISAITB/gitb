import { SystemParameter } from "./system-parameter";

export interface SystemParameterWithValue extends SystemParameter {

    configured: boolean
    value?: string

}
