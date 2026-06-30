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

/** Describes how the test suite archive is supplied in an automation API deploy request. */
sealed trait TestSuiteArchiveSource

/** The archive is provided as a base64-encoded string. */
case class Base64ArchiveSource(base64: String) extends TestSuiteArchiveSource

/** The archive should be fetched from the given URI. */
case class UriArchiveSource(uri: String) extends TestSuiteArchiveSource
