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

import { Constants } from "../common/constants"
import { Alert } from "../types/alert.type"
import { ErrorDescription } from "../types/error-description"

export abstract class BaseComponent {

    Constants = Constants
    alerts: Alert[] = []

    numberOrEmpty(value: any): boolean {
        return value == undefined || !isNaN(value)
    }

    numberProvided(value: any, minimum?: number): boolean {
        return value != undefined && !isNaN(value) && (minimum == undefined || minimum <= value)
    }

    textProvided(value: string|undefined): boolean {
        return value != undefined && value!.trim().length > 0
    }

    isValidEmail(email: string|undefined): boolean {
        let valid = true
        if (email === undefined || !Constants.EMAIL_REGEX.test(email)) {
            valid = false
        }
        return valid
    }

    isValidRegularExpression(expression: string|undefined): boolean {
      let valid = this.textProvided(expression)
      if (valid && expression != undefined) {
        try {
          new RegExp(expression);
        } catch (e) {
          valid = false
        }
      }
      return valid
    }

    isDifferent(value1: any, value2: any): boolean {
        let valid = true
        if (value1 === value2) {
            valid = false
         }
        return valid
    }

    isValidUsername(username: string|undefined): boolean {
        return this.textProvided(username) && !/\s/g.test(username!.trim())
    }

    isValidAbsoluteHttpUrl(value: string|undefined): boolean {
      if (value == undefined || /\s/.test(value)) {
        return false
      } else {
        try {
          const u = new URL(value);
          const protocol = u.protocol.toLowerCase()
          return protocol === 'http:' || protocol === 'https:';
        } catch {
          return false;
        }
      }
    }

    isComplexPassword(password: string|undefined): boolean {
        let valid = true
        if (password != undefined) {
            return Constants.PASSWORD_REGEX.test(password)
        }
        return valid
    }

    addAlertInfo(message: string):void {
      this.addAlert({type:'info', msg: message})
    }

    addAlertError(message: string = 'An error occurred'):void {
        this.addAlert({type:'danger', msg: message})
    }

    addAlertWarning(message: string):void {
        this.addAlert({type:'warning', msg: message})
    }

    addAlertSuccess(message: string):void {
        this.addAlert({type:'success', msg: message})
    }

    addAlert(alert: Alert):void {
        this.alerts.push(alert)
    }

    clearAlerts(): void {
        if (this.alerts.length > 0) {
            this.alerts = []
        }
    }

    trimString(textToTrim: string|undefined): string|undefined {
      if (textToTrim != undefined) {
        return textToTrim.trim()
      } else {
        return undefined
      }
    }

  trimSearchString(searchString: string|undefined): string|undefined {
    if (searchString != undefined) {
      searchString = searchString.trim()
      if (searchString.length == 0) {
        searchString = undefined
      } else {
        searchString = searchString.toLocaleLowerCase()
      }
    }
    return searchString
  }

  protected isErrorDescription(obj: ErrorDescription|any): obj is ErrorDescription {
      return obj != undefined && ((obj as ErrorDescription).error_description != undefined || (obj as ErrorDescription).error_id != undefined)
  }

}
