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

package exceptions

object ErrorCodes {

  //Account Service Related Errors
  val MISSING_PARAMS = 101
  val INVALID_PARAM = 102
  val INVALID_REQUEST = 103
  val INVALID_CREDENTIALS = 104
  val NOT_SUPPORTED_YET = 105
  val INVALID_ACTIVATION_CODE = 108
  val ACCOUNT_IS_SUSPENDED = 109
  val EMAIL_EXISTS:Int = 110
  val NO_SUCH_USER:Int = 111
  val CANNOT_UPDATE:Int = 112
  val CANNOT_DELETE:Int = 113
  val NAME_EXISTS:Int = 114
  val EMAIL_ATTACHMENT_COUNT_EXCEEDED:Int = 115
  val EMAIL_ATTACHMENT_SIZE_EXCEEDED:Int = 116
  val EMAIL_ATTACHMENT_TYPE_NOT_ALLOWED:Int = 117
  val VIRUS_FOUND:Int = 118
  val CONFORMANCE_STATEMENT_EXISTS:Int = 119
  val INVALID_CERTIFICATE:Int = 120
  val NO_SUT_TEST_CASES_FOR_ACTOR:Int = 121
  val DUPLICATE_SELFREG_TOKEN:Int = 122
  val DUPLICATE_ORGANISATION_TEMPLATE:Int = 123
  val INCORRECT_SELFREG_TOKEN:Int = 124
  val INVALID_SELFREG_DATA:Int = 125
  val SELF_REG_RESTRICTION_USER_EMAIL:Int = 126
  val SELF_REG_RESTRICTION_USER_EMAIL_DOMAIN:Int = 127

  //Auth Service Related Errors
  val INVALID_ACCESS_TOKEN = 201
  val INVALID_REFRESH_TOKEN = 202
  val INVALID_AUTHORIZATION_HEADER = 203
  val AUTHORIZATION_REQUIRED = 204
  val UNAUTHORIZED_ACCESS = 205

  //System Service Related Errors
  val SYSTEM_NOT_FOUND = 301

  //General Errors
  val GATEWAY_TIMEOUT = 401

  // Automation API errors
  val API_ORGANISATION_NOT_FOUND = 501
  val API_SYSTEM_NOT_FOUND = 502
  val API_COMMUNITY_DOES_NOT_ENABLE_API = 503
  val API_ACTOR_NOT_FOUND = 504
  val API_NO_TEST_CASES_TO_EXECUTE = 505
  val API_TEST_CASE_NOT_FOUND = 506
  val API_TEST_SUITE_NOT_FOUND = 507
  val API_TEST_CASE_INPUT_MAPPING_INVALID = 508
  val API_TEST_SUITE_INPUT_MAPPING_INVALID = 509
  val INPUT_WITHOUT_NAME = 510
  val INPUT_WITH_RESERVED_NAME = 511
  val INPUT_WITH_INVALID_NAME = 512
  val MISSING_REQUIRED_CONFIGURATION = 513
  val API_STATEMENT_EXISTS = 514
  val API_STATEMENT_DOES_NOT_EXIST = 515
  val API_SNAPSHOT_DOES_NOT_EXIST = 516
  val API_DOMAIN_NOT_FOUND = 517
  val API_SPECIFICATION_GROUP_NOT_FOUND = 518
  val API_SPECIFICATION_NOT_FOUND = 519
  val API_ACTOR_IDENTIFIER_EXISTS = 520
  val API_COMMUNITY_NOT_FOUND = 521
  val API_COMMUNITY_DOMAIN_CHANGE_NOT_ALLOWED = 522
  val API_INVALID_CONFIGURATION_PROPERTY_DEFINITION = 523
  val API_MULTIPLE_DOMAINS_FOUND = 524

  // Test suite errors
  val TEST_SUITE_EXISTS = 601
  val SHARED_TEST_SUITE_EXISTS = 602
  val SHARED_TEST_SUITE_LINKED_TO_NO_SPECIFICATIONS = 603
  val SHARED_TEST_SUITE_LINKED_TO_MULTIPLE_SPECIFICATIONS = 604

}