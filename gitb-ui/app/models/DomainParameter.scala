package models

/**
 * Created by simatosc.
 */
case class DomainParameter(id: Long, name: String, desc: Option[String], kind: String, value: String, domain: Long) {
}
