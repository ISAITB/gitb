import { StepReport } from "./report/step-report";

export interface StepData {

    id: string, 
    report?: StepReport,
    status?: number,
    steps: StepData[],
    type: string,
    then?: StepData[],
    else?: StepData[],
    threads?: StepData[][],
    level?: number,
    from?: string,
    to?: string,
    with?: string,
    interactions?: StepData[],
    order?: number,
    fromIndex?: number,
    toIndex?: number,
    span?: number,
    title?: string,
    sequences?: StepData[],
    desc?: string,
    documentation?: string,
    currentIndex?: number

}
