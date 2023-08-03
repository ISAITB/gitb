import { EntityWithId } from "./entity-with-id"

export interface TestCase extends EntityWithId {

    identifier: string
    sname: string,
    description?: string
    documentation?:string
    hasDocumentation?: boolean
    optional?: boolean
    disabled?: boolean

}
