package models.automation

import models.TestCaseDeploymentAction

import scala.collection.mutable

case class TestSuiteDeployRequest(
   specification: Option[String],
   ignoreWarnings: Boolean,
   replaceTestHistory: Option[Boolean],
   updateSpecification: Option[Boolean],
   testCaseUpdates: mutable.HashMap[String, TestCaseDeploymentAction],
   sharedTestSuite: Boolean,
   showIdentifiers: Boolean
) {

  def withTestSuiteUpdateMetadata(replaceTestHistory: Boolean, updateSpecification: Boolean): TestSuiteDeployRequest = {
    copy(replaceTestHistory = Some(replaceTestHistory), updateSpecification = Some(updateSpecification))
  }

}