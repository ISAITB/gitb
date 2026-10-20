/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import { EntityWithId } from "./entity-with-id";
import { CustomContent } from "./custom-content";

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
    selfRegistrationToken?: string
    selfRegistrationDefault: boolean
    community: number

    landingPages?: CustomContent
    legalNotices?: CustomContent
    errorTemplates?: CustomContent
    communityLegalNoticeAppliesAndExists?: boolean

}
