import { Domain } from "./domain";
import { TypedLabelConfig } from "./typed-label-config.type";

export interface Community {

    id: number
    sname: string
    fname: string
    description?: string
    selfRegType: number
    selfRegRestriction: number
    selfRegToken?: string
    selfRegTokenHelpText?: string
    selfRegNotification: boolean
    selfRegForceTemplateSelection?: boolean    
    selfRegForceRequiredProperties?: boolean
    email?: string
    domain?: Domain
    domainId?: number
    allowCertificateDownload: boolean
    allowSystemManagement: boolean
    allowStatementManagement: boolean
    allowPostTestOrganisationUpdates: boolean
    allowPostTestSystemUpdates: boolean
    allowPostTestStatementUpdates: boolean

    sameDescriptionAsDomain: boolean
    activeDescription?: string
    labels?: TypedLabelConfig[]

}
