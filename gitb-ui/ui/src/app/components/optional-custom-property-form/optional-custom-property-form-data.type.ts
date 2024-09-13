import { CustomProperty } from "src/app/types/custom-property.type";

export interface OptionalCustomPropertyFormData {

    owner?: number
    propertyType: 'organisation'|'system'|'statement'
    properties: CustomProperty[]
    edit: boolean

}
