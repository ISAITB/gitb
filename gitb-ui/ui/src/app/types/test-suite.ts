import { EntityWithId } from "./entity-with-id"

export interface TestSuite extends EntityWithId {

    identifier: string
    sname: string
    version: string
    description?: string
    documentation?: string
    specifications?: number[]
    shared: boolean

}
