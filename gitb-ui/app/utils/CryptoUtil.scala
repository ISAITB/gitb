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
