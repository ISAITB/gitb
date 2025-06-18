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

import { Component, EventEmitter } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { AuthService } from 'src/app/services/auth.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';

@Component({
    selector: 'app-disconnect-role',
    templateUrl: './disconnect-role.component.html',
    standalone: false
})
export class DisconnectRoleComponent extends BaseComponent {

  disconnectPending = false
  choice = Constants.DISCONNECT_ROLE_OPTION.CURRENT_PARTIAL
  public result = new EventEmitter<number|undefined>()

  constructor(
    public dataService: DataService,
    private confirmationDialogService: ConfirmationDialogService,
    private authService: AuthService,
    public modalRef: BsModalRef
  ) { super() }

  disconnect() {
    let message: string|undefined
    if (this.choice == Constants.DISCONNECT_ROLE_OPTION.CURRENT_PARTIAL) {
      message = "This action will also end your current session. Are you sure you want to proceed?"
    } else {
      message = "This action will end your current session and cannot be undone. Are you sure you want to proceed?"
    }
    this.confirmationDialogService.confirmedDangerous("Confirmation", message, "End session", "Cancel")
    .subscribe(() => {
      this.disconnectPending = true
      this.authService.disconnectFunctionalAccount(this.choice).subscribe(() => {
        this.modalRef.hide()
        this.result.emit(this.choice)
      }).add(() => {
        this.disconnectPending = false
      })
    })
  }

  cancel() {
    this.modalRef.hide()
    this.result.emit()
  }

}
