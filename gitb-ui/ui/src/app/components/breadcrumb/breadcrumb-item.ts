import { BreadcrumbType } from "src/app/types/breadcrumb-type"

export interface BreadcrumbItem {

    action: () => any
    type: BreadcrumbType
    
    label?: string
    typeId?: number|string

}
