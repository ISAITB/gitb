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

import {WelcomeTexts} from './welcome-texts';

export interface WelcomeTextInfo {
  key: keyof WelcomeTexts
  id: string
  label: string
  tooltip: string
}

/**
 * The configurable welcome page plain text values (the banner title plus the option card texts), in
 * the order in which they are displayed on the welcome page settings form. All are always shown, even
 * when the corresponding card is not currently displayed (see each tooltip for its display condition).
 */
export const WELCOME_TEXT_INFOS: WelcomeTextInfo[] = [
  { key: 'title', id: 'welcomeTitle', label: 'Banner title',
    tooltip: 'The title to display on the welcome page banner.' },
  { key: 'logInCardTitle', id: 'welcomeLogInCardTitle', label: 'Login (title)',
    tooltip: 'The title of the main login card. This card is always present.' },
  { key: 'logInCardContent', id: 'welcomeLogInCardContent', label: 'Login (content)',
    tooltip: 'The content of the main login card. This card is always present.' },
  { key: 'confirmRoleCardTitle', id: 'welcomeConfirmRoleCardTitle', label: 'Confirm role (title)',
    tooltip: 'The title of the role confirmation card. This card is present when accounts can be linked to multiple roles.' },
  { key: 'confirmRoleCardContent', id: 'welcomeConfirmRoleCardContent', label: 'Confirm role (content)',
    tooltip: 'The content of the role confirmation card. This card is present when accounts can be linked to multiple roles.' },
  { key: 'registerCardTitle', id: 'welcomeRegisterCardTitle', label: 'Register (title)',
    tooltip: 'The title of the self-registration card. This card is present when self-registration is enabled.' },
  { key: 'registerCardContent', id: 'welcomeRegisterCardContent', label: 'Register (content)',
    tooltip: 'The content of the self-registration card. This card is present when self-registration is enabled.' },
  { key: 'demoCardTitle', id: 'welcomeDemoCardTitle', label: 'Demo (title)',
    tooltip: 'The title of the demo card. This card is present when demos are enabled.' },
  { key: 'demoCardContent', id: 'welcomeDemoCardContent', label: 'Demo (content)',
    tooltip: 'The content of the demo card. This card is present when demos are enabled.' }
]
