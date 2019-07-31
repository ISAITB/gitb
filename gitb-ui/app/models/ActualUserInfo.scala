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
