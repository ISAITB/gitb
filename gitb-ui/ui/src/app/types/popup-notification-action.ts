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

import {NavigationTarget} from './navigation-target';

/**
 * Optional action of a popup notification, triggered by clicking the notification's text. If a target is
 * provided the text is rendered as a regular link (supporting e.g. opening in a new tab). The handler
 * is invoked on plain (non-modified) clicks and can be used for navigation-less actions (e.g. a download).
 * Activating the action always dismisses the notification.
 */
export interface PopupNotificationAction {

  target?: NavigationTarget;
  handler?: () => void;

}
