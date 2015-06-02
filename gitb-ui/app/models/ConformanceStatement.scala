package models

/**
 * Created by serbay on 11/3/14.
 */
case class TestResults(completed: Int, total: Int)
case class ConformanceStatement(actor:Actors, specification: Specifications, options: List[Options], results: TestResults)
