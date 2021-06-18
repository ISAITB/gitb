import { SystemConfigurationParameter } from "src/app/types/system-configuration-parameter";

export interface ConformanceEndpoint {

    id: number
    name: string
    description?: string
    parameters: SystemConfigurationParameter[]

}
