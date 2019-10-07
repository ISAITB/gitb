package models

class SystemConfigurationEndpoint(_endpoint: Endpoints, _endpointParameters: Option[List[SystemConfigurationParameter]]) {
  var endpoint: Endpoints = _endpoint
  var endpointParameters: Option[List[SystemConfigurationParameter]] = _endpointParameters
}
