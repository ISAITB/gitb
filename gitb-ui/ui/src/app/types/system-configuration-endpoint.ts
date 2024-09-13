import { EndpointParameter } from "./endpoint-parameter";

export interface SystemConfigurationEndpoint {

    id: number
    name: string
    description?: string
    parameters: EndpointParameter[]

}
