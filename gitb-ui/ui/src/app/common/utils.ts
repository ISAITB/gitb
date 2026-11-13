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
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {Constants} from './constants';
import {PropertyKind} from '../types/property-kind.type';

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

    public static dismissTooltip(pop?: NgbTooltip): void {
      if (pop) {
        pop.disableTooltip = true
        pop.close()
        setTimeout(() => {
          pop.disableTooltip = false
        }, Constants.TOOLTIP_DELAY + 50)
      }
    }

    /**
     * The display label for a property/parameter "kind" value, shared across the property definition
     * lists (community properties, endpoint parameters, trigger filters).
     */
    public static propertyKindLabel(kind: PropertyKind|string): string {
        switch (kind) {
            case 'SIMPLE': return 'Simple'
            case 'BINARY': return 'Binary'
            case 'SECRET': return 'Secret'
            case 'MULTILINE_TEXT': return 'Multiline text'
            case 'CODE': return 'Code'
            case 'RICH_TEXT': return 'Rich text'
            case 'RADIO_BUTTON': return 'Radio buttons'
            case 'CHECKBOX': return 'Checkboxes'
            default: return kind
        }
    }

    /**
     * Whether the kind behaves as a SIMPLE property: a plain string value that can define preset values, a default
     * value, a dependency, and inclusion in exports.
     */
    public static isSimpleLikeKind(kind: PropertyKind|string|undefined): boolean {
        return kind == 'SIMPLE' || kind == 'RADIO_BUTTON' || kind == 'CHECKBOX'
    }

    /**
     * Whether the property is a checkbox without preset values, i.e. a single checkbox the value of which is
     * "true" or "false".
     */
    public static isLoneCheckboxProperty(kind: PropertyKind|string|undefined, allowedValues: string|undefined): boolean {
        if (kind != 'CHECKBOX') {
            return false
        }
        if (allowedValues == undefined || allowedValues.trim().length == 0) {
            return true
        }
        try {
            const values = JSON.parse(allowedValues)
            return !Array.isArray(values) || values.length == 0
        } catch (e) {
            return true
        }
    }

    /**
     * The values recorded for a checkbox property (a comma-separated list).
     */
    public static checkboxPropertyValues(value: string|undefined): string[] {
        if (value == undefined) {
            return []
        }
        return value.split(',').map(item => item.trim()).filter(item => item.length > 0)
    }

    /**
     * Whether a rich text value has any actual content once markup and non-breaking spaces are stripped -
     * used to determine whether a required rich text input/property was answered (an editor left with only
     * empty tags should not count as provided).
     */
    public static hasRichTextContent(value: string|undefined): boolean {
        if (value == undefined) {
            return false
        }
        return value.replace(/<[^>]*>/g, '').replace(/&nbsp;/gi, ' ').trim().length > 0
    }

    /**
     * Applies a partial update onto a target object in place (mutated, not replaced), one key at a time, and
     * returns the keys whose value actually changed (arrays compared by content).
     */
    public static applyChangedKeys<T, K extends keyof T = keyof T>(target: T, update: Partial<T>): K[] {
        const changedKeys: K[] = []
        for (const key of Object.keys(update) as K[]) {
            const newValue = update[key] as T[K]
            const currentValue = target[key]
            const changed = Array.isArray(newValue) || Array.isArray(currentValue)
                ? JSON.stringify(newValue) != JSON.stringify(currentValue)
                : newValue != currentValue
            if (changed) {
                target[key] = newValue
                changedKeys.push(key)
            }
        }
        return changedKeys
    }

}
