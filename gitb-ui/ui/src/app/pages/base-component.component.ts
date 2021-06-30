import { Constants } from "../common/constants"
import { Alert } from "../types/alert.type"
import { ErrorDescription } from "../types/error-description"

export abstract class BaseComponent {

    Constants = Constants
    alerts: Alert[] = []

    numberOrEmpty(value: any): boolean {
        return value == undefined || !isNaN(value)
    }

    textProvided(value: string|undefined): boolean {
        return value != undefined && value!.trim().length > 0
    }

    requireText(value: string|undefined, message?: string): boolean {
        let valid = true
        if (!this.textProvided(value)) {
            valid = false
            if (message !== undefined) {
              this.addAlertError(message)
            }
        }
        return valid
    }

    requireDefined(input: any, message?: string): boolean {
      let valid = true
      if (input === undefined || input === null) {
          valid = false
          if (message !== undefined) {
            this.addAlertError(message)
          }
      }
      return valid
    }

    requireSame(value1: any, value2: any, message?: string): boolean {
        let valid = true
        if (value1 !== value2) {
            valid = false
            if (message !== undefined) {
                this.addAlertError(message)
             }
         }
        return valid
    }

    requireDifferent(value1: any, value2: any, message?: string): boolean {
        let valid = true
        if (value1 === value2) {
            valid = false
            if (message !== undefined) {
                this.addAlertError(message)
             }
         }
        return valid
    }

    requireValidEmail(email: string|undefined, message?: string): boolean {
        let valid = true
        if (email === undefined || !Constants.EMAIL_REGEX.test(email)) {
            valid = false
            if (message !== undefined) {
               this.addAlertError(message)
            }
        }
        return valid
    }

    requireComplexPassword(password: string|undefined, message?: string): boolean {
        let valid = true
        if (password != undefined) {
            valid = Constants.PASSWORD_REGEX.test(password)
            if (!valid && message != undefined) {
                this.addAlertError(message)
            }
        }
        return valid
    }

    addAlertError(message: string = 'An error occurred'):void {
        this.addAlert({type:'danger', msg: message})
    }

    addAlert(alert: Alert):void {
        this.alerts.push(alert)
    }

    clearAlerts(): void {
        if (this.alerts.length > 0) {
            this.alerts = []
        }
    }

    protected isErrorDescription(obj: ErrorDescription|any): obj is ErrorDescription {
        return obj != undefined && ((obj as ErrorDescription).error_description != undefined || (obj as ErrorDescription).error_id != undefined)
    }
        
}
