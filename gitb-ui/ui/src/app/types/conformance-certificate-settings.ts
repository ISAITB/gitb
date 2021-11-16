export interface ConformanceCertificateSettings {

    id: number
    title?: string
    message?: string
    includeMessage: boolean
    includeTestStatus: boolean
    includeTestCases: boolean
    includeDetails: boolean
    includeSignature: boolean
    keystoreFile?: File
    keystoreType?: string
    passwordsSet: boolean
    keystoreDefined: boolean
    community: number
    keystorePassword? : string
    keyPassword?: string

}
