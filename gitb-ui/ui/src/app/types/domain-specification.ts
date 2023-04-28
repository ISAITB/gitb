import { Specification } from "./specification"

export interface DomainSpecification {

    id: number
    sname: string
    fname: string
    description?: string
    hidden: boolean
    group: boolean
    option: boolean
    groupId?: number
    options?: DomainSpecification[]
    collapsed: boolean
    domain: number
    order: number

    movePending?: boolean
    copyPending?: boolean
    removePending?: boolean

}
