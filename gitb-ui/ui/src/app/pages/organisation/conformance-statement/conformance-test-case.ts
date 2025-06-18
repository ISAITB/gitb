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

import {SpecificationReferenceInfo} from 'src/app/types/specification-reference-info';
import {TestCaseTag} from 'src/app/types/test-case-tag';
import {UserInteraction} from 'src/app/types/user-interaction';

export interface ConformanceTestCase extends SpecificationReferenceInfo {

  id: number;
  sname: string;
  description?: string;
  outputMessage?: string;
  hasDocumentation: boolean;
  result: string;
  preliminary?: UserInteraction[];
  sessionId?: string;
  updateTime?: string;
  optional?: boolean;
  disabled?: boolean;
  tags?: string;
  group?: number;

  parsedTags?: TestCaseTag[];
  executionPending?: boolean;
  resultToShow?: string;
  groupTag?: TestCaseTag;
  groupFirst?: boolean;
  groupLast?: boolean;

}
