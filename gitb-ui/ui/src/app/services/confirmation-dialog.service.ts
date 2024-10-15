import { EventEmitter, Injectable } from '@angular/core';
import { BsModalService } from 'ngx-bootstrap/modal'
import { Observable, ReplaySubject } from 'rxjs';
import { ConfirmationComponent } from '../modals/confirmation/confirmation.component'

@Injectable({
  providedIn: 'root'
})
export class ConfirmationDialogService {


  constructor(private modalService: BsModalService) { }

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

  notified(headerText: string, bodyText: string, buttonText: string): Observable<void> {
    const result = new ReplaySubject<void>(1)
    this.notify(headerText, bodyText, buttonText).subscribe(() => {
      result.next()
      result.complete()
    })
    return result
  }

  confirmDangerous(headerText: string, bodyText: string, actionButtonText: string, closeButtonText: string): EventEmitter<boolean> {
    return this.confirm(headerText, bodyText, actionButtonText, closeButtonText, false, true)
  }

  confirm(headerText: string, bodyText: string, actionButtonText: string, closeButtonText: string, sameStyles?: boolean, dangerous?: boolean): EventEmitter<boolean> {
    const modal = this.modalService.show(ConfirmationComponent, {
      initialState: {
        headerText: headerText,
        bodyText: bodyText,
        actionButtonText: actionButtonText,
        closeButtonText: closeButtonText,
        sameStyles: sameStyles,
        oneButton: false,
        actionClass: dangerous?'btn btn-danger':'btn btn-secondary'
      }
    })
    return modal.content!.result
  }

  confirmedDangerous(headerText: string, bodyText: string, actionButtonText: string, closeButtonText: string): Observable<void> {
    return this.confirmed(headerText, bodyText, actionButtonText, closeButtonText, false, true)
  }

  confirmed(headerText: string, bodyText: string, actionButtonText: string, closeButtonText: string, sameStyles?: boolean, dangerous?: boolean): Observable<void> {
    const result = new ReplaySubject<void>(1)
    this.confirm(headerText, bodyText, actionButtonText, closeButtonText, sameStyles, dangerous).subscribe((choice: boolean) => {
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
