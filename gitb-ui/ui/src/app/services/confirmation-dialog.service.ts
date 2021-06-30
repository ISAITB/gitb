import { EventEmitter, Injectable } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal'
import { Observable, ReplaySubject } from 'rxjs';
import { ConfirmationComponent } from '../modals/confirmation/confirmation.component'

@Injectable({
  providedIn: 'root'
})
export class ConfirmationDialogService {

  public sessionNotificationOpen = false

  constructor(private modalService: BsModalService) { }

  invalidSessionNotification(): Observable<boolean> {
    return new Observable<boolean>((observer) => {
      if (!this.sessionNotificationOpen) {
        this.sessionNotificationOpen = true
        this.notify("Invalid session", "Your current session is invalid. You will now return to the login screen to reconnect.", "Close").subscribe(() => {
          this.sessionNotificationOpen = false
          observer.next(true)
          observer.complete()
        })
      } else {
        observer.next(false)
        observer.complete()
      }
    })
  }

  notify(headerText: string, bodyText: string, buttonText: string) {
    const modal = this.modalService.show(ConfirmationComponent, {
      initialState: {
        headerText: headerText,
        bodyText: bodyText,
        actionButtonText: buttonText,
        oneButton: true
      },
      backdrop: 'static',
      keyboard: false
    })
    return modal.content!.result
  }

  confirm(headerText: string, bodyText: string, actionButtonText: string, closeButtonText: string, sameStyles?: boolean): EventEmitter<boolean> {
    const modal = this.modalService.show(ConfirmationComponent, {
      initialState: {
        headerText: headerText,
        bodyText: bodyText,
        actionButtonText: actionButtonText,
        closeButtonText: closeButtonText,
        sameStyles: sameStyles,
        oneButton: false
      }
    })
    return modal.content!.result
  }

  confirmed(headerText: string, bodyText: string, actionButtonText: string, closeButtonText: string, sameStyles?: boolean): Observable<void> {
    const result = new ReplaySubject<void>(1)
    this.confirm(headerText, bodyText, actionButtonText, closeButtonText, sameStyles).subscribe((choice: boolean) => {
      if (choice) {
        result.next()
        result.complete()
      }
    })
    return result
  }

  rejected(headerText: string, bodyText: string, actionButtonText: string, closeButtonText: string, sameStyles?: boolean): Observable<void> {
    const result = new ReplaySubject<void>(1)
    this.confirm(headerText, bodyText, actionButtonText, closeButtonText, sameStyles).subscribe((choice: boolean) => {
      if (!choice) {
        result.next()
        result.complete()
      }
    })
    return result
  }

}
