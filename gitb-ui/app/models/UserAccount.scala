/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
