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
