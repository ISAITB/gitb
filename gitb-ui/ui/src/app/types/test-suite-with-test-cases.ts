import {TestCase} from './test-case';
import {TestSuite} from './test-suite';
import {ConformanceTestCaseGroup} from '../pages/organisation/conformance-statement/conformance-test-case-group';

export interface TestSuiteWithTestCases extends TestSuite {

  testCases: TestCase[];
  testCaseGroups?: ConformanceTestCaseGroup[];

}
