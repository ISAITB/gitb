export interface AppConfigurationProperties {

    emailEnabled: boolean
    emailContactFormEnabled: boolean,
    emailAttachmentsMaxCount?: number
    emailAttachmentsMaxSize?: number
    emailAttachmentsAllowedTypes?: string
    surveyEnabled: boolean
    surveyAddress: string
    moreInfoEnabled: boolean
    moreInfoAddress: string
    releaseInfoEnabled: boolean
    releaseInfoAddress: string
    userGuideOU: string
    userGuideOA: string
    userGuideCA: string
    userGuideTA: string
    ssoEnabled: boolean
    ssoInMigration: boolean
    demosEnabled: boolean
    demosAccount: number
    registrationEnabled: boolean
    savedFileMaxSize: number
    mode: string
    automationApiEnabled: boolean
    versionNumber: string
    
}
