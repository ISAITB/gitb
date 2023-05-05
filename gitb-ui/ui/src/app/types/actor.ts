import { EntityWithId } from "./entity-with-id"

export interface Actor extends EntityWithId {

    actorId: string
    name: string
    description?:string
    hidden: boolean
    default: boolean
    displayOrder?:number
    specification: number
    apiKey?: string

}
