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

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Constants } from 'src/app/common/constants';

@Component({
    selector: 'app-tag',
    templateUrl: './tag.component.html',
    styleUrls: ['./tag.component.less'],
    standalone: false
})
export class TagComponent implements OnInit {

  @Input() id?: number
  @Input() label?: string
  @Input() value?: string
  @Input() tooltipText?: string
  @Input() foreground?: string
  @Input() background?: string
  @Input() styleClass?: string
  @Input() editable? = false
  @Input() icon? = false
  @Input() pill? = false
  @Input() toggleEnabled?: boolean = false
  @Input() toggledByDefault?: boolean = false
  @Input() darkBorder? = false

  @Output() edit = new EventEmitter<number>()
  @Output() delete = new EventEmitter<number>()
  @Output() toggle = new EventEmitter<boolean>()

  Constants = Constants
  setDefaultBorder!: boolean
  toggled = false

  constructor() { }

  ngOnInit(): void {
    if (this.toggleEnabled) {
      this.toggled = this.toggledByDefault === true
    }
    this.setDefaultBorder = this.background == undefined
      || this.background!.toLowerCase() == '#fff'
      || this.background!.toLowerCase() == '#ffffff'
  }

  editTag() {
    this.edit.emit(this.id)
  }

  deleteTag() {
    this.delete.emit(this.id)
  }

  tagClicked(event: Event) {
    if (this.toggleEnabled) {
      event.stopPropagation()
      this.toggled = !this.toggled
      this.toggle.emit(this.toggled)
    }
  }

}
