export interface Actor {

    id: number
    actorId: string
    name: string
    description?:string
    hidden: boolean
    default: boolean
    displayOrder?:number
    specification: number
    apiKey?: string

}
