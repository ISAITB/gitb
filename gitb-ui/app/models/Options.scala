package models

case class Options(
											 id: Long,
                       sname: String,
                       fname: String,
                       description:Option[String],
                       actor:Long
                  )