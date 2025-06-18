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

import { InvalidFormControlConfig } from "./invalid-form-control-config";
import { ErrorDescription } from "./error-description";
import { ReplaySubject } from "rxjs";

export class ValidationState {

    private emitterMap: Record<string|number, ReplaySubject<InvalidFormControlConfig>> = {}

    public set(identifier?: string|number): ReplaySubject<InvalidFormControlConfig> {
        let identifierToUse = identifier
        if (identifierToUse == undefined) {
            identifierToUse = '_'
        }
        const emitter = new ReplaySubject<InvalidFormControlConfig>()
        this.emitterMap[identifierToUse] = emitter
        return emitter
    }

    public get(identifier?: string|number): ReplaySubject<InvalidFormControlConfig> {
        let matchedEmitter: ReplaySubject<InvalidFormControlConfig>|undefined
        if (identifier) {
            matchedEmitter = this.emitterMap[identifier]
        } else {
            for (const id in this.emitterMap) {
                matchedEmitter = this.emitterMap[id]
                break
            }
        }
        if (!matchedEmitter) {
            matchedEmitter = this.set(identifier)
        }
        return matchedEmitter
    }

    public clear(identifier: string) {
        this.update(identifier, { invalid: false })
    }

    public invalid(identifier: string, feedback?: string) {
        this.update(identifier, { invalid: true, feedback: feedback })
    }

    public update(identifier: string, config: InvalidFormControlConfig) {
        if (this.emitterMap[identifier]) {
            this.emitterMap[identifier].next(config)
        } else {
            throw new Error(`Unable to send validation update for control '${identifier}' as it has not been initialised`)
        }
    }

    public clearErrors() {
        for (const identifier in this.emitterMap) {
            this.clear(identifier)
        }
    }

    public applyError(error: ErrorDescription): boolean {
        let allEmitted = false
        if (error.error_hint) {
            const hints = error.error_hint.split(",")
            let emitCount = 0
            hints.forEach((hint) => {
                const emitter = this.get(hint)
                if (emitter) {
                    emitCount += 1
                    emitter.next({ invalid: true, feedback: error.error_description })
                }
            })
            // Inform the calling component that all reported errors were matched and emitted to form controls
            allEmitted = emitCount == hints.length
        }
        return allEmitted
    }

    public apply(identifier: string, feedback: string): boolean {
      const emitter = this.get(identifier)
      if (emitter) {
        emitter.next({invalid: true, feedback: feedback})
        return true
      } else {
        return false
      }
    }

}
