import { BadgesInfo } from "../components/manage-badges/badges-info"
import { EntityWithId } from "./entity-with-id"

export interface Specification extends EntityWithId {

    sname: string
    fname: string
    description?: string
    hidden: boolean
    domain: number
    apiKey?: string
    group?: number
    order: number
    badges?: BadgesInfo

    checked?: boolean

}
