export interface SpecificationResult {

    specification: number 
    testSuites: string[]
    testCases: string[]
    actors: string[]
    endpoints: string[]
    parameters: string[]
    testSuiteSummary?: string 
    testCaseSummary?: string
    actorSummary?: string
    endpointSummary?: string
    parameterSummary?: string

}
