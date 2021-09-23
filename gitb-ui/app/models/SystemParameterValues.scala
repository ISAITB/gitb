package models

case class SystemParameterValues(system: Long, parameter: Long, value: String, contentType: Option[String]) {

  def withSystemId(systemId: Long) = {
    SystemParameterValues(systemId, parameter, value, contentType)
  }

}