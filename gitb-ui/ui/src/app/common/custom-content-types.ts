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

import {Constants} from './constants';

/** Labels and icons for the types of custom content (see Constants.CUSTOM_CONTENT_TYPE). */
export class CustomContentTypes {

  /** The supported types, in the order they are listed in the UI. */
  static readonly ALL: number[] = [
    Constants.CUSTOM_CONTENT_TYPE.LANDING_PAGE,
    Constants.CUSTOM_CONTENT_TYPE.LEGAL_NOTICE,
    Constants.CUSTOM_CONTENT_TYPE.ERROR_TEMPLATE
  ]

  /** The label of a type, e.g. "Landing page". */
  static label(type: number): string {
    switch (type) {
      case Constants.CUSTOM_CONTENT_TYPE.LANDING_PAGE: return 'Landing page'
      case Constants.CUSTOM_CONTENT_TYPE.LEGAL_NOTICE: return 'Legal notice'
      case Constants.CUSTOM_CONTENT_TYPE.ERROR_TEMPLATE: return 'Error template'
      default: return 'Custom content'
    }
  }

  /** The label of a type for use within a sentence, e.g. "landing page". */
  static labelLower(type: number): string {
    return CustomContentTypes.label(type).toLowerCase()
  }

  static icon(type: number): string {
    switch (type) {
      case Constants.CUSTOM_CONTENT_TYPE.LANDING_PAGE: return Constants.BUTTON_ICON.LANDING_PAGE
      case Constants.CUSTOM_CONTENT_TYPE.LEGAL_NOTICE: return Constants.BUTTON_ICON.LEGAL_NOTICE
      case Constants.CUSTOM_CONTENT_TYPE.ERROR_TEMPLATE: return Constants.BUTTON_ICON.ERROR_TEMPLATE
      default: return Constants.BUTTON_ICON.CUSTOM_CONTENT
    }
  }

  /** The first path segment of the routes for the pages of a type (e.g. "pages" for landing pages). */
  static pathSegment(type: number): string {
    switch (type) {
      case Constants.CUSTOM_CONTENT_TYPE.LEGAL_NOTICE: return 'notices'
      case Constants.CUSTOM_CONTENT_TYPE.ERROR_TEMPLATE: return 'errortemplates'
      default: return 'pages'
    }
  }

  /** The name of the path parameter holding the ID of a content item of the type. */
  static pathParam(type: number): string {
    switch (type) {
      case Constants.CUSTOM_CONTENT_TYPE.LEGAL_NOTICE: return Constants.NAVIGATION_PATH_PARAM.LEGAL_NOTICE_ID
      case Constants.CUSTOM_CONTENT_TYPE.ERROR_TEMPLATE: return Constants.NAVIGATION_PATH_PARAM.ERROR_TEMPLATE_ID
      default: return Constants.NAVIGATION_PATH_PARAM.LANDING_PAGE_ID
    }
  }

}
