import { EndpointParameter } from "src/app/types/endpoint-parameter";

export interface ConformanceEndpoint {

    id: number
    name: string
    description?: string
    parameters: EndpointParameter[]

}
