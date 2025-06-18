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

case class ConformanceCertificate(
   id:Long,
   title:Option[String],
   includeTitle:Boolean,
   includeMessage:Boolean,
   includeTestStatus:Boolean,
   includeTestCases:Boolean,
   includeDetails:Boolean,
   includeSignature:Boolean,
   includePageNumbers:Boolean,
   message: Option[String],
   community:Long
) {

  def withMessage(message: String): ConformanceCertificate = {
    ConformanceCertificate(id, title, includeTitle, includeMessage, includeTestStatus, includeTestCases, includeDetails, includeSignature, includePageNumbers, message = Some(message), community)
  }

  def toConformanceCertificateInfo(keystore: Option[CommunityKeystore]): ConformanceCertificateInfo = {
    ConformanceCertificateInfo(
      title, includeTitle, includeMessage, includeTestStatus, includeTestCases, includeItemDetails = false, includeDetails, includeSignature, includePageNumbers,
      message, keystore, community
    )
  }

}
