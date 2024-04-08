import { Component, EventEmitter } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { AuthService } from 'src/app/services/auth.service';
import { ConfirmationDialogService } from 'src/app/services/confirmation-dialog.service';
import { DataService } from 'src/app/services/data.service';

@Component({
  selector: 'app-disconnect-role',
  templateUrl: './disconnect-role.component.html'
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
