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

import {Constants} from 'src/app/common/constants';

export interface ReportTypeInfo {
  type: number
  label: string
}

/**
 * The report types manageable both in the system administration report settings and the community
 * report settings, in display order. The label matches the corresponding community report settings panel title.
 */
export const REPORT_TYPE_INFOS: ReportTypeInfo[] = [
  { type: Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_CERTIFICATE, label: 'Conformance statement certificate' },
  { type: Constants.REPORT_TYPE.CONFORMANCE_OVERVIEW_CERTIFICATE, label: 'Conformance overview certificate' },
  { type: Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_REPORT, label: 'Conformance statement report' },
  { type: Constants.REPORT_TYPE.CONFORMANCE_OVERVIEW_REPORT, label: 'Conformance overview report' },
  { type: Constants.REPORT_TYPE.CONFORMANCE_STATEMENT_DOCUMENTATION_REPORT, label: 'Conformance statement documentation' },
  { type: Constants.REPORT_TYPE.TEST_CASE_REPORT, label: 'Test case report' },
  { type: Constants.REPORT_TYPE.TEST_STEP_REPORT, label: 'Test step report' }
]
