/*
 * Copyright (C) 2026 European Union
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

import {Component, ElementRef, EventEmitter, HostListener, Input, Output, TemplateRef, ViewChild} from '@angular/core';
import {Constants} from 'src/app/common/constants';
import {DataService} from 'src/app/services/data.service';
import {PopupService} from 'src/app/services/popup.service';

@Component({
    selector: 'app-api-key-text',
    templateUrl: './api-key-text.component.html',
    standalone: false
})
export class ApiKeyTextComponent {

  @Input() key?: string
  @Input() idName!: string
  @Input() name!: string
  @Input() label?: string
  @Input() inputWidth?: string
  @Input() createLabel = "Generate API key"
  @Input() supportEdit = false
  @Input() supportCreate = false
  @Input() supportUpdate = false
  @Input() supportDelete = false
  @Input() supportCopy = true
  @Input() createPending = false
  @Input() updatePending = false
  @Input() deletePending = false

  @Output() create = new EventEmitter<string|undefined>()
  @Output() update = new EventEmitter<string|undefined>()
  @Output() delete = new EventEmitter<string|undefined>()
  @Output() edit = new EventEmitter<string>()

  @ViewChild("tempKeyField") tempKeyField?: ElementRef;
  Constants = Constants
  keyTemp: string|undefined
  editing = false

  constructor(
    private readonly dataService: DataService,
    private readonly popupService: PopupService) { }

  copy() {
    this.dataService.copyToClipboard(this.key).subscribe(() => {
      this.popupService.success('Value copied to clipboard.')
    })
  }

  doCreate() {
    this.create.emit(this.key)
  }

  doDelete() {
    this.delete.emit(this.key)
  }

  doUpdate() {
    this.update.emit(this.key)
  }

  editStart() {
    this.keyTemp = this.key
    this.editing = true
    setTimeout(() => {
      this.tempKeyField?.nativeElement.focus()
    })
  }

  editConfirm() {
    this.key = this.keyTemp
    this.editing = false
    this.edit.emit(this.key!)
  }

  editCancel() {
    this.editing = false
  }

  @HostListener('keydown.escape', ['$event'])
  onEscape(event: KeyboardEvent) {
    if (this.editing) {
      event.preventDefault();
      this.editCancel();
    }
  }

  @HostListener('keydown.enter', ['$event'])
  onEnter(event: KeyboardEvent) {
    if (this.editing) {
      event.preventDefault();
      this.editConfirm();
    }
  }

}
