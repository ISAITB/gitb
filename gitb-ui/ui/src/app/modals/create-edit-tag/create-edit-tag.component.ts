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

import { AfterViewInit, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { BaseComponent } from 'src/app/pages/base-component.component';
import { DataService } from 'src/app/services/data.service';
import { TestCaseTag } from 'src/app/types/test-case-tag';

@Component({
    selector: 'app-create-edit-tag',
    templateUrl: './create-edit-tag.component.html',
    standalone: false
})
export class CreateEditTagComponent extends BaseComponent implements OnInit, AfterViewInit {

  @Input() tag?: Partial<TestCaseTag>
  @Input() tagToUse!: Partial<TestCaseTag>
  @Output() createdTag = new EventEmitter<TestCaseTag>()
  @Output() updatedTag = new EventEmitter<TestCaseTag>()

  isUpdate!: boolean
  title!: string

  constructor(
    private readonly modalInstance: BsModalRef,
    private readonly dataService: DataService
  ) { super() }

  ngAfterViewInit(): void {
    this.dataService.focus('nameIdentifier', 200)
  }

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
    if (this.isUpdate) {
      this.updatedTag.emit(this.tagToUse! as TestCaseTag)
    } else {
      this.createdTag.emit(this.tagToUse! as TestCaseTag)
    }
    this.cancel();
  }

  cancel() {
    this.modalInstance.hide()
  }
}
