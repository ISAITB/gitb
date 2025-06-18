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

  @Output() edit = new EventEmitter<number>()
  @Output() delete = new EventEmitter<number>()

  Constants = Constants
  setDefaultBorder!: boolean

  constructor() { }

  ngOnInit(): void {
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
}
