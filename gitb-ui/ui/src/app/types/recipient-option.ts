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

import { EntityWithId } from './entity-with-id';

/** An item in the "Send to:" recipient picker(s) of the compose message modal. Group options (e.g.
 * "All community members") carry a synthetic negative id and a targetType (see Constants.MESSAGE_TARGET_TYPE).
 * A specific organisation option uses its real (positive) id with targetType ORGANISATION and organisationId
 * set. In the Test Bed administrator's first-stage community picker, a plain community entry has no
 * targetType at all - selecting it drills into the second-stage picker rather than being sendable itself. */
export interface RecipientOption extends EntityWithId {

    id: number
    fname: string
    targetType?: number
    communityId?: number
    organisationId?: number

}
