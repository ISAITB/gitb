export interface AssertionReport {

    type: "info"|"warning"|"error"
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
