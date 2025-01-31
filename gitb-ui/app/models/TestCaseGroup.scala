package models

case class TestCaseGroup(id: Long, identifier: String, name: Option[String], description: Option[String], testSuite: Long) {

  def withIds(groupId: Long, testSuiteId: Long): TestCaseGroup = {
    TestCaseGroup(groupId, this.identifier, this.name, this.description, testSuiteId)
  }

  override def equals(obj: Any): Boolean = {
    this.id.equals(obj.asInstanceOf[TestCaseGroup].id)
  }

  override def hashCode(): Int = {
    this.id.hashCode()
  }

}
