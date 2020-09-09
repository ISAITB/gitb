package models

import models.Enums.TestSuiteReplacementChoice.TestSuiteReplacementChoice
import models.Enums.TestSuiteReplacementChoiceHistory.TestSuiteReplacementChoiceHistory
import models.Enums.TestSuiteReplacementChoiceMetadata.TestSuiteReplacementChoiceMetadata

class PendingTestSuiteAction(
  var specification: Long,
  var action: TestSuiteReplacementChoice,
  var actionHistory: Option[TestSuiteReplacementChoiceHistory],
  var actionMetadata: Option[TestSuiteReplacementChoiceMetadata]
) {}
