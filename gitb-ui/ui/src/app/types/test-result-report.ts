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

import { TestInteractionData } from "./test-interaction-data";
import { TestResult } from "./test-result";

export interface TestResultReport {

    result: TestResult,
    test: {
        id?: number
        sname?: string
    },
    organization: {
        id?: number,
        sname?: string,
        community?: number,
        parameters?: {[key: string]: string}
    },
    system: {
        id?: number,
        sname?: string,
        owner?: number
        parameters?: {[key: string]: string}
    },
    actor: {
        id: number,
        name: string,
        domain: number
    },
    specification: {
        id?: number,
        sname?: string,
        domain?: number
    },
    domain: {
        id?: number,
        sname?: string
    },
    testSuite: {
        id?: number,
        sname?: string,
        specification?: number
    },
    logs?: string[]
    interactions?: TestInteractionData[]

}
