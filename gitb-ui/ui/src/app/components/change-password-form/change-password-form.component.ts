import { Component, EventEmitter, Input, OnInit } from '@angular/core';
import { PasswordChangeData } from './password-change-data.type';
import { ValidationState } from 'src/app/types/validation-state';

@Component({
    selector: '[app-change-password-form]',
    templateUrl: './change-password-form.component.html',
    standalone: false
})
export class ChangePasswordFormComponent implements OnInit {

  @Input() model!: PasswordChangeData
  @Input() autoFocus = false
  @Input() validation!: ValidationState
  @Input() padded = true
  @Input() focusChange?: EventEmitter<boolean>

  constructor() { }

  ngOnInit(): void {
  }

}
