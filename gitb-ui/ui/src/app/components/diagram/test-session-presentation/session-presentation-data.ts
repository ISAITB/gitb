import { TestStepResult } from "src/app/types/test-step-result";
import { ActorInfo } from "../actor-info";
import { DiagramEvents } from "../diagram-events";
import { StepData } from "../step-data";

export interface SessionPresentationData {

    stepsOfTests: {[key: string]: StepData[]}
    actorInfoOfTests: {[key: string]: ActorInfo[]}
    outputMessage?: string
    outputMessageType?: string
    events: DiagramEvents
    testResultFlat:{[key: string]: TestStepResult}

}
