package models

class SystemConfigurationParameter(_parameter: Parameters, _configured: Boolean, _config: Option[Configs], _mimeType: Option[String], _extension: Option[String]) {
  var parameter: Parameters = _parameter
  var config: Option[Configs] = _config
  var configured: Boolean = _configured
  var mimeType: Option[String] = _mimeType
  var extension: Option[String] = _extension
}
