/*
 * Copyright (C) 2026 European Union
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

import { HttpHeaders, HttpResponse } from "@angular/common/http"
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

    public static uniqueValues<T>(array: T[]): T[] {
      return [...new Set(array)]
    }

    /**
     * Extracts the file name set by the backend on the "Content-Disposition" response header (e.g. for generated
     * report downloads), falling back to the provided default if the header is missing or unparseable.
     */
    public static fileNameFromContentDisposition(response: HttpResponse<any>, fallback: string): string {
        const contentDisposition = response.headers.get('Content-Disposition')
        if (contentDisposition != null) {
            const match = /filename="?([^";]+)"?/.exec(contentDisposition)
            if (match != null) {
                return match[1]
            }
        }
        return fallback
    }

    public static removeFromArray<T>(array: T[]|undefined, predicate: (item: T, index: number, array: T[]) => boolean): T[] {
      const removed: T[] = [];
      if (array != undefined) {
        for (let i = array.length - 1; i >= 0; i--) {
          if (predicate(array[i], i, array)) {
            removed.unshift(array[i]);
            array.splice(i, 1);
          }
        }
      }
      return removed;
    }

    /**
     * Determines whether a click on a navigation link (`<a [navTarget]>`) is a plain, same-tab
     * navigation as opposed to one that opens the link elsewhere (a modifier click, or a
     * non-primary mouse button). Used to guard side effects that should only apply to the current
     * tab (e.g. recording a "return to source" location, or clearing display state) - since a
     * modified click still fires the DOM "click" event even though the browser opens the link in
     * a new tab/window rather than navigating away from the current page.
     */
    public static isPlainNavigationClick(event: MouseEvent): boolean {
        return event.button === 0 && !event.ctrlKey && !event.metaKey && !event.shiftKey && !event.altKey
    }

}
