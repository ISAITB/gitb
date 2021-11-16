import { ActorInfo } from "../components/diagram/actor-info";
import { StepData } from "../components/diagram/step-data";
import { UserInteraction } from "./user-interaction";

export interface TestCaseDefinition {
    metadata: {
        authors?: string
        description?: string
        documentation?: {
            encoding?: string
            from?: string
            import?: string
            value?: string
        }
        lastModified?: string
        name: string
        published?: string
        version: string
        type: number
    }
    actors: {
        actor: ActorInfo[]
    }
    preliminary?: UserInteraction[]
    steps: StepData[]
    output?: string

}
