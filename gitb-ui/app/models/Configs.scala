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

case class Configs(system:Long,
                   parameter: Long,
                   endpoint:Long,
                   value: String,
                   contentType: Option[String]) {
  def withEndpoint(system:Long, parameter:Long, endpoint:Long) = {
    Configs(system, parameter, endpoint, this.value, this.contentType)
  }
  def withContentType(contentType: Option[String]) = {
    Configs(this.system, this.parameter, this.endpoint, this.value, contentType)
  }
}

class Config(_system: Long, _parameter: Long, _endpoint:Long, _value:String, _mimeType: Option[String], _extension: Option[String])
{
  var system:Long = _system
  var parameter:Long = _parameter
  var endpoint:Long = _endpoint
  var value:String = _value
  var mimeType:Option[String] = _mimeType
  var extension:Option[String] = _extension

  def this(_case:Configs) =
    this(_case.system, _case.parameter, _case.endpoint, _case.value, None, None)

  def this(_case:Configs, mimeType: String, extension: String) =
    this(_case.system, _case.parameter, _case.endpoint, _case.value, Some(mimeType), Some(extension))

  def toCaseObject:Configs = {
    Configs(system, parameter, endpoint, value, mimeType)
  }
}
