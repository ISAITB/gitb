import { BreadcrumbItem } from "../components/breadcrumb/breadcrumb-item";
import { BreadcrumbType } from "./breadcrumb-type";

export interface BreadcrumbChange {

    type?: BreadcrumbType,
    id?: number|string,
    label?: string

    breadcrumbs? : BreadcrumbItem[]

}
