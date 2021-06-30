export interface SpecificationChoice {

    specification: number
    history: 'drop'|'keep'
    metadata: 'update'|'skip'
    skipUpdate: boolean
    dataExists: boolean
    testSuiteExists: boolean

}
