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

import {EventEmitter, Injectable} from '@angular/core';
import {Observable, ReplaySubject} from 'rxjs';
import {ConfirmationComponent} from '../modals/confirmation/confirmation.component';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {fromPromise} from 'rxjs/internal/observable/innerFrom';

@Injectable({
  providedIn: 'root'
})
export class ConfirmationDialogService {


  constructor(private readonly modalService: NgbModal) { }

  notify(headerText: string, bodyText: string, buttonText: string, buttonIcon?: string): Observable<void> {
    const modal = this.modalService.open(ConfirmationComponent, {backdrop: 'static', keyboard: false});
    const modalInstance = modal.componentInstance as ConfirmationComponent
    modalInstance.headerText = headerText
    modalInstance.bodyText = bodyText
    modalInstance.actionButtonText = buttonText
    modalInstance.actionButtonIcon = buttonIcon
    modalInstance.oneButton = true
    return fromPromise(modal.result)
  }

  notified(headerText: string, bodyText: string, buttonText: string, buttonIcon?: string): Observable<void> {
    const result = new ReplaySubject<void>(1)
    this.notify(headerText, bodyText, buttonText, buttonIcon).subscribe(() => {
      result.next()
      result.complete()
    })
    return result
  }

  confirmDangerous(headerText: string, bodyText: string, actionButtonText: string, closeButtonText: string, actionButtonIcon?: string, closeButtonIcon?: string): EventEmitter<boolean> {
    return this.confirm(headerText, bodyText, actionButtonText, closeButtonText, actionButtonIcon, closeButtonIcon, false, true)
  }

  confirm(headerText: string, bodyText: string, actionButtonText: string, closeButtonText: string, actionButtonIcon?: string, closeButtonIcon?: string, sameStyles?: boolean, dangerous?: boolean): EventEmitter<boolean> {
    const emitter = new EventEmitter<boolean>()
    const modal = this.modalService.open(ConfirmationComponent)
    const modalInstance = modal.componentInstance as ConfirmationComponent
    modalInstance.headerText = headerText
    modalInstance.bodyText = bodyText
    modalInstance.actionButtonText = actionButtonText
    modalInstance.closeButtonText = closeButtonText
    modalInstance.actionButtonIcon = actionButtonIcon
    modalInstance.closeButtonIcon = closeButtonIcon
    modalInstance.sameStyles = sameStyles
    modalInstance.oneButton = false
    modalInstance.actionClass = dangerous?'btn btn-danger':'btn btn-secondary'
    modal.result.then((result: boolean) => {
        emitter.emit(result)
        emitter.complete?.()
      })
      .catch(() => {
        // dismiss → either ignore or emit a cancel
        emitter.emit(false);
        emitter.complete?.();
      });
    return emitter
  }

  confirmedDangerous(headerText: string, bodyText: string, actionButtonText: string, closeButtonText: string, actionButtonIcon?: string, closeButtonIcon?: string): Observable<void> {
    return this.confirmed(headerText, bodyText, actionButtonText, closeButtonText, actionButtonIcon, closeButtonIcon, false, true)
  }

  confirmed(headerText: string, bodyText: string, actionButtonText: string, closeButtonText: string, actionButtonIcon?: string, closeButtonIcon?: string, sameStyles?: boolean, dangerous?: boolean): Observable<void> {
    const result = new ReplaySubject<void>(1)
    this.confirm(headerText, bodyText, actionButtonText, closeButtonText, actionButtonIcon, closeButtonIcon, sameStyles, dangerous).subscribe((choice: boolean) => {
      if (choice) {
        result.next()
        result.complete()
      }
    })
    return result
  }

  rejected(headerText: string, bodyText: string, actionButtonText: string, closeButtonText: string, sameStyles?: boolean, actionButtonIcon?: string, closeButtonIcon?: string): Observable<void> {
    const result = new ReplaySubject<void>(1)
    this.confirm(headerText, bodyText, actionButtonText, closeButtonText, actionButtonIcon, closeButtonIcon, sameStyles, false).subscribe((choice: boolean) => {
      if (!choice) {
        result.next()
        result.complete()
      }
    })
    return result
  }

}
