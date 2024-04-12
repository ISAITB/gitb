package models.automation

import models.TestSuiteUploadResult

class TestSuiteUploadResultWithApiKeys extends TestSuiteUploadResult {

  var testSuiteIdentifier: Option[String] = None
  var testCaseIdentifiers: Option[List[String]] = None
  var specifications: Option[List[SpecificationActorApiKeys]] = None

}
