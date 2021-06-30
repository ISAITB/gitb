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
  @Output() passwordChanged = new EventEmitter<boolean>()
  changePassword = false

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
  }

  setPasswordClicked() {
    if (this.changePassword) {
      this.dataService.focus('password', 200)
    }
    this.passwordChanged.emit(this.changePassword)
  }

}
