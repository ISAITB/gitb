import { UserInteraction } from "./user-interaction"

export interface TestInteractionData {

    stepId: string
    interactions: UserInteraction[]
    inputTitle?: string
    admin?: boolean
    desc?: string

}
