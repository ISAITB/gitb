import { UserInteraction } from "src/app/types/user-interaction";

export interface ConformanceTestCase {

    id: number
    sname: string
    description?: string
    outputMessage?: string
    hasDocumentation: boolean
    result: string
    preliminary?: UserInteraction[]
    sessionId?: string

}
