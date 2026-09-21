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

import { SessionPresentationData } from "./session-presentation-data";

export interface SessionData {

    session: string
    endTime?: string
    result: 'SUCCESS'|'FAILURE'|'UNDEFINED'
    diagramLoaded?: boolean
    expanded?: boolean
    expansionPending?: boolean
    // Timestamp of the most recent expansion - used to determine, among several simultaneously
    // expanded rows (across both the active and completed tables), which one to restore on a
    // "View XYZ" Back navigation (the most recently expanded one, i.e. the one the user acted from).
    expandedOrder?: number
    testSuite: string
    testCase: string
    diagramState?: SessionPresentationData

    hasUnreadErrorLogs?: boolean
    hasUnreadWarningLogs?: boolean
    hasUnreadMessageLogs?: boolean
    reviewedLogLines?: number

}
