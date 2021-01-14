import { SystemConfigurationParameter } from "src/app/types/system-configuration-parameter";

export interface EndpointRepresentation {

    id: number
    name: string
    description?: string
    parameters: SystemConfigurationParameter[]

}
