/*
 * Copyright (C) 2026 European Union
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

import {ConformanceSnapshot} from '../../types/conformance-snapshot';
import {ApiKeySystemInfo} from '../../types/api-key-system-info';
import {ApiKeyInfo} from '../../types/api-key-info';
import {LoadingStatus} from '../../types/loading-status.type';
import {ApiKeySpecificationInfo} from '../../types/api-key-specification-info';
import {ApiKeyActorInfo} from '../../types/api-key-actor-info';
import {ApiKeyTestSuiteInfo} from '../../types/api-key-test-suite-info';
import {ApiKeyTestCaseInfo} from '../../types/api-key-test-case-info';

export interface ApiKeyInfoState extends LoadingStatus {

  organisationName?: string
  organisationId: number
  communityId: number
  adminOrganisation: boolean
  apiInfo?: ApiKeyInfo
  conformanceSnapshots?: ConformanceSnapshot[]
  selectedSnapshot?: ConformanceSnapshot
  selectedSpecification?: ApiKeySpecificationInfo
  selectedActor?: ApiKeyActorInfo
  selectedTestSuite?: ApiKeyTestSuiteInfo
  selectedTestCase?: ApiKeyTestCaseInfo
  selectedSystem?: ApiKeySystemInfo

}
