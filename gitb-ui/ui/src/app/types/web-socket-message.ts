import { StepReport } from "../components/diagram/report/step-report";
import { SUTConfiguration } from "./sutconfiguration";
import { UserInteraction } from "./user-interaction";
import { UserInteractionRequest } from "./user-interaction-request";

export interface WebSocketMessage {

    tcInstanceId: string
    stepId: string
    status: number
    report: StepReport
    interaction?: UserInteractionRequest
    outputMessage?: string
    notify?: {
        simulatedConfigs?: SUTConfiguration[]
    }
    interactions?: UserInteraction[],
    inputTitle?: string
    stepHistory: { stepId: string, status: number, path: string }[]

}
