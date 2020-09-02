package models.prerequisites

trait WithPrerequisite {

  def prerequisiteKey(): Option[String]
  def prerequisiteValue(): Option[String]
  def currentKey(): String
  def currentValue(): Option[String]

}
