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

/** The default recipient a reply's picker should be pre-selected with, as returned by
 * MessageManager.resolveReplyTarget - see MessagesComponent.buildReplySeed for how this is mapped onto
 * the role-specific picker's static options / loaded list entries. All fields are undefined when no
 * default could be resolved (the picker then simply starts empty). */
export interface ReplyTargetInfo {

    targetType?: number
    communityId?: number
    communityName?: string
    organisationId?: number
    organisationName?: string

}
