/*
 * Copyright (C) 2025 European Union
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

import { SessionData } from "src/app/components/diagram/test-session-presentation/session-data";

export interface TestResultForDisplay extends SessionData {

    domain?: string,
    domainId?: number
    specification?: string,
    specificationId?: number,
    actor?: string,
    actorId?: number,
    organization?: string,
    organizationId?: number,
    system?: string,
    systemId?: number,
    startTime: string,
    obsolete: boolean,
    testSuiteId?: number,
    testCaseId?: number,
    deletePending?: boolean,
    exportPending?: boolean,
    actionPending?: boolean,
    checked?: boolean,
    communityId?: number

}
