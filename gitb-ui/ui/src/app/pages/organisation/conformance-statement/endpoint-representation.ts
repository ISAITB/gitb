import { EndpointParameter } from "src/app/types/endpoint-parameter";

export interface EndpointRepresentation {

    id: number
    name: string
    description?: string
    parameters: EndpointParameter[]

}
