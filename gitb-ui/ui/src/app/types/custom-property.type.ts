import { FileData } from "./file-data.type";
import { Parameter } from "./parameter";

export interface CustomProperty extends Parameter {

    changeValue?: boolean
    showValue?: boolean
    file?: FileData

}
