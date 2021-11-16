import { CustomProperty } from "src/app/types/custom-property.type";

export interface OptionalCustomPropertyFormData {

    owner?: number
    propertyType: 'organisation'|'system'
    properties: CustomProperty[]
    edit: boolean

}
