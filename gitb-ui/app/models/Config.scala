package models

case class Config(system:Long,
                  parameter: Long,
                  endpoint:Long,
	                 value: String) {
  def withEndpoint(system:Long, parameter:Long, endpoint:Long) = {
    Config(system, parameter, endpoint, this.value)
  }
}
