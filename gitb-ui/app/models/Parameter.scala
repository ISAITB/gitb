package models

/**
 * Created by serbay.
 */
case class Parameters(id: Long, name: String, desc: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests:Boolean, endpoint: Long) {
	def withEndpoint(endpoint:Long) = {
		this.copy(endpoint = endpoint)
	}
}
