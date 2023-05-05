package models

case class SpecificationGroups(id: Long, shortname:String, fullname:String, description:Option[String], displayOrder: Short, domain:Long) {
}
