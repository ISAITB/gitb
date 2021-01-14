import { EndpointParameter } from "./endpoint-parameter";

export interface Endpoint {

    id: number
    name: string
    description?: string
    actor: number
    parameters: EndpointParameter[]

}
