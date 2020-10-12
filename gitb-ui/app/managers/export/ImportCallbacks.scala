package managers.export

import slick.dbio.DBIO

object ImportCallbacks {

  def set[A](_fnCreate: (A, ImportItem) => DBIO[Any], _fnUpdate: (A, String, ImportItem) => DBIO[_]): ImportCallbacks[A] = {
    new ImportCallbacks[A](_fnCreate, _fnUpdate, None, None)
  }

  def set[A](_fnCreate: (A, ImportItem) => DBIO[Any], _fnUpdate: (A, String, ImportItem) => DBIO[_], _fnPostCreate: (A, Any, ImportItem) => _): ImportCallbacks[A] = {
    new ImportCallbacks[A](_fnCreate, _fnUpdate, Some(_fnPostCreate), None)
  }

  def set[A](_fnCreate: (A, ImportItem) => DBIO[Any], _fnUpdate: (A, String, ImportItem) => DBIO[_], _fnPostCreate: Option[(A, Any, ImportItem) => _], _fnCreatedIdHandle: (A, String, Any, ImportItem) => _): ImportCallbacks[A] = {
    new ImportCallbacks[A](_fnCreate, _fnUpdate, _fnPostCreate, Some(_fnCreatedIdHandle))
  }

}

class ImportCallbacks[A](_fnCreate: (A, ImportItem) => DBIO[Any], _fnUpdate: (A, String, ImportItem) => DBIO[_], _fnPostCreate: Option[(A, Any, ImportItem) => _], _fnCreatedIdHandle: Option[(A, String, Any, ImportItem) => _]) {
  var fnCreate: (A, ImportItem) => DBIO[Any] = _fnCreate
  var fnUpdate: (A, String, ImportItem) => DBIO[_] = _fnUpdate
  var fnPostCreate: Option[(A, Any, ImportItem) => _] = _fnPostCreate
  var fnCreatedIdHandle: Option[(A, String, Any, ImportItem) => _] = _fnCreatedIdHandle
  var fnSkipButProcessChildren: Option[(A, String, ImportItem) => DBIO[_]] = None

  def withFnForSkipButProcessChildren(fn: (A, String, ImportItem) => DBIO[_]): ImportCallbacks[A] = {
    fnSkipButProcessChildren = Some(fn)
    this
  }
}
