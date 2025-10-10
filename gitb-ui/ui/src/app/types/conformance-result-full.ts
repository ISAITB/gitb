/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
  optionPending?: boolean;
  showBadgeOptions?: boolean;

}
