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
