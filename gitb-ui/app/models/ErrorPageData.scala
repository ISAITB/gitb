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

package models

class ErrorPageData(
  _versionNumber: String,
  _resourceVersionNumber: String,
  _hasDefaultLegalNotice: Boolean,
  _defaultLegalNotice: String,
  _userGuideLink: String,
  _errorId: String,
  _testBedHomeLink: String,
  _releaseInfoEnabled: Boolean,
  _releaseInfoLink: String,
  _contextPath: String,
  _restAPI: Option[String]
) {

  var versionNumber: String = _versionNumber
  var resourceVersionNumber: String = _resourceVersionNumber
  var hasDefaultLegalNotice: Boolean = _hasDefaultLegalNotice
  var defaultLegalNotice: String = _defaultLegalNotice
  var userGuideLink: String = _userGuideLink
  var errorId: String = _errorId
  var testBedHomeLink: String = _testBedHomeLink
  var releaseInfoEnabled: Boolean = _releaseInfoEnabled
  var releaseInfoLink: String = _releaseInfoLink
  var contextPath: String = _contextPath
  var restApi: Option[String] = _restAPI

}
