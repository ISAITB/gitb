package models

import models.Enums.TestCaseUploadMatchType.TestCaseUploadMatchType

class TestSuiteUploadTestCase(val identifier: String, val name: String, val matchType: TestCaseUploadMatchType) {}
