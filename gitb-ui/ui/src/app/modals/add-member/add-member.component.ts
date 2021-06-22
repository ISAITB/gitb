import { AfterViewInit, Component, EventEmitter, OnInit } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { Constants } from 'src/app/common/constants';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { AccountService } from 'src/app/services/account.service';
import { AuthService } from 'src/app/services/auth.service';
import { DataService } from 'src/app/services/data.service';
import { PopupService } from 'src/app/services/popup.service';
import { User } from 'src/app/types/user.type';

@Component({
  selector: 'app-add-member',
  templateUrl: './add-member.component.html'
})
export class AddMemberComponent extends BaseComponent implements OnInit, AfterViewInit {

  udata: User = {}
  memberSpinner = false
  public $memberAdded = new EventEmitter<true>()

  constructor(
    public modalRef: BsModalRef,
    public dataService: DataService,
    private authService: AuthService,
    private accountService: AccountService,
    private popupService: PopupService
  ) { super() }

  ngAfterViewInit(): void {
    if (this.dataService.configuration.ssoEnabled) {
      this.dataService.focus('email')
    } else {
      this.dataService.focus('name')    
    }
  }

  ngOnInit(): void {
  }

  saveMemberDisabled() {
    let disabled = true
    if (this.dataService.configurationLoaded) {
      let isSSO = this.dataService.configuration.ssoEnabled
      if (isSSO) {
        disabled = !this.textProvided(this.udata.email)
      } else {
        disabled = !this.textProvided(this.udata.email) || 
          !this.textProvided(this.udata.name) ||
          !this.textProvided(this.udata.password) ||
          !this.textProvided(this.udata.passwordConfirmation)
      }
    }
    return disabled
  }

  // Check email availability
  checkEmail() {
    if (this.checkForm2()) {
      this.memberSpinner = true
      this.authService.checkEmailOfOrganisationMember(this.udata.email!)
      .subscribe((data) => {
        if (data.available) {
          this.addMember()
        } else {
          this.addAlertError("A user with email "+this.udata.email+" has already been registered.")
          this.udata.email = undefined
          this.udata.password = undefined
          this.udata.passwordConfirmation = undefined
        }
      })
      .add(() => {
        this.memberSpinner = false                        
      })
    } else {
      this.udata.password = undefined
      this.udata.passwordConfirmation = undefined
    }
  }

  addMember() {
    this.memberSpinner = true
    this.accountService.registerUser(this.udata.name!, this.udata.email!, this.udata.password!)
    .subscribe(() => {
      this.modalRef.hide()
      this.popupService.success("User created.")
      this.$memberAdded.emit(true)
    })
    .add(() => {
      this.memberSpinner = false
    })
  }

  close() {
    this.modalRef.hide()
  }

  checkForm2() {
    this.clearAlerts()
    let valid = true
    const emailRegex = Constants.EMAIL_REGEX
    const isSSO = this.dataService.configuration.ssoEnabled
    if (!isSSO && !this.requireText(this.udata.name, "You have to enter the user's name.")) {
      valid = false
    } else if (!this.requireText(this.udata.email, "You have to enter the user's email.")) {
      valid = false
    } else if (!this.requireValidEmail(this.udata.email, "You have to enter a valid email address.")) {
      valid = false
    } else if (!isSSO && (!this.requireText(this.udata.password, "You have to enter the user's password."))) {
      valid = false
    } else if (!isSSO && (!this.requireText(this.udata.passwordConfirmation, "You have to confirm the user's password."))) {
      valid = false
    } else if (!isSSO && (!this.requireSame(this.udata.password, this.udata.passwordConfirmation, "The provided passwords don't match."))) {
      valid = false
    }
    return valid
  }

  popup() {
    this.popupService.success('TEST')
  }
}
