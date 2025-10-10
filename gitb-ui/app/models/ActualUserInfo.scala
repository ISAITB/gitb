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

object ActualUserInfo {

  def fromAttributes(uid: String, email: String, name: Option[String], firstName: Option[String], lastName: Option[String]): ActualUserInfo = {
    val nameToUse = if (firstName.isDefined && lastName.isDefined) {
      firstName.get + " " + lastName.get
    } else if (name.isDefined) {
      name.get
    } else {
      email
    }
    new ActualUserInfo(uid, email, nameToUse)
  }

}

class ActualUserInfo(_uid: String, _email: String, _name: String, _accounts: List[UserAccount]) {

  var uid: String = _uid
  var email: String = _email
  var name: String = _name
  var accounts: List[UserAccount] = _accounts

  def this(_uid: String, _email: String, _name: String) =
    this(_uid, _email, _name, null)

}
