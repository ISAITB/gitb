import {SpecificationReferenceInfo} from 'src/app/types/specification-reference-info';
import {ConformanceTestCase} from './conformance-test-case';
import {ConformanceTestCaseGroup} from './conformance-test-case-group';

export interface ConformanceTestSuite extends SpecificationReferenceInfo {

  id: number;
  sname: string;
  description?: string;
  result: string;
  hasDocumentation: boolean;
  testCases: ConformanceTestCase[];
  testCaseGroups?: ConformanceTestCaseGroup[];

  expanded?: boolean;
  hasOptionalTestCases?: boolean;
  hasDisabledTestCases?: boolean;
  executionPending?: boolean;
  testCaseGroupMap?: Map<number, ConformanceTestCaseGroup>;

}
