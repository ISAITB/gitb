/*
 * Copyright (C) 2026 European Union
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

import java.time.LocalDate

case class TestSessionSearchRequest(domains: Option[List[String]],
                                    groups: Option[List[String]],
                                    specifications: Option[List[String]],
                                    actors: Option[List[String]],
                                    testSuites: Option[List[String]],
                                    testCases: Option[List[String]],
                                    communities: Option[List[String]],
                                    organisations: Option[List[String]],
                                    systems: Option[List[String]],
                                    results: Option[List[String]],
                                    startTimeFrom: Option[LocalDate],
                                    startTimeTo: Option[LocalDate],
                                    withComment: Option[Boolean],
                                    withFlag: Option[Boolean],
                                    flags: Option[List[String]],
                                    active: Option[Boolean],
                                    offset: Int,
                                    limit: Int,
                                    includeTotal: Boolean)
