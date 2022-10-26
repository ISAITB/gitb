package models

case class CommunityResources(id: Long, name: String, description: Option[String], community: Long) {

  def withName(newName: String): CommunityResources = {
    CommunityResources(this.id, newName, this.description, this.community)
  }

}
