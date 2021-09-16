package models

import java.sql.Timestamp

/**
 * Created by simatosc.
 */
case class ConformanceResult(id: Long, sut: Long, spec: Long, actor: Long, testsuite: Long, testcase: Long, result: String, outputMessage: Option[String], testsession: Option[String], updateTime: Option[Timestamp]) {
}
