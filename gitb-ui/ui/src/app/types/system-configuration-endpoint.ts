import { SystemConfigurationParameter } from "./system-configuration-parameter";

export interface SystemConfigurationEndpoint {

    id: number
    name: string
    description?: string
    parameters: SystemConfigurationParameter[]

}
