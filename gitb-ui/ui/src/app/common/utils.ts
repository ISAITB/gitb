import { HttpHeaders } from "@angular/common/http"
import { HttpRequestConfig } from "../types/http-request-config.type"
import { API_ROOT } from "./global"

export class Utils {

    public static objectToFormRequest(data: {[key:string]: any}|undefined): URLSearchParams {
        let body = new URLSearchParams()
        if (data !== undefined) {
            for (let key in data) {
                if (data[key] != undefined) {
                    body.set(key, data[key])
                }
            }
        }
        return body
    }

    public static createHttpHeaders(accessToken?: string, config?: HttpRequestConfig): HttpHeaders {
        let headersToUse = new HttpHeaders().set('X-Requested-With', 'XMLHttpRequest')
        if (accessToken) {
          headersToUse = headersToUse.set('Authorization', 'Bearer ' + accessToken)
        }
        if (config && config.files != undefined && config.files.length > 0) {
            headersToUse = headersToUse.set('enctype', 'multipart/form-data')
        } else {
            headersToUse = headersToUse.set('Content-Type', 'application/x-www-form-urlencoded')
        }
        if (config && config.accept != undefined) {
            headersToUse = headersToUse.set('Accept', config.accept)
        }
        return headersToUse
    }

    public static completePath(path: string): string {
        if (API_ROOT != '/') {
            return API_ROOT + path
          }
          return path
    }

}
