package models

case class SystemParameterValues(system: Long, parameter: Long, value: String) {

  def withSystemId(systemId: Long) = {
    SystemParameterValues(systemId, parameter, value)
  }

}