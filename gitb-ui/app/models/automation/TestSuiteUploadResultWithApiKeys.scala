package models.automation

import com.gitb.tr.TAR
import models.TestSuiteUploadResult

object TestSuiteUploadResultWithApiKeys {

//  def confirm(): TestSuiteUploadResultWithApiKeys = {
//  }

//  def confirm(report: TAR): TestSuiteUploadResultWithApiKeys = {
//    TestSuiteUploadResultWithApiKeys(result = TestSuiteUploadResult(needsConfirmation = true, validationReport = Some(report)))
//  }

}

case class TestSuiteUploadResultWithApiKeys(testSuiteIdentifier: Option[String] = None,
                                            testCaseIdentifiers: Option[List[String]] = None,
                                            specifications: Option[List[SpecificationActorApiKeys]] = None,
                                            result: TestSuiteUploadResult)
