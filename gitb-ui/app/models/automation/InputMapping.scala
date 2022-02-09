package models.automation

import com.gitb.core.AnyContent

case class InputMapping(testSuite: List[String], testCase: List[String], input: AnyContent) {}
