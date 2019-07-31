package models

import models.Enums.UserRole

class UserAccount(_user: Users, _organisation: Organizations, _community: Communities) extends Ordered[UserAccount] {

  val user:Users = _user
  val organisation:Organizations = _organisation
  val community:Communities = _community

  override def compare(that: UserAccount): Int = {
    if (this.user.role == that.user.role) {
      if (this.user.role == UserRole.SystemAdmin.id.toShort) {
        // All test bed admins are considered the same
        0
      } else {
        if (this.community.id == that.community.id) {
          // Same community
          if (this.organisation.id == that.organisation.id) {
            // Same organisation
            0
          } else {
            // Different organisation
            this.organisation.fullname.compareTo(that.organisation.fullname)
          }
        } else {
          // Different community
          this.community.fullname.compareTo(that.community.fullname)
        }
      }
    } else {
      if (this.user.role == UserRole.SystemAdmin.id.toShort
        || this.user.role == UserRole.CommunityAdmin.id.toShort && that.user.role != UserRole.SystemAdmin.id.toShort
        || this.user.role == UserRole.VendorAdmin.id.toShort && that.user.role != UserRole.SystemAdmin.id.toShort && that.user.role != UserRole.CommunityAdmin.id.toShort) {
        -1
      } else {
        1
      }
    }
  }

}
