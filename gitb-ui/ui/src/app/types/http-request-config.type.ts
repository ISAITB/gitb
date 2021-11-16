import { HttpHeaders } from "@angular/common/http";
import { Observable } from "rxjs";
import { FileParam } from "./file-param.type";

export interface HttpRequestConfig {

    path: string
    data?: {[key: string]: any}
    params?: {[key: string]: any}
    headers?: HttpHeaders
    authenticate?: boolean
    arrayBuffer?: boolean
    text?: boolean
    asJSON?: boolean
    files?: FileParam[]
    accept?: string
    httpResponse?: boolean
    errorHandler?: (_: any) => Observable<any>

}
