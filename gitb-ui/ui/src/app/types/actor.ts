import { BadgesInfo } from "../components/manage-badges/badges-info"
import { EntityWithId } from "./entity-with-id"

export interface Actor extends EntityWithId {

    actorId: string
    name: string
    description?:string
    reportMetadata?: string
    hidden: boolean
    default: boolean
    displayOrder?:number
    specification: number
    apiKey?: string
    badges?: BadgesInfo

}
