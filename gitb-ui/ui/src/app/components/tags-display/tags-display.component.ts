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
import { sortBy } from 'lodash';
import { TestCaseTag } from 'src/app/types/test-case-tag';

@Component({
    selector: 'app-tags-display',
    templateUrl: './tags-display.component.html',
    styleUrls: ['./tags-display.component.less'],
    standalone: false
})
export class TagsDisplayComponent implements OnInit {

  @Input() tags?: TestCaseTag[]
  @Input() editable? = false

  @Output() edit = new EventEmitter<number>()
  @Output() delete = new EventEmitter<number>()
  @Output() create = new EventEmitter<void>()

  constructor() { }

  ngOnInit(): void {
    if (this.tags) {
      this.tags = sortBy(this.tags, ['name'])
    }
  }

  tagEdited(id: number) {
    this.edit.emit(id)
  }

  tagDeleted(id: number) {
    this.delete.emit(id)
  }

  createTag() {
    this.create.emit()
  }
}
