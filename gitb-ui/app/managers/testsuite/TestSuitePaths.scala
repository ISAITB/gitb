package managers.testsuite

import java.io.File

case class TestSuitePaths(testSuiteFolder: File, testSuiteDefinitionPath: Option[String], testCasePaths: Map[String, String])
