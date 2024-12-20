import {Counters} from '../components/test-status-icons/counters';
import {ConformanceTestSuite} from '../pages/organisation/conformance-statement/conformance-test-suite';
import {ConformanceIds} from './conformance-ids';

export interface ConformanceResultFull extends ConformanceIds {

  communityId: number;
  communityName: string;
  organizationId: number;
  organizationName: string;
  systemName: string;
  domainName: string;
  specName: string;
  specGroupName?: string;
  specGroupOptionName: string;
  actorName: string;
  testSuiteName: string;
  testCaseName: string;
  testCaseDescription?: string;
  failed: number;
  completed: number;
  undefined: number;
  failedOptional: number;
  completedOptional: number;
  undefinedOptional: number;
  failedToConsider: number;
  completedToConsider: number;
  undefinedToConsider: number;
  result?: string;
  updateTime?: string;
  outputMessage?: string;

  testSuitesLoaded?: boolean;
  hasBadge?: boolean;
  testSuites?: ConformanceTestSuite[];
  counters?: Counters;
  copyBadgePending?: boolean;
  overallStatus?: string;
  exportPdfPending?: boolean;
  exportXmlPending?: boolean;

}
