package models

/**
 * Created by simatosc.
 */
case class DomainParameter(id: Long, name: String, desc: Option[String], kind: String, value: Option[String], domain: Long) {
}
