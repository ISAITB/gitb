import {TestCaseSearchCriteria} from '../../../types/test-case-search-criteria';
import {TestSuiteMinimalInfo} from '../../../types/test-suite-minimal-info';
import {TestCaseFilterOptions} from '../../../components/test-case-filter/test-case-filter-options';

export interface StatementTestCaseSearchCriteria extends TestCaseSearchCriteria {

  selectedTestSuite?: TestSuiteMinimalInfo
  testCaseFilterOptions?: TestCaseFilterOptions

}
