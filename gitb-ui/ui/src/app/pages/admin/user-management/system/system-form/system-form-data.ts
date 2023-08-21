import { System } from "src/app/types/system";

export interface SystemFormData extends System {

    otherSystems?: number
    copySystemParameters: boolean
    copyStatementParameters: boolean

}
