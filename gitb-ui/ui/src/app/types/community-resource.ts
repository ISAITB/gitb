export interface CommunityResource {

    id: number
    name: string
    description?:string
    reference: string
    community: number

    deletePending?:boolean
    downloadPending?:boolean
    checked?: boolean

}
