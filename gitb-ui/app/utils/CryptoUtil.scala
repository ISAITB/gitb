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

package utils

import com.password4j.Password
import config.Configurations

import java.util.UUID

object CryptoUtil {

  def generateApiKey(): String = {
    UUID.randomUUID().toString.toUpperCase.replace('-', 'X')
  }

  /**
   * Encrypts a string (i.e. a password) using a version of Blowfish block cipher
   * @param string String to be encrypted
   * @return
   */
  def encrypt(string:String):String = {
    Password.hash(string).withBcrypt().getResult
  }

  /**
   * Checks that an unencrypted string matches one that has previously been encrypted
   * @param string Unencrypted string to be checked
   * @param encrypted Encrypted string to be checked against unencrypted one
   * @return true if they match, false otherwise
   */
  def check(string:String, encrypted:String):Boolean = {
    Password.check(string, encrypted).withBcrypt()
  }

  /**
   * Check to see that the provided password is accepted in terms of complexity.
   * @return The check result.
   */
  def isAcceptedPassword(value: String): Boolean = {
    Configurations.PASSWORD_COMPLEXITY_RULE_REGEX.pattern.matcher(value).matches()
  }

  def checkPassword(clearPassword: String, hashedPassword: String): Boolean = {
    Password.check(clearPassword, hashedPassword).withBcrypt()
  }

  def hashPassword(clearPassword: String): String = {
    Password.hash(clearPassword).withBcrypt().getResult
  }

}
