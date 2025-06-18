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

import com.gitb.tr.TestResultType

import java.sql.Timestamp

class ConformanceTestCase(
  var id: Long,
  var name: String,
  var description: Option[String],
  var version: Option[String],
  var sessionId: Option[String],
  var updateTime: Option[Timestamp],
  var outputMessage: Option[String],
  var hasDocumentation: Boolean,
  var optional: Boolean,
  var disabled: Boolean,
  var result: TestResultType,
  var tags: Option[String],
  var specReference: Option[String],
  var specDescription: Option[String],
  var specLink: Option[String],
  var group: Option[Long]
) {}
