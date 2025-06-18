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

import { StepReport } from "./report/step-report";

export interface StepData {

    id: string,
    report?: StepReport,
    collapsed?: boolean,
    status?: number,
    steps: StepData[],
    type: string,
    then?: StepData[],
    else?: StepData[],
    threads?: StepData[][],
    level?: number,
    from?: string,
    to?: string,
    with?: string,
    interactions?: StepData[],
    order?: number,
    fromIndex?: number,
    toIndex?: number,
    span?: number,
    title?: string,
    sequences?: StepData[],
    desc?: string,
    documentation?: string,
    currentIndex?: number,
    reply?: boolean
    admin?: boolean

}
