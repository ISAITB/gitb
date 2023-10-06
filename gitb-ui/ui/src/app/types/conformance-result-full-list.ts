import { ConformanceResultFull } from "./conformance-result-full";

export interface ConformanceResultFullList {

    data: ConformanceResultFull[]
    count: number
    orgParameters?: string[]
    sysParameters?: string[]

}
