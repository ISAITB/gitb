import { EntityWithId } from "./entity-with-id"
import { SpecificationReferenceInfo } from "./specification-reference-info"

export interface TestSuite extends EntityWithId, SpecificationReferenceInfo {

    identifier: string
    sname: string
    version: string
    description?: string
    documentation?: string
    specifications?: number[]
    shared: boolean

}
