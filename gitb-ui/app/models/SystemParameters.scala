package models

case class SystemParameters(id: Long, name: String, testKey: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean, inExports: Boolean, community: Long)

class SystemParametersWithValue(_parameter: SystemParameters, _value: Option[SystemParameterValues]) {
  var parameter: SystemParameters = _parameter
  var value: Option[SystemParameterValues] = _value
}