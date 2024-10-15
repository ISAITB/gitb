import { EntityWithId } from "./entity-with-id"

export interface Domain extends EntityWithId {

    sname: string
    fname: string
    description?: string
    reportMetadata?: string
    apiKey?: string
    
}
