import { EndpointParameter } from "src/app/types/endpoint-parameter";

export interface ParameterData extends EndpointParameter {

    kindLabel: string
    useLabel: boolean
    adminOnlyLabel: boolean
    notForTestsLabel: boolean

}
