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

package extensions

import play.api.mvc.{PathBindable, QueryStringBindable}

/**
 * Created by VWYNGAET on 10/11/2016.
 */
object Binders {

  implicit object ShortQueryBindable extends QueryStringBindable[Short] {
    def bind(key: String, params: Map[String, Seq[String]]) = params.get(key).flatMap(_.headOption).map { value =>
      try {
        Right(value.toShort)
      } catch {
        case e: Exception => Left("Cannot parse parameter " + key + " as Short")
      }
    }

    def unbind(key: String, value: Short) = key + "=" + value.toString
  }

  implicit object ShortPathBindable extends PathBindable[Short] {
    def bind(key: String, value: String) = try {
      Right(value.toShort)
    } catch {
      case e: Exception => Left("Cannot parse parameter '" + key + "' as Short")
    }

    def unbind(key: String, value: Short): String = value.toString
  }

}
