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

import { EventEmitter } from "@angular/core";
import { FileData } from "./file-data.type";
import { ValueLabel } from "./value-label";

export interface UserInteraction {

    type: "instruction"|"request"
    desc?: string
    with?: string
    id: string
    options?: string
    optionLabels?: string
    optionData?: ValueLabel[]
    data?: string
    selectedOption?: ValueLabel
    selectedOptions?: ValueLabel[]
    file?: FileData
    name?: string
    variableType?: string
    contentType?: string
    multiple?: boolean
    value?: string
    inputType: "TEXT"|"MULTILINE_TEXT"|"SECRET"|"CODE"|"SELECT_SINGLE"|"SELECT_MULTIPLE"|"UPLOAD"
    mimeType?: string
    forceDisplay?: boolean
    required?: boolean

    reset?: EventEmitter<void>

}
