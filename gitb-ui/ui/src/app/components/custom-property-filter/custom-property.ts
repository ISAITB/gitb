import { CustomProperty as BaseCustomProperty } from 'src/app/types/custom-property.type';

export interface CustomProperty extends Partial<BaseCustomProperty> {

    uuid?: number

}
