import { EntityWithId } from "./entity-with-id"
import { TestCaseTag } from "./test-case-tag"

export interface TestCase extends EntityWithId {

    identifier: string
    sname: string,
    description?: string
    documentation?:string
    hasDocumentation?: boolean
    optional?: boolean
    disabled?: boolean
    tags?: string
    parsedTags?: TestCaseTag[]

}
