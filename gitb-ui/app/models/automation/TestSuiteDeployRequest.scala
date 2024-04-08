package models.automation

import models.TestCaseDeploymentAction

import scala.collection.mutable

case class TestSuiteDeployRequest(
   specification: Option[String],
   ignoreWarnings: Boolean,
   replaceTestHistory: Option[Boolean],
   updateSpecification: Option[Boolean],
   testCaseUpdates: mutable.HashMap[String, TestCaseDeploymentAction],
   sharedTestSuite: Boolean
) {

  def withTestSuiteUpdateMetadata(replaceTestHistory: Boolean, updateSpecification: Boolean): TestSuiteDeployRequest = {
    TestSuiteDeployRequest(this.specification, this.ignoreWarnings, Some(replaceTestHistory), Some(updateSpecification), this.testCaseUpdates, this.sharedTestSuite)
  }

}