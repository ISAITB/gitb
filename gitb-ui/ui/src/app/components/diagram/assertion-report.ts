export interface AssertionReport {

    type: string
    value: {
        assertionID?: string,
        value?: string,
        test?: string,
        location?: string,
        description?: string
    }
    extractedLocation?: {
        type: string, 
        name: string, 
        line: number, 
        column: number
    }

}
