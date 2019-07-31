package models

class PublicConfig(_ssoEnabled: Boolean, _logoPath: String, _footerLogoPath: String, _versionNumber: String, _defaultLegalNotice: String, _inMigrationPeriod: Boolean, _demosEnabled: Boolean, _userGuideLink: String) {

  var ssoEnabled: Boolean = _ssoEnabled
  var logoPath: String = _logoPath
  var footerLogoPath: String = _footerLogoPath
  var versionNumber: String = _versionNumber
  var defaultLegalNotice: String = _defaultLegalNotice
  var inMigrationPeriod: Boolean = _inMigrationPeriod
  var demosEnabled: Boolean = _demosEnabled
  var userGuideLink: String = _userGuideLink

  def this(_versionNumber: String) =
    this(false, null, null, _versionNumber, null, false, false, null)
}
