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

class ActualUserInfo(_uid: String, _email: String, _firstName: String, _lastName: String, _accounts: List[UserAccount]) {

  var uid: String = _uid
  var email: String = _email
  var firstName: String = _firstName
  var lastName: String = _lastName
  var accounts: List[UserAccount] = _accounts

  def this(_uid: String, _email: String, _firstName: String, _lastName: String) =
    this(_uid, _email, _firstName, _lastName, null)

}
