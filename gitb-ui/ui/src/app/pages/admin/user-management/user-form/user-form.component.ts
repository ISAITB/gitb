import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { IdLabel } from 'src/app/types/id-label';
import { User } from 'src/app/types/user.type';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
  selector: 'app-user-form',
  templateUrl: './user-form.component.html',
  styles: [
  ]
})
export class UserFormComponent {

  @Input() user: Partial<User> = {}
  @Input() new = false
  @Input() admin = false
  @Input() roles: IdLabel[] = []
  @Input() sso = false
  @Input() showOrganisation = true
  @Input() showChangePassword = true
  @Input() roleReadonly = false
  @Input() validation?: ValidationState

  @Output() passwordChanged = new EventEmitter<boolean>()
  
  changePassword = false
  passwordFocus = new EventEmitter<boolean>()

  constructor(
    public dataService: DataService
  ) { }

  passwordExpanded() {
    this.passwordFocus.emit(true)
  }

  passwordCollapsed() {
    this.passwordFocus.emit(false)    
  }

  setPasswordClicked() {
    this.passwordChanged.emit(this.changePassword)
  }

}
