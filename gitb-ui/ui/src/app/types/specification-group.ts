import { EntityWithId } from "./entity-with-id"

export interface SpecificationGroup extends EntityWithId {

    sname: string
    fname: string
    description?: string
    reportMetadata?: string
    domain: number
    order: number
    apiKey: string

}
