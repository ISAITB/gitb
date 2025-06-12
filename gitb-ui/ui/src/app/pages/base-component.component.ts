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

    private addAlert(alert: Alert):void {
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

    protected isErrorDescription(obj: ErrorDescription|any): obj is ErrorDescription {
        return obj != undefined && ((obj as ErrorDescription).error_description != undefined || (obj as ErrorDescription).error_id != undefined)
    }

}
