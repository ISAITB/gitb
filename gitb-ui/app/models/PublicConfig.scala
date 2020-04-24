package models

class PublicConfig(_ssoEnabled: Boolean, _logoPath: String, _footerLogoPath: String, _versionNumber: String, _hasDefaultLegalNotice: Boolean, _defaultLegalNotice: String, _inMigrationPeriod: Boolean, _demosEnabled: Boolean, _userGuideLink: String, _registrationEnabled: Boolean, _euLoginUseGuide: String, _euLoginMigrationGuide: String, _cookiePath: String, _initialSandboxRun: Boolean) {

  var ssoEnabled: Boolean = _ssoEnabled
  var logoPath: String = _logoPath
  var footerLogoPath: String = _footerLogoPath
  var versionNumber: String = _versionNumber
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

  def this(_versionNumber: String) =
    this(false, null, null, _versionNumber, false, null, false, false, null, false, null, null, null, false)
}
