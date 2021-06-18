import { ConformanceResultFull } from "./conformance-result-full";

export interface ConformanceResultFullList {

    data: ConformanceResultFull[]
    orgParameters?: string[]
    sysParameters?: string[]

}
