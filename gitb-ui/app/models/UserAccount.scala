package models

import models.Enums.UserRole

class UserAccount(_user: Users, _organisation: Organizations, _community: Communities) extends Ordered[UserAccount] {

  val user:Users = _user
  val organisation:Organizations = _organisation
  val community:Communities = _community

  override def compare(that: UserAccount): Int = {
    if (this.user.role == UserRole.SystemAdmin.id.toShort) {
      // Test bed admins appear first
      -1
    } else {
      if (this.community.id == that.community.id) {
        // Same community
        if (this.user.role == UserRole.CommunityAdmin.id.toShort) {
          // Community admins appear first
          -1
        } else {
          if (this.organisation.id == that.organisation.id) {
            // Same organisation
            if (this.user.role == UserRole.VendorAdmin.id.toShort) {
              -1
            } else {
              1
            }
          } else {
            // Different organisation
            this.organisation.fullname.compareTo(that.organisation.fullname)
          }
        }
      } else {
        // Different community
        this.community.fullname.compareTo(that.community.fullname)
      }
    }
  }

}
