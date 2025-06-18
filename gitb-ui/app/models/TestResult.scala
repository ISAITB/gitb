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

import java.sql.Timestamp

/**
 * Show the result of conformance test for a specific test case for a vendor
 */
case class TestResult(
                       sessionId: String,
                       systemId:Option[Long],
                       system:Option[String],
                       organizationId:Option[Long],
                       organization:Option[String],
                       communityId:Option[Long],
                       community:Option[String],
                       testCaseId:Option[Long],
                       testCase:Option[String],
                       testSuiteId:Option[Long],
                       testSuite:Option[String],
                       actorId:Option[Long],
                       actor:Option[String],
                       specificationId:Option[Long],
                       specification:Option[String],
                       domainId:Option[Long],
                       domain:Option[String],
                       result:String,
                       startTime:Timestamp,
                       endTime:Option[Timestamp],
                       outputMessage: Option[String]
)