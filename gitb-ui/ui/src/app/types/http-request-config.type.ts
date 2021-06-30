import { HttpHeaders } from "@angular/common/http";
import { Observable } from "rxjs";

export interface HttpRequestConfig {

    path: string
    data?: {[key: string]: any}
    params?: {[key: string]: any}
    headers?: HttpHeaders
    authenticate?: boolean
    arrayBuffer?: boolean
    text?: boolean
    asJSON?: boolean
    file?: any
    errorHandler?: (_: any) => Observable<any>

}
