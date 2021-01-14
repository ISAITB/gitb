import { ParameterReference } from "src/app/types/parameter-reference";

export interface ParameterModalOptions {

    hideInExport?: boolean
    hideInRegistration?: boolean
    existingValues: ParameterReference[]
    notForTests?: boolean
    adminOnly?: boolean
    nameLabel?: string
    hasKey?: boolean
    modalTitle?: string
    reservedKeys?: string[]
    confirmMessage?: string

}
