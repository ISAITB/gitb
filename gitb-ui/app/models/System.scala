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

case class Systems(id:Long, shortname:String, fullname:String, description:Option[String], version:Option[String], apiKey: String, badgeKey: String, owner:Long) {

  def withOrganizationId(id:Long): Systems = {
    Systems(this.id, this.shortname, this.fullname, this.description, this.version, this.apiKey, this.badgeKey, id)
  }

  def withApiKey(apiKey: String): Systems = {
    Systems(this.id, this.shortname, this.fullname, this.description, this.version, apiKey, this.badgeKey, this.owner)
  }

  def withBadgeKey(badgeKey: String): Systems = {
    Systems(this.id, this.shortname, this.fullname, this.description, this.version, this.apiKey, badgeKey, this.owner)
  }

}

class System(_id: Long, _shortname: String, _fullname:String, _description:Option[String], _version:Option[String], _apiKey: String, _badgeKey: String,
               _owner:Option[Organizations], _admins:Option[List[Users]])
{
  var id:Long = _id
  var shortname:String = _shortname
  var fullname:String = _fullname
  var description:Option[String] = _description
  var version:Option[String] = _version
  var apiKey: String = _apiKey
  var badgeKey: String = _badgeKey
  var owner:Option[Organizations] = _owner
  var admins:Option[List[Users]] = _admins

  def this(_case:Systems) =
    this(_case.id, _case.shortname, _case.fullname, _case.description, _case.version, _case.apiKey, _case.badgeKey, None, None)

  def this(_case:Systems, _owner:Organizations) =
      this(_case.id, _case.shortname, _case.fullname, _case.description, _case.version, _case.apiKey, _case.badgeKey, Some(_owner), None)

  def toCaseObject:Systems = {
    if(owner.isDefined) {
      Systems(id, shortname, fullname, description, version, apiKey, badgeKey, owner.get.id)
    } else{
      Systems(id, shortname, fullname, description, version, apiKey, badgeKey, 0)
    }
  }
}