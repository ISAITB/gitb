/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import { HttpHeaders } from "@angular/common/http"
import { HttpRequestConfig } from "../types/http-request-config.type"

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

    public static webSocketURL(url: string) {
        const isSecure = window.location.href.toLowerCase().startsWith('https://')
        if (isSecure && url.startsWith('ws://')) {
            return 'wss'+url.substring(2)
        } else if (!isSecure && url.startsWith('wss://')) {
            return 'ws'+url.substring(3)
        } else {
            return url
        }
    }

}
