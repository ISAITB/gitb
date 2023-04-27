import { Community } from "./community";
import { EntityWithId } from "./entity-with-id";
import { ErrorTemplate } from "./error-template";
import { LandingPage } from "./landing-page";
import { LegalNotice } from "./legal-notice";
import { System } from "./system";
import { User } from "./user.type";

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
    admin?: User
    systems?: System[]
    landingPages?: LandingPage
    legalNotices?: LegalNotice
    errorTemplates?: ErrorTemplate
    communities?: Community

}
