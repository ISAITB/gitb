import { BreadcrumbType } from "src/app/types/breadcrumb-type"

export interface BreadcrumbItem {

    type: BreadcrumbType
    action?: Function
    
    label?: string
    typeId?: number|string

}
