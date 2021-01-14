import { Endpoint } from "src/app/types/endpoint";
import { ParameterData } from "./parameter-data";

export interface EndpointData extends Endpoint {

    labelledParameters: ParameterData[]

}
