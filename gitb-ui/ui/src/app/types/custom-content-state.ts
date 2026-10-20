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

import {CustomContent} from './custom-content';
import {LoadingStatus} from './loading-status.type';

/** The state of a custom content tab, kept by its parent page so that it survives switching between tabs. */
export interface CustomContentState extends LoadingStatus {

  content: CustomContent[]
  total: number
  page: number
  /** The content types currently shown (all by default). */
  types?: number[]
  /** The types of content for which the Test Bed defines a default (loaded once, only needed for community tabs). */
  testBedDefaults?: {[type: number]: boolean}

}
