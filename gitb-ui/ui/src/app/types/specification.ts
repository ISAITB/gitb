import { BadgesInfo } from "../components/manage-badges/badges-info"
import { EntityWithId } from "./entity-with-id"
import { SpecificationGroup } from "./specification-group"

export interface Specification extends EntityWithId {

    sname: string
    fname: string
    description?: string
    reportMetadata?: string
    hidden: boolean
    domain: number
    apiKey?: string
    group?: number
    order: number
    badges?: BadgesInfo

    checked?: boolean
    groupObject?: SpecificationGroup
    groups?: SpecificationGroup[]

}
