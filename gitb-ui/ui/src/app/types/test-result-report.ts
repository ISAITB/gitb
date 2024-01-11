import { TestInteractionData } from "./test-interaction-data";
import { TestResult } from "./test-result";

export interface TestResultReport {

    result: TestResult,
    test: {
        id?: number
        sname?: string
    },
    organization: {
        id?: number,
        sname?: string,
        community?: number,
        parameters?: {[key: string]: string}
    },
    system: {
        id?: number,
        sname?: string,
        owner?: number
        parameters?: {[key: string]: string}
    },
    actor: {
        id: number,
        name: string,
        domain: number
    },
    specification: {
        id?: number,
        sname?: string,
        domain?: number
    },
    domain: {
        id?: number,
        sname?: string
    },
    testSuite: {
        id?: number,
        sname?: string,
        specification?: number
    },
    logs?: string[]
    interactions?: TestInteractionData[]

}
