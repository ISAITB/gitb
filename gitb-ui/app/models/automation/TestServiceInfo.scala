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

package models.automation

import models.Enums.TestServiceApiType
import models.{DomainParameter, TestService}
import models.Enums.TestServiceApiType.TestServiceApiType
import models.Enums.TestServiceAuthTokenPasswordType.TestServiceAuthTokenPasswordType
import models.Enums.TestServiceType.TestServiceType

case class TestServiceInfo(parameterInfo: KeyValue, description: Option[Option[String]],
                           serviceType: Option[TestServiceType], apiType: Option[TestServiceApiType],
                           authBasicUsername: Option[Option[String]], authBasicPassword: Option[Option[String]],
                           authTokenUsername: Option[Option[String]], authTokenPassword: Option[Option[String]], authTokenPasswordType: Option[Option[TestServiceAuthTokenPasswordType]],
                           identifier: Option[Option[String]], version: Option[Option[String]], domainApiKey: Option[String], replaceExisting: Boolean) {

  def getParameter(parameterId: Option[Long], domainId: Long): DomainParameter = {
    DomainParameter(parameterId.getOrElse(0L), parameterInfo.key, description.flatten, "SIMPLE", parameterInfo.value, inTests = true, None, isTestService = true, domainId)
  }

  def getAsNewTestService(serviceId: Option[Long], parameterId: Long): TestService = {
    TestService(serviceId.getOrElse(0L), serviceType.get.id.toShort, apiType.getOrElse(TestServiceApiType.SoapApi).id.toShort, identifier.flatten, version.flatten, authBasicUsername.flatten, authBasicPassword.flatten, authTokenUsername.flatten, authTokenPassword.flatten, authTokenPasswordType.flatten.map(_.id.toShort), parameterId)
  }

}
