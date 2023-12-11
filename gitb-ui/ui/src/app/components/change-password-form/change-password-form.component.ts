import { Component, Input, OnInit } from '@angular/core';
import { PasswordChangeData } from './password-change-data.type';

@Component({
  selector: '[app-change-password-form]',
  templateUrl: './change-password-form.component.html'
})
export class ChangePasswordFormComponent implements OnInit {

  @Input() model!: PasswordChangeData
  @Input() autoFocus = false

  constructor() { }

  ngOnInit(): void {
  }

}
