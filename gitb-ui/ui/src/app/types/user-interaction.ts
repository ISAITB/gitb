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
