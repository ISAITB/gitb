export interface TestSuite {

    id: number
    identifier: string
    sname: string
    version: string
    description?: string
    documentation?: string
    specifications?: number[]
    shared: boolean

}
