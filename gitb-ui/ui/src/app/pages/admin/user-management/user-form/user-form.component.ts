import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { IdLabel } from 'src/app/types/id-label';
import { User } from 'src/app/types/user.type';

@Component({
  selector: 'app-user-form',
  templateUrl: './user-form.component.html',
  styles: [
  ]
})
export class UserFormComponent implements OnInit {

  @Input() user: Partial<User> = {}
  @Input() new = false
  @Input() admin = false
  @Input() roles: IdLabel[] = []
  @Input() sso = false
  @Input() showOrganisation = true
  @Input() showChangePassword = true
  @Input() roleReadonly = false
  @Output() passwordChanged = new EventEmitter<boolean>()
  changePassword = false
  passwordFocus = new EventEmitter<boolean>()

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
  }

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
