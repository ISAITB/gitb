package models

class PublicConfig(
  _ssoEnabled: Boolean,
  _versionNumber: String,
  _resourceVersionNumber: String,
  _hasDefaultLegalNotice: Boolean,
  _defaultLegalNotice: String,
  _inMigrationPeriod: Boolean,
  _demosEnabled: Boolean,
  _userGuideLink: String,
  _registrationEnabled: Boolean,
  _euLoginUseGuide: String,
  _euLoginMigrationGuide: String,
  _cookiePath: String,
  _initialSandboxRun: Boolean,
  _developmentMode: Boolean,
  _contextPath: String,
  _moreInfoEnabled: Boolean,
  _moreInfoLink: String,
  _releaseInfoEnabled: Boolean,
  _releaseInfoLink: String,
  _welcomeText: String,
  _internalContextPath: String,
  _restAPI: Option[String]) {

  var ssoEnabled: Boolean = _ssoEnabled
  var versionNumber: String = _versionNumber
  var resourceVersionNumber: String = _resourceVersionNumber
  var hasDefaultLegalNotice: Boolean = _hasDefaultLegalNotice
  var defaultLegalNotice: String = _defaultLegalNotice
  var inMigrationPeriod: Boolean = _inMigrationPeriod
  var demosEnabled: Boolean = _demosEnabled
  var registrationEnabled: Boolean = _registrationEnabled
  var userGuideLink: String = _userGuideLink
  var euLoginUseGuideLink: String = _euLoginUseGuide
  var euLoginMigrationGuideLink: String = _euLoginMigrationGuide
  var cookiePath: String = _cookiePath
  var initialSandboxRun: Boolean = _initialSandboxRun
  var developmentMode: Boolean = _developmentMode
  var contextPath: String = _contextPath
  var moreInfoEnabled: Boolean = _moreInfoEnabled
  var moreInfoLink: String = _moreInfoLink
  var releaseInfoEnabled: Boolean = _releaseInfoEnabled
  var releaseInfoLink: String = _releaseInfoLink
  var welcomeText: String = _welcomeText
  var internalContextPath: String = _internalContextPath
  var restApi: Option[String] = _restAPI

  def this(_resourceVersionNumber: String, _cookiePath: String, _contextPath: String, _internalContextPath: String) =
    this(false, null, _resourceVersionNumber, false, null, false, false, null, false, null, null, _cookiePath, false, false, _contextPath, false, null, false, null, null, _internalContextPath, None)
}
