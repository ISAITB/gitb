import { SpecificationReferenceInfo } from "src/app/types/specification-reference-info";
import { TestCaseTag } from "src/app/types/test-case-tag";
import { UserInteraction } from "src/app/types/user-interaction";

export interface ConformanceTestCase extends SpecificationReferenceInfo {

    id: number
    sname: string
    description?: string
    outputMessage?: string
    hasDocumentation: boolean
    result: string
    preliminary?: UserInteraction[]
    sessionId?: string
    updateTime?: string
    optional?: boolean
    disabled?: boolean
    tags?: string

    parsedTags?: TestCaseTag[]
    executionPending?: boolean

}
