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

package controllers.util

import config.Configurations
import exceptions.{ErrorCodes, InvalidRequestException}
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request}
import play.api.routing.Router

/**
 * Singleton object to carry out sanitization checks on received requests.
 * The sanitization configuration is driven through the "input-sanitizer.conf" configuration file.
 *
 * Input sanitization actually results in complete requests being blocked if bad content is received.
 * Sanitization is not considered a means of business validation of received values but rather a security
 * mechanism to limit XSS vulnerabilities (primarily stored but also reflected).
 *
 * This object checks all received request parameters (received via query string, body or as multipart form parts)
 * for which their values are checked against whitelist expressions, defaulting to a blacklist. This sanitization
 * does not by itself constitute a complete XSS defense as it eventually results in regular expression checks. It
 * complements the existing output encoding, HTML sanitization, and XSS-related HTTP headers (e.g. CSP).
 */
object InputSanitizer {

  private def logger = LoggerFactory.getLogger("InputSanitizer")

  def check[A](request: Request[A]): Unit = {
    if (Configurations.INPUT_SANITIZER__ENABLED && Configurations.INPUT_SANITIZER__METHODS_TO_CHECK.contains(request.method)) {
      // Define the target prefix (class plus action method) acting as the configuration root key for the parameters
      val target = requestTarget(request)
      // Check query string parameters
      request.queryString.foreach { entry =>
        entry._2.foreach { value =>
          failIfBadValue(target, entry._1, value)
        }
      }
      request.body match {
        case content: AnyContent =>
          // Check body content
          if (content.asFormUrlEncoded.isDefined) {
            content.asFormUrlEncoded.get.foreach { entry =>
              entry._2.foreach { value =>
                failIfBadValue(target, entry._1, value)
              }
            }
          }
        case multipart: play.api.mvc.MultipartFormData[_] =>
          // Process only the data parts
          multipart.asFormUrlEncoded.foreach { entry =>
            entry._2.foreach { value =>
              failIfBadValue(target, entry._1, value)
            }
          }
        case _ =>
          logger.warn("Unexpected body content type for path ["+request.path+"]")
      }
    }
  }

  private def requestTarget[A](request: Request[A]): String = {
    val controller = request.attrs(Router.Attrs.HandlerDef).controller
    val method = request.attrs(Router.Attrs.HandlerDef).method
    controller + "." + method
  }

  private def jsonPropertyNames(fullParameterKey: String, parameterValue: String): List[(String, String)] = {
    if (parameterValue == null) {
      List()
    } else {
      val nameValueMap: scala.collection.mutable.ListBuffer[(String, String)] = scala.collection.mutable.ListBuffer()
      collectJsonPropertyNames(Json.parse(parameterValue), fullParameterKey, nameValueMap)
      nameValueMap.toList
    }
  }

  private def collectJsonPropertyNames(json: JsValue, namePrefix: String, names: scala.collection.mutable.ListBuffer[(String, String)]): Unit = {
    if (json != null && json != JsNull) {
      json match {
        case array: JsArray =>
          array.value.foreach { item =>
            collectJsonPropertyNames(item, namePrefix, names)
          }
        case obj: JsObject =>
          obj.fields.foreach { field =>
            collectJsonPropertyNames(field._2, namePrefix + "." + field._1, names)
          }
        case num: JsNumber =>
          names += ((namePrefix, num.value.toString))
        case str: JsString =>
          names += ((namePrefix, str.value))
        case bool: JsBoolean =>
          names += ((namePrefix, bool.value.toString))
        case _ =>
          logger.warn("Unexpected type of JSON value for [" + namePrefix + "]")
      }
    }
  }

  private def failIfBadValue(target: String, parameterKey: String, parameterValue: String): Unit = {
    // The full parameter key to consider is postfixed with the parameter name
    val fullParameterKey = target + "." + parameterKey
    // Check whether the check should be skipped
    if (!Configurations.INPUT_SANITIZER__PARAMETERS_TO_SKIP.contains(fullParameterKey)) {
      var parametersToCheck: List[(String, String)] = null
      if (Configurations.INPUT_SANITIZER__PARAMETERS_AS_JSON.contains(fullParameterKey)) {
        if (logger.isDebugEnabled) {
          logger.debug("Treating as JSON ["+fullParameterKey+"]")
        }
        // We need to parse the value as JSON and consider each property separately
        parametersToCheck = jsonPropertyNames(fullParameterKey, parameterValue)
      } else {
        parametersToCheck = List((fullParameterKey, parameterValue))
      }
      parametersToCheck.foreach { parameterNameValue =>
        if (Configurations.INPUT_SANITIZER__PARAMETERS_TO_SKIP.contains(parameterNameValue._1) && StringUtils.isNotBlank(parameterNameValue._2)) {
          if (logger.isDebugEnabled) {
            logger.debug("Skipping ["+parameterNameValue._1+"]")
          }
        } else {
          if (Configurations.INPUT_SANITIZER__PARAMETER_WHITELIST_EXPRESSIONS.contains(parameterNameValue._1)) {
            if (logger.isDebugEnabled) {
              logger.debug("Whitelist check for ["+parameterNameValue._1+"]")
            }
            // A whitelist rule is defined for the parameter - value must match
            if (!Configurations.INPUT_SANITIZER__PARAMETER_WHITELIST_EXPRESSIONS(parameterNameValue._1).pattern.matcher(parameterNameValue._2).matches()) {
              logger.warn("Parameter [" + parameterNameValue._1 + "] failed whitelist validation against [" + Configurations.INPUT_SANITIZER__PARAMETER_WHITELIST_EXPRESSIONS(parameterNameValue._1).regex + "]")
              throwError("A provided value was blocked due to unexpected content ["+parameterKey+"].")
            }
          } else {
            if (logger.isDebugEnabled) {
              logger.debug("Blacklist check for ["+parameterNameValue._1+"]")
            }
            // The default blacklist applies - value must not match
            if (Configurations.INPUT_SANITIZER__DEFAULT_BLACKLIST_EXPRESSION.pattern.matcher(parameterNameValue._2).matches()) {
              logger.warn("Parameter ["+parameterNameValue._1+"] failed default blacklist validation")
              throwError("A provided value was blocked due to potentially harmful content ["+parameterKey+"].")
            }
          }
        }
      }
    }
  }

  private def throwError(message: String): Unit = {
    throw InvalidRequestException(ErrorCodes.INVALID_PARAM, message)
  }

}
