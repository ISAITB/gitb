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

    checked?: boolean

}
