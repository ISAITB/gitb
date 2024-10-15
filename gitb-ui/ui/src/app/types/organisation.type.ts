import { EntityWithId } from "./entity-with-id";
import { ErrorTemplate } from "./error-template";
import { LandingPage } from "./landing-page";
import { LegalNotice } from "./legal-notice";

export interface Organisation extends EntityWithId {

    sname: string
    fname: string
    type: number
    landingPage?: number
    legalNotice?: number
    errorTemplate?: number
    template: boolean
    templateName?: string
    adminOrganization: boolean
    community: number

    landingPages?: LandingPage
    legalNotices?: LegalNotice
    errorTemplates?: ErrorTemplate
    communityLegalNoticeAppliesAndExists?: boolean

}
