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
  _releaseInfoLink: String
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

}
