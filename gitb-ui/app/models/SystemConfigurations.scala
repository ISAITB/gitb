package models

case class SystemConfigurations(name: String, parameter: Option[String], description: Option[String])

class SystemConfiguration(_name: String, _parameter: Option[String], _description: Option[String]) {
  var name: String = _name
  var parameter: Option[String] = _parameter
  var description: Option[String] = _description

  def this(_case: SystemConfigurations) =
    this(_case.name, _case.parameter, _case.description)

  def toCaseObject: SystemConfigurations = {
    SystemConfigurations(name, parameter, description)
  }

}