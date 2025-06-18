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

import { ActorInfo } from "../components/diagram/actor-info";
import { StepData } from "../components/diagram/step-data";
import { UserInteraction } from "./user-interaction";

export interface TestCaseDefinition {
    metadata: {
        authors?: string
        description?: string
        documentation?: {
            encoding?: string
            from?: string
            import?: string
            value?: string
        }
        lastModified?: string
        name: string
        published?: string
        version: string
        type: number
    }
    actors: {
        actor: ActorInfo[]
    }
    preliminary?: UserInteraction[]
    steps: StepData[]
    output?: string

}
