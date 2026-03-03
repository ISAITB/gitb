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

import {Component, Input, OnInit} from '@angular/core';
import {BaseComponent} from 'src/app/pages/base-component.component';
import {TagData} from 'src/app/types/tag-data';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';

@Component({
    selector: 'app-create-edit-tag',
    templateUrl: './create-edit-tag.component.html',
    standalone: false
})
export class CreateEditTagComponent extends BaseComponent implements OnInit {

  @Input() tag?: Partial<TagData>

  tagToUse!: Partial<TagData>
  isUpdate!: boolean
  title!: string

  constructor(
    private readonly modalInstance: NgbActiveModal
  ) { super() }

  ngOnInit(): void {
    this.isUpdate = this.tag != undefined
    if (this.isUpdate) {
      this.title = 'Update tag'
      this.tagToUse = {
        id: this.tag?.id,
        name: this.tag?.name,
        description: this.tag?.description,
        foreground: this.tag?.foreground,
        background: this.tag?.background
      }
    } else {
      this.title = 'Create tag'
      this.tagToUse = {}
    }
    if (this.tagToUse.background == undefined) {
      this.tagToUse.background = '#FFFFFF'
    }
    if (this.tagToUse.foreground == undefined) {
      this.tagToUse.foreground = '#777777'
    }
  }

  saveDisabled() {
    return !this.textProvided(this.tagToUse.name)
  }

  save() {
    if (!this.saveDisabled()) {
      this.modalInstance.close(this.tagToUse! as TagData)
    }
  }

  cancel() {
    this.modalInstance.dismiss()
  }
}
