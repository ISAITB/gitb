package models

/**
 * Created by simatosc.
 */
case class ConformanceResult(id: Long, sut: Long, spec: Long, actor: Long, testsuite: Long, testcase: Long, result: String, testsession: Option[String]) {
}
